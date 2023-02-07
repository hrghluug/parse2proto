package com.xgh;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author xgh 2023/2/6
 */
public class ParseHelper {
    private static final Map<String, String> REFERENCE_MAP = new HashMap<>();
    private static final Map<String, String> PRIMITIVE_MAP = new HashMap<>();

    private static final String CLASS_PREFIX = "message";
    private static final String ENUM_PREFIX = "enum";
    private static final Pattern NULLABLE_PATTERN = Pattern.compile("^@(\\w+\\.)*Nullable$");
    private static final String GENERIC_TYPE = "google.protobuf.Any";

    static {
        REFERENCE_MAP.put(Byte.class.getName(), "google.protobuf.BytesValue");
        REFERENCE_MAP.put(Boolean.class.getName(), "google.protobuf.BoolValue");
        REFERENCE_MAP.put(Character.class.getName(), "google.protobuf.StringValue");
        REFERENCE_MAP.put(Short.class.getName(), "google.protobuf.Int32Value");
        REFERENCE_MAP.put(Integer.class.getName(), "google.protobuf.Int32Value");
        REFERENCE_MAP.put(Long.class.getName(), "google.protobuf.Int64Value");
        REFERENCE_MAP.put(Float.class.getName(), "google.protobuf.FloatValue");
        REFERENCE_MAP.put(Double.class.getName(), "google.protobuf.DoubleValue");
        REFERENCE_MAP.put(String.class.getName(), "google.protobuf.StringValue");
        REFERENCE_MAP.put(Date.class.getName(), "google.protobuf.Timestamp");
        REFERENCE_MAP.put(LocalDate.class.getName(), "google.protobuf.Timestamp");
        REFERENCE_MAP.put(LocalDateTime.class.getName(), "google.protobuf.Timestamp");
        REFERENCE_MAP.put(Object.class.getName(), "google.protobuf.Any");


        PRIMITIVE_MAP.put("int", "int32");
        PRIMITIVE_MAP.put("long", "int64");
        PRIMITIVE_MAP.put("double", "double");
        PRIMITIVE_MAP.put("short", "int32");
        PRIMITIVE_MAP.put("char", "string");
        PRIMITIVE_MAP.put("boolean", "bool");
        PRIMITIVE_MAP.put("float", "float");
        PRIMITIVE_MAP.put("byte", "bytes");

    }


    public static List<String> parse(PsiClass psiClass) {
        List<String> list = new ArrayList<>();
        List<PsiClass> classes = getAllClasses(psiClass);
        classes.forEach(
                clazz -> {
                    if (clazz.isEnum()) {
                        list.addAll(parseEnum(clazz, ""));
                    } else {
                        if (clazz.isInterface()) {
                            list.add("// interface 不能转换");
                        } else {
                            list.addAll(parseClass(clazz, ""));
                        }
                    }
                }
        );

        return list;
    }

    private static List<PsiClass> getAllClasses(PsiClass psiClass) {
        ArrayList<PsiClass> list = new ArrayList<>();
        list.add(psiClass);
        PsiClass[] innerClasses = psiClass.getInnerClasses();
        if (innerClasses.length > 0) {
            for (PsiClass innerClass : innerClasses) {
                list.addAll(getAllClasses(innerClass));
            }
        }
        return list;
    }

    private static List<String> parseClass(PsiClass psiClass, String indent) {
        List<String> list = new ArrayList<>();
        Set<String> genericTypes = Arrays.stream(psiClass.getTypeParameters()).map(PsiTypeParameter::getName).collect(Collectors.toSet());
        addDoc(psiClass, indent, list);
        list.add(indent + CLASS_PREFIX + " " + psiClass.getName() + " {");
        PsiField[] fields = psiClass.getFields();
        String fieldIndent = indent + "\t";
        int index = 1;
        for (int i = 0; i < fields.length; i++) {
            PsiField field = fields[i];
            if (field.hasModifier(JvmModifier.STATIC) || field.hasModifier(JvmModifier.FINAL)) {
                continue;
            }
            String type = getProtoType(field.getType(), genericTypes);
            if (type.contains("google.protobuf.StringValue")) {
                if (!hasNullableAnnotation(field)) {
                    type = type.replace("google.protobuf.StringValue", "string");
                }
            }
            addDoc(field, indent, list);
            list.add(fieldIndent + type + " " + field.getName() + " = " + index++ + ";");
        }
        list.add("}");
        return list;
    }

    private static String getProtoType(PsiType type, Set<String> genericType) {
        if (type instanceof PsiClassReferenceType) {
            PsiClassReferenceType pt = (PsiClassReferenceType) type;
            String className = pt.getReference().getQualifiedName();
            if (REFERENCE_MAP.containsKey(className)) {
                return REFERENCE_MAP.get(className);
            }
            Class jClass = isPresent(className);
            if (jClass != null) {
                if (Collection.class.isAssignableFrom(jClass)) {
                    if (pt.getParameters().length > 0) {
                        return "repeated " + getProtoType(pt.getParameters()[0], genericType);
                    } else {
                        return "repeated " + GENERIC_TYPE;
                    }
                }
                if (Map.class.isAssignableFrom(jClass)) {
                    if (pt.getParameters().length > 0) {
                        String keyProtoType = getProtoType(pt.getParameters()[0], genericType);
                        String valProtoType = getProtoType(pt.getParameters()[1], genericType);
                        return String.format("map<%s, %s>", keyProtoType, valProtoType);
                    } else {
                        return String.format("map<%s, %s>", GENERIC_TYPE, GENERIC_TYPE);
                    }
                }
            }
            if (genericType.contains(className)) {
                return GENERIC_TYPE;
            }

            return className.substring(className.lastIndexOf(".") + 1);
        } else if (type instanceof PsiPrimitiveType) {
            return PRIMITIVE_MAP.get(((PsiPrimitiveType) type).getName());
        } else if (type instanceof PsiArrayType) {
            return "repeated " + getProtoType(((PsiArrayType) type).getComponentType(), genericType);
        } else {
            throw new IllegalArgumentException("未知的类型:" + type.getCanonicalText());
        }
    }

    private static boolean hasNullableAnnotation(PsiField field) {
        PsiAnnotation[] annotations = field.getAnnotations();
        if (annotations.length > 0) {
            for (PsiAnnotation annotation : annotations) {
                String text = annotation.getText();
                if (NULLABLE_PATTERN.matcher(text).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Class isPresent(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }


    private static void addDoc(PsiJavaDocumentedElement element, String indent, List<String> list) {
        PsiDocComment comment = element.getDocComment();
        if (comment != null) {
            String commentText = comment.getText();
            String s = Arrays.stream(commentText.split("\n")).map(String::trim).map(str -> indent + str).collect(Collectors.joining("\n "+indent));
            list.add(s);
        }
    }


    private static List<String> parseEnum(PsiClass psiClass, String indent) {
        List<String> list = new ArrayList<>();
        addDoc(psiClass, indent, list);
        list.add(indent + ENUM_PREFIX + " " + psiClass.getName() + " {");
        PsiField[] fields = psiClass.getFields();
        for (int i = 0; i < fields.length; i++) {
            PsiField field = fields[0];
            list.add(indent + field.getName() + " = " + i);
        }
        list.add(indent + "}");
        return list;
    }
}
