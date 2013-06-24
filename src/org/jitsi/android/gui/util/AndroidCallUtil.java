/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import java.util.*;

import org.jitsi.*;

import android.content.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.call.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class AndroidCallUtil
{
    /**
     * The logger for this class.
     */
    private final static Logger logger
        = Logger.getLogger(AndroidCallUtil.class);

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
    private static void createCall( final Context context,
                                    final String destination,
                                    final ProtocolProviderService provider)
    {
        new Thread()
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
            }
        }.start();
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

}
