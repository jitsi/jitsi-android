/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * Flag stores the discard state. It is reseted in
     * <tt>onSaveInstanceState</tt> in order to prevent from discarding
     * the request when the device is rotated.
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
    protected void onResume()
    {
        super.onResume();

        discard = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        this.discard = false;
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
