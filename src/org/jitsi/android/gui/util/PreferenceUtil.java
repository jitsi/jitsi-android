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
        logger.debug("Setting "+isChecked+" on "+prefKey);
        cbPref.setChecked(isChecked);
    }
}
