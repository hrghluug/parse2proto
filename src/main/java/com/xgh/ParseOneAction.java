package com.xgh;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageType;
import org.jetbrains.annotations.NotNull;

/**
 * @author xgh 2023/2/6
 */
public class ParseOneAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Notification notification = new NotificationGroup("test", NotificationDisplayType.BALLOON).createNotification("hello world", MessageType.ERROR);
        Notifications.Bus.notify(notification);
    }
}
