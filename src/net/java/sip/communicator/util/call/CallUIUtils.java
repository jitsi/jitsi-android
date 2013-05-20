package net.java.sip.communicator.util.call;

import java.util.*;

import org.jitsi.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author Yana Stamcheva
 */
public class CallUIUtils
{
    public static byte[] getCalleeAvatar(Call incomingCall)
    {
        Iterator<? extends CallPeer> peersIter = incomingCall.getCallPeers();

        if (incomingCall.getCallPeerCount() == 1)
        {
            final CallPeer peer = peersIter.next();
            byte[] image = CallManager.getPeerImage(peer);

            if (image != null && image.length > 0)
                return image;
        }

        return UtilActivator.getResources()
            .getImageInBytes("service.gui.DEFAULT_USER_PHOTO");
    }

    public static String getCalleeAddress(Call incomingCall)
    {
        Iterator<? extends CallPeer> peersIter = incomingCall.getCallPeers();

        String textAddress = "";

        while (peersIter.hasNext())
        {
            final CallPeer peer = peersIter.next();

            // More peers.
            if (peersIter.hasNext())
            {
                String peerAddress = getPeerDisplayAddress(peer);

                if(!StringUtils.isNullOrEmpty(peerAddress))
                    textAddress = textAddress + peerAddress + ", ";
            }
            // Only one peer.
            else
            {
                String peerAddress = getPeerDisplayAddress(peer);

                if(!StringUtils.isNullOrEmpty(peerAddress))
                    textAddress = peerAddress ;
            }
        }

        return textAddress;
    }

    /**
     * Initializes the label of the received call.
     *
     * @param incomingCall the call
     */
    public static String getCalleeDisplayName(Call incomingCall)
    {
        Iterator<? extends CallPeer> peersIter = incomingCall.getCallPeers();

        boolean hasMorePeers = false;
        String textDisplayName = "";

        while (peersIter.hasNext())
        {
            final CallPeer peer = peersIter.next();

            // More peers.
            if (peersIter.hasNext())
            {
                textDisplayName = textDisplayName
                    + getPeerDisplayName(peer) + ", ";
            }
            // Only one peer.
            else
            {

                textDisplayName = getPeerDisplayName(peer);
            }
        }

        // Remove the last semicolon.
        if (hasMorePeers)
            textDisplayName = textDisplayName
                .substring(0, textDisplayName.lastIndexOf(","));

        return textDisplayName;
    }

    /**
     * A informative text to show for the peer. If display name is missing
     * return the address.
     * @param peer the peer.
     * @return the text contain display name.
     */
    private static String getPeerDisplayName(CallPeer peer)
    {
        String displayName = peer.getDisplayName();

        return
            StringUtils.isNullOrEmpty(displayName, true)
                ? peer.getAddress()
                : displayName;
    }

    /**
     * A informative text to show for the peer. If display name and
     * address are the same return null.
     * @param peer the peer.
     * @return the text contain address.
     */
    private static String getPeerDisplayAddress(CallPeer peer)
    {
        String peerAddress = peer.getAddress();

        if(StringUtils.isNullOrEmpty(peerAddress, true))
            return null;
        else
        {
            return
                peerAddress.equalsIgnoreCase(peer.getDisplayName())
                    ? null
                    : peerAddress;
        }
    }
}
