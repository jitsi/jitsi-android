/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import android.widget.*;
import net.java.sip.communicator.service.contactlist.*;
import org.jitsi.android.gui.contactlist.model.*;

/**
 * Implements contact groups expand memory.
 *
 * @author Pawel Domas
 */
public class MetaGroupExpandHandler
    implements ExpandableListView.OnGroupExpandListener,
               ExpandableListView.OnGroupCollapseListener

{
    /**
     * Data key used to remember group state.
     */
    private static final String KEY_EXPAND_MEMORY = "key.expand.memory";

    /**
     * Meta contact list adapter used by this instance.
     */
    private final MetaContactListAdapter contactList;

    /**
     * The contact list view.
     */
    private final ExpandableListView contactListView;

    /**
     * Creates new instance of <tt>MetaGroupExpandHandler</tt>.
     * @param contactList contact list data model.
     * @param contactListView contact list view.
     */
    public MetaGroupExpandHandler(MetaContactListAdapter contactList,
                                  ExpandableListView contactListView)
    {
        this.contactList = contactList;
        this.contactListView = contactListView;
    }

    /**
     * Binds the listener and restores previous groups expanded/collapsed state.
     */
    public void bindAndRestore()
    {
        for(int gIdx=0; gIdx<contactList.getGroupCount(); gIdx++)
        {
            MetaContactGroup metaGroup
                = (MetaContactGroup) contactList.getGroup(gIdx);

            if(Boolean.FALSE.equals(metaGroup.getData(KEY_EXPAND_MEMORY)))
            {
                contactListView.collapseGroup(gIdx);
            }
            else
            {
                // Will expand by default
                contactListView.expandGroup(gIdx);
            }
        }

        contactListView.setOnGroupExpandListener(this);
        contactListView.setOnGroupCollapseListener(this);
    }

    /**
     * Unbinds the listener.
     */
    public void unbind()
    {
        contactListView.setOnGroupExpandListener(null);
        contactListView.setOnGroupCollapseListener(null);
    }

    @Override
    public void onGroupCollapse(int groupPosition)
    {
        ((MetaContactGroup)contactList.getGroup(groupPosition))
            .setData(KEY_EXPAND_MEMORY, false);
    }

    @Override
    public void onGroupExpand(int groupPosition)
    {
        ((MetaContactGroup)contactList.getGroup(groupPosition))
            .setData(KEY_EXPAND_MEMORY, true);
    }
}
