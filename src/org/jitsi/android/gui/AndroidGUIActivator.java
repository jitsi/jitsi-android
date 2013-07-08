/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.content.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.android.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.login.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import org.osgi.framework.*;

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
     * Returns currently registered <tt>MetaContactListService</tt> instance.
     * @return currently registered <tt>MetaContactListService</tt> instance.
     */
    public static MetaContactListService getContactListService()
    {
        return ServiceUtils.getService( bundleContext,
                                        MetaContactListService.class );
    }

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
        AndroidGUIActivator.bundleContext = bundleContext;

        Context androidContext = JitsiApplication.getGlobalContext();

        SecurityAuthority secuirtyAuthority
                = new AndroidSecurityAuthority(androidContext);

        AndroidLoginRenderer loginRenderer
                = new AndroidLoginRenderer(androidContext, secuirtyAuthority);

        loginManager = new LoginManager(loginRenderer);

        // Register the alert service android implementation.
        AlertUIService alertServiceImpl = new AlertUIServiceImpl(
                androidContext);

        bundleContext.registerService(
                AlertUIService.class.getName(),
                alertServiceImpl,
                null);

        // Registers UIService stub
        AndroidUIService uiService = new AndroidUIService( secuirtyAuthority);

        bundleContext.registerService(
                UIService.class.getName(),
                uiService,
                null);

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
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {

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
        return ServiceUtils.getService( bundleContext,
                                        ConfigurationService.class );
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
}
