/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
    package net.java.sip.communicator.impl.sysactivity;

import android.content.*;
import org.jitsi.android.*;
import org.jitsi.service.osgi.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;

/**
 * Listens for broadcasts from ConnectivityManager to get notified
 * for network changes.
 *
 * @author Damian Minkov
 */
public class ConnectivityManagerListenerImpl
    extends BroadcastReceiver
    implements SystemActivityManager
{
    /**
     * The action name we will receive broadcasts for to get informed
     * for connectivity changes.
     */
    private static final String CONNECTIVITY_CHANGE_ACTION =
        "android.net.conn.CONNECTIVITY_CHANGE";

    /**
     * The only instance of this impl.
     */
    private static ConnectivityManagerListenerImpl connectivityManagerListenerImpl;

    /**
     * Whether we are working.
     */
    private boolean connected = false;

    /**
     * Gets the instance of <tt>ConnectivityManagerListenerImpl</tt>.
     * @return the ConnectivityManagerListenerImpl.
     */
    public static ConnectivityManagerListenerImpl getInstance()
    {
        if(connectivityManagerListenerImpl == null)
            connectivityManagerListenerImpl =
                new ConnectivityManagerListenerImpl();

        return connectivityManagerListenerImpl;
    }

    /**
     * Starts
     */
    public void start()
    {
        Context context = JitsiApplication.getGlobalContext();
        context.registerReceiver(this,
            new IntentFilter(CONNECTIVITY_CHANGE_ACTION));

        connected = true;
    }

    /**
     * Stops.
     */
    public void stop()
    {
        Context context = JitsiApplication.getGlobalContext();
        context.unregisterReceiver(this);

        connected = false;
    }

    /**
     * Whether the underlying implementation is currently connected and
     * working.
     * @return whether we are connected and working.
     */
    public boolean isConnected()
    {
        return connected;
    }

    /**
     * Receiving broadcast for network change.
     * @param context the context.
     * @param intent the intent for the broadcast.
     */
    public void onReceive(Context context, Intent intent)
    {
        if(intent.getAction().equals(CONNECTIVITY_CHANGE_ACTION))
        {
            SystemActivityEvent evt = new SystemActivityEvent(
                SysActivityActivator.getSystemActivityService(),
                SystemActivityEvent.EVENT_NETWORK_CHANGE);

            SysActivityActivator.getSystemActivityService()
                .fireSystemActivityEvent(evt);
        }
    }
}
