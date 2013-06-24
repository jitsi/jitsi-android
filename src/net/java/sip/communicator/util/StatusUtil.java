package net.java.sip.communicator.util;

import net.java.sip.communicator.service.protocol.*;

public class StatusUtil
{
    /**
     * Returns the image corresponding to the given presence status.
     * @param status The presence status.
     * @return the image corresponding to the given presence status.
     */
    public static byte[] getStatusIcon(PresenceStatus status)
    {
        if(status != null)
        {
            int connectivity = status.getStatus();

            if(connectivity < PresenceStatus.ONLINE_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.USER_OFFLINE_ICON");
            }
            else if(connectivity < PresenceStatus.AWAY_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.USER_DND_ICON");
            }
            else if(connectivity == PresenceStatus.AWAY_THRESHOLD)
            {
                // the special status On The Phone is state
                // between DND and AWAY states.
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.USER_ON_THE_PHONE_ICON");
            }
            else if(connectivity < PresenceStatus.AVAILABLE_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.USER_AWAY_ICON");
            }
            else if(connectivity
                        < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.USER_ONLINE_ICON");
            }
            else if(connectivity < PresenceStatus.MAX_STATUS_VALUE)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.USER_FFC_ICON");
            }
            else
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.USER_OFFLINE_ICON");
            }
        }
        else
        {
            return UtilActivator.getResources().getImageInBytes(
                "service.gui.statusicons.USER_OFFLINE_ICON");
        }
    }

    /**
     * Returns the image corresponding to the given presence status.
     * @param status The presence status.
     * @return the image corresponding to the given presence status.
     */
    public static byte[] getContactStatusIcon(PresenceStatus status)
    {
        if(status != null)
        {
            int connectivity = status.getStatus();

            if(connectivity < PresenceStatus.ONLINE_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.CONTACT_OFFLINE_ICON");
            }
            else if(connectivity < PresenceStatus.AWAY_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.CONTACT_DND_ICON");
            }
            else if(connectivity == PresenceStatus.AWAY_THRESHOLD)
            {
                // the special status On The Phone is state
                // between DND and AWAY states.
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.CONTACT_ON_THE_PHONE_ICON");
            }
            else if(connectivity < PresenceStatus.AVAILABLE_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.CONTACT_AWAY_ICON");
            }
            else if(connectivity
                        < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.CONTACT_ONLINE_ICON");
            }
            else if(connectivity < PresenceStatus.MAX_STATUS_VALUE)
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.CONTACT_FFC_ICON");
            }
            else
            {
                return UtilActivator.getResources().getImageInBytes(
                    "service.gui.statusicons.CONTACT_OFFLINE_ICON");
            }
        }
        else
        {
            return UtilActivator.getResources().getImageInBytes(
                "service.gui.statusicons.CONTACT_OFFLINE_ICON");
        }
    }
}
