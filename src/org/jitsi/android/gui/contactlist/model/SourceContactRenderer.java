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
