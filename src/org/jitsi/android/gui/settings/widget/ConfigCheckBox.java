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
