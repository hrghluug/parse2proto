package com.xgh;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author xgh 2023/2/6
 */
public class ParseOneAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        PsiClass psiClass = PsiTreeUtil.getChildOfType(psiFile, PsiClass.class);

        List<List<String>> parse = ParseHelper.parse(psiClass, true);
        parse.get(0).forEach(System.out::println);
        System.out.println(psiClass);
        Notification notification = new NotificationGroup("test", NotificationDisplayType.BALLOON).createNotification("hello world", MessageType.ERROR);
        Notifications.Bus.notify(notification);
    }
}
