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
import org.jitsi.android.gui.util.*;
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
     * Stores last clicked <tt>MetaContact</tt>.
     */
    private MetaContact clickedContact;

    /**
     * Stores recently clicked contact group.
     */
    private MetaContactGroup clickedGroup;

    /**
     * Stores current chat id, when the activity is paused.
     */
    private String currentChatId;

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

        this.contactListService
                = ServiceUtils.getService( AndroidGUIActivator.bundleContext,
                                           MetaContactListService.class);

        contactListView = (ExpandableListView) content
                .findViewById(R.id.contactListView);

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

        // If we have stored state use savedInstanceState bundle
        // or the arguments otherwise
        Bundle state
                = savedInstanceState != null
                    ? savedInstanceState : getArguments();
        if(state != null)
        {
            String intentChatId
                    = state.getString(ChatSessionManager.CHAT_IDENTIFIER);
            if(intentChatId != null)
            {
                selectChatSession(
                        ChatSessionManager.getActiveChat(intentChatId));
            }
        }

        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause()
    {
        super.onPause();

        // Manages current chat id only on tablet layout
        if(AndroidUtils.isTablet())
        {
            currentChatId = ChatSessionManager.getCurrentChatId();
            ChatSessionManager.setCurrentChatId(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        // Manages current chat id only on tablet layout
        if(AndroidUtils.isTablet())
        {
            ChatSessionManager.setCurrentChatId(currentChatId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(ChatSessionManager.CHAT_IDENTIFIER, currentChatId);

        super.onSaveInstanceState(outState);
    }

    /**
     * {@inheritDoc}
     */
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

        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        int type =
                ExpandableListView.getPackedPositionType(info.packedPosition);

        int group =
                ExpandableListView.getPackedPositionGroup(info.packedPosition);

        int child =
                ExpandableListView.getPackedPositionChild(info.packedPosition);

        // Only create a context menu for child items
        MenuInflater inflater = getActivity().getMenuInflater();
        if(type == ExpandableListView.PACKED_POSITION_TYPE_GROUP)
        {
            createGroupCtxMenu(menu, inflater, group);
        }
        else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
        {
            createContactCtxMenu(menu, inflater, group, child);
        }
    }

    /**
     * Inflates contact context menu.
     * @param menu the menu to inflate into.
     * @param inflater the menu inflater.
     * @param group clicked group index.
     * @param child clicked contact index.
     */
    private void createContactCtxMenu(Menu menu, MenuInflater inflater,
                                      int group, int child)
    {
        // Inflate contact list context menu
        inflater.inflate(R.menu.contact_menu, menu);

        // Remembers clicked contact
        clickedContact
                = ((MetaContact) contactListAdapter.getChild(group, child));

        // Checks if close chats options should be visible
        boolean closeChatsVisible
                = ChatSessionManager.getActiveChat(clickedContact) != null;
        menu.findItem(R.id.close_chat).setVisible(closeChatsVisible);
        menu.findItem(R.id.close_all_chats).setVisible(closeChatsVisible);

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
     * Inflates group context menu.
     * @param menu the menu to inflate into.
     * @param inflater the inflater.
     * @param group clicked group index.
     */
    private void createGroupCtxMenu(Menu menu, MenuInflater inflater, int group)
    {
        // Inflate contact list context menu
        inflater.inflate(R.menu.group_menu, menu);

        this.clickedGroup
                = (MetaContactGroup) contactListAdapter.getGroup(group);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.re_request_auth:
                if(clickedContact != null)
                    requestAuthorization(clickedContact.getDefaultContact());
                return true;
            case R.id.remove_contact:
                MetaContactListManager.removeMetaContact(clickedContact);
                return true;
            case R.id.remove_group:
                MetaContactListManager.removeMetaContactGroup(clickedGroup);
                return true;
            case R.id.close_chat:
                ChatSessionManager.removeActiveChat(
                        ChatSessionManager.getActiveChat(clickedContact));
                closeCurrentChat();
                contactListAdapter.notifyDataSetChanged();
                return true;
            case R.id.close_all_chats:
                ChatSessionManager.removeAllActiveChats();
                closeCurrentChat();
                contactListAdapter.notifyDataSetChanged();
                return true;
        }
        return super.onContextItemSelected(item);
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
            if(!metaContact.getContactsForOperationSet(
                    OperationSetBasicInstantMessaging.class).isEmpty())
            {
                startChatActivity(metaContact);
            }
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
        ChatSession chatSession
                = (ChatSession) ChatSessionManager.findChatForContact(
                                    metaContact.getDefaultContact(), true);

        selectChatSession(chatSession);
    }

    /**
     * Selects current chat session. Depends on the current layout new chat
     * fragment will be selected or new <tt>ChatActivity</tt> will be started.
     *
     * @param currentChat current chat session to be selected.
     */
    private void selectChatSession(ChatSession currentChat)
    {
        currentChatId = currentChat.getChatId();

        ChatSessionManager.setCurrentChatId(currentChatId);

        View chatExtendedView = getActivity().findViewById(R.id.chatView);

        if (chatExtendedView != null)
        {
            ChatTabletFragment chatTabletFragment
                    = ChatTabletFragment.newInstance(currentChat.getChatId());
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.chatView, chatTabletFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
        else
        {
            Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
            chatIntent.putExtra(ChatSessionManager.CHAT_IDENTIFIER,
                                currentChat.getChatId());
            getActivity().startActivity(chatIntent);
        }
    }

    /**
     * Closes currently opened chat and it's chat fragment.
     */
    private void closeCurrentChat()
    {
        currentChatId = null;
        ChatSessionManager.setCurrentChatId(currentChatId);

        View chatExtendedView = getActivity().findViewById(R.id.chatView);
        if (chatExtendedView != null)
        {
            FragmentManager fragmentManager
                = getActivity().getSupportFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .remove(fragmentManager.findFragmentById(R.id.chatView))
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
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
