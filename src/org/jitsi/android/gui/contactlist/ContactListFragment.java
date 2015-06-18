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
package org.jitsi.android.gui.contactlist;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.*;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.contactlist.model.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.*;

/**
 *
 */
public class ContactListFragment
    extends OSGiFragment
    implements  OnChildClickListener,
                OnGroupClickListener
{
    /**
     * The logger
     */
    private final static Logger logger
        = Logger.getLogger(ContactListFragment.class);

    /**
     * Search options menu items.
     */
    private MenuItem searchItem;

    /**
     * Contact list data model.
     */
    protected MetaContactListAdapter contactListAdapter;

    /**
     * Meta contact groups expand memory.
     */
    private MetaGroupExpandHandler listExpandHandler;

    /**
     * List model used to search contact list and contact sources.
     */
    private QueryContactListAdapter sourcesAdapter;

    /**
     * The contact list view.
     */
    protected ExpandableListView contactListView;

    /**
     * Stores last clicked <tt>MetaContact</tt>.
     */
    private MetaContact clickedContact;

    /**
     * Stores recently clicked contact group.
     */
    private MetaContactGroup clickedGroup;

    /**
     * Contact list item scroll position.
     */
    private static int scrollPosition;

    /**
     * Contact list scroll top position.
     */
    private static int scrollTopPosition;

    /**
     * Creates new instance of <tt>ContactListFragment</tt>.
     */
    public ContactListFragment()
    {
        // This fragment will create options menu.
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(   LayoutInflater inflater,
                                ViewGroup container,
                                Bundle savedInstanceState)
    {
        if(AndroidGUIActivator.bundleContext == null)
        {
            return null;
        }

        View content = inflater.inflate( R.layout.contact_list,
                                         container,
                                         false);

        contactListView = (ExpandableListView) content
                .findViewById(R.id.contactListView);
        contactListView.setSelector(R.drawable.contact_list_selector);
        contactListView.setOnChildClickListener(this);
        contactListView.setOnGroupClickListener(this);

        // Adds context menu for contact list items
        registerForContextMenu(contactListView);

        return content;
    }

    /**
     * Invoked when the options menu is created. Creates our own options menu
     * from the corresponding xml.
     *
     * @param menu the options menu
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
    {
        super.onCreateOptionsMenu(menu, menuInflater);

        Activity activity = getActivity();

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager
            = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);

        this.searchItem = menu.findItem(R.id.search);

        // OnActionExpandListener not supported prior API 14
        if(AndroidUtils.hasAPI(14))
        {
            searchItem.setOnActionExpandListener(
                new MenuItem.OnActionExpandListener()
            {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item)
                {
                    filterContactList("");

                    return true; // Return true to collapse action view
                }
                public boolean onMenuItemActionExpand(MenuItem item)
                {
                    return true; // Return true to expand action view
                }
            });
        }

        if(AndroidUtils.hasAPI(11))
        {
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setSearchableInfo(
                searchManager.getSearchableInfo(activity.getComponentName()));

            int id = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
            TextView textView = (TextView) searchView.findViewById(id);
            textView.setTextColor(getResources().getColor(R.color.white));
            textView.setHintTextColor(getResources().getColor(R.color.white));

            bindSearchListener();
        }
    }

    private void bindSearchListener()
    {
        if(searchItem != null)
        {
            SearchView searchView = (SearchView) searchItem.getActionView();

            SearchViewListener listener = new SearchViewListener();
            searchView.setOnQueryTextListener(listener);
            searchView.setOnCloseListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        contactListView.setAdapter(getContactListAdapter());

        // Attach contact groups expand memory
        listExpandHandler = new MetaGroupExpandHandler(contactListAdapter,
                                                       contactListView);
        listExpandHandler.bindAndRestore();

        // Update active chats
        contactListAdapter.invalidateViews();

        // Restore search state based on entered text
        if(searchItem != null)
        {
            SearchView searchView = (SearchView) searchItem.getActionView();
            int id = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
            TextView textView = (TextView) searchView.findViewById(id);

            filterContactList(textView.getText().toString());

            bindSearchListener();
        }

        // Restore scroll position
        contactListView.setSelectionFromTop(scrollPosition,
                                            scrollTopPosition);
    }

    private MetaContactListAdapter getContactListAdapter()
    {
        if(contactListAdapter == null)
        {
            contactListAdapter = new MetaContactListAdapter(this);
            contactListAdapter.initModelData();
        }
        return contactListAdapter;
    }

    private QueryContactListAdapter getSourcesAdapter()
    {
        if(sourcesAdapter == null)
        {
            sourcesAdapter
                = new QueryContactListAdapter(
                        this, getContactListAdapter());

            sourcesAdapter.initModelData();
        }
        return sourcesAdapter;
    }

    private void disposeSourcesAdapter()
    {
        if(sourcesAdapter != null)
        {
            sourcesAdapter.dispose();
        }
        sourcesAdapter = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause()
    {
        super.onPause();

        // Unbind search listener
        if(searchItem != null)
        {
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
        }

        // Save scroll position
        scrollPosition = contactListView.getFirstVisiblePosition();
        View itemView = contactListView.getChildAt(0);
        scrollTopPosition = itemView == null ? 0 : itemView.getTop();

        // Dispose of group expand memory
        if(listExpandHandler != null)
        {
            listExpandHandler.unbind();
            listExpandHandler = null;
        }

        contactListView.setAdapter((ExpandableListAdapter)null);

        if(contactListAdapter != null)
        {
            contactListAdapter.dispose();

            contactListAdapter = null;
        }

        disposeSourcesAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        if( contactListView.getExpandableListAdapter()
                != getContactListAdapter() )
        {
            return;
        }

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
    private void createContactCtxMenu(ContextMenu menu, MenuInflater inflater,
                                      int group, int child)
    {
        // Inflate contact list context menu
        inflater.inflate(R.menu.contact_menu, menu);

        // Remembers clicked contact
        clickedContact
                = ((MetaContact) contactListAdapter.getChild(group, child));

        menu.setHeaderTitle(clickedContact.getDisplayName());

        // Checks if close chats options should be visible
        boolean closeChatsVisible
                = ChatSessionManager.getActiveChat(clickedContact) != null;
        menu.findItem(R.id.close_chat).setVisible(closeChatsVisible);
        menu.findItem(R.id.close_all_chats).setVisible(closeChatsVisible);

        // Checks if the re-request authorization item should be visible
        Contact contact = clickedContact.getDefaultContact();
        if(contact == null)
        {
            logger.warn("No default contact for: "+clickedContact);
            return;
        }

        ProtocolProviderService pps = contact.getProtocolProvider();
        if(pps == null)
        {
            logger.warn("No protocol provider found for: "+contact);
            return;
        }

        OperationSetExtendedAuthorizations authOpSet
            = pps.getOperationSet(OperationSetExtendedAuthorizations.class);

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
    private void createGroupCtxMenu(ContextMenu menu,
                                    MenuInflater inflater, int group)
    {
        this.clickedGroup
            = (MetaContactGroup) contactListAdapter.getGroup(group);

        // Inflate contact list context menu
        inflater.inflate(R.menu.group_menu, menu);

        menu.setHeaderTitle(clickedGroup.getGroupName());
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
            case R.id.move_contact:
                // Show move contact dialog
                FragmentTransaction ft
                    = getFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                DialogFragment newFragment
                    = MoveToGroupDialog.getInstance(clickedContact);
                newFragment.show(ft, "dialog");
                return true;
            case R.id.remove_group:
                MetaContactListManager.removeMetaContactGroup(clickedGroup);
                return true;
            case R.id.close_chat:
                ChatSession clickedChat
                    = ChatSessionManager.getActiveChat(clickedContact);
                if(clickedChat != null)
                {
                    onCloseChat(clickedChat);
                }
                return true;
            case R.id.close_all_chats:
                onCloseAllChats();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Method fired when given chat is being closed.
     * @param closedChat closed <tt>ChatSession</tt>.
     */
    protected void onCloseChat(ChatSession closedChat)
    {
        ChatSessionManager.removeActiveChat(closedChat);
        contactListAdapter.notifyDataSetChanged();
    }

    /**
     * Method fired when all chats are being closed.
     */
    protected void onCloseAllChats()
    {
        ChatSessionManager.removeAllActiveChats();
        contactListAdapter.notifyDataSetChanged();
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
        BaseContactListAdapter adapter
            = (BaseContactListAdapter) listView.getExpandableListAdapter();

        int position
            = adapter.getListIndex(groupPosition, childPosition);

        contactListView.setSelection(position);
        adapter.invalidateViews();

        Object clicked = adapter.getChild(groupPosition, childPosition);
        if(!(clicked instanceof MetaContact))
        {
            logger.debug(
                "No meta contact at "+groupPosition+", "+childPosition);
            return false;
        }

        MetaContact metaContact = (MetaContact) clicked;

        if(!metaContact.getContactsForOperationSet(
                OperationSetBasicInstantMessaging.class).isEmpty())
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
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public boolean onGroupClick(ExpandableListView parent, View v,
        int groupPosition, long id)
    {
        if (contactListView.isGroupExpanded(groupPosition))
            contactListView.collapseGroup(groupPosition);
        else
        {
            // Expand animation is supported since API14
            if(AndroidUtils.hasAPI(14))
            {
                contactListView.expandGroup(groupPosition, true);
            }
            else
            {
                contactListView.expandGroup(groupPosition);
            }
        }

        return true;
    }

    /**
     * Starts the chat activity for the given metaContact.
     *
     * @param metaContact <tt>MetaContact</tt> for which chat activity will be
     *                    started.
     */
    public void startChatActivity(MetaContact metaContact)
    {
        Intent chatIntent = ChatSessionManager.getChatIntent(metaContact);

        if(chatIntent != null)
        {
            getActivity().startActivity(chatIntent);
        }
        else
        {
            logger.warn("Failed to start chat with " + metaContact);
        }
    }

    /**
     * Filters contact list for given <tt>query</tt>.
     * @param query the query string that will be used for filtering contacts.
     */
    private void filterContactList(String query)
    {
        if(StringUtils.isNullOrEmpty(query))
        {
            // Cancel any pending queries
            disposeSourcesAdapter();

            // Display the contact list
            if( contactListView.getExpandableListAdapter()
                != getContactListAdapter() )
            {
                contactListView.setAdapter(getContactListAdapter());

                contactListAdapter.filterData("");
            }

            // Restore previously collapsed groups
            if(listExpandHandler != null)
            {
                listExpandHandler.bindAndRestore();
            }
        }
        else
        {
            // Unbind group expand memory
            if(listExpandHandler != null)
                listExpandHandler.unbind();

            // Display search results
            if( contactListView.getExpandableListAdapter()
                != getSourcesAdapter() )
            {
                contactListView.setAdapter(getSourcesAdapter());
            }

            // Update query string
            sourcesAdapter.filterData(query);
        }

        updateSearchView(query);
    }

    private void updateSearchView(final String query)
    {
        View view = getView();
        if(view == null)
        {
            logger.error("No view created yet!!! query: " + query);
            return;
        }

        RelativeLayout callSearchLayout
            = (RelativeLayout) view.findViewById(R.id.callSearchLayout);

        if (StringUtils.isNullOrEmpty(query))
        {
            if (callSearchLayout != null)
            {
                callSearchLayout.setVisibility(View.INVISIBLE);
                callSearchLayout.getLayoutParams().height = 0;
            }
        }
        else
        {
            if (callSearchLayout != null)
            {
                TextView searchContactView
                    = (TextView) callSearchLayout
                    .findViewById(R.id.callSearchContact);

                searchContactView.setText(query);
                callSearchLayout.getLayoutParams().height
                    = searchContactView.getResources()
                        .getDimensionPixelSize(R.dimen.account_list_row_height);

                callSearchLayout.setVisibility(View.VISIBLE);

                final ImageButton callButton
                    = (ImageButton) callSearchLayout
                            .findViewById(R.id.contactCallButton);
                callButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        AndroidCallUtil
                            .createAndroidCall(
                                getActivity(),
                                callButton,
                                query);
                    }
                });
            }
        }
    }

    /**
     * Class used to implement <tt>SearchView</tt> listeners for compatibility
     * purposes.
     *
     */
    class SearchViewListener
        implements SearchView.OnQueryTextListener,
                   SearchView.OnCloseListener
    {
        @Override
        public boolean onClose()
        {
            filterContactList("");

            return true;
        }

        @Override
        public boolean onQueryTextChange(String query)
        {
            filterContactList(query);

            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query)
        {
            filterContactList(query);

            return true;
        }
    }
}
