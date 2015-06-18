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

import android.annotation.*;
import android.app.*;
import android.graphics.drawable.*;
import android.os.*;
import android.widget.*;
import net.java.sip.communicator.util.*;
import org.jitsi.R;
import org.jitsi.android.*;

/**
 * The <tt>ActionBarUtil</tt> provides utility methods for setting action bar
 * avatar and display name.
 *
 * @author Yana Stamcheva
 */
public class ActionBarUtil
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ActionBarUtil.class);

    /**
     * The avatar drawable.
     */
    private static LayerDrawable avatarDrawable;

    /**
     * Sets the action bar title for the given acitivity.
     *
     * @param a the <tt>Activity</tt>, for which we set the action bar title
     * @param title the title string to set
     */
    public static void setTitle(Activity a, CharSequence title)
    {
        if(!AndroidUtils.hasAPI(11) || a == null)
            return;

        ActionBar actionBar = a.getActionBar();
        // Some activities don't have ActionBar
        if(actionBar == null)
            return;

        TextView actionBarText
            = (TextView) actionBar.getCustomView()
                .findViewById(R.id.actionBarText);

        actionBarText.setText(title);
    }

    /**
     * Sets the action bar subtitle for the given acitivity.
     *
     * @param a the <tt>Activity</tt>, for which we set the action bar subtitle
     * @param subtitle the subtitle string to set
     */
    public static void setSubtitle(Activity a, String subtitle)
    {
        if(!AndroidUtils.hasAPI(11))
            return;

        TextView actionBarText
            = (TextView) a.getActionBar().getCustomView()
                .findViewById(R.id.actionBarStatusText);

        actionBarText.setText(subtitle);
    }

    /**
     * Sets the avatar icon of the action bar.
     *
     * @param a the current activity where the status should be displayed
     * @param avatar the avatar to set
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void setAvatar(Activity a, byte[] avatar)
    {
        if (avatarDrawable == null)
            avatarDrawable = getDefaultAvatarIcon();

        BitmapDrawable avatarBmp = null;
        if(avatar != null)
        {
            if(avatar.length < 256*1024)
            {
                avatarBmp = AndroidImageUtil.roundedDrawableFromBytes(avatar);
            }
            else
            {
                logger.error("Avatar image is too large: " + avatar.length);
            }

            if(avatarBmp != null)
            {
                avatarDrawable
                    .setDrawableByLayerId(R.id.avatarDrawable, avatarBmp);
            }
            else
            {
                logger.error("Failed to get avatar drawable from bytes");
            }
        }

        // setLogo not supported prior API 14
        if(AndroidUtils.hasAPI(14) && a != null)
        {
            ActionBar actionBar = a.getActionBar();
            if(actionBar != null)
                actionBar.setLogo(avatarDrawable);
        }
    }

    /**
     * Sets the status icon of the action bar avatar.
     *
     * @param a the current activity where the status should be displayed
     * @param statusIcon the status icon to set
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void setStatus(Activity a, byte[] statusIcon)
    {
        if (avatarDrawable == null)
            avatarDrawable = getDefaultAvatarIcon();

        avatarDrawable
            .setDrawableByLayerId(R.id.contactStatusDrawable,
                AndroidImageUtil.drawableFromBytes(statusIcon));

        // setLogo not supported prior API 14
        if(AndroidUtils.hasAPI(14))
        {
            a.getActionBar().setLogo(avatarDrawable);
        }
    }

    /**
     * Returns the default avatar {@link Drawable}
     *
     * @return the default avatar {@link Drawable}
     */
    private static LayerDrawable getDefaultAvatarIcon()
    {
        return (LayerDrawable) JitsiApplication.getAppResources()
            .getDrawable(R.drawable.avatar_layer_drawable);
    }
}
