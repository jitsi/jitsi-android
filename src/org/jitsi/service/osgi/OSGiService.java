/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.service.osgi;

import android.app.*;
import android.content.res.*;
import android.support.v4.app.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.impl.osgi.*;

import android.content.*;
import android.os.*;

/**
 * Implements an Android {@link Service} which (automatically) starts and stops
 * an OSGi framework (implementation).
 *
 * @author Lyubomir Marinov
 */
public class OSGiService
    extends Service
{
    /**
     * The ID of Jitsi notification icon
     */
    private static int GENERAL_NOTIFICATION_ID = R.string.app_name;

    /**
     * Indicates if the service has been started and general notification
     * icon is available
     */
    private static boolean serviceStarted;

    /**
     * The very implementation of this Android <tt>Service</tt> which is split
     * out of the class <tt>OSGiService</tt> so that the class
     * <tt>OSGiService</tt> may remain in a <tt>service</tt> package and be
     * treated as public from the Android point of view and the class
     * <tt>OSGiServiceImpl</tt> may reside in an <tt>impl</tt> package and be
     * recognized as internal from the Jitsi point of view.
     */
    private final OSGiServiceImpl impl;

    /**
     * Initializes a new <tt>OSGiService</tt> implementation.
     */
    public OSGiService()
    {
        impl = new OSGiServiceImpl(this);
    }

    public IBinder onBind(Intent intent)
    {
        return impl.onBind(intent);
    }

    @Override
    public void onCreate()
    {
        impl.onCreate();
    }

    @Override
    public void onDestroy()
    {
        impl.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        int result = impl.onStartCommand(intent, flags, startId);

        startForegroundService();

        return result;
    }

    /**
     * Start the service in foreground and creates shows general notification
     * icon.
     */
    private void startForegroundService()
    {
        //The intent to launch when the user clicks the expanded notification
        Intent intent
            = new Intent(this, JitsiApplication.getHomeScreenActivityClass());
        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent =
                PendingIntent.getActivity(this, 0, intent, 0);

        Resources res = getResources();
        String title = res.getString(R.string.app_name);

        NotificationCompat.Builder nBuilder
                = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.notificationicon);
        nBuilder.setContentIntent(pendIntent);

        Notification notice = nBuilder.build();
        notice.flags |= Notification.FLAG_NO_CLEAR;

        this.startForeground(GENERAL_NOTIFICATION_ID, notice);
        
        serviceStarted = true;
    }

    /**
     * Stops the foreground service and hides general notification icon
     */
    public void stopForegroundService()
    {
        serviceStarted = false;
        stopForeground(true);
    }

    /**
     * Returns general notification ID that can be used to post notification
     * bound to our global icon
     * 
     * @return the notification ID greater than 0 or -1 if service is not 
     *  running
     */
    public static int getGeneralNotificationId()
    {
        if(serviceStarted)
        {
            return GENERAL_NOTIFICATION_ID;
        }
        return -1;
    }
    
}
