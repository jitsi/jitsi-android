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
package org.jitsi.android.gui;

import android.content.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.systray.*;

import net.java.sip.communicator.service.protocol.globalstatus.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.android.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.login.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.otr.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import org.osgi.framework.*;

import java.util.*;

/**
 * Creates <tt>LoginManager</tt> and registers <tt>AlertUIService</tt>.
 * It's moved here from launcher <tt>Activity</tt> because it could be created
 * multiple times and result in multiple objects/registrations for those
 * services. It also guarantees that they wil be registered each time OSGI
 * service starts.
 *
 * @author Pawel Domas
 */
public class AndroidGUIActivator
        implements BundleActivator
{
    /**
     * The {@link LoginManager}
     */
    private static LoginManager loginManager;

    /**
     * The OSGI bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The presence status handler
     */
    private PresenceStatusHandler presenceStatusHandler;

    /**
     * Android login renderer impl.
     */
    private static AndroidLoginRenderer loginRenderer;

    /**
     * Configuration service instance.
     */
    private static ConfigurationService configService;

    /**
     * <tt>GlobalDisplayDetailsService</tt> instance.
     */
    private static GlobalDisplayDetailsService globalDisplayService;

    /**
     * <tt>MetaContactListService</tt> cached instance.
     */
    private static MetaContactListService metaContactList;

    private static MessageHistoryService messageHistoryService;

    /**
     * Replacement services observer.
     */
    private static ServiceObserver<ReplacementService> replacementServices
            = new ServiceObserver<ReplacementService>(ReplacementService.class);

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
        Context androidContext = JitsiApplication.getGlobalContext();

        SecurityAuthority secuirtyAuthority
                = new AndroidSecurityAuthority();

        loginRenderer = new AndroidLoginRenderer(secuirtyAuthority);

        loginManager = new LoginManager(loginRenderer);

        // Register the alert service android implementation.
        AlertUIService alertServiceImpl = new AlertUIServiceImpl(
                androidContext);

        bundleContext.registerService(
                AlertUIService.class.getName(),
                alertServiceImpl,
                null);

        // Creates and registers presence status handler
        this.presenceStatusHandler = new PresenceStatusHandler();
        presenceStatusHandler.start(bundleContext);

        AccountManager accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);

        if(accountManager.getStoredAccounts().size() > 0)
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    loginManager.runLogin();
                }
            }).start();
        }

        ConfigurationUtils.loadGuiConfigurations();
        // Start watching replacement services
        replacementServices.start(bundleContext);

        // Register show history settings OTR link listener
        ChatSessionManager.addChatLinkListener(
            new OtrFragment.ShowHistoryLinkListener());

        AndroidGUIActivator.bundleContext = bundleContext;

        // Registers UIService stub
        AndroidUIServiceImpl uiService
            = new AndroidUIServiceImpl( secuirtyAuthority);

        bundleContext.registerService(
            UIService.class.getName(), uiService, null);
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {
        replacementServices.stop(bundleContext);
        presenceStatusHandler.stop(bundleContext);

        // Clears chat sessions
        ChatSessionManager.dispose();

        loginRenderer = null;
        loginManager = null;
        configService = null;
        globalDisplayService = null;
        metaContactList = null;
        AndroidGUIActivator.bundleContext = null;
    }

    /**
     * Returns <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResourcesService()
    {
        return ServiceUtils.getService( bundleContext,
                                        ResourceManagementService.class);
    }

    /**
     * Returns <tt>MetaContactListService</tt>.
     *
     * @return the <tt>MetaContactListService</tt>.
     */
    public static MetaContactListService getContactListService()
    {
        if(metaContactList == null)
        {
            metaContactList = ServiceUtils.getService(
                bundleContext, MetaContactListService.class);
        }
        return metaContactList;
    }

    /**
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle
     * context.
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle
     * context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if(bundleContext == null)
            return null;

        return ServiceUtils.getService( bundleContext,
                                        GlobalStatusService.class);
    }

    /**
     * Returns the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context
     */
    public static GlobalDisplayDetailsService getGlobalDisplayDetailsService()
    {
        if(globalDisplayService == null)
        {
            globalDisplayService
                = ServiceUtils.getService( bundleContext,
                                           GlobalDisplayDetailsService.class );
        }
        return globalDisplayService;
    }

    public static MetaHistoryService getMetaHistoryService()
    {
        return ServiceUtils.getService(bundleContext, MetaHistoryService.class);
    }

    /**
     * Gets the service giving access to message history.
     *
     * @return the service giving access to message history.
     */
    public static MessageHistoryService getMessageHistoryService()
    {
        if (messageHistoryService == null)
            messageHistoryService = ServiceUtils.getService(bundleContext,
                MessageHistoryService.class);
        return messageHistoryService;
    }

    /**
     * Returns the <tt>LoginManager</tt> for Android application.
     * @return the <tt>LoginManager</tt> for Android application.
     */
    public static LoginManager getLoginManager()
    {
        return loginManager;
    }

    /**
     * Returns the <tt>ConfigurationService</tt>.
     * @return the <tt>ConfigurationService</tt>.
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService
                = ServiceUtils.getService( bundleContext,
                                           ConfigurationService.class );
        }
        return configService;
    }

    /**
     * Returns the <tt>CredentialsStorageService</tt>.
     * @return the <tt>CredentialsStorageService</tt>.
     */
    public static CredentialsStorageService getCredentialsStorageService()
    {
        return ServiceUtils.getService( bundleContext,
                                        CredentialsStorageService.class );
    }

    /**
     * Returns <tt>SystrayService</tt> instance.
     * @return <tt>SystrayService</tt> instance.
     */
    public static SystrayService getSystrayService()
    {
        return ServiceUtils.getService( bundleContext,
                                        SystrayService.class );
    }

    /**
     * Return Android login renderer.
     * @return Android login renderer.
     */
    public static AndroidLoginRenderer getLoginRenderer()
    {
        return loginRenderer;
    }

    /**
     * Returns all <tt>ReplacementService</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ReplacementService</tt> implementation obtained from the
     *         bundle context
     */
    public static List<ReplacementService> getReplacementSources()
    {
        return replacementServices.getServices();
    }
}
