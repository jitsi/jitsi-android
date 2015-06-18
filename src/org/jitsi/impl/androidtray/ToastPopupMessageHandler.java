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
package org.jitsi.impl.androidtray;

import android.content.*;
import android.widget.*;
import net.java.sip.communicator.service.systray.*;
import org.jitsi.android.*;

/**
 * TODO: Toast popup handler stub. It should be registered by displayed Activity
 * as we need to hold the UI thread to show Toasts. Also
 * {@link org.jitsi.android.gui.widgets.ClickableToastController} may be used to
 * catch the clicks.
 *
 * @author Pawel Domas
 */
public class ToastPopupMessageHandler
    extends AbstractPopupMessageHandler
{
    /**
     * {@inheritDoc}
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        Context context = JitsiApplication.getGlobalContext();
        CharSequence text = popupMessage.getMessage();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    /**
     * {@inheritDoc}
     */
    public int getPreferenceIndex()
    {
        // +1 for native toaster
        return 1;
    }
}
