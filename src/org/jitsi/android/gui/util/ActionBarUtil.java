/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.annotation.*;
import android.app.*;
import android.graphics.drawable.*;
import android.os.*;
import android.widget.*;
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

        avatarDrawable
            .setDrawableByLayerId(R.id.avatarDrawable,
                AndroidImageUtil.roundedDrawableFromBytes(avatar));

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
