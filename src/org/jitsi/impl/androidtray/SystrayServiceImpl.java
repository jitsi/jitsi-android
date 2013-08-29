/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidtray;

import android.content.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;

import net.java.sip.communicator.util.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;

/**
 * Android system tray implementation. Makes use of status bar notifications to
 * provide tray functionality.
 *
 * @author Pawel Domas
 */
public class SystrayServiceImpl
    extends AbstractSystrayService
{
    /**
     *  The logger
     */
    private static final Logger logger
            = Logger.getLogger(SystrayServiceImpl.class);

    /**
     * Popup message handler.
     */
    private final NotificationPopupHandler trayPopupHandler
            = new NotificationPopupHandler();
    /**
     * Popup message click listener impl.
     */
    private final PopupListenerImpl popupMessageListener
            = new PopupListenerImpl();

    /**
     * <tt>BroadcastReceiver</tt> that catches "on click" and "clear" events for
     * displayed notifications.
     */
    private final PopupClickReceiver clickReceiver;

    /**
     * Creates new instance of <tt>SystrayServiceImpl</tt>.
     */
    public SystrayServiceImpl()
    {
        super(AndroidTrayActivator.bundleContext);

        AndroidTrayActivator.bundleContext.registerService(
                PopupMessageHandler.class,
                trayPopupHandler, null );

        initHandlers();

        this.clickReceiver = new PopupClickReceiver(trayPopupHandler);
    }

    /**
     * Set the handler which will be used for popup message
     * @param newHandler the handler to set. providing a null handler is like
     * disabling popup.
     * @return the previously used popup handler.
     */
    public PopupMessageHandler setActivePopupMessageHandler(
            PopupMessageHandler newHandler)
    {
        PopupMessageHandler oldHandler = getActivePopupHandler();

        if (oldHandler != null)
            oldHandler.removePopupMessageListener(popupMessageListener);
        if (newHandler != null)
            newHandler.addPopupMessageListener(popupMessageListener);

        return super.setActivePopupMessageHandler(newHandler);
    }

    /**
     * Starts the service.
     */
    public void start()
    {
        clickReceiver.registerReceiver();
    }

    /**
     * Stops the service.
     */
    public void stop()
    {
        clickReceiver.unregisterReceiver();

        trayPopupHandler.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public void setSystrayIcon(int i)
    {
        System.err.println("Requested icon: "+i);
        switch (i)
        {
            case SC_IMG_AWAY_TYPE:
                //TODO: set away icon here
                break;
            case SC_IMG_DND_TYPE:
                //TODO: dnd icon here
                break;
            case SC_IMG_FFC_TYPE:
                //TODO: set FFC icon here
                break;
            case SC_IMG_OFFLINE_TYPE:
                //TODO: set offline icon here
                break;
            case SC_IMG_TYPE:
                //TODO: set image icon here
                break;
            case ENVELOPE_IMG_TYPE:
                //TODO: set envelope icon here
                break;
        }
    }

    /**
     * Class implements <tt>SystrayPopupMessageListener</tt> in order to display
     * chat <tt>Activity</tt> when popup is clicked.
     */
    private class PopupListenerImpl implements SystrayPopupMessageListener
    {

        /**
         * Indicates that user has clicked on the systray popup message.
         *
         * @param evt the event triggered when user clicks on the systray popup
         *            message
         */
        public void popupMessageClicked(SystrayPopupMessageEvent evt)
        {
            Object tag = evt.getTag();
            if(tag instanceof Contact)
            {
                Contact contact = (Contact) tag;
                MetaContact metaContact
                    = AndroidGUIActivator.getMetaContactListService()
                        .findMetaContactByContact(contact);
                if(metaContact == null)
                {
                    logger.error("Meta contact not found for "+contact);
                    return;
                }

                Context ctx = JitsiApplication.getGlobalContext();
                Intent chat = ChatSessionManager.getChatIntent(metaContact);
                ctx.startActivity(chat);

                return;
            }

            // Displays popup message details when the notification is clicked
            Object src = evt.getSource();
            if(src instanceof PopupMessage)
            {
                PopupMessage message = (PopupMessage) evt.getSource();
                DialogActivity.showDialog(
                        JitsiApplication.getGlobalContext(),
                        message.getMessageTitle(),
                        message.getMessage());
            }
        }
    }
}
