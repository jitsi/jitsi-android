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
 * Edit text preference which persists it's value through the
 * <tt>ConfigurationService</tt>. Current value is reflected in the summary.
 *
 * @author Pawel Domas
 */
public class ConfigEditText
    extends EditTextPreference
{
    public ConfigEditText(Context context,
                          AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public ConfigEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ConfigEditText(Context context)
    {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        super.onSetInitialValue(restoreValue,defaultValue);

        // Set summary on init
        setSummary(getText());
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
        setSummary(value);

        return true;
    }
}
