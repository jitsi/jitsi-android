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

import android.graphics.drawable.*;

/**
 * Interface used to obtain data required to display contacts.
 * Implementing classes can expect to receive their implementation specific
 * objects in calls to any method of this interface.
 *
 * @author Pawel Domas
 */
public interface UIContactRenderer
{
    /**
     * Return <tt>true</tt> if given contact is considered to be currently
     * selected.
     * @param contactImpl contact instance.
     * @return <tt>true</tt> if given contact is considered to be currently
     *         selected.
     */
    boolean isSelected(Object contactImpl);

    /**
     * Returns contact display name.
     * @param contactImpl contact instance.
     * @return contact display name.
     */
    String getDisplayName(Object contactImpl);

    /**
     * Returns contact status message.
     * @param contactImpl contact instance.
     * @return contact status message.
     */
    String getStatusMessage(Object contactImpl);

    /**
     * Returns <tt>true</tt> if given contact name should be displayed in bold.
     * @param contactImpl contact instance.
     * @return <tt>true</tt> if given contact name should be displayed in bold.
     */
    boolean isDisplayBold(Object contactImpl);

    /**
     * Returns contact avatar image.
     * @param contactImpl contact instance.
     * @return contact avatar image.
     */
    Drawable getAvatarImage(Object contactImpl);

    /**
     * Returns contact status image.
     * @param contactImpl contact instance.
     * @return contact status image.
     */
    Drawable getStatusImage(Object contactImpl);

    /**
     * Returns <tt>true</tt> if video call button should be displayed for given
     * contact. That is if contact has valid default address that can be used to
     * make video calls.
     * @param contactImpl contact instance.
     * @return <tt>true</tt> if video call button should be displayed for given
     *         contact.
     */
    boolean isShowVideoCallBtn(Object contactImpl);

    /**
     * Returns <tt>true</tt> if call button should be displayed next to the
     * contact. That means that it will returns valid default address that can
     * be used to make audio calls.
     * @param contactImpl contact instance.
     * @return <tt>true</tt> if call button should be displayed next to the
     *         contact.
     */
    boolean isShowCallBtn(Object contactImpl);

    /**
     * Returns default contact address that can be used to establish an outgoing
     * connection.
     * @param contactImpl contact instance.
     * @return default contact address that can be used to establish an outgoing
     *         connection.
     */
    String getDefaultAddress(Object contactImpl);
}
