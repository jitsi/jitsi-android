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

import android.os.*;
import android.view.*;

import org.jitsi.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * Activity controls authentication dialog for
 * <tt>AuthenticationWindowService</tt>.
 *
 * @author Pawel Domas
 */
public class AuthWindowActivity
    extends OSGiActivity
{
    /**
     * Request id key.
     */
    static final String REQUEST_ID_EXTRA = "request_id";

    /**
     * Authentication window instance
     */
    private AuthWindowImpl authWindow;

    /**
     * Changes will be stored only if flag is set to <tt>false</tt>.
     */
    private boolean cancelled = true;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        long requestId = getIntent().getLongExtra(REQUEST_ID_EXTRA, -1);
        if(requestId == -1)
            throw new IllegalArgumentException();

        this.authWindow = AuthWindowServiceImpl.getAuthWindow(requestId);

        // Content view
        setContentView(R.layout.auth_window);
        View content = findViewById(android.R.id.content);

        // Server name
        String server = authWindow.getServer();

        // Title
        String title = authWindow.getWindowTitle();
        if(title == null)
        {
            title = getString(R.string.service_gui_AUTHENTICATION_WINDOW_TITLE,
                              server);
        }
        setTitle(title);

        // Message
        String text = authWindow.getWindowText();
        if(text == null)
        {
            text = getString(
                    R.string.service_gui_AUTHENTICATION_REQUESTED_SERVER,
                    server);
        }
        ViewUtil.setTextViewValue(content, R.id.text, text);

        // Username filed and label
        if(authWindow.getUsernameLabel() != null)
            ViewUtil.setTextViewValue(content,
                                      R.id.username_label,
                                      authWindow.getUsernameLabel());

        ViewUtil.ensureEnabled(content,
                               R.id.username,
                               authWindow.isUserNameEditable());

        // Password filed and label
        if(authWindow.getPasswordLabel() != null)
            ViewUtil.setTextViewValue(content,
                                      R.id.password_label,
                                      authWindow.getPasswordLabel());

        ViewUtil.setCompoundChecked(content,
                                    R.id.store_password,
                                    authWindow.isRememberPassword());
        ViewUtil.ensureVisible(content,
                               R.id.store_password,
                               authWindow.isAllowSavePassword());
    }

    /**
     * Fired when the ok button is clicked.
     * @param v ok button's <tt>View</tt>
     */
    public void onOkClicked(View v)
    {
        cancelled = false;
        finish();
    }

    /**
     * Fired when the cancel button is clicked.
     * @param v cancel button's <tt>View</tt>
     */
    public void onCancelClicked(View v)
    {
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(!cancelled)
        {
            View content = findViewById(android.R.id.content);
            authWindow.setUsername(
                    ViewUtil.getTextViewValue(content, R.id.username));
            authWindow.setPassword(
                    ViewUtil.getTextViewValue(content, R.id.password));
            authWindow.setRememberPassword(
                    ViewUtil.isCompoundChecked(content, R.id.store_password));
        }

        authWindow.setCanceled(cancelled);

        authWindow.windowClosed();
    }
}
