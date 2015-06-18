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
package org.jitsi.android.gui.account;

import android.content.*;
import android.graphics.drawable.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.*;
import org.jitsi.android.gui.util.event.EventListenerList;
import org.jitsi.service.resources.*;

import org.osgi.framework.*;

import java.beans.*;
import java.io.*;

/**
 * Class exposes account information for specified {@link AccountID}
 * in a form that can be easily used for building GUI.
 * It tracks changes of {@link PresenceStatus}, {@link RegistrationState}
 * and avatar changes and passes them as an {@link AccountEvent}
 * to registered {@link EventListener}s.<br/>
 * It also provides default values for fields that may be currently
 * unavailable from corresponding {@link OperationSet}
 * or {@link ProtocolProviderService}.
 * 
 * @author Pawel Domas
 */
public class Account
    implements ProviderPresenceStatusListener,
        RegistrationStateChangeListener,
        ServiceListener,
        AvatarListener
{
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(Account.class);

    /**
     * The {@link ProtocolProviderService} if is currently available
     */
    private ProtocolProviderService protocolProvider;

    /**
     * The encapsulated {@link AccountID}
     */
    private final AccountID accountID;

    /**
     * The {@link BundleContext} of parent
     * {@link org.jitsi.service.osgi.OSGiActivity}
     */
    private final BundleContext bundleContext;

    /**
     * The {@link Context} of parent {@link android.app.Activity}
     */
    private final Context activityContext;

    /**
     * List of {@link EventListener}s that listen for {@link AccountEvent}s.
     */
    private EventListenerList<AccountEvent> listeners =
            new EventListenerList<AccountEvent>();
    
    /**
     * The {@link Drawable} representing protocol's image
     */
    private Drawable protocolIcon;

    /**
     * Current avatar image
     */
    private Drawable avatarIcon;

    /**
     * Creates new instance of {@link Account}
     *
     * @param accountID the {@link AccountID} that will be encapsulated
     *  by this class
     * @param context the {@link BundleContext} of parent
     *  {@link org.jitsi.service.osgi.OSGiActivity}
     * @param activityContext the {@link Context} of parent
     *  {@link android.app.Activity}
     */
    Account(AccountID accountID, BundleContext context, Context activityContext)
    {
        this.accountID = accountID;

        setProtocolProvider(
                AccountUtils.getRegisteredProviderForAccount(accountID));

        this.bundleContext = context;
        this.bundleContext.addServiceListener(this);

        this.activityContext = activityContext;

        this.protocolIcon = initProtocolIcon();
    }

    /**
     * Tries to retrieve the protocol's icon
     *
     * @return protocol's icon
     */
    private Drawable initProtocolIcon()
    {
        byte[] blob = null;

        if(protocolProvider != null)
            blob = protocolProvider.getProtocolIcon().getIcon(
                    ProtocolIcon.ICON_SIZE_32x32);

        if(blob != null)
            return AndroidImageUtil.drawableFromBytes(blob);

        String iconPath = accountID.getAccountPropertyString(
                ProtocolProviderFactory.ACCOUNT_ICON_PATH);

        if (iconPath != null)
        {
            blob = loadIcon(iconPath);
            if(blob != null)
                return AndroidImageUtil.drawableFromBytes(blob);
        }

        return null;
    }

    /**
     * Loads an image from a given image path.
     *
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath)
    {
        ResourceManagementService resources
                = AndroidGUIActivator.getResourcesService();
        byte[] icon = null;

        if (resources != null)
        {
            InputStream is = resources.getImageInputStreamForPath(imagePath);

            if(is == null)
                return null;

            try
            {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while (-1 != (read = is.read(buffer)))
                {
                    bout.write(buffer, 0, read);
                }
                icon = bout.toByteArray();
            }
            catch (IOException ioex)
            {
                logger.error("Failed to load protocol icon: "+imagePath, ioex);
            }
        }
        return icon;
    }

    /**
     * Gets the {@link ProtocolProviderService} for encapsulated
     * {@link AccountID}
     *
     * @return the {@link ProtocolProviderService} if currently registered
     *  for encapsulated {@link AccountID} or <tt>null</tt> otherwise
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Tries to get the {@link OperationSetPresence} for encapsulated
     * {@link AccountID}
     *
     * @return the {@link OperationSetPresence} if the protocol is active
     *  and supports it or <tt>null</tt> otherwise
     */
    OperationSetPresence getPresenceOpSet()
    {
        if(protocolProvider == null)
            return null;

        return protocolProvider.getOperationSet(OperationSetPresence.class);
    }

    /**
     * Tries to get the {@link OperationSetAvatar} if the protocol supports it
     * and is currently active
     *
     * @return the {@link OperationSetAvatar} for encapsulated {@link AccountID}
     * if it's supported and active or <tt>null</tt> otherwise
     */
    OperationSetAvatar getAvatarOpSet()
    {
        if(protocolProvider == null)
            return null;

        return protocolProvider.getOperationSet(OperationSetAvatar.class);
    }

    /**
     * Tracks the de/registration of {@link ProtocolProviderService}
     * for encapsulated {@link AccountID}
     *
     * @param event the {@link ServiceEvent}
     */
    public void serviceChanged(ServiceEvent event) 
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING) 
        {
            return;
        }
        Object sourceService =
                bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(sourceService instanceof ProtocolProviderService)) 
        {
            return;
        }

        ProtocolProviderService protocolProvider
                = (ProtocolProviderService) sourceService;
        if (!protocolProvider.getAccountID().equals(accountID)) 
        {
            // Only interested for this account
            return;
        }

        if (event.getType() == ServiceEvent.REGISTERED) 
        {
            setProtocolProvider(protocolProvider);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING) 
        {
            setProtocolProvider(null);
        }
    }

    /**
     * Sets the currently active {@link ProtocolProviderService}
     * for encapsulated {@link #accountID}.
     *
     * @param protocolProvider if not <tt>null</tt> all listeners are
     *  registered otherwise listeners are unregistered from current
     *  {@link #protocolProvider}
     */
    private void setProtocolProvider(ProtocolProviderService protocolProvider)
    {
        if(this.protocolProvider != null
                && protocolProvider != null)
        {
            if(this.protocolProvider == protocolProvider)
            {
                // It's the same
                return;
            }

            logger.warn("This account have already registered provider "
                            + "- will update");
            // Unregister old
            setProtocolProvider(null);
            // Register new
            setProtocolProvider(protocolProvider);
        }

        if(protocolProvider != null)
        {
            protocolProvider.addRegistrationStateChangeListener(this);

            OperationSetPresence presenceOpSet =
                    protocolProvider.getOperationSet(
                            OperationSetPresence.class);
            if(presenceOpSet == null)
            {
                logger.warn(protocolProvider.getProtocolDisplayName()
                        + "does not support presence operations");
            }
            else
            {
                presenceOpSet.addProviderPresenceStatusListener(this);
            }

            OperationSetAvatar avatarOpSet =
                    protocolProvider.getOperationSet(OperationSetAvatar.class);
            if(avatarOpSet != null)
            {
                avatarOpSet.addAvatarListener(this);
            }

            logger.trace("Registered listeners for "+protocolProvider);
        }
        else if(this.protocolProvider != null)
        {
            // Unregister listeners
            this.protocolProvider.removeRegistrationStateChangeListener(this);

            OperationSetPresence presenceOpSet =
                    this.protocolProvider.getOperationSet(
                            OperationSetPresence.class);
            if(presenceOpSet != null)
            {
                presenceOpSet.removeProviderPresenceStatusListener(this);
            }

            OperationSetAvatar avatarOpSet =
                    this.protocolProvider.getOperationSet(
                            OperationSetAvatar.class);
            if(avatarOpSet != null)
            {
                avatarOpSet.removeAvatarListener(this);
            }
        }
        this.protocolProvider = protocolProvider;
    }

    /**
     * Unregisters from all services and clears {@link #listeners}
     */
    public void destroy()
    {
        setProtocolProvider(null);

        bundleContext.removeServiceListener(this);

        listeners.clear();
    }

    /**
     * Adds {@link EventListener} that will be listening for changed
     * that occurred to this {@link Account}. In particular these are
     * the registration status, presence status and avatar events.
     *
     * @param listener the {@link EventListener} that listens for changes
     *  on this {@link Account} object
     */
    public void addAccountEventListener(EventListener<AccountEvent> listener)
    {
        logger.trace("Added change listener "+listener);
        listeners.addEventListener(listener);
    }

    /**
     * Removes the given <tt>listener</tt> from observers list
     *
     * @param listener the {@link EventListener} that doesn't want to be
     *  notified about the changes to this {@link Account} anymore
     */
    public void removeAccountEventListener(EventListener<AccountEvent> listener)
    {
        logger.trace("Removed change listener "+listener);
        listeners.removeEventListener(listener);
    }

    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {
        logger.trace("Provider status notifcation");        
        listeners.notifyEventListeners(
                new AccountEvent(
                        this, 
                        AccountEvent.PRESENCE_STATUS_CHANGE)
        );
    }

    public void providerStatusMessageChanged(PropertyChangeEvent evt)
    {
        logger.trace("Provider status msg notification");
        listeners.notifyEventListeners(
                new AccountEvent(
                        this, 
                        AccountEvent.STATUS_MSG_CHANGE)
        );
    }

    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        logger.trace("Provider registration notifcation");
        listeners.notifyEventListeners(
                new AccountEvent(
                        this,
                        AccountEvent.REGISTRATION_CHANGE)
        );
    }

    public void avatarChanged(AvatarEvent event)
    {
        logger.trace("Avatar changed notification");
        updateAvatar(event.getNewAvatar());
        listeners.notifyEventListeners(
                new AccountEvent(
                        this,
                        AccountEvent.AVATAR_CHANGE)
        );
    }

    /**
     * Returns the display name
     *
     * @return the display name of this {@link Account}
     */
    public String getAccountName()
    {
        return accountID.getDisplayName();
    }

    /**
     * Returns the current presence status name of this {@link Account}
     *
     * @return current presence status name
     */
    public String getStatusName()
    {
        OperationSetPresence presence = getPresenceOpSet();
        if(presence != null)
        {
            return presence.getPresenceStatus().getStatusName();
        }
        return GlobalStatusEnum.OFFLINE_STATUS;
    }

    /**
     * Returns the {@link Drawable} protocol icon
     *
     * @return the protocol's icon valid for this {@link Account}
     */
    public Drawable getProtocolIcon()
    {
        return protocolIcon;
    }

    /**
     * Returns the current {@link PresenceStatus} icon
     *
     * @return the icon describing actual {@link PresenceStatus}
     *  of this {@link Account}
     */
    public Drawable getStatusIcon()
    {
        OperationSetPresence presence = getPresenceOpSet();

        if(presence != null)
        {
            byte[] statusBlob = presence.getPresenceStatus().getStatusIcon();

            if(statusBlob != null)
                return AndroidImageUtil.drawableFromBytes(statusBlob);
        }

        return AccountUtil.getDefaultPresenceIcon(
                activityContext,
                accountID.getProtocolName());
    }

    /**
     * Returns <tt>true</tt> if this {@link Account} is enabled
     *
     * @return <tt>true</tt> if this {@link Account} is enabled
     */
    boolean isEnabled()
    {
        return accountID.isEnabled();
    }

    /**
     * Returns encapsulated {@link AccountID}
     *
     * @return the {@link AccountID} encapsulated by this instance
     *  of {@link Account}
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Sets the avatar icon. If <tt>newAvatar</tt> is specified as <tt>null</tt>
     * the the default one is set
     *
     * @param newAvatar an array of bytes with raw avatar image data
     */
    private void updateAvatar(byte[] newAvatar)
    {
        if(newAvatar == null)
        {
            avatarIcon = AccountUtil.getDefaultAvatarIcon(
                    activityContext);
        }
        else
        {
            avatarIcon = AndroidImageUtil.drawableFromBytes(newAvatar);
        }
    }

    /**
     * Returns the {@link Drawable} of account avatar
     *
     * @return the {@link Drawable} of account avatar
     */
    public Drawable getAvatarIcon()
    {
        if(avatarIcon == null)
        {
            byte[] avatarBlob = null;

            try
            {
                OperationSetAvatar avatarOpSet = getAvatarOpSet();
                if(avatarOpSet != null)
                {
                    avatarBlob = avatarOpSet.getAvatar();
                }
            }
            catch (IllegalStateException exc)
            {
                logger.error(
                    "Error retrieving avatar: " + exc.getMessage());
            }

            updateAvatar(avatarBlob);
        }
        return avatarIcon;
    }
}