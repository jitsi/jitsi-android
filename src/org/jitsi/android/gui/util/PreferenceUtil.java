/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.preference.*;

import net.java.sip.communicator.util.*;

/**
 * Utility class exposing methods to operate on <tt>Preference</tt> subclasses.
 *
 * @author Pawel Domas
 */
public class PreferenceUtil
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(PreferenceUtil.class);

    /**
     * Sets the <tt>CheckBoxPreference</tt> "checked" property.
     * @param screen the <tt>PreferenceScreen</tt> containing the
     *        <tt>CheckBoxPreference</tt> we want to edit.
     * @param prefKey preference key id from <tt>R.string</tt>.
     * @param isChecked the value we want to set to the "checked" property of
     *        <tt>CheckBoxPreference</tt>.
     */
    static public void setCheckboxVal( PreferenceScreen screen,
                                       String prefKey,
                                       boolean isChecked )
    {
        CheckBoxPreference cbPref
                = (CheckBoxPreference) screen.findPreference(prefKey);
        logger.debug("Setting "+isChecked+" on "+prefKey);
        cbPref.setChecked(isChecked);
    }

    /**
     * Sets the text of <tt>EditTextPreference</tt> identified by given
     * preference key string.
     * @param screen the <tt>PreferenceScreen</tt> containing the
     *        <tt>EditTextPreference</tt> we want to edit.
     * @param prefKey preference key id from <tt>R.string</tt>.
     * @param txtValue the text value we want to set on
     *                 <tt>EditTextPreference</tt>
     */
    public static void setEditTextVal( PreferenceScreen screen,
                                       String prefKey,
                                       String txtValue )
    {
        EditTextPreference cbPref
            = (EditTextPreference) screen.findPreference(prefKey);

        logger.debug("Setting "+txtValue+" on "+prefKey);

        cbPref.setText(txtValue);
    }

    /**
     * Sets the value of <tt>ListPreference</tt> identified by given
     * preference key string.
     * @param screen the <tt>PreferenceScreen</tt> containing the
     *        <tt>ListPreference</tt> we want to edit.
     * @param prefKey preference key id from <tt>R.string</tt>.
     * @param value the value we want to set on <tt>ListPreference</tt>
     */
    public static void setListVal( PreferenceScreen screen,
                                       String prefKey,
                                       String value )
    {
        ListPreference lstPref
                = (ListPreference) screen.findPreference(prefKey);

        logger.debug("Setting "+value+" on "+prefKey);

        lstPref.setValue(value);
    }
}
