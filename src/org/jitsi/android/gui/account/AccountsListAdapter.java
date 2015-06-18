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
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.EventListener;
import org.osgi.framework.*;

import java.util.*;

/**
 * This is a convenience class which implements an {@link Adapter} interface
 * to put the list of {@link Account}s into Android widgets.
 *
 * The {@link View}s for each row are created from the layout resource id
 * given in constructor. This view should contain:
 * <br/>
 *  - <tt>R.id.accountName</tt> for the account name text ({@link TextView})
 * <br/>
 *  - <tt>R.id.accountProtoIcon</tt> for the protocol icon of type
 *      ({@link ImageView})
 * <br/>
 *  - <tt>R.id.accountStatusIcon</tt> for the presence status icon
 *      ({@link ImageView})
 * <br/>
 *  - <tt>R.id.accountStatus</tt> for the presence status name
 *      ({@link TextView})
 * <br/>
 * It implements {@link EventListener} to refresh the list on any
 * changes to the {@link Account}.
 * 
 * @author Pawel Domas
 */
public class AccountsListAdapter
    extends CollectionAdapter<Account>
    implements EventListener<AccountEvent>,
        ServiceListener
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(AccountsListAdapter.class);

    /**
     * The {@link View} resources ID describing list's row
     */
    private final int listRowResourceID;

    /**
     * The {@link View} resources ID describing list's row
     */
    private final int dropDownRowResourceID;

    /**
     * The {@link BundleContext} of parent
     * {@link org.jitsi.service.osgi.OSGiActivity}
     */
    private final BundleContext bundleContext;
    /**
     * The flag indicates whether disabled accounts should be filtered
     * out from the list
     */
    private final boolean filterDisabledAccounts;

    /**
     * Creates new instance of {@link AccountsListAdapter}
     *
     * @param parent the {@link Activity} running this adapter
     * @param accounts collection of accounts that will be displayed
     * @param listRowResourceID the layout resource ID see
     *  {@link AccountsListAdapter} for detailed description
     * @param filterDisabledAccounts flag indicates if disabled accounts
     *  should be filtered out from the list
     */
    public AccountsListAdapter(
            Activity parent,
            int listRowResourceID,
            int dropDownRowResourceID,
            Collection<AccountID> accounts,
            boolean filterDisabledAccounts)
    {
        super(parent);

        this.filterDisabledAccounts = filterDisabledAccounts;

        this.listRowResourceID = listRowResourceID;
        this.dropDownRowResourceID = dropDownRowResourceID;

        this.bundleContext = AndroidGUIActivator.bundleContext;
        bundleContext.addServiceListener(this);

        initAccounts(accounts);
    }

    /**
     * Initialize the list and filters out disabled accounts if necessary.
     *
     * @param collection set of {@link AccountID} that will be displayed
     */
    private void initAccounts(Collection<AccountID> collection)
    {
        ArrayList<Account> accounts = new ArrayList<Account>();

        for(AccountID acc : collection)
        {
            Account account = new Account( acc,
                                           bundleContext,
                                           getParentActivity());
            if( filterDisabledAccounts
                && !account.isEnabled() )
                continue;

            // Skip hidden accounts
            if(acc.isHidden())
                continue;

            account.addAccountEventListener(this);
            accounts.add(account);
        }

        setList(accounts);
    }

    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
        {
            return;
        }
        Object sourceService =
                bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(sourceService instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService protocolProvider
                = (ProtocolProviderService) sourceService;

        // Add or remove the protocol provider from our accounts list.
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            Account acc = findAccountID(protocolProvider.getAccountID());
            if(acc == null)
            {
                addAccount(
                        new Account( protocolProvider.getAccountID(),
                                     bundleContext,
                                     getParentActivity().getBaseContext()));
            }
            else
            {
                // Register for events if account exists on this list
                acc.addAccountEventListener(this);
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            Account acc = findAccountID(protocolProvider.getAccountID());
            if(acc != null && acc.isEnabled())
            {
                // Remove enabled accounts
                if(acc.isEnabled())
                {
                    // Remove the account completely
                    removeAccount(protocolProvider.getAccountID());
                }
                else
                {
                    // Quit from listening to updates
                    acc.removeAccountEventListener(this);
                }
            }
        }
    }

    /**
     * Unregisters status update listeners for accounts
     */
    void deinitStatusListeners()
    {
        for(int accIdx=0; accIdx < getCount(); accIdx++)
        {
            Account account = getObject(accIdx);

            account.destroy();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected View getView( boolean isDropDown,
                            Account account,
                            ViewGroup parent,
                            LayoutInflater inflater )
    {

        int rowResID = listRowResourceID;

        if(isDropDown && dropDownRowResourceID != -1)
        {
            rowResID = dropDownRowResourceID;
        }

        View statusItem = inflater.inflate(rowResID, parent, false);

        TextView accountName =
                (TextView) statusItem.findViewById(R.id.accountName);
        ImageView accountProtocol =
                (ImageView) statusItem.findViewById(R.id.accountProtoIcon);
        ImageView statusIconView =
                (ImageView) statusItem.findViewById(R.id.accountStatusIcon);
        TextView accountStatus =
                (TextView) statusItem.findViewById(R.id.accountStatus);

        // Sets account's properties
        if(accountName != null)
            accountName.setText(account.getAccountName());

        if(accountProtocol != null)
        {
            Drawable protoIcon = account.getProtocolIcon();
            if(protoIcon != null)
            {
                accountProtocol.setImageDrawable(protoIcon);
            }
        }

        if(accountStatus != null)
            accountStatus.setText(account.getStatusName());

        if(statusIconView != null)
        {
            Drawable statusIcon = account.getStatusIcon();
            if(statusIcon != null)
            {
                statusIconView.setImageDrawable(statusIcon);
            }
        }

        return statusItem;
    }

    /**
     * Check if given <tt>account</tt> exists on the list
     *
     * @param account {@link AccountID} that has to be found on the list
     *
     * @return <tt>true</tt> if account is on the list
     */
    private Account findAccountID(AccountID account)
    {
        for(int i=0; i<getCount(); i++)
        {
            Account acc = getObject(i);
            if(acc.getAccountID().equals(account))
                return acc;
        }
        return null;
    }

    /**
     * Adds new account to the list
     *
     * @param account {@link Account} that will be added to the list
     */
    public void addAccount(Account account)
    {
        if(filterDisabledAccounts &&
           !account.isEnabled())
            return;

        if(account.getAccountID().isHidden())
            return;

        logger.debug("Account added: " + account.getAccountName());
        add(account);
        account.addAccountEventListener(this);
    }

    /**
     * Removes the account from the list
     * @param account the {@link AccountID} that will be removed from the list
     */
    public void removeAccount(AccountID account)
    {
        Account acc = findAccountID(account);
        if(acc != null)
        {
            acc.removeAccountEventListener(this);
            remove(acc);
            logger.debug("Account removed: " + account.getDisplayName());
        }
    }

    /**
     * Does refresh the list
     *
     * @param accountEvent the {@link AccountEvent} that caused the change event
     */
    public void onChangeEvent(AccountEvent accountEvent)
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("Received accountEvent update "
                    + accountEvent.getSource().getAccountName()+" "+accountEvent.toString(),
                    new Throwable());
        }
        doRefreshList();
    }
}
