/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.controller;

import android.view.*;

/**
 * The controller when set as <tt>View.OnTouchListener</tt> of given
 * <tt>View</tt> makes it draggable on the screen.
 *
 * @author Pawel domas
 */
public class SimpleDragController
    implements View.OnTouchListener
{
    /**
     * {@inheritDoc}
     */
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        int action = motionEvent.getAction();
        if(action == MotionEvent.ACTION_MOVE)
        {
            view.setX(view.getX() + motionEvent.getX() - (view.getWidth()/2));
            view.setY(view.getY() + motionEvent.getY() - (view.getHeight()/2));
        }
        return true;
    }
}
