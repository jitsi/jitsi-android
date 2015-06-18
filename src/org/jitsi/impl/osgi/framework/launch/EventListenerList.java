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
package org.jitsi.impl.osgi.framework.launch;

import java.lang.reflect.*;
import java.util.*;

import org.osgi.framework.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class EventListenerList
{
    private final List<Element<?>> elements = new LinkedList<Element<?>>();

    public synchronized <T extends EventListener> boolean add(
            Bundle bundle,
            Class<T> clazz,
            T listener)
    {
        if (bundle == null)
            throw new NullPointerException("bundle");
        if (clazz == null)
            throw new NullPointerException("clazz");
        if (listener == null)
            throw new NullPointerException("listener");

        int index = indexOf(bundle, clazz, listener);

        if (index == -1)
            return elements.add(new Element<T>(bundle, clazz, listener));
        else
            return false;
    }

    public synchronized <T extends EventListener> T[] getListeners(
            Class<T> clazz)
    {
        EventListener[] eventListeners = new EventListener[elements.size()];
        int count = 0;

        for (Element<?> element : elements)
                if (element.clazz == clazz)
                    eventListeners[count++] = element.listener;

        @SuppressWarnings("unchecked")
        T[] listeners = (T[]) Array.newInstance(clazz, count);

        System.arraycopy(eventListeners, 0, listeners, 0, count);
        return listeners;
    }

    private synchronized <T extends EventListener> int indexOf(
            Bundle bundle,
            Class<T> clazz,
            T listener)
    {
        for (int index = 0, count = elements.size(); index < count; index++)
        {
            Element<?> element = elements.get(index);

            if (element.bundle.equals(bundle)
                    && (element.clazz == clazz)
                    && (element.listener == listener))
                return index;
        }
        return -1;
    }

    public synchronized <T extends EventListener> boolean remove(
            Bundle bundle,
            Class<T> clazz,
            T listener)
    {
        int index = indexOf(bundle, clazz, listener);

        if (index == -1)
            return false;
        else
        {
            elements.remove(index);
            return true;
        }
    }

    public synchronized boolean removeAll(Bundle bundle)
    {
        boolean changed = false;

        for (int index = 0, count = elements.size(); index < count;)
        {
            if (elements.get(index).bundle.equals(bundle)
                    && (elements.remove(index) != null))
                changed = true;
            else
                index++;
        }

        return changed;
    }

    private static class Element<T extends EventListener>
    {
        public final Bundle bundle;

        public final Class<T> clazz;

        public final T listener;

        public Element(Bundle bundle, Class<T> clazz, T listener)
        {
            this.bundle = bundle;
            this.clazz = clazz;
            this.listener = listener;
        }
    }
}
