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
package org.jitsi.android.gui.util;

import java.util.*;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.call.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;

/**
 * 
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class AndroidCallUtil
{
    /**
     * The logger for this class.
     */
    private final static Logger logger
        = Logger.getLogger(AndroidCallUtil.class);

    /**
     * Field used to track the thread used to create outgoing calls.
     */
    private static Thread createCallThread;

    /**
     * Creates an android call.
     *
     * @param context the android context
     * @param callButtonView the button view that generated the call
     * @param contact the contact address to call
     */
    public static void createAndroidCall(   Context context,
                                            View callButtonView,
                                            String contact)
    {
        if (AccountUtils.getRegisteredProviders().size() > 1)
            showCallViaMenu(context, callButtonView, contact);
        else
            createCall(context, contact);
    }

    /**
     * Creates new call to target <tt>destination</tt>.
     *
     * @param context the android context
     * @param destination the target callee name that will be used.
     */
    private static void createCall( Context context,
                                    String destination)
    {
        Iterator<ProtocolProviderService> allProviders =
                AccountUtils.getRegisteredProviders().iterator();

        if(!allProviders.hasNext())
        {
            logger.error("No registered providers found");
            return;
        }

        createCall(context, destination, allProviders.next());
    }

    /**
     * Creates new call to given <tt>destination</tt> using selected
     * <tt>provider</tt>.
     *
     * @param context the android context
     * @param destination target callee name.
     * @param provider the provider that will be used to make a call.
     */
    public static void createCall( final Context context,
                                    final String destination,
                                    final ProtocolProviderService provider)
    {
        if(createCallThread != null)
        {
            logger.warn("Another call is already being created");
            return;
        }
        else if(CallManager.getActiveCallsCount() > 0)
        {
            logger.warn("Another call is in progress");
            return;
        }

        final long dialogId
            = ProgressDialogFragment.showProgressDialog(
                    JitsiApplication.getResString(
                        R.string.service_gui_OUTGOING_CALL),
                    JitsiApplication.getResString(
                        R.string.service_gui_OUTGOING_CALL_MSG,
                        destination));

        createCallThread = new Thread("Create call thread")
        {
            public void run()
            {
                try
                {
                    CallManager.createCall(provider, destination);
                }
                catch(Throwable t)
                {
                    logger.error("Error creating the call: "+t.getMessage(), t);
                    AndroidUtils.showAlertDialog(
                            context,
                            context.getString(R.string.service_gui_ERROR),
                            t.getMessage());
                }
                finally
                {
                    if(DialogActivity.waitForDialogOpened(dialogId))
                    {
                        DialogActivity.closeDialog(
                            JitsiApplication.getGlobalContext(), dialogId);
                    }
                    else
                    {
                        logger.error(
                            "Failed to wait for the dialog: " + dialogId );
                    }
                    createCallThread = null;
                }
            }
        };

        createCallThread.start();
    }


    /**
     * Shows "call via" menu allowing user to selected from multiple providers.
     *
     * @param context the android context
     * @param v the View that will contain the popup menu.
     * @param destination target callee name.
     */
    private static void showCallViaMenu(final Context context,
                                        View v,
                                        final String destination)
    {
        PopupMenu popup = new PopupMenu(context, v);

        Menu menu = popup.getMenu();

        Iterator<ProtocolProviderService> registeredProviders
                = AccountUtils.getRegisteredProviders().iterator();

        while (registeredProviders.hasNext())
        {
            final ProtocolProviderService provider = registeredProviders.next();
            String accountAddress = provider.getAccountID().getAccountAddress();

            MenuItem menuItem = menu.add(   Menu.NONE,
                                            Menu.NONE,
                                            Menu.NONE,
                                            accountAddress);

            menuItem.setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener()
                    {
                        public boolean onMenuItemClick(MenuItem item)
                        {
                            createCall(context, destination, provider);

                            return false;
                        }
                    });
        }

        popup.show();
    }

    /**
     * Checks if there is a call in progress. If true then shows a warning toast
     * and finishes the activity.
     * @param activity activity doing a check.
     * @return <tt>true</tt> if there is call in progress and <tt>Activity</tt>
     *         was finished.
     */
    public static boolean checkCallInProgress(Activity activity)
    {
        if(CallManager.getActiveCallsCount() >0)
        {
            logger.warn("Call is in progress");

            Toast t = Toast.makeText(
                activity,
                R.string.service_gui_WARN_CALL_IN_PROGRESS,
                Toast.LENGTH_SHORT);
            t.show();

            activity.finish();
            return true;
        }
        else
        {
            return false;
        }
    }

}
