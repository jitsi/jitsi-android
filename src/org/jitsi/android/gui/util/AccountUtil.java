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
