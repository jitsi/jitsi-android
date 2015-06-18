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
package org.jitsi.android.gui.contactlist.model;

import android.os.*;
import android.os.Handler;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.Logger;

import java.util.*;
import java.util.regex.*;

/**
 * Contact list model is responsible for caching current contact list obtained
 * from contact sources.(It will apply contact source filters which result in
 * different output model).
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class MetaContactListAdapter
    extends BaseContactListAdapter
    implements MetaContactListListener,
               ContactPresenceStatusListener,
               UIGroupRenderer
{
    /**
     * The logger for this class.
     */
    private final Logger logger
        = Logger.getLogger(MetaContactListAdapter.class);

    /**
     * UI thread handler used to call all operations that access data model.
     * This guarantees that it's accessed from the single thread.
     */
    private final Handler uiHandler = OSGiActivity.uiHandler;

    /**
     * The current filter query.
     */
    private String currentQuery;

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
     * The <tt>MetaContactListService</tt>, which is the back end of this
     * contact list adapter.
     */
    private MetaContactListService contactListService;

    /**
     * <tt>MetaContactRenderer</tt> instance used by this adapter.
     */
    private MetaContactRenderer contactRenderer;

    public MetaContactListAdapter(ContactListFragment contactListFragment)
    {
        super(contactListFragment);

        this.originalContacts = new LinkedList<TreeSet<MetaContact>>();
        this.contacts = new LinkedList<TreeSet<MetaContact>>();
        this.originalGroups = new LinkedList<MetaContactGroup>();
        this.groups = new LinkedList<MetaContactGroup>();
    }

    /**
     * Initializes the adapter data.
     *
     */
    public void initModelData()
    {
        contactListService
            = ServiceUtils.getService(
                AndroidGUIActivator.bundleContext,
                MetaContactListService.class);

        addContacts(contactListService.getRoot());

        contactListService.addMetaContactListListener(this);
    }

    /**
     * Releases all resources used by this instance.
     */
    public void dispose()
    {
        if(contactListService == null)
            return;

        contactListService.removeMetaContactListListener(this);

        removeContacts(contactListService.getRoot());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UIGroupRenderer getGroupRenderer(int groupPosition)
    {
        return this;
    }

    /**
     * Assert that current thread is the UI thread.
     */
    private void assertUIThread()
    {
        assert Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    /**
     * Returns the group at the given <tt>groupPosition</tt>.
     *
     * @param groupPosition the index of the group
     */
    @Override
    public Object getGroup(int groupPosition)
    {
        assertUIThread();

        return groups.get(groupPosition);
    }

    /**
     * Returns the count of all groups contained in this adapter.
     */
    @Override
    public int getGroupCount()
    {
        assertUIThread();

        return groups.size();
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
        assertUIThread();

        return getContactList(groupPosition).size();
    }

    @Override
    public UIContactRenderer getContactRenderer(int groupPosition)
    {
        if(contactRenderer == null)
        {
            contactRenderer = new MetaContactRenderer();
        }
        return contactRenderer;
    }

    /**
     * Get group contact list from original contact list.
     * @param groupIndex contact group index.
     * @return group contact list from original contact list.
     */
    private TreeSet<MetaContact> getOriginalCList(int groupIndex)
    {
        if(groupIndex >= 0 && groupIndex < originalContacts.size())
        {
            return originalContacts.get(groupIndex);
        }
        else
        {
            logger.warn("Get original contact list for idx: " + groupIndex
                            + ", list size: " + originalContacts.size(),
                        new Throwable());
            return null;
        }
    }

    /**
     * Get group contact list from filtered contact list.
     * @param groupIndex contact group index.
     * @return group contact list from filtered contact list.
     */
    private TreeSet<MetaContact> getContactList(int groupIndex)
    {
        if(groupIndex >= 0 && groupIndex < contacts.size())
        {
            return contacts.get(groupIndex);
        }
        else
        {
            logger.warn("Get contact list for idx: " + groupIndex
                            + ", list size: " + contacts.size(),
                        new Throwable());
            return null;
        }
    }

    /**
     * Finds group index for given <tt>MetaContactGroup</tt>.
     * @param group the group for which we need the index.
     * @return index of given <tt>MetaContactGroup</tt> or -1 if not found
     */
    public int getGroupIndex(MetaContactGroup group)
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
    public int getChildIndex(int groupIndex, MetaContact contact)
    {
        return getChildIndex(getContactList(groupIndex), contact);
    }

    /**
     * Adds all child contacts for the given <tt>group</tt>.
     *
     * @param group the group, which child contacts to add
     */
    private void addContacts(final MetaContactGroup group)
    {
        addGroup(group);

        Iterator<MetaContact> childContacts
            = group.getChildContacts();

        while (childContacts.hasNext())
        {
            addContact(group, childContacts.next());
        }

        Iterator<MetaContactGroup> subGroups = group.getSubgroups();
        while (subGroups.hasNext())
        {
            addContacts(subGroups.next());
        }
    }

    /**
     * Removes the contacts contained in the given group.
     *
     * @param group the <tt>MetaContactGroup</tt>, which contacts we'd like to
     * remove
     */
    private void removeContacts(final MetaContactGroup group)
    {
        removeGroup(group);

        Iterator<MetaContact> childContacts
            = group.getChildContacts();

        while (childContacts.hasNext())
        {
            removeContact(group, childContacts.next());
        }

        Iterator<MetaContactGroup> subGroups = group.getSubgroups();
        while (subGroups.hasNext())
        {
            removeContacts(subGroups.next());
        }
    }

    /**
     * Adds the given <tt>group</tt> to this list.
     *
     * @param group the <tt>MetaContactGroup</tt> to add
     */
    private void addGroup(final MetaContactGroup group)
    {
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

        int origGroupIndex = originalGroups.indexOf(metaGroup);

        int groupIndex = groups.indexOf(metaGroup);

        boolean isMatchingQuery
            = isMatching(metaContact, currentQuery);

        if (origGroupIndex < 0
            || (isMatchingQuery && groupIndex < 0))
        {
            addGroup(metaGroup);

            // Update -1 index to new value, after group is added
            groupIndex = groups.indexOf(metaGroup);
        }

        TreeSet<MetaContact> origContactList
            = getOriginalCList(groupIndex);

        if (origContactList != null
            && getChildIndex(origContactList, metaContact) < 0)
        {
            origContactList.add(metaContact);
        }

        TreeSet<MetaContact> contactList
            = getContactList(groupIndex);

        if (isMatchingQuery
            && contactList != null
            && getChildIndex(contactList, metaContact) < 0)
        {
            contactList.add(metaContact);
        }
    }

    private void removeGroup(final MetaContactGroup metaGroup)
    {
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

        int origGroupIndex = originalGroups.indexOf(metaGroup);

        // Remove the contact from the original list.
        if (origGroupIndex >= 0)
        {
            TreeSet<MetaContact> origContactList
                = getOriginalCList(origGroupIndex);

            origContactList.remove(metaContact);

            if (origContactList.size() <= 0)
                removeGroup(metaGroup);
        }

        // Remove the contact from the filtered list.
        int groupIndex = groups.indexOf(metaGroup);
        if (groupIndex >= 0)
        {
            TreeSet<MetaContact> contactList
                = getContactList(groupIndex);

            contactList.remove(metaContact);

            if (contactList.size() <= 0)
                removeGroup(metaGroup);
        }
    }

    /**
     * Updates the display name of the given <tt>metaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which display name to update
     */
    private void updateDisplayName(final MetaContact metaContact)
    {
        int groupIndex = groups.indexOf(
            metaContact.getParentMetaContactGroup());

        if (groupIndex < 0)
            return;

        int contactIndex
            = getChildIndex(getContactList(groupIndex),
                            metaContact);

        if (contactIndex >= 0)
            updateDisplayName(groupIndex, contactIndex);
    }

    /**
     * Updates the avatar of the given <tt>metaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which avatar to update
     */
    private void updateAvatar(final MetaContact metaContact)
    {
        int groupIndex = groups.indexOf(
            metaContact.getParentMetaContactGroup());

        if (groupIndex < 0)
            return;

        int contactIndex
            = getChildIndex(getContactList(groupIndex), metaContact);

        if (contactIndex >= 0)
            updateAvatar(groupIndex, contactIndex, metaContact);
    }

    /**
     * Updates the status of the given <tt>metaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which status to update
     */
    private void updateStatus(final MetaContact metaContact)
    {
        int groupIndex = groups.indexOf(
            metaContact.getParentMetaContactGroup());

        if (groupIndex < 0)
            return;

        int contactIndex
            = getChildIndex(getContactList(groupIndex), metaContact);

        if (contactIndex >= 0)
            updateStatus(groupIndex, contactIndex, metaContact);
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been added to the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAdded(final MetaContactEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "CONTACT ADDED: " + evt.getSourceMetaContact());

                addContact(evt.getParentGroup(),
                           evt.getSourceMetaContact());

                notifyDataSetChanged();
            }
        });
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been removed from the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRenamed(final MetaContactRenamedEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("CONTACT RENAMED: "
                                     + evt.getSourceMetaContact());

                updateDisplayName(evt.getSourceMetaContact());
            }
        });
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> has been added to
     * the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void protoContactAdded(final ProtoContactEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("PROTO CONTACT ADDED: " + evt.getNewParent());

                updateStatus(evt.getNewParent());
            }
        });
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> has been modified.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void protoContactModified(final ProtoContactEvent evt)
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
    public void protoContactRemoved(final ProtoContactEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("PROTO CONTACT REMOVED: "
                                     + evt.getProtoContact().getAddress());

                updateStatus(evt.getOldParent());
            }
        });
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> has been moved.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void protoContactMoved(final ProtoContactEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("PROTO CONTACT MOVED: "
                                     + evt.getProtoContact().getAddress());

                updateStatus(evt.getOldParent());
                updateStatus(evt.getNewParent());
            }
        });
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been removed from the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRemoved(final MetaContactEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("CONTACT REMOVED: "
                                     + evt.getSourceMetaContact());

                removeContact( evt.getParentGroup(),
                               evt.getSourceMetaContact());

                notifyDataSetChanged();
            }
        });
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been moved.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactMoved(final MetaContactMovedEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("CONTACT MOVED: "
                                     + evt.getSourceMetaContact());

                MetaContactGroup oldParent = evt.getOldParent();
                MetaContactGroup newParent = evt.getNewParent();
                // Modify original group
                int oldGroupIdx = originalGroups.indexOf(oldParent);
                int newGroupIdx = originalGroups.indexOf(newParent);
                if (oldGroupIdx < 0 || newGroupIdx < 0)
                {
                    logger.error(
                        "Move group error - original list, srcGroupIdx: "
                            + oldGroupIdx + ", dstGroupIdx: " + newGroupIdx);
                } else
                {
                    TreeSet<MetaContact> srcGroup
                        = getOriginalCList(oldGroupIdx);
                    if (srcGroup != null)
                    {
                        srcGroup.remove(evt.getSourceMetaContact());
                    }
                    TreeSet<MetaContact> dstGroup
                        = getOriginalCList(newGroupIdx);
                    if (dstGroup != null)
                    {
                        dstGroup.add(evt.getSourceMetaContact());
                    }
                }
                // Move search results group
                oldGroupIdx = groups.indexOf(oldParent);
                newGroupIdx = groups.indexOf(newParent);
                if (oldGroupIdx < 0 || newGroupIdx < 0)
                {
                    logger.error(
                        "Move group error, srcGroupIdx: "
                            + oldGroupIdx + ", dstGroupIdx: " + newGroupIdx);
                } else
                {
                    TreeSet<MetaContact> srcGroup
                        = getContactList(oldGroupIdx);
                    if (srcGroup != null)
                    {
                        srcGroup.remove(evt.getSourceMetaContact());
                    }
                    TreeSet<MetaContact> dstGroup
                        = getContactList(newGroupIdx);
                    if (dstGroup != null)
                    {
                        dstGroup.add(evt.getSourceMetaContact());
                    }
                }
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Indicates that a <tt>MetaContactGroup</tt> has been added to the list.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactGroupAdded(final MetaContactGroupEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("GROUP ADDED: "
                                     + evt.getSourceMetaContactGroup());

                addContacts(evt.getSourceMetaContactGroup());

                notifyDataSetChanged();
            }
        });
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
    public void metaContactGroupRemoved(final MetaContactGroupEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("GROUP REMOVED: "
                                     + evt.getSourceMetaContactGroup());

                removeGroup(evt.getSourceMetaContactGroup());

                notifyDataSetChanged();
            }
        });
    }

    /**
     * Indicates that the child contacts of a given <tt>MetaContactGroup</tt>
     * has been reordered.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void childContactsReordered(final MetaContactGroupEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("CHILD CONTACTS REORDERED: "
                                     + evt.getSourceMetaContactGroup());

                MetaContactGroup group = evt.getSourceMetaContactGroup();

                int origGroupIndex = originalGroups.indexOf(group);
                int groupIndex = groups.indexOf(group);

                if (origGroupIndex >= 0)
                {
                    TreeSet<MetaContact> contactList
                        = getOriginalCList(origGroupIndex);

                    if (contactList != null)
                    {
                        originalContacts.remove(contactList);
                        originalContacts.add(
                            origGroupIndex,
                            new TreeSet<MetaContact>(contactList));
                    }
                }

                if (groupIndex >= 0)
                {
                    TreeSet<MetaContact> contactList
                        = getContactList(groupIndex);

                    if (contactList != null)
                    {
                        contacts.remove(contactList);
                        contacts.add(groupIndex,
                                     new TreeSet<MetaContact>(contactList));
                    }
                }

                notifyDataSetChanged();
            }
        });
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
        final MetaContactAvatarUpdateEvent evt)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (logger.isDebugEnabled())
                    logger.debug("META CONTACT AVATAR UPDATED: "
                                     + evt.getSourceMetaContact());

                updateAvatar(evt.getSourceMetaContact());
            }
        });
    }

    /**
     * Returns the contained object on the given <tt>groupPosition</tt> and
     * <tt>childPosition</tt>.
     * Note that this method must be called on UI thread.
     *
     * @param groupPosition the index of the group
     * @param childPosition the index of the child
     * @return the contained object on the given <tt>groupPosition</tt> and
     * <tt>childPosition</tt>
     */
    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        if (contacts.size() <= 0)
            return null;

        Iterator<MetaContact> contactList
            = getContactList(groupPosition).iterator();
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
     * Filters list data to match the given <tt>query</tt>.
     * Note that this method must be called on UI thread.
     *
     * @param query the query we'd like to match
     */
    public void filterData(final String query)
    {
        assertUIThread();

        currentQuery = query.toLowerCase();

        groups.clear();
        contacts.clear();

        if (query.isEmpty())
        {
            groups.addAll(originalGroups);
            contacts.addAll(originalContacts);
        }
        else
        {
            for (MetaContactGroup metaGroup: originalGroups)
            {
                int groupIndex = originalGroups.indexOf(metaGroup);

                TreeSet<MetaContact> contactList
                    = getOriginalCList(groupIndex);

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

        Pattern queryPattern
            = Pattern.compile(query,
                              Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

        if (queryPattern.matcher(
                metaContact.getDisplayName()).find())
            return true;
        else
        {
            Iterator<Contact> contacts = metaContact.getContacts();
            while (contacts.hasNext())
            {
                Contact contact = contacts.next();

                if (queryPattern.matcher(contact.getDisplayName()).find()
                    || queryPattern.matcher(contact.getAddress()).find())
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
     * child <tt>MetaContact</tt>, which is matching the current filter.<br/>
     * Note that this method must be called on UI thread.
     *
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @param query the query string to check for matches
     * @return <tt>true</tt> to indicate that the given <tt>metaGroup</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(MetaContactGroup metaGroup, String query)
    {
        // This method must be run on UI thread
        assertUIThread();

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
        final ContactPresenceStatusChangeEvent event)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Contact sourceContact = event.getSourceContact();

                if (logger.isDebugEnabled())
                    logger.debug("Contact presence status changed: "
                                     + sourceContact.getAddress());

                MetaContact metaContact
                    = contactListService
                    .findMetaContactByContact(sourceContact);

                if (metaContact != null)
                    updateStatus(metaContact);
            }
        });
    }

    /**
     * Adds the given <tt>MessageListener</tt> to listen for message events in
     * this chat session.
     *
     * @param metaContact the <tt>MetaContact</tt> for which we add the listener
     * @param l the <tt>MessageListener</tt> to add
     */
    private void addContactStatusListener( MetaContact metaContact,
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
    private void removeContactStatusListener(MetaContact metaContact,
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

    /**
     * Checks if given <tt>metaContact</tt> is considered to be selected. That
     * is if the chat session with given <tt>metaContact</tt> is the one
     * currently visible.
     * @param metaContact the <tt>MetaContact</tt> to check.
     * @return <tt>true</tt> if given <tt>metaContact</tt> is considered to be
     *         selected.
     */
    public static boolean isContactSelected(MetaContact metaContact)
    {
        return ChatSessionManager.getCurrentChatId() != null
            && ChatSessionManager.getActiveChat(metaContact) != null
            && ChatSessionManager.getCurrentChatId().equals(
            ChatSessionManager.getActiveChat(metaContact).getChatId());
    }

    /**
     * Implements {@link UIGroupRenderer}.
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName(Object groupImpl)
    {
        MetaContactGroup metaGroup = (MetaContactGroup) groupImpl;
        if (metaGroup.equals(contactListService.getRoot()))
            return JitsiApplication.getResString(
                R.string.service_gui_CONTACTS);
        else
            return metaGroup.getGroupName();
    }
}
