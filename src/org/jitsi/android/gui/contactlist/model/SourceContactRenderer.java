/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist.model;

import android.graphics.drawable.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * Class used to obtain UI specific data for <tt>SourceContact</tt> instances.
 *
 * @author Pawel Domas
 */
public class SourceContactRenderer
    implements UIContactRenderer
{
    /**
     * Class is stateless and does not take any parameters so we can use single
     * instance for now.
     */
    public static final SourceContactRenderer instance
        = new SourceContactRenderer();

    private SourceContactRenderer()
    {

    }

    @Override
    public boolean isSelected(Object contactImpl)
    {
        return false;
    }

    @Override
    public String getDisplayName(Object contactImpl)
    {
        SourceContact contact = (SourceContact) contactImpl;
        return contact.getDisplayName();
    }

    @Override
    public String getStatusMessage(Object contactImpl)
    {
        SourceContact contact = (SourceContact) contactImpl;
        return contact.getDisplayDetails();
    }

    @Override
    public boolean isDisplayBold(Object contactImpl)
    {
        return false;
    }

    @Override
    public Drawable getAvatarImage(Object contactImpl)
    {
        SourceContact contact = (SourceContact) contactImpl;

        return MetaContactRenderer.getCachedAvatarFromBytes(
                    contact.getImage());
    }

    @Override
    public Drawable getStatusImage(Object contactImpl)
    {
        return null;
    }

    @Override
    public boolean isShowVideoCallBtn(Object contactImpl)
    {
        return false;
    }

    @Override
    public boolean isShowCallBtn(Object contactImpl)
    {
        return true;
    }

    @Override
    public String getDefaultAddress(Object contactImpl)
    {
        return ((SourceContact)contactImpl).getContactAddress();
    }
}
