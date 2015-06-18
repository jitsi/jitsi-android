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
