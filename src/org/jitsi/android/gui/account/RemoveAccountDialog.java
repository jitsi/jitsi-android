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
package org.jitsi.android.gui.account;

import android.app.*;
import android.content.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.service.configuration.*;

import java.util.*;

/**
 * Helper class that produces "remove account dialog". It asks the user for
 * account removal confirmation and finally removes the account.
 * Interface <tt>OnAccountRemovedListener</tt> is used to notify about account
 * removal which will not be fired if the user cancels the dialog.
 *
 * @author Pawel Domas
 */
public class RemoveAccountDialog
{
    public static AlertDialog create(Context ctx,
                                     final AccountID account,
                                     final OnAccountRemovedListener listener)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        return alert
                .setTitle(R.string.service_gui_REMOVE_ACCOUNT)
                .setMessage(
                        ctx.getString(
                                R.string.service_gui_REMOVE_ACCOUNT_MESSAGE,
                                account.getDisplayName()))
                .setPositiveButton(R.string.service_gui_YES,
                                   new DialogInterface.OnClickListener()
                   {
                       @Override
                       public void onClick(DialogInterface dialog, int which)
                       {
                           onRemoveClicked(dialog, account, listener);
                       }
                   })
                .setNegativeButton(R.string.service_gui_NO,
                                   new DialogInterface.OnClickListener()
                   {
                       @Override
                       public void onClick(
                               DialogInterface dialog,
                               int which)
                       {
                           dialog.dismiss();
                       }
                   }).create();
    }

    private static void onRemoveClicked(final DialogInterface dialog,
                                        final AccountID account,
                                        final OnAccountRemovedListener l)
    {
        // Fix "network on main thread"
        final Thread removeAccountThread = new Thread()
        {
            @Override
            public void run()
            {
                removeAccount(account);
            }
        };
        removeAccountThread.start();
        try
        {
            // Simply block UI thread as it shouldn't take too long to uninstall
            removeAccountThread.join();
            // Notify about results
            l.onAccountRemoved(account);
            dialog.dismiss();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes given <tt>AccountID</tt> from the system.
     * @param accountID the account that will be uninstalled from the system.
     */
    private static void removeAccount(AccountID accountID)
    {
        ProtocolProviderFactory providerFactory =
                AccountUtils.getProtocolProviderFactory(
                        accountID.getProtocolName());

        ConfigurationService configService
                = AndroidGUIActivator.getConfigurationService();
        String prefix
                = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts
                = configService.getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID
                    = configService.getString(accountRootPropName);

            if (accountUID.equals(accountID.getAccountUniqueID()))
            {
                configService.setProperty(accountRootPropName, null);
                break;
            }
        }

        boolean isUninstalled
                = providerFactory.uninstallAccount(accountID);

        if (!isUninstalled)
            throw new RuntimeException("Failed to uninstall account");
    }

    /**
     * Interfaces used to notify about account removal which happens after
     * the user confirms the action.
     */
    interface OnAccountRemovedListener
    {
        /**
         * Fired after <tt>accountID</tt> is removed from the system which
         * happens after user confirms the action. Will not be fired when user
         * dismisses the dialog.
         * @param accountID removed <tt>AccountID</tt>.
         */
        void onAccountRemoved(AccountID accountID);
    }

}
