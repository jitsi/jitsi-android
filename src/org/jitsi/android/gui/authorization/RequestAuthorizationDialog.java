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
package org.jitsi.android.gui.authorization;

import android.content.*;
import android.os.*;
import android.view.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * This dialog is displayed in order to prepare the authorization request that
 * has to be sent to the user we want to include in our contact list.
 *
 * @author Pawel Domas
 */
public class RequestAuthorizationDialog
    extends OSGiActivity
{
    /**
     * Request identifier extra key.
     */
    private static final String EXTRA_REQUEST_ID = "request_id";

    /**
     * The request holder.
     */
    private AuthorizationHandlerImpl.AuthorizationRequestedHolder request;

    /**
     * Flag stores the discard state.
     */
    private boolean discard;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.request_authorization);

        long requestId = getIntent().getLongExtra(EXTRA_REQUEST_ID, -1);

        if(requestId == -1)
            throw new IllegalArgumentException();

        this.request = AuthorizationHandlerImpl.getRequest(requestId);

        ViewUtil.setTextViewValue(
                getContentView(), R.id.requestInfo,
                getString(R.string.service_gui_REQUEST_AUTHORIZATION_MSG,
                          request.contact.getDisplayName()));
    }

    /**
     * Method fired when the request button is clicked.
     * @param v the button's <tt>View</tt>
     */
    public void onRequestClicked(View v)
    {
        String requestText
                = ViewUtil.getTextViewValue(getContentView(), R.id.requestText);

        request.submit(requestText);

        discard = false;

        finish();
    }

    /**
     * Method fired when the cancel button is clicked.
     * @param v the button's <tt>View</tt>
     */
    public void onCancelClicked(View v)
    {
        discard = true;

        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy()
    {
        if(discard)
            request.discard();

        super.onDestroy();
    }

    /**
     * Creates the <tt>Intent</tt> to start <tt>RequestAuthorizationDialog</tt>
     * parametrized with given <tt>requestId</tt>.
     * @param requestId the id of authentication request.
     * @return <tt>Intent</tt> that start <tt>RequestAuthorizationDialog</tt>
     *         parametrized with given request id.
     */
    public static Intent getRequestAuthDialogIntent(long requestId)
    {
        Intent intent = new Intent(
                JitsiApplication.getGlobalContext(),
                RequestAuthorizationDialog.class);

        intent.putExtra(EXTRA_REQUEST_ID, requestId);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }
}
