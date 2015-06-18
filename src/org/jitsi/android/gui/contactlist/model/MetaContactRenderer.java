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
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.*;

import java.util.*;

/**
 * Class used to obtain UI specific data for <tt>MetaContact</tt> instances.
 *
 * @author Pawel Domas
 */
public class MetaContactRenderer
    implements UIContactRenderer
{
    @Override
    public boolean isSelected(Object contactImpl)
    {
        return MetaContactListAdapter.isContactSelected(
            (MetaContact) contactImpl);
    }

    @Override
    public String getDisplayName(Object contactImpl)
    {
        return ((MetaContact)contactImpl).getDisplayName();
    }

    @Override
    public String getStatusMessage(Object contactImpl)
    {
        MetaContact metaContact = (MetaContact) contactImpl;
        String displayDetails = getDisplayDetails(metaContact);
        return displayDetails != null ? displayDetails : "";
    }

    @Override
    public boolean isDisplayBold(Object contactImpl)
    {
        return ChatSessionManager
            .getActiveChat((MetaContact) contactImpl) != null;
    }

    @Override
    public Drawable getAvatarImage(Object contactImpl)
    {
        return getAvatarDrawable((MetaContact) contactImpl);
    }

    @Override
    public Drawable getStatusImage(Object contactImpl)
    {
        return getStatusDrawable((MetaContact) contactImpl);
    }

    @Override
    public boolean isShowVideoCallBtn(Object contactImpl)
    {
        return isShowButton( (MetaContact) contactImpl,
                             OperationSetVideoTelephony.class);
    }

    @Override
    public boolean isShowCallBtn(Object contactImpl)
    {
        return isShowButton( (MetaContact) contactImpl,
                             OperationSetBasicTelephony.class);
    }

    @Override
    public String getDefaultAddress(Object contactImpl)
    {
        return ((MetaContact)contactImpl).getDefaultContact().getAddress();
    }

    /**
     * Returns the display details for the underlying <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which details we're looking
     * for
     * @return the display details for the underlying <tt>MetaContact</tt>
     */
    private static String getDisplayDetails(MetaContact metaContact)
    {
        String displayDetails = null;

        boolean subscribed = false;

        Iterator<Contact> protoContacts = metaContact.getContacts();

        String subscriptionDetails = null;

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetExtendedAuthorizations authOpSet
                = protoContact.getProtocolProvider()
                .getOperationSet(OperationSetExtendedAuthorizations.class);

            if (authOpSet != null
                && authOpSet.getSubscriptionStatus(protoContact) != null
                && !authOpSet.getSubscriptionStatus(protoContact)
                .equals(
                    OperationSetExtendedAuthorizations.SubscriptionStatus.Subscribed))
            {
                OperationSetExtendedAuthorizations.SubscriptionStatus status
                    = authOpSet.getSubscriptionStatus(protoContact);

                if (status.equals(
                    OperationSetExtendedAuthorizations.SubscriptionStatus.SubscriptionPending))
                    subscriptionDetails
                        = AndroidGUIActivator.getResourcesService()
                        .getI18NString("service.gui.WAITING_AUTHORIZATION");
                else if (status.equals(
                    OperationSetExtendedAuthorizations.SubscriptionStatus.NotSubscribed))
                    subscriptionDetails
                        = AndroidGUIActivator.getResourcesService()
                        .getI18NString("service.gui.NOT_AUTHORIZED");
            }
            else if (protoContact.getStatusMessage() != null
                && protoContact.getStatusMessage().length() > 0)
            {
                subscribed = true;
                displayDetails = protoContact.getStatusMessage();
                break;
            }
            else
            {
                subscribed = true;
            }
        }

        if ((displayDetails == null
            || displayDetails.length() <= 0)
            && !subscribed
            && subscriptionDetails != null
            && subscriptionDetails.length() > 0)
            displayDetails = subscriptionDetails;

        return displayDetails;
    }

    private static boolean isShowButton(
        MetaContact metaContact,
        Class<? extends OperationSet> opSetClass)
    {
        return metaContact.getDefaultContact(opSetClass) != null;
    }

    /**
     * Returns the status <tt>Drawable</tt> for the given <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which status drawable we're
     * looking for
     * @return a <tt>BitmapDrawable</tt> object representing the status of
     * the given <tt>MetaContact</tt>
     */
    public static BitmapDrawable getAvatarDrawable(MetaContact metaContact)
    {
        return getCachedAvatarFromBytes(metaContact.getAvatar());
    }

    /**
     * Returns avatar <tt>BitmapDrawable</tt> with rounded corners. Bitmap will
     * be cached in app global drawable cache.
     * @param avatar raw avatar image data.
     * @return avatar <tt>BitmapDrawable</tt> with rounded corners
     */
    static BitmapDrawable getCachedAvatarFromBytes(byte[] avatar)
    {
        if(avatar == null)
            return null;

        String bmpKey = String.valueOf(avatar.hashCode());
        DrawableCache cache = JitsiApplication.getImageCache();

        BitmapDrawable avatarImage = cache.getBitmapFromMemCache(bmpKey);
        if(avatarImage == null)
        {
            BitmapDrawable roundedAvatar
                = AndroidImageUtil.roundedDrawableFromBytes(avatar);
            if(roundedAvatar != null)
            {
                avatarImage = roundedAvatar;

                cache.cacheImage(bmpKey, avatarImage);
            }
        }
        return avatarImage;
    }

    /**
     * Returns the status <tt>Drawable</tt> for the given <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, which status drawable we're
     * looking for
     * @return a <tt>Drawable</tt> object representing the status of the given
     * <tt>MetaContact</tt>
     */
    public static Drawable getStatusDrawable(MetaContact metaContact)
    {
        byte[] statusImage = getStatusImage(metaContact);

        if (statusImage != null)
            return AndroidImageUtil.drawableFromBytes(statusImage);

        return null;
    }

    /**
     * Returns the array of bytes representing the status image of the given
     * <tt>MetaContact</tt>.
     *
     * @return the array of bytes representing the status image of the given
     * <tt>MetaContact</tt>
     */
    private static byte[] getStatusImage(MetaContact metaContact)
    {
        PresenceStatus status = null;
        Iterator<Contact> contactsIter = metaContact.getContacts();
        while (contactsIter.hasNext())
        {
            Contact protoContact = contactsIter.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (status == null)
                status = contactStatus;
            else
                status = (contactStatus.compareTo(status) > 0)
                    ? contactStatus
                    : status;
        }

        return StatusUtil.getContactStatusIcon(status);
    }
}
