package com.xgh;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xgh 2023/2/6
 */
public class ParseOneAction extends AnAction {

    public static final NotificationGroup NG = new NotificationGroup(ParseOneAction.class.getSimpleName(), NotificationDisplayType.BALLOON);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        PsiClass psiClass = PsiTreeUtil.getChildOfType(psiFile, PsiClass.class);
        Notification notification = null;
        try {
            List<String> parse = ParseHelper.parse(psiClass);
            copyToClipboard(parse);
            notification = NG.createNotification("生成成功,已复制到剪切版", MessageType.INFO);
        } catch (Exception e) {
            notification = NG.createNotification("生成异常:" + e.getMessage(), MessageType.ERROR);
        } finally {
            Notifications.Bus.notify(notification);
        }
    }

    public void copyToClipboard(String content) {
        CopyPasteManager.getInstance().setContents(new StringSelection(content));
    }

    public void copyToClipboard(List<String> list) {
        copyToClipboard(list.stream().collect(Collectors.joining("\n")));
    }
}
