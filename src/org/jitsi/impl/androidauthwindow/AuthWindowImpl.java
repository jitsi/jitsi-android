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

import android.content.*;
import net.java.sip.communicator.service.gui.*;
import org.jitsi.android.*;

/**
 * Android <tt>AuthenticationWindow</tt> impl. Serves as a static data model
 * for <tt>AuthWindowActivity</tt>. Is identified by the request id passed as
 * an intent extra. All requests are mapped in <tt>AuthWindowServiceImpl</tt>.
 *
 * @author Pawel Domas
 */
public class AuthWindowImpl
    implements AuthenticationWindowService.AuthenticationWindow
{
    /**
     * Lock object used to stop the thread until credentials are obtained.
     */
    private final Object notifyLock = new Object();

    private String userName;

    private char[] password;

    private final String server;

    private final boolean userNameEditable;

    private boolean rememberPassword;

    private final String windowTitle;

    private final String windowText;

    private final String userNameLabelText;

    private final String passwordLabelText;

    private final long requestId;

    private boolean allowSavePassword = true;

    private boolean isCanceled;

    /**
     * Creates new instance of <tt>AuthWindowImpl</tt>
     * @param requestId request identifier managed by
     *                  <tt>AuthWindowServiceImpl</tt>
     * @param userName pre entered username
     * @param password pre entered password
     * @param server name of the server that requested authentication
     * @param rememberPassword indicates if store password filed should be
     *                         checked by default
     * @param windowTitle the title for authentication window
     * @param windowText the message text for authentication window
     * @param usernameLabelText label for login field
     * @param passwordLabelText label for password field
     */
    public AuthWindowImpl(long requestId,
                          String userName,
                          char[] password,
                          String server,
                          boolean userNameEditable,
                          boolean rememberPassword,
                          String windowTitle,
                          String windowText,
                          String usernameLabelText,
                          String passwordLabelText)
    {
        this.requestId = requestId;
        this.userName = userName;
        this.password = password;
        this.server = server;
        this.userNameEditable = userNameEditable;
        this.rememberPassword = rememberPassword;
        this.windowTitle = windowTitle;
        this.windowText = windowText;
        this.userNameLabelText = usernameLabelText;
        this.passwordLabelText = passwordLabelText;

    }

    /**
     * Shows window implementation.
     *
     * @param isVisible specifies whether we should be showing or hiding the
     * window.
     */
    public void setVisible(final boolean isVisible)
    {
        if(!isVisible)
            return;

        Context ctx = JitsiApplication.getGlobalContext();

        Intent authWindowIntent = new Intent(ctx, AuthWindowActivity.class);
        authWindowIntent.putExtra(AuthWindowActivity.REQUEST_ID_EXTRA,
                                  requestId);
        authWindowIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ctx.startActivity(authWindowIntent);

        synchronized (notifyLock)
        {
            try
            {
                notifyLock.wait();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Should be called when authentication window is closed. Releases thread
     * that waits for credentials.
     */
    void windowClosed()
    {
        synchronized (notifyLock)
        {
            notifyLock.notifyAll();

            AuthWindowServiceImpl.clearRequest(requestId);
        }
    }

    /**
     * Indicates if this window has been canceled.
     *
     * @return <tt>true</tt> if this window has been canceled,
     * <tt>false</tt> - otherwise.
     */
    public boolean isCanceled()
    {
        return this.isCanceled;
    }

    /**
     * Sets dialog canceled flag.
     * @param canceled the canceled status to set.
     */
    void setCanceled(boolean canceled)
    {
        this.isCanceled = canceled;
    }

    /**
     * Returns the user name entered by the user or previously set if the
     * user name is not editable.
     *
     * @return the user name.
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * Returns the password entered by the user.
     *
     * @return the password.
     */
    public char[] getPassword()
    {
        return password;
    }

    /**
     * Indicates if the password should be remembered.
     *
     * @return <tt>true</tt> if the password should be remembered,
     * <tt>false</tt> - otherwise.
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Sets the store password flag.
     * @param storePassword <tt>true</tt> if the password should be stored.
     */
    void setRememberPassword(boolean storePassword)
    {
        this.rememberPassword = storePassword;
    }

    /**
     * Returns <tt>true</tt> if username filed is editable.
     * @return <tt>true</tt> if username filed is editable.
     */
    boolean isUserNameEditable()
    {
        return userNameEditable;
    }

    /**
     * Shows or hides the "save password" checkbox.
     * @param allow the checkbox is shown when allow is <tt>true</tt>
     */
    public void setAllowSavePassword(boolean allow)
    {
        this.allowSavePassword = allow;
    }

    /**
     * Returns <tt>true</tt> if it's allowed to save the password.
     * @return <tt>true</tt> if it's allowed to save the password.
     */
    boolean isAllowSavePassword()
    {
        return allowSavePassword;
    }

    /**
     * Returns authentication window message text.
     * @return authentication window message text.
     */
    String getWindowText()
    {
        return windowText;
    }

    /**
     * Returns username description text.
     * @return username description text.
     */
    String getUsernameLabel()
    {
        return userNameLabelText;
    }

    /**
     * Returns the password label.
     * @return the password label.
     */
    public String getPasswordLabel()
    {
        return passwordLabelText;
    }

    /**
     * Sets the username entered by the user.
     * @param username the user name entered by the user.
     */
    void setUsername(String username)
    {
        this.userName = username;
    }

    /**
     * Sets the password entered by the user.
     * @param password the password entered by the user.
     */
    void setPassword(String password)
    {
        this.password = password.toCharArray();
    }

    /**
     * Returns the window title that should be used by authentication dialog.
     * @return the window title that should be used by authentication dialog.
     */
    String getWindowTitle()
    {
        return windowTitle;
    }

    /**
     * Returns name of the server that requested authentication.
     * @return name of the server that requested authentication.
     */
    String getServer()
    {
        return server;
    }
}
