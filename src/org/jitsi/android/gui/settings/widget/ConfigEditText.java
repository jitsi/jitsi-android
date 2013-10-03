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
import android.text.*;
import android.util.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;

/**
 * Edit text preference which persists it's value through the
 * <tt>ConfigurationService</tt>. Current value is reflected in the summary.
 * It also supports minimum and maximum value limits of integer or float type.
 *
 * @author Pawel Domas
 */
public class ConfigEditText
    extends EditTextPreference
    implements Preference.OnPreferenceChangeListener
{
    /**
     * Integer upper bound for accepted value
     */
    private Integer intMax;
    /**
     * Integer lower limit for accepted value
     */
    private Integer intMin;
    /**
     * Float upper bound for accepted value
     */
    private Float floatMin;
    /**
     * Float lower limit for accepted value
     */
    private Float floatMax;

    public ConfigEditText(Context context,
                          AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        initAttributes(context, attrs);
    }

    public ConfigEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        initAttributes(context, attrs);
    }

    public ConfigEditText(Context context)
    {
        super(context);
    }

    /**
     * Parses attributes array.
     * @param context the Android context.
     * @param attrs attributes set.
     */
    private void initAttributes(Context context, AttributeSet attrs)
    {
        TypedArray attArray
                = context.obtainStyledAttributes(attrs,
                                                 R.styleable.ConfigEditText);

        for(int i=0; i<attArray.getIndexCount(); i++)
        {
            int attribute = attArray.getIndex(i);
            switch (attribute)
            {
                case R.styleable.ConfigEditText_intMax:
                    this.intMax = new Integer(attArray.getInt(attribute, -1));
                    break;
                case R.styleable.ConfigEditText_intMin:
                    this.intMin = new Integer(attArray.getInt(attribute, -1));
                    break;
                case R.styleable.ConfigEditText_floatMax:
                    this.floatMax = new Float(attArray.getFloat(attribute, -1));
                    break;
                case R.styleable.ConfigEditText_floatMin:
                    this.floatMin = new Float(attArray.getFloat(attribute, -1));
                    break;
            }
        }
        // Register listener to perform checks before new value is accepted
        setOnPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        super.onSetInitialValue(restoreValue,defaultValue);

        // Set summary on init
        updateSummary(getText());
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
     * Updates the summary with given <tt>text</tt>. If the input type is
     * password variation all characters will be replaced with '*'.
     *
     * @param text the text that will be set as a summary.
     */
    private void updateSummary(String text)
    {
        if( (getEditText().getInputType() & InputType.TYPE_MASK_VARIATION)
                == InputType.TYPE_TEXT_VARIATION_PASSWORD )
        {
            text = text.replaceAll("(?s).", "*");
        }
        setSummary(text);
    }

    /**
     * {@inheritDoc}
     *
     * Performs value range checks before the value is accepted.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        if(intMax != null && intMin != null)
        {
            //Integer range check
            Integer newInt = Integer.parseInt((String)newValue);
            return intMin <= newInt && newInt <= intMax;
        }
        else if(floatMax != null && floatMax != null)
        {
            // Float range check
            Float newFloat = Float.parseFloat((String)newValue);
            return floatMin <= newFloat && newFloat <= floatMax;
        }
        // No checks by default
        return true;
    }
}
