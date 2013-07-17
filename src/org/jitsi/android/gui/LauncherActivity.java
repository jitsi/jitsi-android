/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.os.Bundle;
import android.view.*;
import android.view.animation.*;
import android.widget.*;

import org.jitsi.*;
import org.jitsi.android.*;

import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

/**
 * The splash screen fragment displays animated Jitsi logo and indeterminate
 * progress indicators.
 *
 *
 * TODO: Evetually add exit option to the launcher
 *       Currently it's not possible to cancel OSGi startup.
 *       Attempt to stop service during startup is causing immediate
 *       service restart after shutdown even with synchronization of
 *       onCreate and OnDestroy commands. Maybe there is still some reference
 *       to OSGI service being held at that time ?
 *
 * @author Pawel Domas
 */
public class LauncherActivity
    extends OSGiActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Request indeterminate progress for splash screen
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setProgressBarIndeterminateVisibility(true);

        setContentView(R.layout.main);

        // Starts fade in animation
        ImageView myImageView
                = (ImageView) findViewById(R.id.loadingImage);
        Animation myFadeInAnimation
                = AnimationUtils.loadAnimation(this, R.anim.fadein);
        myImageView.startAnimation(myFadeInAnimation);
    }



    @Override
    protected void start(BundleContext osgiContext)
            throws Exception
    {
        super.start(osgiContext);

        runOnUiThread(new Runnable()
        {
            public void run()
            {
                // Start home screen Activity
                switchActivity(JitsiApplication.getHomeScreenActivityClass());
            }
        });
    }

}
