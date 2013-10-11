/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.controller;

import android.os.*;
import android.view.*;
import android.view.animation.*;

import org.jitsi.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * The fragment is a controller which will hide given <tt>View</tt> after
 * specified delay interval. To reset and prevent from hiding for another period
 * of time call <tt>show</tt> method. This method will also instantly display
 * controlled <tt>View</tt> if it's currently hidden.
 *
 * @author Pawel Domas
 */
public class AutoHideController
    extends OSGiFragment
    implements Animation.AnimationListener
{
    /**
     * Argument key for the identifier of <tt>View</tt> that will be auto
     * hidden. It must exists in parent <tt>Activity</tt> view hierarchy.
     */
    private static final String ARG_VIEW_ID = "view_id";
    /**
     * Argument key for the delay interval, before the <tt>View</tt> will be
     * hidden
     */
    private static final String ARG_HIDE_TIMEOUT = "hide_timeout";

    //private Animation inAnimation;

    /**
     * Hide animation
     */
    private Animation outAnimation;

    /**
     * Controlled <tt>View</tt>
     */
    private View view;

    /**
     * Timer used for the hide task scheduling
     */
    private Timer autoHideTimer;

    /**
     * Hide <tt>View</tt> tiemout
     */
    private long hideTimeout;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        view = getActivity().findViewById(getArguments().getInt(ARG_VIEW_ID));

        hideTimeout = getArguments().getLong(ARG_HIDE_TIMEOUT);

        //inAnimation = AnimationUtils.loadAnimation(getActivity(),
        //                                           R.anim.show_from_bottom);
        //inAnimation.setAnimationListener(this);

        outAnimation = AnimationUtils.loadAnimation(getActivity(),
                                                    R.anim.hide_to_bottom);
        outAnimation.setAnimationListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        reScheduleAutoHideTask();
    }

    /**
     * Makes sure that hide task is scheduled. Cancels the previous one if is
     * currently scheduled.
     */
    private void reScheduleAutoHideTask()
    {
        // Cancel pending task if exists
        cancelAutoHideTask();

        autoHideTimer = new Timer();
        autoHideTimer.schedule(new AutoHideTask(), hideTimeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause()
    {
        super.onPause();

        cancelAutoHideTask();
    }

    /**
     * Makes sure that the hide task is cancelled.
     */
    private void cancelAutoHideTask()
    {
        if(autoHideTimer != null)
        {
            autoHideTimer.cancel();
            autoHideTimer = null;
        }
    }

    /**
     * Hides controlled <tt>View</tt>
     */
    public void hide()
    {
        if (!isViewVisible())
            return;

        // This call is required to clear the timer task
        cancelAutoHideTask();
        // Starts hide animation
        view.startAnimation(outAnimation);
    }

    /**
     * Shows controlled <tt>View</tt> and/or resets hide delay timer.
     */
    public void show()
    {
        // This means that the View is hidden or animation is in progress
        if(autoHideTimer == null)
        {
            view.clearAnimation();
            // Need to re-layout the View
            view.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
        }
        reScheduleAutoHideTask();
    }

    /**
     * Returns <tt>true</tt> if controlled <tt>View</tt> is currently visible.
     * @return <tt>true</tt> if controlled <tt>View</tt> is currently visible.
     */
    private boolean isViewVisible()
    {
        return view.getVisibility() == View.VISIBLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationStart(Animation animation)
    {
        //if(animation == inAnimation)
        //{
        //    view.setVisibility(View.VISIBLE);
        //    reScheduleAutoHideTask();
        //}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationEnd(Animation animation)
    {
        // If it's hide animation and the task wasn't cancelled
        if(animation == outAnimation && autoHideTimer == null)
        {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationRepeat(Animation animation){ }

    /**
     * Hide <tt>View</tt> timer task class.
     */
    class AutoHideTask extends TimerTask
    {
        @Override
        public void run()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    hide();
                }
            });
        }
    }

    /**
     * Creates new parametrized instance of <tt>AutoHideController</tt>.
     * @param viewId identifier of the <tt>View</tt> that will be auto hidden
     * @param hideTimeout auto hide delay in ms
     * @return new parametrized instance of <tt>AutoHideController</tt>.
     */
    public static AutoHideController getInstance(int viewId, long hideTimeout)
    {
        AutoHideController ahCtrl = new AutoHideController();

        Bundle args = new Bundle();
        args.putInt(ARG_VIEW_ID, viewId);
        args.putLong(ARG_HIDE_TIMEOUT, hideTimeout);
        ahCtrl.setArguments(args);

        return ahCtrl;
    }
}
