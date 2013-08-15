package org.jitsi.android.gui.chat;

import android.content.*;
import android.os.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.osgi.*;

import java.net.*;

/**
 * @author Pawel Domas
 */
public class JitsiProtocolReceiver
    extends OSGiActivity
{

    /**
     * The logger
     */
    private final static Logger logger
            = Logger.getLogger(JitsiProtocolReceiver.class);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        logger.info("Jitsi protocol intent received " + intent);

        String urlStr = intent.getDataString();
        if(urlStr != null)
        {
            try
            {
                URI url = new URI(urlStr);
                ChatSessionManager.notifyChatLinkClicked(url);
            }
            catch (URISyntaxException e)
            {
                logger.error("Error parsing clicked URL", e);
            }
        }
        else
        {
            logger.warn("No URL supplied in Jitsi link");
        }

        finish();
    }

}
