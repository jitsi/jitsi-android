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
package org.jitsi.android.gui.util.event;

import java.util.*;

/**
 * Utility class to that stores the list of {@link EventListener}s.
 * Provides add/remove and notify all operations.
 * 
 * @param <T> the event object class
 * 
 * @author Pawel Domas
 */
public class EventListenerList<T>
{
    /**
     * The list of {@link EventListener}
     */
    private ArrayList<EventListener<T>> listeners =
            new ArrayList<EventListener<T>>();

    /**
     * Adds the <tt>listener</tt> to the list
     *
     * @param listener the {@link EventListener} that will
     *  be added to the list
     */
    public void addEventListener(EventListener<T> listener)
    {
        if(!listeners.contains(listener))
            listeners.add(listener);
    }

    /**
     * Removes the <tt>listener</tt> from the list
     *
     * @param listener the {@link EventListener} that will
     *  be removed from the list
     */
    public void removeEventListener(EventListener<T> listener)
    {
        listeners.remove(listener);
    }

    /**
     * Runs the event change notification on listeners list
     *
     * @param eventObject the source object of the event
     */
    public void notifyEventListeners(T eventObject)
    {
        for(EventListener<T> l : listeners)
        {
            l.onChangeEvent(eventObject);
        }
    }

    /**
     * Clears the listeners list
     */
    public void clear()
    {
        listeners.clear();
    }
}
