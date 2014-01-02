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
 * Checkbox preference that persists the value through
 * <tt>ConfigurationService</tt>.
 *
 * @author Pawel Domas
 */
public class ConfigCheckBox
    extends CheckBoxPreference
{
    /**
     * <tt>ConfigWidgetUtil</tt> used by this instance.
     */
    private ConfigWidgetUtil configUtil = new ConfigWidgetUtil(this);

    public ConfigCheckBox(Context context,
                          AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        configUtil.parseAttributes(context, attrs);
    }

    public ConfigCheckBox(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        configUtil.parseAttributes(context, attrs);
    }

    public ConfigCheckBox(Context context)
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

        configUtil.updateSummary(isChecked());
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager)
    {
        // Force load default value from configuration service
        setDefaultValue(getPersistedBoolean(false));

        super.onAttachedToHierarchy(preferenceManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue)
    {
        return AndroidGUIActivator
                .getConfigurationService()
                .getBoolean(getKey(), defaultReturnValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean persistBoolean(boolean value)
    {
        super.persistBoolean(value);

        // Sets boolean value in the ConfigurationService
        configUtil.handlePersistValue(value);

        return true;
    }
}
