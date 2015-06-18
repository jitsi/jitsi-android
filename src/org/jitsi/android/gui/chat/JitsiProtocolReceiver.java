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
