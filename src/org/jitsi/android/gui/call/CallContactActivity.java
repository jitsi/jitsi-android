/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.call;

import android.content.*;

import android.os.Bundle;
import android.support.v4.app.*;
import android.telephony.*;
import org.jitsi.service.osgi.*;

/**
 * Tha <tt>CallContactActivity</tt> can be used to call contact. The phone
 * number can be filled from <tt>Intent</tt> data.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class CallContactActivity
        extends OSGiActivity
{
    /**
     * Called when the activity is starting. Initializes the corresponding
     * call interface.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // There's no need to create fragment if the Activity is being restored.
        if(savedInstanceState == null)
        {
            //Create new call contact fragment
            String phoneNumber = null;
            Intent intent = getIntent();
            if (intent.getDataString() != null)
                phoneNumber = PhoneNumberUtils.getNumberFromIntent( intent,
                                                                    this);
            Fragment ccFragment = CallContactFragment.newInstance(phoneNumber);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, ccFragment)
                    .commit();
        }
    }
}