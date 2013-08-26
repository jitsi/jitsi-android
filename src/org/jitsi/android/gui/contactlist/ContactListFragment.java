/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.service.osgi.*;

import android.content.*;
import android.os.Bundle;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

/**
 *
 */
public class ContactListFragment
    extends OSGiFragment
    implements  OnChildClickListener,
                OnGroupClickListener
{
    /**
     * The logger.
     */
    private final static Logger logger
            = Logger.getLogger(ContactListFragment.class);

    /**
     * If this argument is present fragment will start new chat with
     * <tt>MetaContact</tt> identified by UID carried by this arg.
     */
    public static final String META_CONTACT_UID_ARG="arg.meta_uid";

    /**
     * The <tt>MetaContactListService</tt> giving access to the contact list
     * content.
     */
    private MetaContactListService contactListService;

    /**
     * The adapter containing list data.
     */
    private ContactListAdapter contactListAdapter;

    /**
     * The contact list view.
     */
    private ExpandableListView contactListView;

    private MetaContact clickedContact;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(   LayoutInflater inflater,
                                ViewGroup container,
                                Bundle savedInstanceState)
    {
        View content = inflater.inflate( R.layout.contact_list,
                                         container,
                                         false);

        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart()
    {
        super.onStart();

        this.contactListService
                = ServiceUtils.getService( AndroidGUIActivator.bundleContext,
                                           MetaContactListService.class);

        contactListView
            = (ExpandableListView) getView().findViewById(R.id.contactListView);

        this.contactListAdapter = new ContactListAdapter(this);

        contactListView.setAdapter(contactListAdapter);
        contactListView.setSelector(R.drawable.contact_list_selector);
        contactListView.setOnChildClickListener(this);
        contactListView.setOnGroupClickListener(this);
        // Adds context menu for contact list items
        registerForContextMenu(contactListView);

        // If the MetaContactListService is already available we need to
        // initialize the adapter.
        if (!contactListAdapter.isInitialized())
        {
            contactListAdapter.initAdapterData(contactListService);
        }

        // Check if we have contact UID argument to start new chat
        Bundle args = getArguments();
        if(args == null)
            return;
        String metaUID = args.getString(META_CONTACT_UID_ARG);
        if(metaUID == null)
            return;
        MetaContact metaContact
                = contactListService.findMetaContactByMetaUID(metaUID);
        if(metaContact == null)
        {
            logger.error("Meta contact not found for UID: " + metaUID);
            return;
        }
        logger.info("Start chat with contact: " + metaContact);
        startChatActivity(metaContact);
    }

    @Override
    public void onDestroy()
    {
        if(contactListAdapter != null)
        {
            contactListAdapter.dispose();
        }

        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Inflate contact list context menu
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.contact_menu, menu);

        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        int type =
                ExpandableListView.getPackedPositionType(info.packedPosition);

        int group =
                ExpandableListView.getPackedPositionGroup(info.packedPosition);

        int child =
                ExpandableListView.getPackedPositionChild(info.packedPosition);

        // Only create a context menu for child items
        if (type != ExpandableListView.PACKED_POSITION_TYPE_CHILD)
        {
            return;
        }

        // Remembers clicked contact
        clickedContact
                = ((MetaContact) contactListAdapter.getChild(group, child));

        // Checks if the re-request authorization item should be visible
        Contact contact = clickedContact.getDefaultContact();

        OperationSetExtendedAuthorizations authOpSet
                = contact.getProtocolProvider().getOperationSet(
                OperationSetExtendedAuthorizations.class);

        boolean reRequestVisible = false;

        if (authOpSet != null
                && authOpSet.getSubscriptionStatus(contact) != null
                && !authOpSet.getSubscriptionStatus(contact)
                .equals(OperationSetExtendedAuthorizations
                                .SubscriptionStatus.Subscribed))
        {
            reRequestVisible = true;
        }

        menu.findItem(R.id.re_request_auth).setVisible(reRequestVisible);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.re_request_auth
                && clickedContact != null)
        {
            requestAuthorization(clickedContact.getDefaultContact());
            return true;
        }
        else if(item.getItemId() == R.id.remove_contact)
        {
            removeContact(clickedContact);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Removes given <tt>contact</tt> from the contact list.
     * Asks the user for confirmation, before it's done.
     *
     * @param contact the contact to be removed from the contact list.
     */
    private void removeContact(final MetaContact contact)
    {
        Context ctx = JitsiApplication.getGlobalContext();
        DialogActivity.showConfirmDialog(
                ctx,
                ctx.getString(R.string.service_gui_REMOVE_CONTACT),
                ctx.getString(R.string.service_gui_REMOVE_CONTACT_TEXT,
                              contact.getDisplayName()),
                ctx.getString(R.string.service_gui_REMOVE),
                new DialogActivity.DialogListener()
                {
                    @Override
                    public void onConfirmClicked(DialogActivity dialog)
                    {
                        MetaContactListService mls = AndroidGUIActivator
                                .getMetaContactListService();
                        mls.removeMetaContact(contact);
                    }

                    @Override
                    public void onDialogCancelled(DialogActivity dialog)
                    {
                        // Do nothing
                    }
                }
        );
    }

    /**
     * Requests authorization for contact.
     *
     * @param contact the contact for which we request authorization
     */
    private void requestAuthorization(final Contact contact)
    {
        final OperationSetExtendedAuthorizations authOpSet
                = contact.getProtocolProvider().getOperationSet(
                OperationSetExtendedAuthorizations.class);

        if (authOpSet == null)
            return;

        new Thread()
        {
            @Override
            public void run()
            {
                AuthorizationRequest request
                        = AndroidGUIActivator.getLoginRenderer()
                        .getAuthorizationHandler()
                        .createAuthorizationRequest(contact);

                if(request == null)
                    return;

                try
                {
                    authOpSet.reRequestAuthorization(request, contact);
                }
                catch (OperationFailedException e)
                {
                    Context ctx = JitsiApplication.getGlobalContext();
                    DialogActivity.showConfirmDialog(
                            ctx,
                            ctx.getString(
                                R.string.service_gui_RE_REQUEST_AUTHORIZATION),
                            e.getMessage(), null, null);
                }
            }
        }.start();
    }

    /**
     * Returns the contact list view.
     *
     * @return the contact list view
     */
    public ExpandableListView getContactListView()
    {
        return contactListView;
    }

    /**
     * 
     */
    @Override
    public boolean onChildClick(ExpandableListView listView,
                                View v,
                                int groupPosition,
                                int childPosition,
                                long id)
    {
        int position
            = contactListAdapter.getListIndex(groupPosition, childPosition);
        contactListView.setSelection(position);

        contactListAdapter.notifyDataSetChanged();

        MetaContact metaContact
            = (MetaContact) contactListAdapter
                .getChild(groupPosition, childPosition);

        if (metaContact != null)
        {
            startChatActivity(metaContact);
            return true;
        }
        return false;
    }

    /**
     * Expands/collapses the group given by <tt>groupPosition</tt>.
     *
     * @param parent the parent expandable list view
     * @param v the view
     * @param groupPosition the position of the group
     * @param id the identifier
     *
     * @return <tt>true</tt> if the group click action has been performed
     */
    public boolean onGroupClick(ExpandableListView parent, View v,
        int groupPosition, long id)
    {
        if (contactListView.isGroupExpanded(groupPosition))
            contactListView.collapseGroup(groupPosition);
        else
            contactListView.expandGroup(groupPosition, true);

        return true;
    }

    /**
     * Starts the chat activity for the given metaContact.
     *
     * @param metaContact
     */
    public void startChatActivity(MetaContact metaContact)
    {
        ChatSession chatSession = ChatSessionManager.getActiveChat(metaContact);

        if (chatSession == null)
        {
            chatSession = new ChatSession(metaContact);

            ChatSessionManager.addActiveChat(chatSession);
        }

        ChatSessionManager.setCurrentChatId(chatSession.getChatId());

        View chatExtendedView = getActivity().findViewById(R.id.chatView);

        if (chatExtendedView != null)
        {
            ChatTabletFragment chatTabletFragment
                = ChatTabletFragment.newInstance(chatSession.getChatId());
                    getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.chatView, chatTabletFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
        else
        {
            Intent chatIntent = new Intent(
                getActivity(),
                ChatActivity.class);

            getActivity().startActivity(chatIntent);
        }
    }

    /**
     * 
     * @param query
     */
    public void filterContactList(String query)
    {
        if (contactListAdapter == null)
            return;

        contactListAdapter.filterData(query);
    }
}
