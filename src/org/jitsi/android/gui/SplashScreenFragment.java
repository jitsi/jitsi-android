/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import org.jitsi.*;
import org.jitsi.service.osgi.*;

/**
 * The splash screen fragment displays animated Jitsi logo and indeterminate
 * progress indicators.
 *
 * @author Pawel Domas
 */
public class SplashScreenFragment
    extends OSGiFragmentV4
{
    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View content = inflater.inflate(R.layout.main,
                                        container,
                                        false);

        getActivity().setProgressBarIndeterminateVisibility(true);

        // Starts fade in animation
        ImageView myImageView
                = (ImageView) content.findViewById(R.id.loadingImage);
        Animation myFadeInAnimation
                = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
        myImageView.startAnimation(myFadeInAnimation);

        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart()
    {
        super.onStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop()
    {
        super.onStop();

        getActivity().setProgressBarIndeterminateVisibility(false);
    }
}
