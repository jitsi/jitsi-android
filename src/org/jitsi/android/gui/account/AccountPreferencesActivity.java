/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import org.jitsi.*;

import android.os.*;
import android.preference.*;

/**
 * Stores account preferences.
 *
 * @author Yana Stamcheva
 */
public class AccountPreferencesActivity
    extends PreferenceActivity
{
    private boolean shouldForceSync = false;

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.preferences);
    }
}
