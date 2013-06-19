/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account.settings;

import android.app.*;
import android.widget.*;

/**
 * Class is used in {@link ServerListActivity} to handle list model.
 * It also provides the edit dialog fragment for it's items.
 * 
 * @author Pawel Domas
 */
abstract class ServerItemAdapter
    extends BaseAdapter
{
    /**
     * Parent {@link android.app.Activity} used as a context
     */
    protected final Activity parent;

    /**
     * Creates new instance of {@link ServerItemAdapter}
     *
     * @param parent the parent {@link Activity} used as a context
     */
    public ServerItemAdapter( Activity parent)
    {
        this.parent = parent;
    }

    public long getItemId(int i)
    {
        return i;
    }

    /**
     * Request list repaint
     */
    protected void refresh()
    {
        parent.runOnUiThread(new Runnable()
        {
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Factory method should return a {@link DialogFragment} that will allow
     * user to edit list item at specified <tt>position</tt>.
     *
     * @param position the position of item to edit
     * @return the {@link DialogFragment} that should wil be displayed
     *  when item is clicked
     */
    abstract DialogFragment createItemEditDialogFragment(int position);

}
