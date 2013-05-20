/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.app.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import org.jitsi.*;

/**
 * The splash screen fragment displays animated Jitsi logo and indeterminate
 * progress indicators.
 *
 * @author Pawel Domas
 */
public class SplashScreenFragment
    extends Fragment
{

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.main, container, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart()
    {
        super.onStart();

        getActivity().setProgressBarIndeterminateVisibility(true);

        // Starts fade in animation
        ImageView myImageView
                = (ImageView)getView().findViewById(R.id.loadingImage);
        Animation myFadeInAnimation
                = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
        myImageView.startAnimation(myFadeInAnimation);
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
