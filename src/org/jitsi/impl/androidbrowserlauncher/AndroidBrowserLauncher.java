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
package org.jitsi.impl.androidbrowserlauncher;

import android.content.*;
import android.net.*;
import net.java.sip.communicator.service.browserlauncher.*;
import org.jitsi.android.*;
import org.jitsi.util.*;

/**
 * Android implementation of <tt>BrowserLauncherService</tt>.
 *
 * @author Pawel Domas
 */
public class AndroidBrowserLauncher
    implements BrowserLauncherService
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(AndroidBrowserLauncher.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void openURL(String url)
    {
        try
        {
            Uri uri = Uri.parse(url);

            Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
            launchBrowser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            JitsiApplication.getGlobalContext().startActivity(launchBrowser);
        }
        catch (Exception e)
        {
            logger.error("Error opening URL",e);
        }
    }
}
