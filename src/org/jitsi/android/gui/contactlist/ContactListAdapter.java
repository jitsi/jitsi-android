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
 * 
 * @author Yana Stamcheva
 */
public class ContactListAdapter
    extends BaseExpandableListAdapter
    implements  MetaContactListListener,
                ContactPresenceStatusListener
{
    /**
     * The logger for this class.
     */
    private final Logger logger
        = Logger.getLogger(ContactListAdapter.class);

    final Object dataLock = new Object();

    /**
     * The list of contact list groups
     */
    private LinkedList<MetaContactGroup> groups;

    /**
     * The list of contact list groups
     */
    private LinkedList<MetaContactGroup> originalGroups;

    /**
     * The list of contacts.
     */
    private LinkedList<TreeSet<MetaContact>> contacts;

    /**
     * The list of contacts.
     */
    private LinkedList<TreeSet<MetaContact>> originalContacts;

    /**
     * Indicates if the adapter has been initialized.
     */
    private boolean isInitialized = false;

    /**
     * The <tt>MetaContactListService</tt>, which is the back end of this
     * contact list adapter.
     */
    private MetaContactListService contactListService;

    /**
     * The contact list view.
     */
    private final ContactListFragment contactListFragment;

    /**
     * The list view.
     */
    private final ExpandableListView contactListView;

    /**
     * The current filter query.
     */
    private String currentQuery;

    /**
     * Indicates if we're in an extended chat interface where the chat is shown
     * on the right of the contact list.
     */
    private boolean isExtendedChat;

    /**
     * Creates the contact list adapter.
     *
     * @param clFragment the parent <tt>ContactListFragment</tt>
     */
    public ContactListAdapter(ContactListFragment clFragment)
    {
        contactListFragment = clFragment;
        contactListView = contactListFragment.getContactListView();

        this.originalContacts = new LinkedList<TreeSet<MetaContact>>();
        this.contacts = new LinkedList<TreeSet<MetaContact>>();
        this.originalGroups = new LinkedList<MetaContactGroup>();
        this.groups = new LinkedList<MetaContactGroup>();
    }

    /**
     * Initializes the adapter data.
     *
     * @param clService the <tt>MetaContactListService</tt>, which is the
     * back end of this adapter
     */
    void initAdapterData(MetaContactListService clService)
    {
        contactListService = clService;

        addContacts(contactListService.getRoot());

        contactListService.addMetaContactListListener(this);

        isInitialized = true;

        expandAllGroups();

    }

    /**
     * Releases all resources used by this instance.
     */
    public void dispose()
    {
        if(contactListService != null)
            contactListService.removeMetaContactListListener(this);

        removeContacts(contactListService.getRoot());
    }

    /**
     * Indicates if the adapter has been already initialized.
     * @return
     */
    boolean isInitialized()
    {
        return isInitialized;
    }

    /**
     * Expands all contained groups.
     */
    private void expandAllGroups()
    {
        int count = getGroupCount();
        if (count <= 0)
            return;

        for (int position = 1; position <= count; position++)
            contactListView.expandGroup(position - 1);
    }

    /**
     * Adds all child contacts for the given <tt>group</tt>.
     *
     * @param group the group, which child contacts to add
     */
    private void addContacts(final MetaContactGroup group)
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                addGroup(group, false);

                Iterator<MetaContact> childContacts
                    = group.getChildContacts();

                while (childContacts.hasNext())
                {
                    addContact(group, childContacts.next());
                }

                Iterator<MetaContactGroup> subGroups = group.getSubgroups();
                while(subGroups.hasNext())
                {
                    addContacts(subGroups.next());
                }
            }
        });
    }

    /**
     * Removes the contacts contained in the given group.
     *
     * @param group the <tt>MetaContactGroup</tt>, which contacts we'd like to
     * remove
     */
    private void removeContacts(final MetaContactGroup group)
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                removeGroup(group, false);

                Iterator<MetaContact> childContacts
                    = group.getChildContacts();

                while (childContacts.hasNext())
                {
                    removeContact(group, childContacts.next());
                }

                Iterator<MetaContactGroup> subGroups = group.getSubgroups();
                while(subGroups.hasNext())
                {
                    removeContacts(subGroups.next());
                }
            }
        });
    }

    /**
     * Adds the given <tt>group</tt> to this list.
     *
     * @param group the <tt>MetaContactGroup</tt> to add
     * @param isSynchronized indicates if the call to this method is already
     * synchronized
     */
    private void addGroup(  final MetaContactGroup group,
                            final boolean isSynchronized)
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (!isSynchronized)
                    synchronized (dataLock)
                    {
                        addGroup(group, true);
                    }

                if (originalGroups.indexOf(group) < 0)
                {
                    originalGroups.add(group);

                    TreeSet<MetaContact> originalChildren
                        = new TreeSet<MetaContact>();

                    originalContacts.add(originalChildren);
                }

                if (isMatching(group, currentQuery)
                    && groups.indexOf(group) < 0)
                {
                    groups.add(group);

                    TreeSet<MetaContact> children
                        = new TreeSet<MetaContact>();

                    contacts.add(children);
                }
            }
        });
    }

    /**
     * Adds all child contacts for the given <tt>group</tt>.
     *
     * @param metaGroup the parent group of the child contact to add
     * @param metaContact the <tt>MetaContact</tt> to add
     */
    private void addContact(final MetaContactGroup metaGroup,
                            final MetaContact metaContact)
    {
        addContactStatusListener(metaContact, this);

        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                synchronized (dataLock)
                {
                    int origGroupIndex = originalGroups.indexOf(metaGroup);

                    int groupIndex = groups.indexOf(metaGroup);

                    boolean isMatchingQuery
                        = isMatching(metaContact, currentQuery);

                    if (origGroupIndex < 0
                        || (isMatchingQuery && groupIndex < 0))
                    {
                        addGroup(metaGroup, true);

                        // Update -1 index to new value, after group is added
                        groupIndex = groups.indexOf(metaGroup);
                    }

                    TreeSet<MetaContact> origContactList
                        = originalContacts.get(groupIndex);

                    if (origContactList != null
                        && getChildIndex(origContactList, metaContact) < 0)
                    {
                        origContactList.add(metaContact);
                    }

                    TreeSet<MetaContact> contactList
                        = contacts.get(groupIndex);

                    if (isMatchingQuery
                        && contactList != null
                        && getChildIndex(contactList, metaContact) < 0)
                    {
                        contactList.add(metaContact);
                    }
                }
            }
        });
    }

    private void removeGroup(   final MetaContactGroup metaGroup,
                                final boolean isSynchronized)
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (!isSynchronized)
                    synchronized (dataLock)
                    {
                        removeGroup(metaGroup, true);
                    }

                int origGroupIndex = originalGroups.indexOf(metaGroup);

                if (origGroupIndex >= 0)
                {
                    originalContacts.remove(origGroupIndex);
                    originalGroups.remove(metaGroup);
                }

                int groupIndex = groups.indexOf(metaGroup);

                if (groupIndex >= 0)
                {
                    contacts.remove(groupIndex);
                    groups.remove(metaGroup);
                }
            }
        });
    }

    /**
     * Removes the given <tt>metaContact</tt> from both the original and the
     * filtered list of this adapter.
     *
     * @param metaGroup the parent <tt>MetaContactGroup</tt> of the contact to
     * remove
     * @param metaContact the <tt>MetaContact</tt> to remove
     */
    private void removeContact( final MetaContactGroup metaGroup,
                                final MetaContact metaContact)
    {
        removeContactStatusListener(metaContact, this);

        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                synchronized (dataLock)
                {
                    int origGroupIndex = originalGroups.indexOf(metaGroup);

                    // Remove the contact from the original list.
                    if (origGroupIndex >= 0)
                    {
                        TreeSet<MetaContact> origContactList
                            = originalContacts.get(origGroupIndex);

                        origContactList.remove(metaContact);

                        if (origContactList.size() <= 0)
                            removeGroup(metaGroup, true);
                    }

                    // Remove the contact from the filtered list.
                    int groupIndex = groups.indexOf(metaGroup);
                    if (groupIndex >= 0)
                    {
                        TreeSet<MetaContact> contactList
                            = contacts.get(origGroupIndex);

                        contactList.remove(metaContact);

                        if (contactList.size() <= 0)
                            removeGroup(metaGroup, true);
                    }
                }
            }
        });
    }

    /**
     * Refreshes the list data.
     */
    private void dataChanged()
    {
        if (contactListFragment == null
            || contactListFragment.getActivity() == null)
            return;

        contactListFragment.getActivity().runOnUiThread(new Runnable()
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
    private void invalidateViews()
    {
        if (contactListFragment == null
            || contactListFragment.getActivity() == null
            || contactListView == null)
            return;

        contactListFragment.getActivity().runOnUiThread(new Runnable()
        {
            public void run()
            {
                contactListView.invalidateViews();
            }
        });
    }

    /**
     * Updates the display name of the given <tt>metaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which display name to update
     */
    private void updateDisplayName(final MetaContact metaContact)
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                synchronized (dataLock)
                {
                    int groupIndex = groups.indexOf(
                        metaContact.getParentMetaContactGroup());

                    if (groupIndex < 0)
                        return;

                    int contactIndex
                        = getChildIndex(contacts.get(groupIndex), metaContact);

                    if (contactIndex >= 0)
                        updateDisplayName(groupIndex, contactIndex);
                }
            }
        });
    }

    /**
     * Updates the contact display name.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     */
    private void updateDisplayName( final int groupIndex,
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
     * Updates the avatar of the given <tt>metaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which avatar to update
     */
    private void updateAvatar(final MetaContact metaContact)
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                synchronized(dataLock)
                {
                    int groupIndex = groups.indexOf(
                        metaContact.getParentMetaContactGroup());

                    if (groupIndex < 0)
                        return;

                    int contactIndex
                        = getChildIndex(contacts.get(groupIndex), metaContact);

                    if (contactIndex >= 0)
                        updateAvatar(groupIndex, contactIndex, metaContact);
                }
            }
        });
    }

    /**
     * Updates the contact avatar.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     */
    private void updateAvatar(  final int groupIndex,
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
     * Updates the status of the given <tt>metaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which status to update
     */
    private void updateStatus(final MetaContact metaContact)
    {
        contactListFragment.runOnUiThread(new Runnable()
        {
            public void run()
            {
                synchronized (dataLock)
                {
                    int groupIndex = groups.indexOf(
                        metaContact.getParentMetaContactGroup());

                    if (groupIndex < 0)
                        return;

                    int contactIndex
                        = getChildIndex(contacts.get(groupIndex), metaContact);

                    if (contactIndex >= 0)
                        updateStatus(groupIndex, contactIndex, metaContact);
                }
            }
        });
    }

    /**
     * Updates the contact status.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     * @param metaContact the <tt>MetaContact</tt> that has change status
     */
    private void updateStatus(  final int groupIndex,
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
     * Indicates that a <tt>MetaContact</tt> has been added to the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAdded(MetaContactEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("CONTACT ADDED: " + evt.getSourceMetaContact());

        addContact( evt.getParentGroup(),
                    evt.getSourceMetaContact());

        dataChanged();
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been removed from the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("CONTACT RENAMED: " + evt.getSourceMetaContact());

        updateDisplayName(evt.getSourceMetaContact());
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> has been added to
     * the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("PROTO CONTACT ADDED: " + evt.getNewParent());

        updateStatus(evt.getNewParent());
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> has been modified.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void protoContactModified(ProtoContactEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("PROTO CONTACT MODIFIED: "
                + evt.getProtoContact().getAddress());

        invalidateViews();
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> has been removed from
     * the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("PROTO CONTACT REMOVED: "
                + evt.getProtoContact().getAddress());

        updateStatus(evt.getOldParent());
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> has been moved.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("PROTO CONTACT MOVED: "
                + evt.getProtoContact().getAddress());

        updateStatus(evt.getOldParent());
        updateStatus(evt.getNewParent());
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been removed from the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRemoved(MetaContactEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("CONTACT REMOVED: " + evt.getSourceMetaContact());

        removeContact( evt.getParentGroup(),
                    evt.getSourceMetaContact());

        dataChanged();
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been moved.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactMoved(MetaContactMovedEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("CONTACT MOVED: " + evt.getSourceMetaContact());

        invalidateViews();
    }

    /**
     * Indicates that a <tt>MetaContactGroup</tt> has been added to the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("GROUP ADDED: " + evt.getSourceMetaContactGroup());

        addContacts(evt.getSourceMetaContactGroup());

        dataChanged();
    }

    /**
     * Indicates that a <tt>MetaContactGroup</tt> has been modified.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("GROUP MODIFIED: " + evt.getSourceMetaContactGroup());

        invalidateViews();
    }

    /**
     * Indicates that a <tt>MetaContactGroup</tt> has been removed from
     * the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("GROUP REMOVED: " + evt.getSourceMetaContactGroup());

        removeGroup(evt.getSourceMetaContactGroup(), false);

        dataChanged();
    }

    /**
     * Indicates that the child contacts of a given <tt>MetaContactGroup</tt>
     * has been reordered.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("CHILD CONTACTS REORDERED: "
                + evt.getSourceMetaContactGroup());

      MetaContactGroup group = evt.getSourceMetaContactGroup();

      int origGroupIndex = originalGroups.indexOf(group);
      int groupIndex = groups.indexOf(group);

      if (origGroupIndex >= 0)
      {
          TreeSet<MetaContact> contactList = originalContacts.get(groupIndex);

          if (contactList != null)
          {
              originalContacts.remove(contactList);
              originalContacts.add(groupIndex,
                                  new TreeSet<MetaContact>(contactList));
          }
      }

      if (groupIndex >= 0)
      {
          TreeSet<MetaContact> contactList = contacts.get(groupIndex);

          if (contactList != null)
          {
              contacts.remove(contactList);
              contacts.add(groupIndex, new TreeSet<MetaContact>(contactList));
          }
      }

        dataChanged();
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been modified.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactModified(MetaContactModifiedEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("META CONTACT MODIFIED: "
                + evt.getSourceMetaContact());

        invalidateViews();
    }

    /**
     * Indicates that a <tt>MetaContact</tt> avatar has changed and needs to
     * be updated.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAvatarUpdated(
            MetaContactAvatarUpdateEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug("META CONTACT AVATAR UPDATED: "
                + evt.getSourceMetaContact());

        if(contactListFragment.getActivity() != null)
            updateAvatar(evt.getSourceMetaContact());
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
        synchronized (dataLock)
        {
            if (contacts.size() <= 0)
                return null;

            Iterator<MetaContact> contactList
                = contacts.get(groupPosition).iterator();
            int i = 0;
            while (contactList.hasNext())
            {
                MetaContact metaContact = contactList.next();
                if (i == childPosition)
                {
                    return metaContact;
                }
                i++;
            }

            return null;
        }
    }

    private int getChildIndex(  TreeSet<MetaContact> contactList,
                                MetaContact metaContact)
    {
        Iterator<MetaContact> contactListIter
            = contactList.iterator();

        int i = 0;
        while (contactListIter.hasNext())
        {
            if (metaContact.equals(contactListIter.next()))
                return i;
            i++;
        }

        return -1;
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

                if (isExtendedChat)
                {
                    contactViewHolder.selectedBgView
                        .setVisibility(View.VISIBLE);
                    contactViewHolder.selectedBgView
                        .getLayoutParams().height = 30;
                }
            }
            else
            {
                convertView.setBackgroundResource(
                    R.drawable.contact_list_selector);

                if (isExtendedChat)
                {
                    contactViewHolder.selectedBgView
                        .setVisibility(View.INVISIBLE);
                    contactViewHolder.selectedBgView
                        .getLayoutParams().height = 0;
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
        synchronized (dataLock)
        {
            return contacts.get(groupPosition).size();
        }
    }

    /**
     * Returns the group at the given <tt>groupPosition</tt>.
     *
     * @param groupPosition the index of the group
     */
    @Override
    public Object getGroup(int groupPosition)
    {
        synchronized (dataLock)
        {
            return groups.get(groupPosition);
        }
    }

    /**
     * Returns the count of all groups contained in this adapter.
     */
    @Override
    public int getGroupCount()
    {
        synchronized (dataLock)
        {
            return groups.size();
        }
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
        Contact defaultContact
            = metaContact.getDefaultContact(opSetClass);

        return (defaultContact != null) ? true : false;
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
        int position;
    }

    /**
     * Filters list data to match the given <tt>query</tt>.
     *
     * @param query the query we'd like to match
     */
    public void filterData(final String query)
    {
        currentQuery = query.toLowerCase();

        groups.clear();
        contacts.clear();

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

            groups.addAll(originalGroups);
            contacts.addAll(originalContacts);
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

            for (MetaContactGroup metaGroup: originalGroups)
            {
                int groupIndex = originalGroups.indexOf(metaGroup);

                TreeSet<MetaContact> contactList
                    = originalContacts.get(groupIndex);

                TreeSet<MetaContact> filteredList
                    = new TreeSet<MetaContact>();

                for (MetaContact metaContact: contactList)
                {
                    if (isMatching(metaContact, query))
                        filteredList.add(metaContact);
                }

                if (filteredList.size() > 0)
                {
                    groups.add(metaGroup);
                    contacts.add(filteredList);
                }
            }
        }

        notifyDataSetChanged();

        expandAllGroups();
    }

    /**
     * Checks if the given <tt>metaContact</tt> is matching the given
     * <tt>query</tt>.
     * A <tt>MetaContact</tt> would be matching the filter if one of the
     * following is true:<br>
     * - its display name contains the filter string
     * - at least one of its child protocol contacts has a display name or an
     * address that contains the filter string.
     *
     * @param metaContact the <tt>MetaContact</tt> to check
     * @param query the query string to check for matches
     * @return <tt>true</tt> to indicate that the given <tt>metaContact</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    private boolean isMatching(MetaContact metaContact, String query)
    {
        if (query == null || query.length() <= 0)
            return true;

        if (metaContact.getDisplayName().contains(query))
            return true;
        else
        {
            Iterator<Contact> contacts = metaContact.getContacts();
            while (contacts.hasNext())
            {
                Contact contact = contacts.next();

                if (contact.getDisplayName().contains(query)
                    || contact.getAddress().contains(query))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the given <tt>metaGroup</tt> is matching the current filter. A
     * group is matching the current filter only if it contains at least one
     * child <tt>MetaContact</tt>, which is matching the current filter.
     *
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @param query the query string to check for matches
     * @return <tt>true</tt> to indicate that the given <tt>metaGroup</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(MetaContactGroup metaGroup, String query)
    {
        if (query == null || query.length() <= 0)
            return true;

        Iterator<MetaContact> contacts = metaGroup.getChildContacts();

        while (contacts.hasNext())
        {
            MetaContact metaContact = contacts.next();

            if (isMatching(metaContact, query))
                return true;
        }
        return false;
    }

    @Override
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent event)
    {
        Contact sourceContact = event.getSourceContact();

        if (logger.isDebugEnabled())
            logger.debug("Contact presence status changed: "
                + sourceContact.getAddress());

        MetaContact metaContact
            = contactListService.findMetaContactByContact(sourceContact);

        if (metaContact != null)
            updateStatus(metaContact);
    }

    /**
     * Adds the given <tt>MessageListener</tt> to listen for message events in
     * this chat session.
     *
     * @param metaContact the <tt>MetaContact</tt> for which we add the listener
     * @param l the <tt>MessageListener</tt> to add
     */
    public void addContactStatusListener(   MetaContact metaContact,
                                            ContactPresenceStatusListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetPresence presenceOpSet
                = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetPresence.class);

            if (presenceOpSet != null)
            {
                presenceOpSet.addContactPresenceStatusListener(l);
            }
        }
    }

    /**
     * Removes the given <tt>MessageListener</tt> from this chat session.
     *
     * @param metaContact the <tt>MetaContact</tt> for which we remove the
     * listener
     * @param l the <tt>MessageListener</tt> to remove
     */
    public void removeContactStatusListener(MetaContact metaContact,
                                            ContactPresenceStatusListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetPresence presenceOpSet
                = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetPresence.class);

            if (presenceOpSet != null)
            {
                presenceOpSet.removeContactPresenceStatusListener(l);
            }
        }
    }
}