/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.plugin.otr;

import android.view.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;

import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.event.*;
import org.jitsi.service.osgi.*;

/**
 * Fragment when added to <tt>Activity</tt> will display the padlock allowing
 * user to control OTR chat status. Only currently active chat is handled by
 * this fragment.
 *
 * @author Pawel Domas
 */
public class OtrFragment
    extends OSGiFragment
{
    /**
     * Menu instance used to control the padlock.
     */
    private Menu menu;

    /**
     * Active chat session listener. Updates the padlock when active
     * chat is switched.
     */
    private EventListener<String> activeChatListener
            = new EventListener<String>()
    {
        @Override
        public void onChangeEvent(String eventObject)
        {
            setCurrentChatSession(eventObject);
        }
    };

    /**
     * Creates new instance of <tt>OtrFragment</tt>.
     */
    public OtrFragment()
    {
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        if(menu != null)
            doInit();
    }

    /**
     * Initializes the padlock and registers listeners.
     */
    private void doInit()
    {
        OtrActivator.scOtrEngine.addListener(scOtrEngineListener);
        OtrActivator.scOtrKeyManager.addListener(scOtrKeyManagerListener);

        ChatSessionManager.addCurrentChatListener(activeChatListener);

        setCurrentChatSession(ChatSessionManager.getCurrentChatId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause()
    {
        ChatSessionManager.removeCurrentChatListener(activeChatListener);

        OtrActivator.scOtrEngine.removeListener(scOtrEngineListener);
        OtrActivator.scOtrKeyManager.removeListener(scOtrKeyManagerListener);

        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);

        // Append OTR items
        inflater.inflate(R.menu.otr_menu, menu);

        this.menu = menu;

        // Initialize the padlock when new menu is created
        if(getActivity() != null)
            doInit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.otr_padlock)
        {
            // prevents network on main thread exception
            new Thread()
            {
                @Override
                public void run()
                {
                    doHandleOtrPadlockPressed();
                }
            }.start();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Toggles OTR state when the padlock button is pressed.
     */
    private void doHandleOtrPadlockPressed()
    {
        switch (OtrActivator.scOtrEngine.getSessionStatus(currentContact))
        {
            case ENCRYPTED:
            case FINISHED:
                // Default action for finished and encrypted sessions is
                // end session.
                OtrActivator.scOtrEngine.endSession(currentContact);
                break;
            case PLAINTEXT:
                // Default action for finished and plaintext sessions is
                // start session.
                OtrActivator.scOtrEngine.startSession(currentContact);
                break;
        }
    }

    /**
     * Contact for currently active chat session.
     */
    private Contact currentContact;

    /**
     * OTR engine listener.
     */
    private final ScOtrEngineListener scOtrEngineListener =
            new ScOtrEngineListener()
    {
        public void sessionStatusChanged(Contact contact)
        {
            // OtrMetaContactButton.this.contact can be null.
            if (contact.equals(currentContact))
            {
                setStatus(
                    OtrActivator.scOtrEngine.getSessionStatus(contact));
            }
        }

        public void contactPolicyChanged(Contact contact)
        {
            // OtrMetaContactButton.this.contact can be null.
            if (contact.equals(currentContact))
            {
                setPolicy(
                    OtrActivator.scOtrEngine.getContactPolicy(contact));
            }
        }

        public void globalPolicyChanged()
        {
            if (currentContact != null)
                setPolicy(
                    OtrActivator.scOtrEngine.getContactPolicy(currentContact));
        }
    };

    /**
     * OTR key manager listener.
     */
    private final ScOtrKeyManagerListener scOtrKeyManagerListener =
    new ScOtrKeyManagerListener()
    {
        public void contactVerificationStatusChanged(Contact contact)
        {
            // OtrMetaContactButton.this.contact can be null.
            if (contact.equals(currentContact))
            {
                setStatus(
                        OtrActivator.scOtrEngine.getSessionStatus(contact));
            }
        }
    };

    /**
     * Sets the current <tt>Contact</tt> and updates status and policy.
     *
     * @param contact new <tt>Contact</tt> to be used.
     */
    public void setCurrentContact(Contact contact)
    {
        OperationSetBasicInstantMessaging msgingOpSet = null;

        if(contact != null && contact.getProtocolProvider() != null)
        {
            msgingOpSet
                    = contact.getProtocolProvider().getOperationSet(
                        OperationSetBasicInstantMessaging.class);
        }

        if(msgingOpSet == null)
        {
            // deactivate plugin if messaging is not supported
            contact = null;
        }

        this.currentContact = contact;
        if (contact != null)
        {
            this.setStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
            this.setPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
        }
        else
        {
            this.setStatus(SessionStatus.PLAINTEXT);
            this.setPolicy(null);
        }
    }

    /**
     * Sets current <tt>ChatSession</tt> identified by given
     * <tt>chatSessionKey</tt>.
     *
     * @param chatSessionKey chat session key managed by
     *                       <tt>ChatSessionManager</tt>
     */
    public void setCurrentChatSession(String chatSessionKey)
    {
        ChatSession activeChat
            = ChatSessionManager.getActiveChat(chatSessionKey);

        MetaContact metaContact
            = activeChat != null
                    ? activeChat.getMetaContact() : null;

        setCurrentContact(
            (metaContact != null)
                    ? metaContact.getDefaultContact() : null);
    }

    /**
     * Sets the button enabled status according to the passed in
     * {@link net.java.otr4j.OtrPolicy}.
     *
     * @param contactPolicy the {@link net.java.otr4j.OtrPolicy}.
     */
    private void setPolicy(final OtrPolicy contactPolicy)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                // Hides the padlock when OTR is not supported
                getPadlock().setVisible(
                        contactPolicy != null);

                getPadlock().setEnabled(
                        contactPolicy != null
                                && contactPolicy.getEnableManual());
            }
        });

    }

    /**
     * Sets the padlock icon according to the passed in {@link SessionStatus}.
     *
     * @param status the {@link SessionStatus}.
     */
    private void setStatus(SessionStatus status)
    {
        final int iconId;
        switch (status)
        {
            case ENCRYPTED:
                iconId = OtrActivator.scOtrKeyManager.isVerified(currentContact)
                       ? R.drawable.encrypted_verified
                       : R.drawable.encrypted_unverified;
                break;
            case FINISHED:
                iconId = R.drawable.encrypted_finished;
                break;
            case PLAINTEXT:
                iconId = R.drawable.encrypted_unsecure;
                break;
            default:
                return;
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                getPadlock().setIcon(iconId);
            }
        });
    }

    /**
     * Gets the <tt>MenuItem</tt> used for the padlock.
     * @return the <tt>MenuItem</tt> used for the padlock.
     */
    public MenuItem getPadlock()
    {
        return menu.findItem(R.id.otr_padlock);
    }
}
