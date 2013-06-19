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
import org.jitsi.android.gui.*;
import org.jitsi.service.osgi.*;

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
     * The EXIT action name that is broadcasted to all OSGiActivities
     */
    public static final String ACTION_EXIT = "org.jitsi.android.exit";

    /**
     * The home activity class.
     */
    private static final Class<?> HOME_SCREEN_CLASS = Jitsi.class;

    /**
     * Static instance holder.
     */
    private static JitsiApplication instance;

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
     * Returns home <tt>Activity</tt> class.
     * @return Returns home <tt>Activity</tt> class.
     */
    public static Class<?> getHomeScreenActivityClass()
    {
        return HOME_SCREEN_CLASS;
    }

    /**
     * Creates the home <tt>Activity</tt> <tt>Intent</tt>.
     * @return the home <tt>Activity</tt> <tt>Intent</tt>.
     */
    public static Intent getHomeIntent()
    {
        Intent homeIntent = new Intent(instance, HOME_SCREEN_CLASS);
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
}
