/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.util.*;
import org.jitsi.util.Logger;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

/**
 * Class is responsible for managing contact list <tt>View</tt>. Binds
 * <tt>ContactListModel</tt> to <tt>ContactListFragment</tt>.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ContactListAdapter
    extends BaseExpandableListAdapter
{
    /**
     * The logger for this class.
     */
    private final Logger logger
        = Logger.getLogger(ContactListAdapter.class);

    /**
     * The contact list view.
     */
    private final ContactListFragment contactListFragment;

    /**
     * The list view.
     */
    private final ExpandableListView contactListView;

    /**
     * Indicates if the adapter has been initialized.
     */
    private boolean isInitialized = false;

    /**
     * Indicates if we're in an extended chat interface where the chat is shown
     * on the right of the contact list.
     */
    private boolean isExtendedChat = AndroidUtils.isTablet();

    private final ContactListModel contactListModel;

    /**
     * Creates the contact list adapter.
     *
     * @param clFragment the parent <tt>ContactListFragment</tt>
     */
    public ContactListAdapter(ContactListFragment clFragment)
    {
        contactListFragment = clFragment;
        contactListView = contactListFragment.getContactListView();
        contactListModel = new ContactListModel(this);
    }

    /**
     * Initializes the adapter data.
     */
    void initAdapterData()
    {
        contactListModel.initModelData(
            ServiceUtils.getService(
                AndroidGUIActivator.bundleContext,
                MetaContactListService.class);

        isInitialized = true;

        expandAllGroups();

    }

    /**
     * Releases all resources used by this instance.
     */
    public void dispose()
    {
        contactListModel.dispose();
    }

    /**
     * Indicates if the adapter has been already initialized.
     * @return <tt>true</tt> if the adapter has been already initialized.
     */
    boolean isInitialized()
    {
        return isInitialized;
    }

    /**
     * Expands all contained groups.
     */
    void expandAllGroups()
    {
        int count = getGroupCount();
        if (count <= 0)
            return;

        for (int position = 1; position <= count; position++)
            contactListView.expandGroup(position - 1);
    }

    /**
     * Refreshes the list data.
     */
    void dataChanged()
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Refreshes the list view.
     */
    void invalidateViews()
    {
        if (contactListView == null)
            return;

        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                contactListView.invalidateViews();
            }
        });
    }

    /**
     * Updates the contact display name.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     */
    void updateDisplayName( final int groupIndex,
                            final int contactIndex)
    {
        int firstIndex = contactListView.getFirstVisiblePosition();

        View contactView = contactListView
            .getChildAt(
                getListIndex(groupIndex, contactIndex) - firstIndex);

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

    /**
     * Updates the contact avatar.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     */
    void updateAvatar( final int groupIndex,
                       final int contactIndex,
                       final MetaContact metaContact)
    {
        int firstIndex = contactListView.getFirstVisiblePosition();

        View contactView = contactListView
            .getChildAt(getListIndex(groupIndex, contactIndex) - firstIndex);

        if (contactView != null)
        {
            ImageView avatarView
                = (ImageView) contactView
                    .findViewById(R.id.avatarIcon);

            if (avatarView != null)
                setAvatar(avatarView, metaContact);
        }
    }

    /**
     * Updates the contact status.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     * @param metaContact the <tt>MetaContact</tt> that has change status
     */
    void updateStatus( final int groupIndex,
                       final int contactIndex,
                       final MetaContact metaContact)
    {
        int firstIndex = contactListView.getFirstVisiblePosition();

        View contactView = contactListView
            .getChildAt(
                getListIndex(groupIndex, contactIndex) - firstIndex);

        if (contactView != null)
        {
            ImageView statusView
                = (ImageView) contactView
                    .findViewById(R.id.contactStatusIcon);

            if(statusView == null)
            {
                logger.warn("No status view found for "+metaContact);
                return;
            }

            setStatus(statusView, metaContact);
        }
    }

    /**
     * Returns the flat list index for the given <tt>groupIndex</tt> and
     * <tt>contactIndex</tt>.
     *
     * @param groupIndex the index of the group
     * @param contactIndex the index of the contact
     * @return an int representing the flat list index for the given
     * <tt>groupIndex</tt> and <tt>contactIndex</tt>
     */
    public int getListIndex(int groupIndex, int contactIndex)
    {
        int lastIndex = contactListView.getLastVisiblePosition();

        for (int i = 0; i <= lastIndex; i++)
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

    /**
     * Finds group index for given <tt>MetaContactGroup</tt>.
     * @param group the group for which we need the index.
     * @return index of given <tt>MetaContactGroup</tt> or -1 if not found
     */
    int getGroupIndex(MetaContactGroup group)
    {
        return groups.indexOf(group);
    }

    /**
     * Finds <tt>MetaContact</tt> index in <tt>MetaContactGroup</tt> identified
     * by given <tt>groupIndex</tt>.
     * @param groupIndex index of group we want to search.
     * @param contact the <tt>MetaContact</tt> to find inside the group.
     * @return index of <tt>MetaContact</tt> inside group identified by given
     *         group index.
     */
    int getChildIndex(int groupIndex, MetaContact contact)
    {
        return getChildIndex(getContactList(groupIndex), contact);
    }

    /**
     * Returns the contained object on the given <tt>groupPosition</tt> and
     * <tt>childPosition</tt>.
     *
     * @param groupPosition the index of the group
     * @param childPosition the index of the child
     * @return the contained object on the given <tt>groupPosition</tt> and
     * <tt>childPosition</tt>
     */
    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return contactListModel.getChild(groupPosition, childPosition);
    }

    /**
     * Returns the identifier of the child contained on the given
     * <tt>groupPosition</tt> and <tt>childPosition</tt>.
     *
     * @param groupPosition the index of the group
     * @param childPosition the index of the child
     * @return the identifier of the child contained on the given
     * <tt>groupPosition</tt> and <tt>childPosition</tt>
     */
    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return childPosition;
    }

    /**
     * Returns the child view for the given <tt>groupPosition</tt>, 
     * <tt>childPosition</tt>.
     *
     * @param groupPosition the group position of the desired view
     * @param childPosition the child position of the desired view
     * @param isLastChild indicates if this is the last child
     * @param convertView the view to fill with data
     * @param parent the parent view group
     */
    @Override
    public View getChildView(int groupPosition, int childPosition,
        boolean isLastChild, View convertView, ViewGroup parent)
    {
        // Keeps reference to avoid future findViewById()
        ContactViewHolder contactViewHolder;

        if (convertView == null)
        {
            LayoutInflater inflater
                = contactListFragment.getActivity().getLayoutInflater();
            convertView = inflater.inflate( R.layout.contact_list_row,
                                            parent,
                                            false);

            if (isExtendedChat)
            {
                // Show shadows on the right side for tablet layout
                ViewUtil.ensureVisible(
                    convertView, R.id.rightShadowTop, true);
                ViewUtil.ensureVisible(
                    convertView, R.id.rightShadowBottom, true);
            }

            contactViewHolder = new ContactViewHolder();
            contactViewHolder.displayName
                = (TextView) convertView.findViewById(R.id.displayName);
            contactViewHolder.statusMessage
                = (TextView) convertView.findViewById(R.id.statusMessage);
            contactViewHolder.avatarView
                = (ImageView) convertView.findViewById(R.id.avatarIcon);
            contactViewHolder.statusView
                = (ImageView) convertView.findViewById(R.id.contactStatusIcon);
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
                = convertView.findViewById(R.id.callButtonLayout);
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

                if (isExtendedChat)
                {
                    contactViewHolder.selectedBgView
                        .setVisibility(View.VISIBLE);
                }
            }
            else
            {
                convertView.setBackgroundResource(
                    R.drawable.contact_list_selector);

                if (isExtendedChat)
                {
                    contactViewHolder.selectedBgView
                        .setVisibility(View.GONE);
                }
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
            setAvatar(contactViewHolder.avatarView, metaContact);
            setStatus(contactViewHolder.statusView, metaContact);

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

                AndroidUtils.setOnTouchBackgroundEffect(
                    contactListFragment.getActivity(),
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

    /**
     * Returns the group view for the given <tt>groupPosition</tt>.
     *
     * @param groupPosition the group position of the desired view
     * @param isExpanded indicates if the view is currently expanded
     * @param convertView the view to fill with data
     * @param parent the parent view group
     */
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
        View convertView, ViewGroup parent)
    {
        // Keeps reference to avoid future findViewById()
        GroupViewHolder groupViewHolder;

        if (convertView == null)
        {
            LayoutInflater inflater
                = contactListFragment.getActivity().getLayoutInflater();
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
            if (metaGroup.equals(contactListModel.getRoot()))
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

    /**
     * Returns the count of children contained in the group given by the
     * <tt>groupPosition</tt>.
     *
     * @param groupPosition the index of the group, which children we would
     * like to count
     */
    @Override
    public int getChildrenCount(int groupPosition)
    {
        return contactListModel.getChildrenCount(groupPosition);
    }

    /**
     * Returns the group at the given <tt>groupPosition</tt>.
     *
     * @param groupPosition the index of the group
     */
    @Override
    public Object getGroup(int groupPosition)
    {
        return contactListModel.getGroup(groupPosition);
    }

    /**
     * Returns the count of all groups contained in this adapter.
     */
    @Override
    public int getGroupCount()
    {
        return contactListModel.getGroupCount();
    }

    /**
     * Returns the identifier of the group given by <tt>groupPosition</tt>.
     *
     * @param groupPosition the index of the group, which identifier we're
     * looking for
     */
    @Override
    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    /**
     * 
     */
    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    /**
     * Indicates that all children are selectable.
     */
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }

    void runOnUiThread(Runnable runnable)
    {
        contactListFragment.runOnUiThread(runnable);
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
                = (MetaContact) getChild(groupPosition, childPosition);

            if (metaContact != null)
                AndroidCallUtil
                    .createAndroidCall(
                        contactListFragment.getActivity(),
                        view,
                        metaContact.getDefaultContact().getAddress());
        }
    }

    private boolean isContactSelected(MetaContact metaContact)
    {
        return ChatSessionManager.getCurrentChatId() != null
            && ChatSessionManager.getActiveChat(metaContact) != null
            && ChatSessionManager.getCurrentChatId().equals(
            ChatSessionManager.getActiveChat(metaContact).getChatId());
    }

    /**
     * Returns the display details for the underlying <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which details we're looking
     * for
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

    private static boolean isShowButton(
            MetaContact metaContact,
            Class<? extends OperationSet> opSetClass)
    {
        return metaContact.getDefaultContact(opSetClass) != null;
    }

    /**
     * Sets the avatar icon of the action bar.
     *
     * @param avatarView the avatar image view
     * @param metaContact the <tt>MetaContact</tt>, for which we set an avatar
     */
    public void setAvatar(ImageView avatarView, MetaContact metaContact)
    {
        Drawable avatarImage = getAvatarDrawable(metaContact);

        if (avatarImage == null)
        {
            avatarImage = JitsiApplication.getAppResources()
                .getDrawable(R.drawable.avatar);
        }

        avatarView.setImageDrawable(avatarImage);
    }

    /**
     * Sets the avatar icon of the action bar.
     *
     * @param statusView the status image view
     * @param metaContact the <tt>MetaContact</tt>, for which we set an avatar
     */
    public void setStatus(ImageView statusView, MetaContact metaContact)
    {
        byte[] statusImage = getStatusImage(metaContact);

        statusView.setImageDrawable(
            AndroidImageUtil.drawableFromBytes(statusImage));
    }

    /**
     * Returns the status <tt>Drawable</tt> for the given <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which status drawable we're
     * looking for
     * @return a <tt>Drawable</tt> object representing the status of the given
     * <tt>MetaContact</tt>
     */
    public static Drawable getStatusDrawable(MetaContact metaContact)
    {
        byte[] statusImage = getStatusImage(metaContact);

        if (statusImage != null)
            return AndroidImageUtil.drawableFromBytes(statusImage);

        return null;
    }

    /**
     * Returns the status <tt>Drawable</tt> for the given <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which status drawable we're
     * looking for
     * @return a <tt>Drawable</tt> object representing the status of the given
     * <tt>MetaContact</tt>
     */
    public static Drawable getAvatarDrawable(MetaContact metaContact)
    {
        byte[] avatarImage = metaContact.getAvatar();

        if (avatarImage != null)
            return AndroidImageUtil
                    .scaledDrawableFromBytes(avatarImage, 78, 78);

        return null;
    }

    /**
     * Returns the array of bytes representing the status image of the given
     * <tt>MetaContact</tt>.
     *
     * @return the array of bytes representing the status image of the given
     * <tt>MetaContact</tt>
     */
    private static byte[] getStatusImage(MetaContact metaContact)
    {
        PresenceStatus status = null;
        Iterator<Contact> contactsIter = metaContact.getContacts();
        while (contactsIter.hasNext())
        {
            Contact protoContact = contactsIter.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (status == null)
                status = contactStatus;
            else
                status = (contactStatus.compareTo(status) > 0)
                        ? contactStatus
                        : status;
        }

        return StatusUtil.getContactStatusIcon(status);
    }

    private static class ContactViewHolder
    {
        TextView displayName;
        TextView statusMessage;
        ImageView avatarView;
        ImageView statusView;
        ImageButton callButton;
        ImageView selectedBgView;
        ImageView buttonSeparatorView;
        View callButtonLayout;
        int groupPosition;
        int contactPosition;
    }

    private static class GroupViewHolder
    {
        TextView displayName;
    }

    /**
     * Filters list data to match the given <tt>query</tt>.
     *
     * @param query the query we'd like to match
     */
    public void filterData(final String query)
    {
        RelativeLayout callSearchLayout
            = (RelativeLayout) contactListFragment.getView()
                .findViewById(R.id.callSearchLayout);

        if (query.isEmpty())
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
                callButton.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        AndroidCallUtil
                            .createAndroidCall(
                                contactListFragment.getActivity(),
                                callButton,
                                query);
                    }
                });
            }
        }

        contactListModel.filterData(query);
    }
}