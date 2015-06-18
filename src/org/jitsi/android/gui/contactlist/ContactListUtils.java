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
package org.jitsi.android.gui.contactlist;

import android.content.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;

/**
 * Class gathers utility methods for operations on contact list.
 */
public class ContactListUtils
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(ContactListUtils.class);

    /**
     * Adds a new contact in separate <tt>Thread</tt>.
     *
     * @param protocolProvider parent protocol provider.
     * @param group contact group to which new contact will be added.
     * @param contactAddress new contact address.
     */
    public static void addContact(
            final ProtocolProviderService protocolProvider,
            final MetaContactGroup group,
            final String contactAddress)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    AndroidGUIActivator.getContactListService()
                            .createMetaContact( protocolProvider,
                                                group,
                                                contactAddress);
                }
                catch (MetaContactListException ex)
                {
                    logger.error(ex);
                    ex.printStackTrace();

                    Context ctx = JitsiApplication.getGlobalContext();
                    String title = ctx.getString(
                            R.string.service_gui_ADD_CONTACT_ERROR_TITLE);

                    String msg;
                    int errorCode = ex.getErrorCode();
                    switch(errorCode)
                    {
                        case MetaContactListException
                                .CODE_CONTACT_ALREADY_EXISTS_ERROR:
                            msg = ctx.getString(
                                R.string.service_gui_ADD_CONTACT_EXIST_ERROR,
                                contactAddress);
                            break;
                        case MetaContactListException.CODE_NETWORK_ERROR:
                            msg = ctx.getString(
                                R.string.service_gui_ADD_CONTACT_NETWORK_ERROR,
                                contactAddress);
                            break;
                        case MetaContactListException
                                .CODE_NOT_SUPPORTED_OPERATION:
                            msg = ctx.getString(
                                R.string.service_gui_ADD_CONTACT_NOT_SUPPORTED,
                                contactAddress);
                            break;
                        default:
                            msg = ctx.getString(
                                R.string.service_gui_ADD_CONTACT_ERROR,
                                contactAddress);
                            break;
                    }
                    DialogActivity.showDialog(ctx, title, msg);
                }
            }
        }.start();
    }
}
