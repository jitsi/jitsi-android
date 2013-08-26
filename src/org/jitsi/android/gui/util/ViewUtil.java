/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.view.*;
import android.widget.*;

/**
 * Utility class that encapsulates common operations on some <tt>View</tt> types.
 *
 * @author Pawel Domas
 */
public class ViewUtil
{
    /**
     * Sets given <tt>text</tt> on the <tt>TextView</tt> identified by the
     * <tt>id</tt>. The <tt>TextView</tt> must be inside <tt>container</tt> view
     * hierarchy.
     *
     * @param container the <tt>View</tt> that contains the <tt>TextView</tt>.
     * @param id the id of <tt>TextView</tt> we want to edit.
     * @param text string value that will be set on the <tt>TextView</tt>.
     */
    public static void setTextViewValue(View container, int id, String text)
    {
        TextView tv = (TextView) container.findViewById(id);
        tv.setText(text);
    }

    public static String getTextViewValue(View container, int id)
    {
        return ((TextView)container.findViewById(id)).getText().toString();
    }

    public static boolean isCompoundChecked(View container, int id)
    {
        return ((CompoundButton)container.findViewById(id)).isChecked();
    }

    public static void setCompoundChecked( View container, int id,
                                           boolean isChecked)
    {
        ((CompoundButton)container.findViewById(id)).setChecked(isChecked);
    }

    /**
     * Sets image identified by <tt>drawableId</tt> resource id on the
     * <tt>ImageView</tt>. <tt>ImageView</tt> must exist in <tt>container</tt>
     * view hierarchy.
     *
     * @param container the container <tt>View</tt>.
     * @param imageViewId id of <tt>ImageView</tt> that will be used.
     * @param drawableId the resource id of drawable that will be set.
     */
    public static void setImageViewIcon(View container, int imageViewId,
                                        int drawableId)
    {
        ImageView imageView = (ImageView) container.findViewById(imageViewId);
        imageView.setImageResource(drawableId);
    }

    /**
     * Ensures that the <tt>View</tt> is currently in visible or hidden state
     * which depends on <tt>isVisible</tt> flag.
     *
     * @param container parent <tt>View</tt> that contains displayed
     * <tt>View</tt>.
     * @param viewId the id of <tt>View</tt> that will be shown/hidden.
     * @param isVisible flag telling whether the <tt>View</tt> has to be shown
     * or hidden.
     */
    static public void ensureVisible(View container, int viewId,
                                     boolean isVisible )
    {
        View view = container.findViewById(viewId);
        if( isVisible && view.getVisibility() != View.VISIBLE )
        {
            view.setVisibility(View.VISIBLE);
        }
        else if( !isVisible && view.getVisibility() != View.GONE )
        {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Ensures that the <tt>View</tt> is currently in enabled or disabled state.
     *
     * @param container parent <tt>View</tt> that contains displayed
     * <tt>View</tt>.
     * @param viewId the id of <tt>View</tt> that will be enabled/disabled.
     * @param isEnabled flag telling whether the <tt>View</tt> has to be enabled
     * or disabled.
     */
    static public void ensureEnabled(View container, int viewId,
                                     boolean isEnabled )
    {
        View view = container.findViewById(viewId);
        if( isEnabled && !view.isEnabled() )
        {
            view.setEnabled(isEnabled);
        }
        else if( !isEnabled && view.isEnabled() )
        {
            view.setEnabled(isEnabled);
        }
    }
}
