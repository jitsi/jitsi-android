/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
            int attribute = attArray.getIndex(i);
            switch (attribute)
            {
                case R.styleable.ConfigListPreference_disableDependentsValue:
                    this.dependentValue = attArray.getString(attribute);
                    break;
            }
        }
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
        return super.shouldDisableDependents()
               || (dependentValue != null && getValue().equals(dependentValue));
    }
}
