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
package org.jitsi.android.gui.contactlist.model;

/**
 * Interface used to obtain data required to display the contact group.
 * Implementing classes can expect to receive their implementation specific
 * objects in calls to any method of this interface.
 *
 * @author Pawel Domas
 */
public interface UIGroupRenderer
{
    /**
     * Returns the display name of the contact group identified by given
     * implementation specific group object.
     * @param groupImpl implementation specific contact group instance.
     * @return the display name for given contact group instance.
     */
    public String getDisplayName(Object groupImpl);
}
