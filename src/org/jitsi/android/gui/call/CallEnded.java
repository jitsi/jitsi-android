/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.call;

import android.content.*;
import android.os.*;
import android.view.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * Fragment displayed in <tt>VideoCallActivity</tt> when the call has ended.
 *
 * @author Pawel Domas
 */
public class CallEnded
    extends OSGiFragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.call_ended, container, false);

        ViewUtil.setTextViewValue(v, R.id.callTime,
                                  VideoCallActivity.callState.callDuration);
        String errorReason = VideoCallActivity.callState.errorReason;
        if(!errorReason.isEmpty())
        {
            ViewUtil.setTextViewValue(v, R.id.callErrorReason, errorReason);
        }
        else
        {
            ViewUtil.ensureVisible(v, R.id.callErrorReason, false);
        }

        v.findViewById(R.id.callHangupButton)
            .setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Context ctx = getActivity();
                    getActivity().finish();
                    ctx.startActivity(JitsiApplication.getHomeIntent());
                }
            });

        return v;
    }
}
