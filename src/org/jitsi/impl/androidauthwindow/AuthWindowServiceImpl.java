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
package org.jitsi.impl.androidauthwindow;

import net.java.sip.communicator.service.gui.*;

import java.util.*;

/**
 * Android implementation of <tt>AuthenticationWindowService</tt>. This class
 * manages authentication requests. Each request data is held by the
 * <tt>AuthWindowImpl</tt> identified by assigned request id. Request id is
 * passed to the <tt>AuthWindowActivity</tt> so that it can obtain request data
 * and interact with the user.
 *
 * @author Pawel Domas
 */
public class AuthWindowServiceImpl
    implements AuthenticationWindowService
{
    /**
     * Requests map
     */
    private static Map<Long, AuthWindowImpl> requestMap
            = new HashMap<Long, AuthWindowImpl>();

    /**
     * Creates an instance of the <tt>AuthenticationWindow</tt> implementation.
     *
     * @param server the server name
     * @param isUserNameEditable indicates if the user name is editable
     * @param icon the icon to display on the left of the authentication window
     * @param windowTitle customized window title
     * @param windowText customized window text
     * @param usernameLabelText customized username field label text
     * @param passwordLabelText customized password field label text
     * @param errorMessage an error message if this dialog is shown to indicate
     * the user that something went wrong
     * @param signupLink an URL that allows the user to sign up
     */
    public AuthenticationWindow create(String userName,
                                       char[] password,
                                       String server,
                                       boolean isUserNameEditable,
                                       boolean isRememberPassword,
                                       Object icon,
                                       String windowTitle,
                                       String windowText,
                                       String usernameLabelText,
                                       String passwordLabelText,
                                       String errorMessage,
                                       String signupLink)
    {
        long requestId = System.currentTimeMillis();

        AuthWindowImpl authWindow
                = new AuthWindowImpl(requestId,
                                     userName,
                                     password,
                                     server,
                                     isUserNameEditable,
                                     isRememberPassword,
                                     windowTitle,
                                     windowText,
                                     usernameLabelText,
                                     passwordLabelText);

        requestMap.put(requestId, authWindow);

        return requestMap.get(requestId);
    }

    /**
     * Returns <tt>AuthWindowImpl</tt> for given <tt>requestId</tt>.
     * @param requestId the request identifier
     * @return <tt>AuthWindowImpl</tt> identified by given <tt>requestId</tt>.
     */
    static AuthWindowImpl getAuthWindow(long requestId)
    {
        return requestMap.get(requestId);
    }

    /**
     * Called when authentication request processing for given
     * <tt>requestId</tt> is completed or canceled.
     * @param requestId the request identifier
     */
    static void clearRequest(long requestId)
    {
        requestMap.remove(requestId);
    }
}
