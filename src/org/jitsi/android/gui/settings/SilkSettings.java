/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import org.jitsi.*;

/**
 * The Silk encoder settings screen.
 *
 * @author Pawel Domas
 */
public class SilkSettings
    extends BasicSettingsActivity
{
    /**
     * {@inheritDoc}
     */
    @Override
    protected int getPreferencesXmlId()
    {
        return R.xml.silk_preferences;
    }
}
