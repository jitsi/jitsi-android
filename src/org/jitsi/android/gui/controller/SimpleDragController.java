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
