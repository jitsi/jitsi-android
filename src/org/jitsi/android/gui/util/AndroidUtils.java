/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import java.util.*;

import org.jitsi.*;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.os.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.*;

/**
 * The <tt>AndroidUtils</tt> class provides a set of utility methods allowing
 * an easy way to show an alert dialog on android, show a general notification,
 * etc.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class AndroidUtils
{
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(AndroidUtils.class);

    /**
     * Shows an alert dialog for the given context and a title given by
     * <tt>titleId</tt> and message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param titleId the title identifier in the resources
     * @param messageId the message identifier in the resources
     */
    public static void showAlertDialog( Context context,
                                        final int titleId,
                                        final int messageId)
    {
        String title = context.getResources().getString(titleId);
        String msg = context.getResources().getString(messageId);
        showAlertDialog(context, title, msg);
    }

    /**
     * Shows an alert dialog for the given context and a title given by
     * <tt>titleId</tt> and message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param title the title identifier in the resources
     * @param message the message identifier in the resources
     * @param button the confirm button string identifier
     * @param listener the <tt>DialogInterface.DialogListener</tt> to attach to
     * the confirm button
     */
    public static void showAlertConfirmDialog(  Context context,
                                                final String title,
                                                final String message,
                                                final String button,
                                                final DialogActivity.DialogListener listener)
    {
        DialogActivity.showConfirmDialog(
                context,
                title,
                message,
                button,
                listener);
    }

    /**
     * Shows an alert dialog for the given context and a title given by
     * <tt>titleId</tt> and message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param title the title of the message
     * @param message the message
     */
    public static void showAlertDialog( final Context context,
                                        final String title,
                                        final String message)
    {
        DialogActivity.showDialog(context, title, message);
    }

    /**
     * Shows an alert dialog for the given context and a title given by
     * <tt>titleId</tt> and message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param title the title of the message
     * @param message the message
     * @param date the date on which the event corresponding to the notification
     * happened
     * @param resultActivityClass the result activity
     *
     * @return the identifier of this notification
     */
    public static int showGeneralNotification( Context context,
                                                String title,
                                                String message,
                                                long date,
                                                Class<?> resultActivityClass)
    {
        return updateGeneralNotification(   context,
                                            (int)(System.currentTimeMillis()
                                                    % Integer.MAX_VALUE),
                                            title,
                                            message,
                                            date,
                                            resultActivityClass);
    }

    /**
     * Clears the general notification.
     *
     * @param appContext the <tt>Context</tt> that will be used to create new
     * activity from notification <tt>Intent</tt>.
     */
    public static void clearGeneralNotification(Context appContext)
    {
        int id = OSGiService.getGeneralNotificationId();
        if(id < 0)
        {
            logger.warn("There's no global notification icon bound");
            return;
        }
        AndroidUtils.updateGeneralNotification(
                appContext,
                id,
                appContext.getString(R.string.app_name),
                "",
                System.currentTimeMillis(),
                JitsiApplication.getHomeScreenActivityClass());
    }

    /**
     * Shows an alert dialog for the given context and a title given by
     * <tt>titleId</tt> and message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param notificationID the identifier of the notification to update
     * @param title the title of the message
     * @param message the message
     * @param date the date on which the event corresponding to the notification
     * happened
     * @param resultActivityClass the result activity
     *
     * @return the identifier of this notification
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static int updateGeneralNotification(Context context,
                                                int notificationID,
                                                String title,
                                                String message,
                                                long date,
                                                Class<?> resultActivityClass)
    {
        Intent resultIntent = new Intent(context, resultActivityClass);

        return updateGeneralNotification(
                context, notificationID, title, message, date,
                resultActivityClass, resultIntent);
    }

    /**
     * Shows an alert dialog for the given context and a title given by
     * <tt>titleId</tt> and message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param notificationID the identifier of the notification to update
     * @param title the title of the message
     * @param message the message
     * @param date the date on which the event corresponding to the notification
     * happened
     * @param resultActivityClass the result activity
     *
     * @return the identifier of this notification
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static int updateGeneralNotification(Context context,
                                                int notificationID,
                                                String title,
                                                String message,
                                                long date,
                                                Class<?> resultActivityClass,
                                                Intent resultIntent)
    {
        Notification.Builder nBuilder
            = new Notification.Builder(context)
            .setContentTitle(title)
            .setContentText(message)
            .setWhen(date)
            .setSmallIcon(R.drawable.notificationicon);

        isActivityRunning(context, resultActivityClass);

        // The stack builder object will contain an artificial back stack for
        // the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(resultActivityClass);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                 stackBuilder.getPendingIntent(
                     0,
                     PendingIntent.FLAG_UPDATE_CURRENT
                 );
        nBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager
            = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        Notification notification = nBuilder.getNotification();

        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
                                & Notification.FLAG_FOREGROUND_SERVICE
                                & Notification.FLAG_NO_CLEAR;

        // mId allows you to update the notification later on.
        mNotificationManager.notify(notificationID, notification);

        return notificationID;
    }

    /**
     * Indicates if the service given by <tt>activityClass</tt> is currently
     * running.
     *
     * @param context the Android context
     * @param activityClass the activity class to check
     * @return <tt>true</tt> if the activity given by the class is running,
     * <tt>false</tt> - otherwise
     */
    public static boolean isActivityRunning( Context context,
                                            Class<?> activityClass)
    {
        ActivityManager activityManager
            = (ActivityManager) context
                .getSystemService (Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> services
            = activityManager.getRunningTasks(Integer.MAX_VALUE);

        boolean isServiceFound = false;

        for (int i = 0; i < services.size(); i++)
        {
            if (services.get(i).topActivity.getClassName()
                    .equals(activityClass.getName()))
            {
                isServiceFound = true;
            }
        } 
        return isServiceFound; 
    }
}