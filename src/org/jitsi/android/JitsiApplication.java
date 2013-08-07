/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android;

import android.app.*;
import android.content.*;
import android.content.res.*;

import android.hardware.*;
import android.media.*;
import android.os.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.android.gui.*;
import org.jitsi.android.gui.LauncherActivity;
import org.jitsi.android.gui.account.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

/**
 * <tt>JitsiApplication</tt> is used, as a global context and utility class for
 * global actions(like EXIT broadcast).
 *
 * @author Pawel Domas
 */
public class JitsiApplication
    extends Application
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(JitsiApplication.class);

    /**
     * The EXIT action name that is broadcasted to all OSGiActivities
     */
    public static final String ACTION_EXIT = "org.jitsi.android.exit";

    /**
     * Static instance holder.
     */
    private static JitsiApplication instance;

    /**
     * The currently shown activity.
     */
    private static Activity currentActivity = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate()
    {
        super.onCreate();

        instance = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTerminate()
    {
        instance = null;

        super.onTerminate();
    }

    /**
     * Shutdowns the app by stopping <tt>OSGiService</tt> and broadcasting
     * {@link #ACTION_EXIT}.
     *
     */
    public static void shutdownApplication()
    {
        instance.doShutdownApplication();
    }

    /**
     * Shutdowns the OSGI service and sends the EXIT action broadcast.
     */
    private void doShutdownApplication()
    {

        // Shutdown the OSGi service
        stopService(new Intent(this, OSGiService.class));
        // Broadcast the exit action
        Intent exitIntent = new Intent();
        exitIntent.setAction(ACTION_EXIT);
        sendBroadcast(exitIntent);
    }

    /**
     * Retrieves <tt>AudioManager</tt> instance using application context.
     *
     * @return <tt>AudioManager</tt> service instance.
     */
    public static AudioManager getAudioManager()
    {
        return (AudioManager) getGlobalContext()
                .getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Retrieves <tt>PowerManager</tt> instance using application context.
     *
     * @return <tt>PowerManager</tt> service instance.
     */
    public static PowerManager getPowerManager()
    {
        return (PowerManager) getGlobalContext()
                .getSystemService(Context.POWER_SERVICE);
    }

    /**
     * Retrieves <tt>SensorManager</tt> instance using application context.
     *
     * @return <tt>SensorManager</tt> service instance.
     */
    public static SensorManager getSensorManager()
    {
        return (SensorManager) getGlobalContext()
                .getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Retrieves <tt>NotificationManager</tt> instance using application
     * context.
     *
     * @return <tt>NotificationManager</tt> service instance.
     */
    public static NotificationManager getNotificationManager()
    {
        return (NotificationManager) getGlobalContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Returns global application context.
     *
     * @return Returns global application <tt>Context</tt>.
     */
    public static Context getGlobalContext()
    {
        return instance.getApplicationContext();
    }

    /**
     * Returns application <tt>Resources</tt> object.
     * @return application <tt>Resources</tt> object.
     */
    public static Resources getAppResources()
    {
        return instance.getResources();
    }

    /**
     * Returns Android string resource for given <tt>id</tt>.
     * @param id the string identifier.
     * @return Android string resource for given <tt>id</tt>.
     */
    public static String getResString(int id)
    {
        return getAppResources().getString(id);
    }

    /**
     * Returns Android string resource for given <tt>id</tt> and format
     * arguments that will be used for substitution.
     * @param id the string identifier.
     * @param arg the format arguments that will be used for substitution.
     * @return Android string resource for given <tt>id</tt> and format
     *         arguments.
     */
    public static String getResString(int id, Object ... arg)
    {
        return getAppResources().getString(id, arg);
    }

    /**
     * Returns home <tt>Activity</tt> class.
     * @return Returns home <tt>Activity</tt> class.
     */
    public static Class<?> getHomeScreenActivityClass()
    {
        BundleContext osgiContext = AndroidGUIActivator.bundleContext;
        if(osgiContext == null)
        {
            // If OSGI has not started show splash screen as home
            return LauncherActivity.class;
        }

        AccountManager accountManager
                = ServiceUtils.getService(osgiContext, AccountManager.class);

        // If account manager is null it means that OSGI has not started yet
        if(accountManager == null)
            return LauncherActivity.class;

        final int accountCount = accountManager.getStoredAccounts().size();

        if (accountCount == 0)
        {
            // Start new account Activity
            return AccountLoginActivity.class;
        }
        else
        {
            // Start main view
            return Jitsi.class;
        }
    }

    /**
     * Creates the home <tt>Activity</tt> <tt>Intent</tt>.
     * @return the home <tt>Activity</tt> <tt>Intent</tt>.
     */
    public static Intent getHomeIntent()
    {
        Intent homeIntent = new Intent(instance, getHomeScreenActivityClass());
        // Home is singleTask anyway, but this way it can be started from
        // non Activity context.
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return homeIntent;
    }

    /**
     * Creates home activity pending <tt>Intent</tt>.
     * @return new home activity pending <tt>Intent</tt>.
     */
    public static PendingIntent getHomePendingIntent()
    {
        return PendingIntent.getActivity(
                getGlobalContext(), 0,
                getHomeIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Sets the current activity.
     * 
     * @param a the current activity to set
     */
    public static void setCurrentActivity(Activity a)
    {
        logger.info("Current activity set to "+a);
        currentActivity = a;
    }

    /**
     * Returns the current activity.
     *
     * @return the current activity
     */
    public static Activity getCurrentActivity()
    {
        return currentActivity;
    }

    /**
     * Checks if current <tt>Activity</tt> is the home one.
     * @return <tt>true</tt> if the home <tt>Activity</tt> is currently active.
     */
    public static boolean isHomeActivityActive()
    {
        if(currentActivity == null)
            return false;

        return currentActivity.getClass().equals(getHomeScreenActivityClass());
    }
}
