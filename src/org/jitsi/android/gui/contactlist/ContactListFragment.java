/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;

import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import android.widget.ExpandableListView.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import java.util.*;

/**
 *
 */
public class ContactListFragment
    extends OSGiFragmentV4
    implements  OnChildClickListener,
                OnGroupClickListener
{
    /**
     * The logger of this class.
     */
    private Logger logger = Logger.getLogger(ContactListFragment.class);

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
    public void start(BundleContext bundleContext)
            throws Exception
    {
        super.start(bundleContext);

        this.contactListService
                = ServiceUtils.getService( bundleContext,
                                           MetaContactListService.class);

        // If the contact list adapter has been already created we need to
        // initialize it.
        if (contactListAdapter != null && !contactListAdapter.isInitialized())
        {
            initAdapterData();
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link android.app.Activity#onResume() Activity.onResume} of the
     * containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        contactListView
            = (ExpandableListView) getView().findViewById(R.id.contactListView);

        this.contactListAdapter = new ContactListAdapter();
        contactListView.setAdapter(contactListAdapter);
        contactListView.setSelector(R.drawable.contact_list_selector);
        contactListView.setOnChildClickListener(this);
        contactListView.setOnGroupClickListener(this);

        // If the MetaContactListService is already available we need to
        // initialize the adapter.
        if (contactListService != null && !contactListAdapter.isInitialized())
        {
            initAdapterData();
        }
    }

    private void initAdapterData()
    {
        contactListAdapter.initAdapterData();

        int count = contactListAdapter.getGroupCount();
        for (int position = 1; position <= count; position++)
            contactListView.expandGroup(position - 1);

        MetaContact firstMetaContact
            = (MetaContact) contactListAdapter.getChild(0, 0);

        View chatExtendedView = getActivity().findViewById(R.id.chatView);

        // In extended/tablet view we pre-select the first contact.
        if (firstMetaContact != null && chatExtendedView != null)
            startChatActivity(firstMetaContact);
    }

    class ContactListAdapter
        extends BaseExpandableListAdapter
        implements MetaContactListListener
    {

        private LinkedList<MetaContactGroup> groups;
        private LinkedList<LinkedList<MetaContact>> contacts;

        private boolean isInitialized = false;

        ContactListAdapter()
        {
            this.contacts = new LinkedList<LinkedList<MetaContact>>();
            this.groups = new LinkedList<MetaContactGroup>();
        }

        void initAdapterData()
        {
            if (contactListService != null)
            {
                addContacts(contactListService.getRoot());

                contactListService.addMetaContactListListener(this);

                isInitialized = true;
            }
        }

        boolean isInitialized()
        {
            return isInitialized;
        }

        private void addContacts(final MetaContactGroup group)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    groups.add(group);

                    LinkedList<MetaContact> children
                        = new LinkedList<MetaContact>();

                    contacts.add(children);

                    Iterator<MetaContact> childContacts
                        = group.getChildContacts();

                    while(childContacts.hasNext())
                    {
                        MetaContact metaContact = childContacts.next();
                        children.add(metaContact);
                    }

                    Iterator<MetaContactGroup> subGroups = group.getSubgroups();
                    while(subGroups.hasNext())
                    {
                        addContacts(subGroups.next());
                    }
                }
            });
        }

        private void removeContacts(final MetaContactGroup group)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    contacts.remove(groups.indexOf(group));
                    groups.remove(group);
                }
            });
        }

        private void dataChanged()
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    notifyDataSetChanged();
                }
            });
        }

        private void updateDisplayName( final int groupIndex,
                                        final int contactIndex)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    View contactView = contactListView
                        .getChildAt(getListIndex(groupIndex, contactIndex));

                    if (contactView != null)
                    {
                        MetaContact metaContact
                            = (MetaContact) getChild(groupIndex, contactIndex);
                        ViewUtil.setTextViewValue(
                            contactView,
                            R.id.displayName,
                            metaContact.getDisplayName());
                    }
                }
            });
        }

        private void updateAvatar(final int groupIndex, final int contactIndex)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    View contactView = contactListView
                        .getChildAt(getListIndex(groupIndex, contactIndex));

                    if (contactView != null)
                    {
                        MetaContact metaContact
                            = (MetaContact) getChild(groupIndex, contactIndex);

                        ImageView avatarView
                            = (ImageView) contactView
                                .findViewById(R.id.avatarIcon);

                        Drawable avatarIcon = getAvatarDrawable(metaContact);

                        if (avatarIcon != null)
                            avatarView.setImageDrawable(avatarIcon);
                    }
                }
            });
        }

        private void updateStatus(final int groupIndex, final int contactIndex)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    View contactView = contactListView
                        .getChildAt(getListIndex(groupIndex, contactIndex));

                    if (contactView != null)
                    {
                        MetaContact metaContact
                            = (MetaContact) getChild(groupIndex, contactIndex);

                        ImageView statusView
                            = (ImageView) contactView
                                .findViewById(R.id.statusIcon);

                        Drawable statusIcon = getStatusDrawable(metaContact);

                        if (statusIcon != null)
                            statusView.setImageDrawable(statusIcon);
                    }
                }
            });
        }

        private int getListIndex(int groupIndex, int contactIndex)
        {
            int firstIndex = contactListView.getFirstVisiblePosition();
            int lastIndex = contactListView.getLastVisiblePosition();

            for (int i = firstIndex; i <= lastIndex; i++)
            {
                long longposition
                    = contactListView.getExpandableListPosition(i);

                int groupPosition = ExpandableListView
                        .getPackedPositionGroup(longposition);
                int childPosition = ExpandableListView
                        .getPackedPositionChild(longposition);

                if (groupIndex == groupPosition
                    && contactIndex == childPosition)
                {
                    return i;
                }
            }
            return -1;
        }

        public void metaContactAdded(MetaContactEvent evt)
        {
            logger.error(evt.toString());

            MetaContact metaContact = evt.getSourceMetaContact();
            int groupIndex
                = groups.indexOf(metaContact.getParentMetaContactGroup());

            if (groupIndex < 0)
            {
                addContacts(metaContact.getParentMetaContactGroup());
            }
            else if (contacts.get(groupIndex) != null)
                contacts.get(groupIndex).add(evt.getSourceMetaContact());
            else
            {
                LinkedList<MetaContact> children
                    = new LinkedList<MetaContact>();
                children.add(metaContact);
                contacts.add(children);
            }

            dataChanged();
        }

        public void metaContactRenamed(MetaContactRenamedEvent evt)
        {
            logger.error(evt.toString());
            MetaContact metaContact = evt.getSourceMetaContact();
            int groupIndex
                = groups.indexOf(metaContact.getParentMetaContactGroup());
            int contactIndex = contacts.get(groupIndex).indexOf(metaContact);

            if (contactIndex >= 0)
                updateDisplayName(groupIndex, contactIndex);
        }

        public void protoContactAdded(ProtoContactEvent evt)
        {
            logger.error(evt.toString());

            MetaContact metaContact = evt.getNewParent();
            int groupIndex
                = groups.indexOf(metaContact.getParentMetaContactGroup());
            int contactIndex = contacts.get(groupIndex).indexOf(metaContact);

            if (contactIndex >= 0)
                updateStatus(groupIndex, contactIndex);
        }

        public void protoContactModified(ProtoContactEvent evt)
        {
            logger.error(evt.toString());
            dataChanged();
        }

        public void protoContactRemoved(ProtoContactEvent evt)
        {
            logger.error(evt.toString());

            MetaContact metaContact = evt.getOldParent();
            int groupIndex
                = groups.indexOf(metaContact.getParentMetaContactGroup());
            int contactIndex = contacts.get(groupIndex).indexOf(metaContact);

            if (contactIndex >= 0)
                updateStatus(groupIndex, contactIndex);
        }

        public void protoContactMoved(ProtoContactEvent evt)
        {
            logger.error(evt.toString());
            MetaContact oldMetaContact = evt.getOldParent();
            int oldGroupIndex
                = groups.indexOf(oldMetaContact.getParentMetaContactGroup());
            int oldContactIndex
                = contacts.get(oldGroupIndex).indexOf(oldMetaContact);

            if (oldContactIndex >= 0)
                updateStatus(oldGroupIndex, oldContactIndex);

            MetaContact newMetaContact = evt.getNewParent();
            int newGroupIndex
                = groups.indexOf(newMetaContact.getParentMetaContactGroup());
            int newContactIndex
                = contacts.get(newGroupIndex).indexOf(newMetaContact);

            if (newContactIndex >= 0)
                updateStatus(newGroupIndex, newContactIndex);
        }

        public void metaContactRemoved(MetaContactEvent evt)
        {
            logger.error(evt.toString());

            MetaContact metaContact = evt.getSourceMetaContact();
            int groupIndex
                = groups.indexOf(metaContact.getParentMetaContactGroup());
            int contactIndex = contacts.get(groupIndex).indexOf(metaContact);

            contacts.get(groupIndex).remove(contactIndex);

            dataChanged();
        }

        public void metaContactMoved(MetaContactMovedEvent evt)
        {
            logger.error(evt.toString());
            dataChanged();
        }

        public void metaContactGroupAdded(MetaContactGroupEvent evt)
        {
            logger.error(evt.toString());
            addContacts(evt.getSourceMetaContactGroup());
            dataChanged();
        }

        public void metaContactGroupModified(MetaContactGroupEvent evt)
        {
            logger.error(evt.toString());
            dataChanged();
        }

        public void metaContactGroupRemoved(MetaContactGroupEvent evt)
        {
            logger.error(evt.toString());
            removeContacts(evt.getSourceMetaContactGroup());

            dataChanged();
        }

        public void childContactsReordered(MetaContactGroupEvent evt)
        {
            logger.error(evt.toString());
            dataChanged();
        }

        public void metaContactModified(MetaContactModifiedEvent evt)
        {
            logger.error(evt.toString());
            dataChanged();
        }

        public void metaContactAvatarUpdated(
                MetaContactAvatarUpdateEvent evt)
        {
            logger.error(evt.toString());

            MetaContact metaContact = evt.getSourceMetaContact();
            int groupIndex
                = groups.indexOf(metaContact.getParentMetaContactGroup());
            int contactIndex = contacts.get(groupIndex).indexOf(metaContact);

            if (contactIndex >= 0)
                updateAvatar(groupIndex, contactIndex);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition)
        {
            System.err.println("GROUP POSITION====" + groupPosition);
            System.err.println("CHILD POSITION====" + childPosition);
            return contacts.get(groupPosition).get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition)
        {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent)
        {
            // Keeps reference to avoid future findViewById()
            ContactViewHolder contactViewHolder;

            if (convertView == null)
            {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                convertView = inflater.inflate( R.layout.contact_list_row,
                                                parent,
                                                false);

                contactViewHolder = new ContactViewHolder();
                contactViewHolder.displayName
                    = (TextView) convertView.findViewById(R.id.displayName);
                contactViewHolder.statusMessage
                    = (TextView) convertView.findViewById(R.id.statusMessage);
                contactViewHolder.avatarView
                    = (ImageView) convertView.findViewById(R.id.avatarIcon);
                contactViewHolder.statusView
                    = (ImageView) convertView.findViewById(R.id.statusIcon);
                contactViewHolder.callButton
                    = (ImageButton) convertView
                        .findViewById(R.id.contactCallButton);
                contactViewHolder.selectedBgView
                    = (ImageView) convertView
                        .findViewById(R.id.selectedBackgroundIcon);
                contactViewHolder.buttonSeparatorView
                    = (ImageView) convertView
                        .findViewById(R.id.buttonSeparatorView);
                contactViewHolder.callButtonLayout
                    = (View) convertView.findViewById(R.id.callButtonLayout);
                contactViewHolder.groupPosition = groupPosition;
                contactViewHolder.contactPosition = childPosition;

                convertView.setTag(contactViewHolder);
            }
            else
            {
                contactViewHolder = (ContactViewHolder) convertView.getTag();
            }

            contactViewHolder.callButtonLayout.setOnClickListener(
                new CallButtonClickListener(
                        contactViewHolder.groupPosition,
                        contactViewHolder.contactPosition));

            MetaContact metaContact
                = (MetaContact) getChild(groupPosition, childPosition);

            if (metaContact != null)
            {
                if (isContactSelected(metaContact))
                {
                    convertView.setBackgroundResource(
                        R.drawable.list_selection_gradient);
                    contactViewHolder.selectedBgView
                        .setVisibility(View.VISIBLE);
                }
                else
                {
                    convertView.setBackgroundResource(
                        R.drawable.contact_list_selector);

                    contactViewHolder.selectedBgView
                        .setVisibility(View.INVISIBLE);
                }

                // Set display name value.
                contactViewHolder.displayName
                    .setText(metaContact.getDisplayName());

                if (!StringUtils.isNullOrEmpty(getDisplayDetails(metaContact)))
                    contactViewHolder.statusMessage
                        .setText(getDisplayDetails(metaContact));
                else
                    contactViewHolder.statusMessage
                        .setText("");

                if (ChatSessionManager.getActiveChat(metaContact) != null)
                {
                    contactViewHolder.displayName
                        .setTypeface(Typeface.DEFAULT_BOLD);
                }
                else
                {
                    contactViewHolder.displayName
                        .setTypeface(Typeface.DEFAULT);
                }

                // Set avatar.
                Drawable avatarIcon = getAvatarDrawable(metaContact);

                if (avatarIcon != null)
                    contactViewHolder.avatarView.setImageDrawable(avatarIcon);
                else
                    contactViewHolder.avatarView
                        .setImageResource(R.drawable.avatar);

                // Set status icon.
                Drawable statusIcon = getStatusDrawable(metaContact);
                if (statusIcon != null)
                    contactViewHolder.statusView.setImageDrawable(statusIcon);

                // Show call button.
                boolean isShowVideoCall
                    = isShowButton( metaContact,
                                    OperationSetVideoTelephony.class);

                boolean isShowCall
                    = isShowButton( metaContact,
                                    OperationSetBasicTelephony.class);

                if (isShowVideoCall || isShowCall)
                {
                    contactViewHolder.callButtonLayout
                        .setVisibility(View.VISIBLE);

                    AndroidUtils.setOnTouchBackgroundEffect(getActivity(),
                        contactViewHolder.callButtonLayout);

                    if (isContactSelected(metaContact))
                    {
                        if (isShowVideoCall)
                            contactViewHolder.callButton
                                .setImageResource(
                                    R.drawable.video_call_selected);
                        else
                            contactViewHolder.callButton
                                .setImageResource(
                                    R.drawable.contact_call_selected);
                    }
                    else
                    {
                        if (isShowVideoCall)
                            contactViewHolder.callButton
                                .setImageResource(
                                    R.drawable.video_call);
                        else
                            contactViewHolder.callButton
                                .setImageResource(
                                    R.drawable.contact_call);
                    }
                }
                else
                {
                    contactViewHolder.callButtonLayout
                        .setVisibility(View.INVISIBLE);
                }
            }

            return convertView;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent)
        {
            // Keeps reference to avoid future findViewById()
            GroupViewHolder groupViewHolder;

            if (convertView == null)
            {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                convertView = inflater.inflate( R.layout.contact_list_group_row,
                                                parent,
                                                false);

                groupViewHolder = new GroupViewHolder();
                groupViewHolder.displayName
                    = (TextView) convertView.findViewById(R.id.displayName);

                convertView.setTag(groupViewHolder);
            }
            else
            {
                groupViewHolder = (GroupViewHolder) convertView.getTag();
            }

            MetaContactGroup metaGroup
                = (MetaContactGroup) getGroup(groupPosition);

            if (metaGroup != null)
            {
                if (metaGroup.equals(contactListService.getRoot()))
                {
                    groupViewHolder.displayName.setText(
                        R.string.service_gui_CONTACTS);
                }
                else
                {
                    // Set display name value.
                    groupViewHolder.displayName
                        .setText(metaGroup.getGroupName());
                }
            }

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition)
        {
            return contacts.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition)
        {
            return groups.get(groupPosition);
        }

        @Override
        public int getGroupCount()
        {
            return groups.size();
        }

        @Override
        public long getGroupId(int groupPosition)
        {
            return groupPosition;
        }

        @Override
        public boolean hasStableIds()
        {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition)
        {
            return true;
        }
    }

    private boolean isContactSelected(MetaContact metaContact)
    {
        return ChatSessionManager.getCurrentChatSession() != null
            && ChatSessionManager.getActiveChat(metaContact) != null
            && ChatSessionManager.getCurrentChatSession().equals(
            ChatSessionManager.getActiveChat(metaContact).getChatId());
    }

    /**
     * Returns the general status icon of the given MetaContact. Detects the
     * status using the priority status table. The priority is defined on
     * the "availability" factor and here the most "available" status is
     * returned.
     *
     * @return PresenceStatus The most "available" status from all
     * sub-contact statuses.
     */
    private static byte[] getStatusImage(MetaContact metaContact)
    {
        PresenceStatus status = null;
        Iterator<Contact> i = metaContact.getContacts();
        while (i.hasNext()) {
            Contact protoContact = i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (status == null)
                status = contactStatus;
            else
                status = (contactStatus.compareTo(status) > 0)
                        ? contactStatus
                        : status;
        }

        if (status != null)
            return StatusUtil.getContactStatusIcon(status);

        return null;
    }

    public static Drawable getStatusDrawable(MetaContact metaContact)
    {
        byte[] statusImage = getStatusImage(metaContact);

        if (statusImage != null)
            return AndroidImageUtil.drawableFromBytes(statusImage);

        return null;
    }

    public static Drawable getAvatarDrawable(MetaContact metaContact)
    {
        byte[] avatarImage = metaContact.getAvatar();

        if (avatarImage != null)
            return AndroidImageUtil.drawableFromBytes(avatarImage);

        return null;
    }

    static class ContactViewHolder
    {
        TextView displayName;
        TextView statusMessage;
        ImageView statusView;
        ImageView avatarView;
        ImageButton callButton;
        ImageView selectedBgView;
        ImageView buttonSeparatorView;
        View callButtonLayout;
        int groupPosition;
        int contactPosition;
    }

    static class GroupViewHolder
    {
        TextView displayName;
        int position;
    }

    private static boolean isShowButton(
                                    MetaContact metaContact,
                                    Class<? extends OperationSet> opSetClass)
    {
        Contact defaultContact
            = metaContact.getDefaultContact(opSetClass);

        return (defaultContact != null) ? true : false;
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
    private void startChatActivity(MetaContact metaContact)
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
            ((OSGiFragmentActivity) getActivity()).getSupportFragmentManager()
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
     * Returns the display details for the underlying <tt>MetaContact</tt>.
     * @return the display details for the underlying <tt>MetaContact</tt>
     */
    private static String getDisplayDetails(MetaContact metaContact)
    {
        String displayDetails = null;

        boolean subscribed = false;

        Iterator<Contact> protoContacts = metaContact.getContacts();

        String subscriptionDetails = null;

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetExtendedAuthorizations authOpSet
                = protoContact.getProtocolProvider()
                    .getOperationSet(OperationSetExtendedAuthorizations.class);

            if (authOpSet != null
                && authOpSet.getSubscriptionStatus(protoContact) != null
                && !authOpSet.getSubscriptionStatus(protoContact)
                    .equals(SubscriptionStatus.Subscribed))
            {
                SubscriptionStatus status
                    = authOpSet.getSubscriptionStatus(protoContact);

                if (status.equals(SubscriptionStatus.SubscriptionPending))
                    subscriptionDetails
                        = AndroidGUIActivator.getResourcesService()
                        .getI18NString("service.gui.WAITING_AUTHORIZATION");
                else if (status.equals(SubscriptionStatus.NotSubscribed))
                    subscriptionDetails
                        = AndroidGUIActivator.getResourcesService()
                        .getI18NString("service.gui.NOT_AUTHORIZED");
            }
            else if (protoContact.getStatusMessage() != null
                && protoContact.getStatusMessage().length() > 0)
            {
                subscribed = true;
                displayDetails = protoContact.getStatusMessage();
                break;
            }
            else
            {
                subscribed = true;
            }
        }

        if ((displayDetails == null
            || displayDetails.length() <= 0)
            && !subscribed
            && subscriptionDetails != null
            && subscriptionDetails.length() > 0)
            displayDetails = subscriptionDetails;

        return displayDetails;
    }

    private class CallButtonClickListener
        implements View.OnClickListener
    {
        private int groupPosition;
        private int childPosition;

        public CallButtonClickListener( int groupPosition,
                                        int childPosition)
        {
            this.groupPosition = groupPosition;
            this.childPosition = childPosition;
        }

        public void onClick(View view)
        {
            MetaContact metaContact
                = (MetaContact) contactListAdapter
                    .getChild(groupPosition, childPosition);

            if (metaContact != null)
                AndroidCallUtil
                    .createAndroidCall(
                        getActivity(),
                        view,
                        metaContact.getDefaultContact().getAddress());
        }
    }
}
