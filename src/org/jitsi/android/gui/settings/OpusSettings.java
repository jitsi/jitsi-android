/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import org.jitsi.*;

/**
 * Opus settings screen.
 *
 * @author Pawel Domas
 */
public class OpusSettings
    extends BasicSettingsActivity
{
    @Override
    protected int getPreferencesXmlId()
    {
        return R.xml.opus_preferences;
    }
}
