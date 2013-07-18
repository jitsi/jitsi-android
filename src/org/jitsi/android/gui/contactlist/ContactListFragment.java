/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
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
            logger.error("Meta contact not found for UID: "+metaUID);
            return;
        }
        logger.info("Start chat with contact: "+metaContact);
        startChatActivity(metaContact);
    }

    /**
     * Returns 
     *
     * @return
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
     * 
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

        ChatSessionManager.setCurrentChatSession(chatSession.getChatId());

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
                getActivity().getApplicationContext(),
                ChatActivity.class);

            chatIntent.setFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().getApplicationContext().startActivity(chatIntent);
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
