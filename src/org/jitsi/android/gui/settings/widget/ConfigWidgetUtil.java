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
import android.text.*;
import android.util.*;
import org.jitsi.*;
import org.jitsi.android.gui.*;

/**
 * Class that handles common attributes and operations for all configuration
 * widgets.
 *
 * @author Pawel Domas
 */
class ConfigWidgetUtil
{
    /**
     * The parent <tt>Preference</tt> handled by this instance.
     */
    private final Preference parent;

    /**
     * Flag indicates whether configuration property should be stored in
     * separate thread to prevent network on main thread exceptions.
     */
    private boolean useNewThread;

    /**
     * Flag indicates whether value should be mapped to the summary.
     */
    private boolean mapSummary;

    /**
     * Creates new instance of <tt>ConfigWidgetUtil</tt> for given
     * <tt>parent</tt> <tt>Preference</tt>.
     *
     * @param parent the <tt>Preference</tt> that will be handled by this
     *               instance.
     */
    ConfigWidgetUtil(Preference parent)
    {
        this.parent = parent;
    }

    /**
     * Creates new instance of <tt>ConfigWidgetUtil</tt> for given
     * <tt>parent</tt> <tt>Preference</tt>.
     *
     * @param parent the <tt>Preference</tt> that will be handled by this
     *               instance.
     * @param mapSummary indicates whether value should be displayed
     *                   as a summary
     */
    ConfigWidgetUtil(Preference parent, boolean mapSummary)
    {
        this.parent = parent;
        this.mapSummary = true;
    }

    /**
     * PArses the attributes. Should be called by parent <tt>Preference</tt>.
     * @param context the Android context
     * @param attrs the attribute set
     */
    void parseAttributes(Context context, AttributeSet attrs)
    {
        TypedArray attArray
                = context.obtainStyledAttributes(attrs,
                                                 R.styleable.ConfigWidget);

        useNewThread = attArray.getBoolean(
                R.styleable.ConfigWidget_storeInNewThread, false);

        mapSummary = attArray.getBoolean(
                R.styleable.ConfigWidget_mapSummary, mapSummary);
    }

    /**
     * Updates the summary if necessary. Should be called by parent
     * <tt>Preference</tt> on value initialization.
     *
     * @param value the current value
     */
    void updateSummary(Object value)
    {
        if(mapSummary)
        {
            String text = value != null ? value.toString() : "";

            if(parent instanceof EditTextPreference)
            {
                int inputType
                        = ((EditTextPreference) parent)
                        .getEditText()
                        .getInputType();

                if((inputType & InputType.TYPE_MASK_VARIATION)
                        == InputType.TYPE_TEXT_VARIATION_PASSWORD )
                {
                    text = text.replaceAll("(?s).", "*");
                }
            }

            parent.setSummary(text);
        }
    }

    /**
     * Persists new value through the <tt>getConfigurationService</tt>.
     * @param value the new value to persist.
     */
    void handlePersistValue(final Object value)
    {
        updateSummary(value);

        Thread store = new Thread()
        {
            @Override
            public void run()
            {
                AndroidGUIActivator
                        .getConfigurationService()
                        .setProperty(parent.getKey(), value);
            }
        };

        if(useNewThread)
            store.start();
        else
            store.run();
    }
}
