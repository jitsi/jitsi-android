/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.plugin.otr;

import android.os.*;
import android.view.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 *
 * @author Pawel Domas
 */
public class OtrFragment
    extends OSGiFragment
    implements ChatListener
{
    private Menu menu;

    public OtrFragment()
    {
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(menu != null)
            doInit();
    }

    private void doInit()
    {
        OtrActivator.scOtrEngine.addListener(scOtrEngineListener);
        OtrActivator.scOtrKeyManager.addListener(scOtrKeyManagerListener);

        ChatSessionManager.addChatListener(this);

        ChatSession chatSession
                = ChatSessionManager.getActiveChat(
                        ChatSessionManager.getCurrentChatSession());

        setCurrentContact(chatSession.getMetaContact());
    }

    @Override
    public void onPause()
    {
        ChatSessionManager.removeChatListener(this);

        OtrActivator.scOtrEngine.removeListener(scOtrEngineListener);
        OtrActivator.scOtrKeyManager.removeListener(scOtrKeyManagerListener);

        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);

        // Append OTR items
        inflater.inflate(R.menu.otr_menu, menu);

        this.menu = menu;

        doInit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.otr_padlock)
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Contact currentContact;

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

    /*
     * Implements PluginComponent#setCurrentContact(Contact).
     */
    public void setCurrentContact(Contact contact)
    {
        if (this.currentContact == contact)
            return;

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

    /*
     * Implements PluginComponent#setCurrentContact(MetaContact).
     */
    public void setCurrentContact(MetaContact metaContact)
    {
        setCurrentContact((metaContact == null) ? null : metaContact
                .getDefaultContact());
    }

    /**
     * Sets the button enabled status according to the passed in
     * {@link net.java.otr4j.OtrPolicy}.
     *
     * @param contactPolicy the {@link net.java.otr4j.OtrPolicy}.
     */
    private void setPolicy(final OtrPolicy contactPolicy)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                getButton().setEnabled(
                        contactPolicy != null
                                && contactPolicy.getEnableManual());
            }
        });

    }

    /**
     * Sets the button icon according to the passed in {@link SessionStatus}.
     *
     * @param status the {@link SessionStatus}.
     */
    private void setStatus(SessionStatus status)
    {
        final int iconId;
        switch (status)
        {
            case ENCRYPTED:
                iconId
                        = OtrActivator.scOtrKeyManager.isVerified(currentContact)
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

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                getButton().setIcon(iconId);
            }
        });
    }

    public MenuItem getButton()
    {
        return menu.findItem(R.id.otr_padlock);
    }

    @Override
    public void chatClosed(Chat chat)
    {
        ChatSession chatSession = (ChatSession) chat;
        if(chatSession.getMetaContact().containsContact(currentContact))
        {
            // Close session
            setCurrentContact((Contact)null);
        }
    }

    @Override
    public void chatCreated(Chat chat)
    {
        ChatSession chatSession = (ChatSession) chat;
        setCurrentContact(chatSession.getMetaContact());
    }
}
