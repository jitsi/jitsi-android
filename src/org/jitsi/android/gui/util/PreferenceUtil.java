/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.preference.*;

/**
 * Utility class exposing methods to operate on <tt>Preference</tt> subclasses.
 *
 * @author Pawel Domas
 */
public class PreferenceUtil
{
    /**
     * Sets the <tt>CheckBoxPreference</tt> "checked" property.
     * @param fragment the <tt>PreferenceFragment</tt> containing the
     *        <tt>CheckBoxPreference</tt> we want to edit.
     * @param prefKey preference key id from <tt>R.string</tt>.
     * @param isChecked the value we want to set to the "checked" property of
     *        <tt>CheckBoxPreference</tt>.
     */
    static public void setCheckboxVal( PreferenceFragment fragment,
                                       String prefKey,
                                       boolean isChecked )
    {
        CheckBoxPreference cbPref
                = (CheckBoxPreference) fragment
                        .findPreference(prefKey);
        cbPref.setChecked(isChecked);
    }

}
