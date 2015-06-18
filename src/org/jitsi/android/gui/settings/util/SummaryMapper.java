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
package org.jitsi.android.gui.settings.util;

import android.content.*;
import android.preference.*;

import java.util.*;

/**
 * The class can be used to set {@link Preference} value as it's summary text.
 * Optionally the empty string can be provided that will be used when value is
 * <tt>null</tt> or empty <tt>String</tt>. To make it work it have to be
 * registered to the {@link SharedPreferences} instance containing preferences
 * we want handle. Single instance can map multiple {@link Preference} at one
 * time.
 *
 * @author Pawel Domas
 */
public class SummaryMapper
    implements SharedPreferences.OnSharedPreferenceChangeListener
{

    /**
     * The key to {@link android.preference.Preference} mapping
     */
    private Map<String, Preference> mappedPreferences =
            new HashMap<String, Preference>();

    /**
     * Mapping containing optional {@link SummaryConverter} that can provide
     * custom operation on the value before it is applied as a summary.
     */
    private Map<String, SummaryConverter> convertersMap =
            new HashMap<String, SummaryConverter>();

    /**
     * Mapping containing empty string definitions
     */
    private Map<String, String> emptyStrMap =
            new HashMap<String, String>();

    /**
     * Includes the {@link Preference} into summary mapping.
     *
     * @param pref the {@link Preference} to be included
     * @param empty optional empty String that will be set when the
     *              <tt>Preference</tt> value is <tt>null</tt> or empty
     * @param converter optional {@link SummaryConverter}
     *
     * @see {@link SummaryMapper}
     */
    public void includePreference( Preference pref,
                                   String empty,
                                   SummaryConverter converter)
    {
        if(pref == null)
            throw new NullPointerException("The preference can not be null");

        String key = pref.getKey();
        mappedPreferences.put(key, pref);
        emptyStrMap.put(key, empty);

        if(converter != null)
            convertersMap.put(pref.getKey(), converter);

        setSummary(pref.getSharedPreferences(), pref);
    }

    /**
     * Triggers summary update on all registered <tt>Preference</tt>s.
     */
    public void updatePreferences()
    {
        for(Preference pref : mappedPreferences.values())
        {
            setSummary(pref.getSharedPreferences(), pref);
        }
    }

    /**
     * Overload method for
     * {@link #includePreference(Preference, String, SummaryConverter)}
     *
     * @see #includePreference(Preference, String, SummaryConverter)
     */
    public void includePreference(Preference pref, String empty)
    {
        includePreference(pref, empty, null);
    }

    /**
     * Sets the summary basing on actual {@link Preference} value
     *
     * @param sharedPrefs the {@link SharedPreferences} that manages the
     *                    <tt>preference</tt>
     * @param preference
     */
    private void setSummary( SharedPreferences sharedPrefs,
                             Preference preference)
    {
        String key = preference.getKey();
        String value = sharedPrefs.getString(key, "");

        // Map entry instead of value for ListPreference
        if(preference instanceof ListPreference)
        {
            CharSequence entry = ((ListPreference) preference).getEntry();
            value = entry != null ? entry.toString() : "";
        }

        if(!value.isEmpty())
        {
            SummaryConverter converter = convertersMap.get(key);
            if(converter != null)
                value = converter.convertToSummary(value);
        }
        else
        {
            value = emptyStrMap.get(key);
        }

        preference.setSummary(value);
    }

    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences,
                                           String key)
    {
        Preference pref = mappedPreferences.get(key);
        if(pref != null)
        {
            setSummary(sharedPreferences, pref);
        }
    }

    /**
     * The interface is used to provide custom value into summary conversion.
     */
    public interface SummaryConverter
    {
        /**
         * The method shall return summary text for given <tt>input</tt> value.
         *
         * @param input {@link Preference} value as a <tt>String</tt>
         * @return output summary value
         */
        String convertToSummary(String input);
    }

    /**
     * Class is used for password preferences to display text as "*".
     * 
     */
    public static class PasswordMask 
        implements SummaryConverter
    {
        public String convertToSummary(String input)
        {
            return input.replaceAll("(?s).", "*");
        }
    }
}
