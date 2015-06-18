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
import android.content.res.*;
import android.preference.*;
import android.util.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;

/**
 * List preference that stores it's value through the
 * <tt>ConfigurationService</tt>. It also supports "disable dependents value"
 * attribute.
 *
 * @author Pawel Domas
 */
public class ConfigListPreference
    extends ListPreference
{
    /**
     * Optional attribute which contains value that disables all dependents.
     */
    private String dependentValue;
    /**
     * Disables dependents when current value is different than
     * <tt>dependentValue</tt>.
     */
    private boolean disableOnNotEqual;

    public ConfigListPreference(Context context,
                                AttributeSet attrs)
    {
        super(context, attrs);

        initAttributes(context, attrs);
    }

    public ConfigListPreference(Context context)
    {
        super(context);
    }

    /**
     * Parses attribute set.
     * @param context Android context.
     * @param attrs attribute set.
     */
    private void initAttributes(Context context, AttributeSet attrs)
    {
        TypedArray attArray
            = context.obtainStyledAttributes(attrs,
                                             R.styleable.ConfigListPreference);

        for(int i=0; i<attArray.getIndexCount(); i++)
        {
            int attrIdx = attArray.getIndex(i);
            switch (attrIdx)
            {
                case R.styleable.ConfigListPreference_disableDependentsValue:
                    this.dependentValue = attArray.getString(attrIdx);
                    break;
                case R.styleable.ConfigListPreference_disableOnNotEqualValue:
                    this.disableOnNotEqual = attArray.getBoolean(attrIdx,false);
                    break;
            }
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager)
    {
        // Force load default value from configuration service
        setDefaultValue(getPersistedString(null));

        super.onAttachedToHierarchy(preferenceManager);
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
        return AndroidGUIActivator.getConfigurationService()
            .getString(getKey(), defaultReturnValue);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String value)
    {
        super.setValue(value);

        // Disables dependents
        notifyDependencyChange(shouldDisableDependents());
    }

    /**
     * {@inheritDoc}
     *
     * Additionally checks if current value is equal to disable dependents
     * value.
     */
    @Override
    public boolean shouldDisableDependents()
    {
        return  super.shouldDisableDependents()
                || (dependentValue != null
                    && disableOnNotEqual != dependentValue.equals(getValue()));
    }
}
