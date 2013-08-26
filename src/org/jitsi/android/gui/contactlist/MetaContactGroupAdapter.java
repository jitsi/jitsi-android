/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import android.app.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.contactlist.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;

import java.util.*;

/**
 * This adapter displays <tt>MetaContactGroup</tt> items.
 *
 * @author Pawel Domas
 */
public class MetaContactGroupAdapter
    extends CollectionAdapter<MetaContactGroup>
{
    /**
     * Creates new instance of <tt>MetaContactGroupAdapter</tt>.
     * It will be filled with all currently available <tt>MetaContactGroup</tt>.
     *
     * @param parent the parent <tt>Activity</tt>.
     */
    public MetaContactGroupAdapter(Activity parent)
    {
        super(parent,getAllContactGroups().iterator());
    }

    /**
     * Returns the list of all currently available <tt>MetaContactGroup</tt>.
     * @return the list of all currently available <tt>MetaContactGroup</tt>.
     */
    public static List<MetaContactGroup> getAllContactGroups()
    {
        MetaContactListService contactListService
                = AndroidGUIActivator.getContactListService();

        MetaContactGroup root = contactListService.getRoot();
        ArrayList<MetaContactGroup> merge = new ArrayList<MetaContactGroup>();
        merge.add(root);
        Iterator<MetaContactGroup> mcg = root.getSubgroups();
        while(mcg.hasNext())
        {
            merge.add(mcg.next());
        }

        return merge;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected View getView(boolean isDropDown,
                           MetaContactGroup item, ViewGroup parent,
                           LayoutInflater inflater)
    {
        int rowResId = isDropDown
                ? android.R.layout.simple_spinner_dropdown_item
                : android.R.layout.simple_spinner_item;

        View rowView = inflater.inflate(rowResId, parent, false);

        TextView tv = (TextView) rowView.findViewById(android.R.id.text1);

        if(item.equals(
                AndroidGUIActivator.getContactListService().getRoot()))
        {
            // Root
            tv.setText(R.string.service_gui_SELECT_NO_GROUP);
        }
        else
        {
            tv.setText(item.getGroupName());
        }

        return rowView;
    }
}
