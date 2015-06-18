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
package net.java.sip.communicator.util.call;

import java.util.*;

import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class CallManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(CallManager.class);

    public static final String CALLEE_DISPLAY_NAME = "CalleeDisplayName";

    public static final String CALLEE_ADDRESS = "CalleeAddress";

    public static final String CALLEE_AVATAR = "CalleeAvatar";

    public static final String CALL_IDENTIFIER = "CallIdentifier";

    /**
     * A table mapping protocol <tt>Call</tt> objects to the GUI dialogs
     * that are currently used to display them.
     */
    private static Map<String, Call> activeCalls = new HashMap<String, Call>();

    public synchronized static String addActiveCall(Call call)
    {
        String key = String.valueOf(System.currentTimeMillis());

        synchronized (activeCalls)
        {
            activeCalls.put(key, call);
        }

        return key;
    }

    public synchronized static void removeActiveCall(String callKey)
    {
        synchronized (activeCalls)
        {
            activeCalls.remove(callKey);
        }
    }

    public synchronized static void removeActiveCall(Call call)
    {
        synchronized (activeCalls)
        {
            if (!activeCalls.containsValue(call))
                return;

            Iterator<String> activeCallsIter = activeCalls.keySet().iterator();
            ArrayList<String> toRemove = new ArrayList<String>();
            while (activeCallsIter.hasNext())
            {
                String key = activeCallsIter.next();
                if (activeCalls.get(key).equals(call))
                    toRemove.add(key);
            }

            for(String removeKey:toRemove)
            {
                removeActiveCall(removeKey);
            }
        }
    }

    /**
     * 
     * @param callKey
     * @return
     */
    public synchronized static Call getActiveCall(String callKey)
    {
        synchronized (activeCalls)
        {
            return activeCalls.get(callKey);
        }
    }

    /**
     * Returns currently active calls.
     *
     * @return collection of currently active calls.
     */
    public static Collection<Call> getActiveCalls()
    {
        synchronized (activeCalls)
        {
            return activeCalls.values();
        }
    }

    /**
     * Returns the number of currently active calls.
     *
     * @return the number of currently active calls.
     */
    public synchronized static int getActiveCallsCount()
    {
        synchronized (activeCalls)
        {
            return activeCalls.size();
        }
    }

    /**
     * Hang ups the given call.
     *
     * @param call the call to hang up
     */
    public static void hangupCall(final Call call)
    {
        new HangupCallThread(call).start();
    }

    /**
     * Hang-ups all call peers in the given call.
     */
    private static class HangupCallThread
        extends Thread
    {
        private final Call call;

        public HangupCallThread(Call call)
        {
            this.call = call;
        }

        @Override
        public void run()
        {
            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<? extends CallPeer> peers = call.getCallPeers();

            while (peers.hasNext())
            {
                CallPeer peer = peers.next();
                OperationSetBasicTelephony<?> telephony
                    = pps.getOperationSet(OperationSetBasicTelephony.class);

                try
                {
                    telephony.hangupCallPeer(peer);
                }
                catch (OperationFailedException e)
                {
                    System.err.println("Could not hang up : " + peer
                        + " caused by the following exception: " + e);
                }
            }

            removeActiveCall(call);
        }
    }

    /**
     * Answers the given call.
     *
     * @param call the call to answer
     */
    public static void answerCall(Call call, boolean useVideo)
    {
        answerCall(call, null, useVideo);
    }

    /**
     * Answers a specific <tt>Call</tt> with or without video and, optionally,
     * does that in a telephony conference with an existing <tt>Call</tt>.
     *
     * @param call
     * @param existingCall
     * @param video
     */
    private static void answerCall(Call call, Call existingCall, boolean video)
    {
//        if (existingCall == null)
//            openCallContainerIfNecessary(call);

        new AnswerCallThread(call, existingCall, video).start();
    }

    /**
     * Creates a call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createCall(  ProtocolProviderService protocolProvider,
                                    String contact)
        throws Throwable
    {
        new CreateCallThread(protocolProvider, contact, false /* audio-only */)
            .run();
    }

    /**
     * Creates a video call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createVideoCall(ProtocolProviderService protocolProvider,
                                        String contact)
        throws Throwable
    {
        new CreateCallThread(protocolProvider, contact, true /* video */)
            .run();
    }

    /**
     * Returns the image corresponding to the given <tt>peer</tt>.
     *
     * @param peer the call peer, for which we're returning an image
     * @return the peer image
     */
    public static byte[] getPeerImage(CallPeer peer)
    {
        byte[] image = null;
        // We search for a contact corresponding to this call peer and
        // try to get its image.
        if (peer.getContact() != null)
        {
//            MetaContact metaContact = UtilActivator.getContactListService()
//                .findMetaContactByContact(peer.getContact());
//
//            image = metaContact.getAvatar();
        }

        // If the icon is still null we try to get an image from the call
        // peer.
        if ((image == null || image.length == 0)
                && peer.getImage() != null)
            image = peer.getImage();

        return image;
    }

    /**
     * Answers to all <tt>CallPeer</tt>s associated with a specific
     * <tt>Call</tt> and, optionally, does that in a telephony conference with
     * an existing <tt>Call</tt>.
     */
    private static class AnswerCallThread
        extends Thread
    {
        /**
         * The <tt>Call</tt> which is to be answered.
         */
        private final Call call;

        /**
         * The existing <tt>Call</tt>, if any, which represents a telephony
         * conference in which {@link #call} is to be answered.
         */
        private final Call existingCall;

        /**
         * The indicator which determines whether this instance is to answer
         * {@link #call} with video.
         */
        private final boolean video;

        public AnswerCallThread(Call call, Call existingCall, boolean video)
        {
            this.call = call;
            this.existingCall = existingCall;
            this.video = video;
        }

        @Override
        public void run()
        {
//            if (existingCall != null)
//                call.setConference(existingCall.getConference());

            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<? extends CallPeer> peers = call.getCallPeers();

            while (peers.hasNext())
            {
                CallPeer peer = peers.next();

                if (video)
                {
                    OperationSetVideoTelephony telephony
                        = pps.getOperationSet(OperationSetVideoTelephony.class);

                    try
                    {
                        telephony.answerVideoCallPeer(peer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Could not answer "
                                    + peer
                                    + " with video"
                                    + " because of the following exception: "
                                    + ofe);
                    }
                }
                else
                {
                    OperationSetBasicTelephony<?> telephony
                        = pps.getOperationSet(OperationSetBasicTelephony.class);

                    try
                    {
                        telephony.answerCallPeer(peer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Could not answer "
                                    + peer
                                    + " because of the following exception: ",
                                ofe);
                    }
                }
            }
        }
    }

    /**
     * Enables/disables local video for a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> to enable/disable to local video for
     * @param enable <tt>true</tt> to enable the local video; otherwise,
     * <tt>false</tt>
     */
    public static void enableLocalVideo(Call call, boolean enable)
    {
        new EnableLocalVideoThread(call, enable).start();
    }

    /**
     * Indicates if the desktop sharing is currently enabled for the given
     * <tt>call</tt>.
     *
     * @param call the <tt>Call</tt>, for which we would to check if the desktop
     * sharing is currently enabled
     * @return <tt>true</tt> if the desktop sharing is currently enabled for the
     * given <tt>call</tt>, <tt>false</tt> otherwise
     */
    public static boolean isLocalVideoEnabled(Call call)
    {
        OperationSetVideoTelephony telephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetVideoTelephony.class);

        return (telephony != null) && telephony.isLocalVideoAllowed(call);
    }

    /**
     * Indicates if the given call is currently muted.
     *
     * @param call the call to check
     * @return <tt>true</tt> if the given call is currently muted,
     * <tt>false</tt> - otherwise
     */
    public static boolean isMute(Call call)
    {
        if(call instanceof MediaAwareCall<?,?,?>)
        {
            return ((MediaAwareCall<?,?,?>)call).isMute();
        }
        else
        {
            return false;
        }
    }

    /**
     * Mutes/unmutes the given call.
     *
     * @param call the call to mute/unmute
     * @param isMute <tt>true</tt> to mute the call, <tt>false</tt> to unmute it
     */
    public static void setMute(Call call, boolean isMute)
    {
        logger.trace("Set mute to "+isMute);
        new MuteThread(call, isMute).start();

    }

    /**
     * Creates the mute call thread.
     */
    private static class MuteThread
        extends Thread
    {
        private final Call call;

        private final boolean isMute;

        public MuteThread(Call call, boolean isMute)
        {
            this.call = call;
            this.isMute = isMute;
        }

        public void run()
        {
            if (call != null)
            {
                OperationSetBasicTelephony<?> telephony
                    = call.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                telephony.setMute(call, isMute);
            }
        }
    }

    /**
     * Checks if the call has been put on hold by local user.
     *
     * @param call the <tt>Call</tt> that will be checked.
     *
     * @return <tt>true</tt> if given <tt>Call</tt> is locally on hold.
     */
    public static boolean isLocallyOnHold(Call call)
    {
        boolean onHold = false;
        Iterator<? extends CallPeer> peers = call.getCallPeers();
        if(peers.hasNext())
        {
            CallPeerState peerState = call.getCallPeers().next().getState();
            onHold = CallPeerState.ON_HOLD_LOCALLY.equals(peerState)
                    || CallPeerState.ON_HOLD_MUTUALLY.equals(peerState);
        }
        else
        {
            logger.warn("No peer belongs to call: "+call.toString());
        }

        return onHold;
    }

    /**
     * Puts on or off hold the given <tt>call</tt>.
     * @param call  the peer to put on/off hold
     * @param isOnHold  indicates the action (on hold or off hold)
     */
    public static void putOnHold(Call call, boolean isOnHold)
    {
        Iterator<? extends CallPeer> peers = call.getCallPeers();
        while(peers.hasNext())
        {
            putOnHold(peers.next(), isOnHold);
        }
    }

    /**
     * Puts on or off hold the given <tt>callPeer</tt>.
     * @param callPeer the peer to put on/off hold
     * @param isOnHold indicates the action (on hold or off hold)
     */
    public static void putOnHold(CallPeer callPeer, boolean isOnHold)
    {
        new PutOnHoldCallPeerThread(callPeer, isOnHold).start();
    }

    /**
     * Puts on hold the given <tt>CallPeer</tt>.
     */
    private static class PutOnHoldCallPeerThread
            extends Thread
    {
        private final CallPeer callPeer;

        private final boolean isOnHold;

        public PutOnHoldCallPeerThread(CallPeer callPeer, boolean isOnHold)
        {
            this.callPeer = callPeer;
            this.isOnHold = isOnHold;
        }

        @Override
        public void run()
        {
            OperationSetBasicTelephony<?> telephony
                    = callPeer.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

            try
            {
                if (isOnHold)
                    telephony.putOnHold(callPeer);
                else
                    telephony.putOffHold(callPeer);
            }
            catch (OperationFailedException ex)
            {
                logger.error(
                        "Failed to put"
                                + callPeer.getAddress()
                                + (isOnHold ? " on hold." : " off hold. "),
                                ex);
            }
        }
    }

    /**
     * Creates the enable local video call thread.
     */
    private static class EnableLocalVideoThread
        extends Thread
    {
        private final Call call;

        private final boolean enable;

        /**
         * Creates the enable local video call thread.
         *
         * @param call the call, for which to enable/disable
         * @param enable
         */
        public EnableLocalVideoThread(Call call, boolean enable)
        {
            this.call = call;
            this.enable = enable;
        }

        @Override
        public void run()
        {
            OperationSetVideoTelephony telephony
                = call.getProtocolProvider()
                    .getOperationSet(OperationSetVideoTelephony.class);

            if (telephony != null)
            {
                try
                {
                    telephony.setLocalVideoAllowed(call, enable);
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                        "Failed to toggle the streaming of local video.",
                        ex);
                }
            }
        }
    }

    /**
     * Creates a new (audio-only or video) <tt>Call</tt> to a contact specified
     * as a <tt>Contact</tt> instance or a <tt>String</tt> contact
     * address/identifier.
     */
    private static class CreateCallThread
//        extends Thread
    {
        private final Contact contact;

        private final ProtocolProviderService protocolProvider;

        private final String stringContact;

        /**
         * The indicator which determines whether this instance is to create a
         * new video (as opposed to audio-only) <tt>Call</tt>.
         */
        private final boolean video;

        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                Contact contact,
                boolean video)
        {
            this(protocolProvider, contact, null, video);
        }

        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                String contact,
                boolean video)
        {
            this(protocolProvider, null, contact, video);
        }

        /**
         * Initializes a new <tt>CreateCallThread</tt> instance which is to
         * create a new <tt>Call</tt> to a contact specified either as a
         * <tt>Contact</tt> instance or as a <tt>String</tt> contact
         * address/identifier.
         * <p>
         * The constructor is private because it relies on its arguments being
         * validated prior to its invocation.
         * </p>
         *
         * @param protocolProvider the <tt>ProtocolProviderService</tt> which is
         * to perform the establishment of the new <tt>Call</tt>
         * @param contact
         * @param stringContact
         * @param video <tt>true</tt> if this instance is to create a new video
         * (as opposed to audio-only) <tt>Call</tt>
         */
        private CreateCallThread(
                ProtocolProviderService protocolProvider,
                Contact contact,
                String stringContact,
                boolean video)
        {
            this.protocolProvider = protocolProvider;
            this.contact = contact;
            this.stringContact = stringContact;
            this.video = video;
        }

        public void run()
            throws Throwable
        {
            Contact contact = this.contact;
            String stringContact = this.stringContact;

            if (ConfigurationUtils.isNormalizePhoneNumber())
            {
                if (contact != null)
                {
                    stringContact = contact.getAddress();
                    contact = null;
                }

                stringContact = PhoneNumberI18nService.normalize(stringContact);
            }

            Call call = null;
            try
            {
                if (video)
                {
                    OperationSetVideoTelephony telephony
                        = protocolProvider.getOperationSet(
                                OperationSetVideoTelephony.class);

                    if (telephony != null)
                    {
                        if (contact != null)
                            call = telephony.createVideoCall(contact);
                        else if (stringContact != null)
                            call = telephony.createVideoCall(stringContact);
                    }
                }
                else
                {
                    OperationSetBasicTelephony<?> telephony
                        = protocolProvider.getOperationSet(
                                OperationSetBasicTelephony.class);

                    if (telephony != null)
                    {
                        if (contact != null)
                            call = telephony.createCall(contact);
                        else if (stringContact != null)
                            call = telephony.createCall(stringContact);
                    }
                }
                if (call != null)
                    addActiveCall(call);
            }
            catch (Throwable t)
            {
                logger.error("The call could not be created: ", t);

                throw t;
            }
        }
    }
}
