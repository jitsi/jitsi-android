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
package org.jitsi.android.gui;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.jitsi.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * Activity displayed when shutdown procedure is in progress.
 *
 * @author Pawel Domas
 */
public class ShutdownActivity
    extends Activity
{
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        if(!OSGiService.hasStarted())
        {
            startActivity(new Intent(this, LauncherActivity.class));
            finish();
            return;
        }

        if(AndroidUtils.hasAPI(11))
        {
            ActionBar actionBar = getActionBar();
            if(actionBar != null)
            {

                // Disable up arrow
                actionBar.setDisplayHomeAsUpEnabled(false);
                if(AndroidUtils.hasAPI(14))
                {
                    actionBar.setHomeButtonEnabled(false);
                }
                ActionBarUtil.setTitle(this, getTitle());
            }
        }

        setProgressBarIndeterminateVisibility(true);

        setContentView(R.layout.main);

        ((TextView)findViewById(R.id.restoring))
            .setText(R.string.service_gui_SHUTDOWN_IN_PROGRESS);
    }
}
