/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
