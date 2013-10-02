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
    public ConfigCheckBox(Context context,
                          AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public ConfigCheckBox(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ConfigCheckBox(Context context)
    {
        super(context);
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
        AndroidGUIActivator
                .getConfigurationService()
                .setProperty(getKey(), value);

        return true;
    }
}
