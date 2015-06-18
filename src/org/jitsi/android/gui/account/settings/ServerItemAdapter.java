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
