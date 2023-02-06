package com.xgh;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author xgh 2023/2/6
 */
public class ParseHelper {
    private static final Map<String, String> BASIC_TYPE_MAPPING = new HashMap<>();

    private static final String CLASS_PREFIX = "message";
    private static final String ENUM_PREFIX = "enum";
    private static final String STRING_WRAPPER = "google.protobuf.StringValue";
    private static final String OBJECT_WRAPPER = "google.protobuf.Any";
    private static final String STRING_NAME = String.class.getName();
    private static final Pattern NULLABLE_PATTERN = Pattern.compile("^@(\\w+\\.)*Nullable$");

    static {
        BASIC_TYPE_MAPPING.put("int", "int32");
        BASIC_TYPE_MAPPING.put(Integer.class.getName(), "google.protobuf.Int32Value");
        BASIC_TYPE_MAPPING.put("long", "int64");
        BASIC_TYPE_MAPPING.put(Long.class.getName(), "google.protobuf.Int64Value");
        BASIC_TYPE_MAPPING.put("double", "double");
        BASIC_TYPE_MAPPING.put(Double.class.getName(), "google.protobuf.DoubleValue");
        BASIC_TYPE_MAPPING.put("short", "int32");
        BASIC_TYPE_MAPPING.put(Short.class.getName(), "google.protobuf.Int32Value");
        BASIC_TYPE_MAPPING.put("char", "string");
        BASIC_TYPE_MAPPING.put(Character.class.getName(), "google.protobuf.StringValue");
        BASIC_TYPE_MAPPING.put("boolean", "bool");
        BASIC_TYPE_MAPPING.put(Boolean.class.getName(), "google.protobuf.BoolValue");
        BASIC_TYPE_MAPPING.put("float", "float");
        BASIC_TYPE_MAPPING.put(Float.class.getName(), "google.protobuf.FloatValue");
        BASIC_TYPE_MAPPING.put("byte", "bytes");
        BASIC_TYPE_MAPPING.put(Byte.class.getName(), "google.protobuf.BytesValue");
        BASIC_TYPE_MAPPING.put(STRING_NAME, "string");
        BASIC_TYPE_MAPPING.put(Date.class.getName(), "google.protobuf.Timestamp");
        BASIC_TYPE_MAPPING.put(LocalDate.class.getName(), "google.protobuf.Timestamp");
        BASIC_TYPE_MAPPING.put(LocalDateTime.class.getName(), "google.protobuf.Timestamp");
    }


    public static List<List<String>> parse(PsiClass psiClass, boolean asBasic) {
        List<List<String>> list = new ArrayList<>();
        if (psiClass.isEnum()) {
            list.add(parseEnum(psiClass, ""));
        } else {
            if (psiClass.isInterface()) {
                list.add(Collections.singletonList("// interface 不能转换"));
            } else {
                list.add(parseClass(psiClass, "", asBasic));
            }
        }
        return list;
    }

    private static List<String> parseClass(PsiClass psiClass, String indent, boolean asBasic) {
        List<String> list = new ArrayList<>();
        PsiDocComment classComment = psiClass.getDocComment();
        if (classComment != null) {
            list.add(classComment.getText());
        }
        list.add(indent + CLASS_PREFIX + " " + psiClass.getName() + " {");
        PsiField[] fields = psiClass.getFields();
        String fieldIndent = indent + "\t";
        int index = 0;
        for (int i = 0; i < fields.length; i++) {
            PsiField field = fields[i];
            if (field.hasModifier(JvmModifier.STATIC) || field.hasModifier(JvmModifier.FINAL)) {
                continue;
            }
            if (asBasic || BASIC_TYPE_MAPPING.containsKey(field.getType().getCanonicalText())) {
                list.add(String.format(copeFieldAsBasicType(field, fieldIndent), index++));
            } else {
                list.addAll(copeObjectTypeField(field, fieldIndent));
            }
        }
        list.add("}");
        return list;
    }

    private static String copeFieldAsBasicType(PsiField field, String indent) {
        String type = "";
        String typeName = field.getType().getCanonicalText();
        if (STRING_NAME.equals(typeName) && hasNullableAnnotation(field)) {
            type = STRING_WRAPPER;
        } else if (BASIC_TYPE_MAPPING.containsKey(typeName)) {
            type = BASIC_TYPE_MAPPING.get(typeName);
        } else if (Object.class.getName().equals(typeName)) {
            type = OBJECT_WRAPPER;
        } else if (field.getType() instanceof PsiClassReferenceType){
            PsiJavaCodeReferenceElement reference = ((PsiClassReferenceType) field.getType()).getReference();
            String className = reference.getQualifiedName();
            type = field.getType().getPresentableText();
        }
        return indent + type + " " + field.getName() + " = %s";
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

    private static List<String> copeObjectTypeField(PsiField psiField, String indent) {


        return null;
    }


    private static List<String> parseEnum(PsiClass psiClass, String indent) {
        List<String> list = new ArrayList<>();
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
