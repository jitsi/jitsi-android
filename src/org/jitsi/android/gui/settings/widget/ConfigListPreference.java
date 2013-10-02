/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings.widget;

import android.content.*;
import android.preference.*;
import android.util.*;

import org.jitsi.android.gui.*;

/**
 * @author Pawel Domas
 */
public class ConfigListPreference
    extends ListPreference
{
    public ConfigListPreference(Context context,
                                AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ConfigListPreference(Context context)
    {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        super.onSetInitialValue(restoreValue, defaultValue);

        // Update summary every time the value is read
        updateSummary(getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPersistedString(String defaultReturnValue)
    {
        String value
            = AndroidGUIActivator.getConfigurationService()
                    .getString(getKey(), defaultReturnValue);

        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean persistString(String value)
    {
        super.persistString(value);

        AndroidGUIActivator
                .getConfigurationService()
                .setProperty(getKey(), value);

        // Update summary when the value has changed
        updateSummary(value);

        return true;
    }

    /**
     * Updates the summary using entry corresponding to currently selected
     * value.
     *
     * @param value the current value
     */
    private void updateSummary(String value)
    {
        int idx = findIndexOfValue(value);
        if(idx != -1)
        {
            setSummary(getEntries()[idx]);
        }
    }
}
