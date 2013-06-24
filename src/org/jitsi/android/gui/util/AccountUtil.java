/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import net.java.sip.communicator.service.protocol.*;
import org.jitsi.*;

/**
 * Class containing utility methods that may concern accounts.
 * Provide default values for some fields.
 * 
 * @author Pawel Domas
 */
public class AccountUtil
{
    /**
     * Returns {@link Drawable} representing default presence status
     * for specified <tt>protocolName</tt>
     *
     * @param context {@link Context} of current {@link android.app.Activity}
     * @param protocolName the name of the protocol
     * @return {@link Drawable} for default presence status or <tt>null</tt>
     *  otherwise
     */
    static public Drawable getDefaultPresenceIcon( Context context,
                                                   String protocolName)
    {
        if(protocolName.equals(ProtocolNames.SIP))
        {
            return new BitmapDrawable(
                    BitmapFactory.decodeResource(
                        context.getResources(),
                        R.drawable.default_sip_status));
        }
        else if(protocolName.equals(ProtocolNames.JABBER))
        {
            return new BitmapDrawable(
                    BitmapFactory.decodeResource(
                        context.getResources(),
                        R.drawable.default_jabber_status));
        }

        return null;
    }

    /**
     * Returns the default avatar {@link Drawable}
     *
     * @param context current application {@link Context}
     * @return the default avatar {@link Drawable}
     */
    static public LayerDrawable getDefaultAvatarIcon(Context context)
    {
        return (LayerDrawable) context.getResources()
            .getDrawable(R.drawable.avatar_layer_drawable);
    }
}
