/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import org.jitsi.*;
import org.jitsi.android.*;

import android.app.*;
import android.content.*;
import android.graphics.drawable.*;
import android.widget.*;

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
    public static void setTitle(Activity a, String title)
    {
        TextView actionBarText
            = (TextView) a.getActionBar().getCustomView()
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
        TextView actionBarText
            = (TextView) a.getActionBar().getCustomView()
                .findViewById(R.id.actionBarStatusText);

        actionBarText.setText(subtitle);
    }

    /**
     * Sets the avatar icon of the action bar.
     *
     * @param context the android context
     * @param avatar the avatar to set
     */
    public static void setAvatar(Context context, byte[] avatar)
    {
        if (avatarDrawable == null)
            avatarDrawable = getDefaultAvatarIcon(context);

        avatarDrawable
            .setDrawableByLayerId(R.id.avatarDrawable,
                AndroidImageUtil.drawableFromBytes(avatar));

        Activity currentActivity = JitsiApplication.getCurrentActivity();

        if (currentActivity.getClass()
                .equals(JitsiApplication.getHomeScreenActivityClass()))
        {
            currentActivity.getActionBar().setLogo(avatarDrawable);
        }
    }

    /**
     * Sets the status icon of the action bar avatar.
     *
     * @param context the android context
     * @param statusIcon the status icon to set
     */
    public static void setStatus(Context context, byte[] statusIcon)
    {
        if (avatarDrawable == null)
            avatarDrawable = getDefaultAvatarIcon(context);

        avatarDrawable
            .setDrawableByLayerId(R.id.contactStatusDrawable,
                AndroidImageUtil.drawableFromBytes(statusIcon));

        Activity currentActivity = JitsiApplication.getCurrentActivity();

        if (currentActivity.getClass()
                .equals(JitsiApplication.getHomeScreenActivityClass()))
        {
            currentActivity.getActionBar().setLogo(avatarDrawable);
        }
    }

    /**
     * Returns the default avatar {@link Drawable}
     *
     * @param context current application {@link Context}
     * @return the default avatar {@link Drawable}
     */
    private static LayerDrawable getDefaultAvatarIcon(Context context)
    {
        return (LayerDrawable) context.getResources()
            .getDrawable(R.drawable.avatar_layer_drawable);
    }
}
