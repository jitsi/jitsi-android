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
import android.os.*;
import android.view.*;
import android.widget.*;
import org.jitsi.*;
import org.jitsi.android.gui.widgets.*;

import java.io.*;
import java.util.*;

/**
 * The fragments allows user to edit encodings and their priorities.
 *
 * @author Pawel Domas
 */
public class EncodingsFragment
    extends Fragment
    implements TouchInterceptor.DropListener
{
    /**
     * Argument key for list of encodings as strings
     * (see {@link EncodingActivity} for utility methods.)
     */
    public static final String ARG_ENCODINGS = "arg.encodings";

    /**
     * Argument key for encodings priorities.
     */
    public static final String ARG_PRIORITIES = "arg.priorities";

    /**
     * The {@link TouchInterceptor} widget that allows user to drag items
     * to set their order
     */
    private TouchInterceptor listWidget;

    /**
     * Adapter encapsulating manipulation of encodings list and their priorities
     */
    private OrderListAdapter adapter;

    /**
     * List of encodings
     */
    private List<String> encodings;

    /**
     * List of priorities
     */
    private List<Integer> priorities;

    /**
     * Flag holding enabled status for the fragment. All views will be grayed
     * out if the fragment is not enabled.
     */
    private boolean isEnabled=true;

    /**
     * Flag tells us if there were any changes made.
     */
    private boolean hasChanges = false;

    /**
     * Sets enabled status for this fragment.
     * @param isEnabled <tt>true</tt> to enable the fragment.
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
        adapter.invalidate();
    }

    /**
     * Returns <tt>true</tt> if this fragment is holding any uncommitted changes
     * @return <tt>true</tt> if this fragment is holding any uncommitted changes
     */
    public boolean hasChanges()
    {
        return hasChanges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Bundle state = savedInstanceState == null
                ? getArguments()
                : savedInstanceState;

        encodings = (List<String>) state.get(ARG_ENCODINGS);
        priorities = (List<Integer>) state.get(ARG_PRIORITIES);

        View content = inflater.inflate(R.layout.encoding, container, false);

        this.listWidget
                = (TouchInterceptor) content
                        .findViewById(R.id.encodingList);
        this.adapter = new OrderListAdapter(R.layout.encoding_item);

        listWidget.setAdapter(adapter);
        listWidget.setDropListener(this);
        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_ENCODINGS, (Serializable) encodings);
        outState.putSerializable(ARG_PRIORITIES, (Serializable) priorities);
    }

    /**
     * Implements {@link TouchInterceptor.DropListener}
     *
     * @param from index indicating source position
     * @param to index indicating destination position
     */
    public void drop(int from, int to)
    {
        adapter.swapItems(from, to);
        hasChanges = true;
    }

    /**
     * Function used to calculate priority based on item index
     *
     * @param idx the index of encoding on the list
     * @return encoding priority value for given <tt>idx</tt>
     */
    static public int calcPriority(List<?> encodings, int idx)
    {
        return encodings.size() - idx;
    }

    /**
     * Utility method for calculating encodings priorities.
     * @param idx encoding index in the list
     * @return the priority value for given encoding index.
     */
    private int calcPriority(int idx)
    {
        return calcPriority(encodings, idx);
    }

    /**
     * Creates new <tt>EncodingsFragment</tt> for given list of encodings and
     * priorities.
     *
     * @param encodings list of encodings as strings.
     * @param priorities list of encodings priorities.
     *
     * @return parametrized instance of <tt>EncodingsFragment</tt>.
     */
    static public EncodingsFragment newInstance( List<String> encodings,
                                                 List<Integer> priorities )
    {
        EncodingsFragment fragment = new EncodingsFragment();

        Bundle args = new Bundle();
        args.putSerializable(ARG_ENCODINGS, (Serializable) encodings);
        args.putSerializable(ARG_PRIORITIES, (Serializable) priorities);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Returns encodings strings list.
     * @return encodings strings list.
     */
    public List<String> getEncodings()
    {
        return encodings;
    }

    /**
     * Returns encodings priorities list.
     * @return encodings priorities list.
     */
    public List<Integer> getPriorities()
    {
        return priorities;
    }

    /**
     * Class implements encodings model for the list widget. Enables/disables
     * each encoding and sets it's priority. Also responsible for creating
     * Views for list rows.
     */
    class OrderListAdapter extends BaseAdapter
    {
        /**
         * ID of the list row layout
         */
        private final int viewResId;

        /**
         * Creates new instance of {@link OrderListAdapter}.
         *
         * @param viewResId ID of the list row layout
         */
        public OrderListAdapter( int viewResId)
        {
            this.viewResId = viewResId;
        }

        /**
         * Swaps encodings on the list and changes their priorities
         * @param from source item position
         * @param to destination items position
         */
        void swapItems(int from, int to)
        {
            // Swap positions
            String swap = encodings.get(from);
            int swapPrior = priorities.get(from);
            encodings.remove((int)from);
            priorities.remove((int)from);

            // Swap priorities
            encodings.add(to, swap);
            priorities.add(to, swapPrior);

            for(int i=0; i<encodings.size();i++)
            {
                priorities.set(i, priorities.get(i) > 0 ? calcPriority(i) : 0);
            }

            // Update the UI
            invalidate();
        }

        /**
         * Refresh the list on UI thread
         */
        public void invalidate()
        {
            getActivity().runOnUiThread(
                    new Runnable()
                    {
                        public void run()
                        {
                            notifyDataSetChanged();
                        }
                    });
        }

        public int getCount()
        {
            return encodings.size();
        }

        public Object getItem(int i)
        {
            return encodings.get(i);
        }

        public long getItemId(int i)
        {
            return i;
        }

        public View getView(final int i, View view, ViewGroup viewGroup)
        {
            // Creates the list row view
            ViewGroup gv
                    = (ViewGroup) getActivity()
                    .getLayoutInflater()
                    .inflate(this.viewResId,
                             viewGroup,
                             false);
            // Creates the enable/disable button
            CompoundButton cb =
                    (CompoundButton) gv.findViewById(android.R.id.checkbox);
            cb.setChecked(priorities.get(i) > 0);
            cb.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener()
                    {
                        public void onCheckedChanged( CompoundButton cButton,
                                                      boolean b)
                        {
                            priorities.set(i, b ? calcPriority(i) : 0);
                            hasChanges = true;
                        }
                    });
            // Create string for given format entry
            String mf = encodings.get(i);
            TextView tv = (TextView) gv.findViewById(android.R.id.text1);
            tv.setText(mf);
            // Creates the drag handle view(used to grab list entries)
            ImageView iv = (ImageView) gv.findViewById(R.id.dragHandle);
            if(!isEnabled)
                gv.removeView(iv);
            cb.setEnabled(isEnabled);
            tv.setEnabled(isEnabled);

            return gv;
        }
    }
}
