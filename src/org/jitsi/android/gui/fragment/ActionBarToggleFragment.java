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
package org.jitsi.android.gui.fragment;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.jitsi.R;
import org.jitsi.service.osgi.*;

/**
 * Fragment adds a toggle button to the action bar with text description to
 * the right of it. Button is handled through the <tt>ActionBarToggleModel</tt>
 * which must be implemented by parent <tt>Activity</tt>.
 *
 * @author Pawel Domas
 */
public class ActionBarToggleFragment
    extends OSGiFragment
{
    /**
     * Text description's argument key
     */
    private static final String ARG_LABEL_TEXT="text";

    /**
     * Button model
     */
    private ActionBarToggleModel model;

    /**
     * Menu instance used to update the button
     */
    private Menu menu;

    /**
     * Creates new instance of <tt>ActionBarToggleFragment</tt>
     */
    public ActionBarToggleFragment()
    {
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        this.model = (ActionBarToggleModel) activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.actionbar_toggle_menu, menu);

        // Binds the button
        CompoundButton cBox =
                (CompoundButton) menu.findItem(R.id.toggleView)
                        .getActionView().findViewById(android.R.id.toggle);
        cBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton cb, boolean checked)
            {
                model.setChecked(checked);
            }
        });

        // Set label text
        ((TextView)menu.findItem(R.id.toggleView)
                .getActionView()
                .findViewById(android.R.id.text1))
                .setText(
                        getArguments().getString(ARG_LABEL_TEXT));

        this.menu = menu;
        updateChecked();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        updateChecked();
    }

    /**
     * {@inheritDoc}
     */
    private void updateChecked()
    {
        if(menu == null)
            return;

        ((CompoundButton) menu.findItem(R.id.toggleView).getActionView()
                .findViewById(android.R.id.toggle))
                        .setChecked(model.isChecked());
    }

    /**
     * Creates new instance of <tt>ActionBarToggleFragment</tt> with given
     * description(can be empty but not <tt>null</tt>).
     * @param labelText toggle button's description(can be empty, but not
     *                  <tt>null</tt>).
     * @return new instance of <tt>ActionBarToggleFragment</tt> parametrized
     *         with description argument.
     */
    static public ActionBarToggleFragment create(String labelText)
    {
        ActionBarToggleFragment fragment = new ActionBarToggleFragment();

        Bundle args = new Bundle();
        args.putString(ARG_LABEL_TEXT, labelText);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Toggle button's model that has to be implemented by parent
     * <tt>Activity</tt>.
     */
    public interface ActionBarToggleModel
    {
        /**
         * Return <tt>true</tt> if button's model is currently in checked state.
         * @return <tt>true</tt> if button's model is currently in checked
         *         state.
         */
        boolean isChecked();

        /**
         * Method fired when the button is clicked.
         * @param isChecked <tt>true</tt> if new button's state is checked.
         */
        void setChecked(boolean isChecked);
    }
}
