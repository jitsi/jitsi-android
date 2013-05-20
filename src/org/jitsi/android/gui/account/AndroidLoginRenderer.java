/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import java.beans.*;

import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.call.*;
import org.jitsi.android.gui.util.*;

import android.content.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.account.*;
import org.jitsi.service.osgi.*;

/**
 * The <tt>AndroidLoginRenderer</tt> is the Android renderer for login events.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class AndroidLoginRenderer
    implements LoginRenderer
{
    /**
     * The logger
     */
    private final static Logger logger 
            = Logger.getLogger(AndroidLoginRenderer.class);
    
    /**
     * The android application context.
     */
    private final Context androidContext;

    /**
     * The <tt>CallListener</tt>.
     */
    private CallListener androidCallListener;

    /**
     * The android implementation of the provider presence listener.
     */
    private ProviderPresenceStatusListener androidPresenceListener;

    /**
     * The security authority used by this login renderer.
     */
    private final SecurityAuthority securityAuthority;

    /**
     * Creates an instance of <tt>AndroidLoginRenderer</tt> by specifying the
     * current <tt>Context</tt>.
     *
     * @param appContext the current android application context
     * @param defaultSecurityAuthority the security authority that will be used
     *        by this login renderer
     */
    public AndroidLoginRenderer( Context appContext,
                                 SecurityAuthority defaultSecurityAuthority )
    {
        this.androidContext = appContext;

        androidCallListener = new AndroidCallListener(appContext);

        securityAuthority = defaultSecurityAuthority;
    }

    /**
     * Adds the user interface related to the given protocol provider.
     *
     * @param protocolProvider the protocol provider for which we add the user
     * interface
     */
    public void addProtocolProviderUI(ProtocolProviderService protocolProvider)
    {
        OperationSetBasicTelephony<?> telOpSet
            = protocolProvider.getOperationSet(
                OperationSetBasicTelephony.class);

        if (telOpSet != null)
        {
            telOpSet.addCallListener(androidCallListener);
        }

        OperationSetPresence presenceOpSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        if (presenceOpSet != null)
        {
            androidPresenceListener = new UIProviderPresenceStatusListener();

            presenceOpSet.addProviderPresenceStatusListener(
                androidPresenceListener);
        }
    }

    /**
     * Removes the user interface related to the given protocol provider.
     *
     * @param protocolProvider the protocol provider to remove
     */
    public void removeProtocolProviderUI(
        ProtocolProviderService protocolProvider)
    {
        OperationSetBasicTelephony<?> telOpSet
            = protocolProvider.getOperationSet(
                    OperationSetBasicTelephony.class);

        if (telOpSet != null)
        {
            telOpSet.removeCallListener(androidCallListener);
        }

        OperationSetPresence presenceOpSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        if (presenceOpSet != null && androidPresenceListener != null)
        {
            presenceOpSet.removeProviderPresenceStatusListener(
                androidPresenceListener);
        }
    }

    /**
     * Starts the connecting user interface for the given protocol provider.
     *
     * @param protocolProvider the protocol provider for which we add the
     * connecting user interface
     */
    public void startConnectingUI(ProtocolProviderService protocolProvider) {}

    /**
     * Stops the connecting user interface for the given protocol provider.
     *
     * @param protocolProvider the protocol provider for which we remove the
     * connecting user interface
     */
    public void stopConnectingUI(ProtocolProviderService protocolProvider) {}

    /**
     * Indicates that the given protocol provider has been connected at the
     * given time.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the connected account
     * @param date the date/time at which the account has connected
     */
    public void protocolProviderConnected(
        ProtocolProviderService protocolProvider,
        long date)
    {
        showStatusNotification(
            protocolProvider,
            androidContext.getString(R.string.service_gui_ONLINE),
            date);
    }

    /**
     * Indicates that a protocol provider connection has failed.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * connection failed
     * @param loginManagerCallback the <tt>LoginManager</tt> implementation,
     * which is managing the process
     */
    public void protocolProviderConnectionFailed(
        final ProtocolProviderService protocolProvider,
        final LoginManager loginManagerCallback)
    {
        AccountID accountID = protocolProvider.getAccountID();

        AndroidUtils.showAlertConfirmDialog(
            androidContext,
            androidContext.getString(R.string.service_gui_ERROR),
            androidContext.getString(
                R.string.service_gui_CONNECTION_FAILED_MSG,
                accountID.getUserID(),
                accountID.getService()),
            androidContext.getString(R.string.service_gui_RETRY),
            new DialogActivity.DialogListener()
            {
                public void onConfirmClicked(DialogActivity dialog)
                {
                    loginManagerCallback.login(protocolProvider);
                }

                public void onDialogCancelled(DialogActivity dialog)
                {

                }
            });
    }

    /**
     * Returns the <tt>SecurityAuthority</tt> implementation related to this
     * login renderer.
     *
     * @param protocolProvider the specific <tt>ProtocolProviderService</tt>,
     * for which we're obtaining a security authority
     * @return the <tt>SecurityAuthority</tt> implementation related to this
     * login renderer
     */
    public SecurityAuthority getSecurityAuthorityImpl(
        ProtocolProviderService protocolProvider)
    {
        return securityAuthority;
    }

    /**
     * Shows a status notification for the given <tt>protocolProvider</tt>,
     * <tt>status</tt> and <tt>date</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account concerned by the status change
     * @param status the new status string
     * @param date the date on which the status change has happened
     */
    private void showStatusNotification(
                                    ProtocolProviderService protocolProvider,
                                    String status,
                                    long date)
    {
        int notificationID = OSGiService.getGeneralNotificationId();
        if(notificationID == -1)
        {
            logger.warn("Failed to display status notification because" +
                    " there's no global notification icon available.");            
            return;
        }
        
        AndroidUtils.updateGeneralNotification(
            androidContext,
            notificationID,
            androidContext.getString(R.string.app_name),
            protocolProvider.getAccountID().getAccountAddress()
                + " " + status,
            date,
            JitsiApplication.getHomeScreenActivityClass());
    }

    /**
     * Listens for all providerStatusChanged and providerStatusMessageChanged
     * events in order to refresh the account status panel, when a status is
     * changed.
     */
    private class UIProviderPresenceStatusListener
        implements ProviderPresenceStatusListener
    {
        public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
        {
            showStatusNotification(
                evt.getProvider(),
                evt.getNewStatus().getStatusName(),
                System.currentTimeMillis());
        }

        public void providerStatusMessageChanged(PropertyChangeEvent evt) {}
    }

    /**
     * Indicates if the given <tt>protocolProvider</tt> related user interface
     * is already rendered.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * related user interface we're looking for
     * @return <tt>true</tt> if the given <tt>protocolProvider</tt> related user
     * interface is already rendered
     */
    public boolean containsProtocolProviderUI(
        ProtocolProviderService protocolProvider)
    {
        return false;
    }
}
