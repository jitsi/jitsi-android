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
import org.jitsi.android.gui.util.event.EventListener;

import java.util.*;

/**
 * This adapter displays all <tt>MetaContactGroup</tt> items.
 * If in the constructor <tt>AdapterView</tt> id will be passed it will include
 * "create new group" functionality. That means extra item "create group.." will
 * be appended on the last position and when selected create group dialog will
 * popup automatically. When new group is eventually created it is implicitly
 * included into this adapter.
 *
 * @author Pawel Domas
 */
public class MetaContactGroupAdapter
    extends CollectionAdapter<Object>
{
    /**
     * Object instance used to identify "Create group..." item.
     */
    private static final Object ADD_NEW_OBJECT = new Object();

    /**
     * Creates new instance of <tt>MetaContactGroupAdapter</tt>.
     * It will be filled with all currently available <tt>MetaContactGroup</tt>.
     *
     * @param parent the parent <tt>Activity</tt>.
     */
    public MetaContactGroupAdapter(Activity parent, int adapterViewId)
    {
        super(parent,getAllContactGroups(adapterViewId != -1).iterator());

        if(adapterViewId != -1)
            init(adapterViewId);
    }

    private void init(int adapterViewId)
    {
        AdapterView aView
            = (AdapterView) getParentActivity().findViewById(adapterViewId);

        // Handle add new group action
        aView.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position,
                                       long id)
            {
                Object item = parent.getAdapter().getItem(position);
                if (item == MetaContactGroupAdapter.ADD_NEW_OBJECT)
                {
                    AddGroupDialog.showCreateGroupDialog(
                            getParentActivity(),
                            new EventListener<MetaContactGroup>()
                            {
                                @Override
                                public void onChangeEvent(
                                        MetaContactGroup newGroup)
                                {
                                    onNewGroupCreated(newGroup);
                                }
                            });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){ }

        });
    }

    /**
     * Returns the list of all currently available <tt>MetaContactGroup</tt>.
     * @param includeCreateNew indicates whether "create new group" item should
     *                         be included in the list.
     * @return the list of all currently available <tt>MetaContactGroup</tt>.
     */
    private static List<Object> getAllContactGroups(boolean includeCreateNew)
    {
        MetaContactListService contactListService
                = AndroidGUIActivator.getContactListService();

        MetaContactGroup root = contactListService.getRoot();
        ArrayList<Object> merge = new ArrayList<Object>();
        merge.add(root);
        Iterator<MetaContactGroup> mcg = root.getSubgroups();
        while(mcg.hasNext())
        {
            merge.add(mcg.next());
        }

        //Add new group item
        if(includeCreateNew)
            merge.add(ADD_NEW_OBJECT);

        return merge;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected View getView(boolean isDropDown,
                           Object item, ViewGroup parent,
                           LayoutInflater inflater)
    {
        int rowResId = isDropDown
                ? android.R.layout.simple_spinner_dropdown_item
                : android.R.layout.simple_spinner_item;

        View rowView = inflater.inflate(rowResId, parent, false);

        TextView tv = (TextView) rowView.findViewById(android.R.id.text1);

        if(item.equals(ADD_NEW_OBJECT))
        {
            tv.setText(R.string.service_gui_CREATE_GROUP);
        }
        else if(item.equals(
                AndroidGUIActivator.getContactListService().getRoot()))
        {
            // Root
            tv.setText(R.string.service_gui_SELECT_NO_GROUP);
        }
        else
        {
            tv.setText(((MetaContactGroup)item).getGroupName());
        }

        return rowView;
    }

    /**
     * Handles on new group created event by append item into the list and
     * notifying about data set change.
     * @param newGroup new contact group if was created or <tt>null</tt> if user
     *                 cancelled the dialog.
     */
    private void onNewGroupCreated(MetaContactGroup newGroup)
    {
        if(newGroup == null)
            return;

        final Spinner groupSpinner
                = (Spinner) getParentActivity()
                        .findViewById(R.id.selectGroupSpinner);

        int pos = getCount()-1;
        insert(pos, newGroup);

        groupSpinner.setSelection(pos);
        notifyDataSetChanged();
    }
}
