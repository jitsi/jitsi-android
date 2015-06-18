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
package org.jitsi.android.gui.controller;

import android.app.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;

import net.java.sip.communicator.util.*;

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
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(AutoHideController.class);

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
     * Hide <tt>View</tt> timeout
     */
    private long hideTimeout;

    /**
     * Listener object
     */
    private AutoHideListener listener;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();

        if(activity instanceof AutoHideListener)
        {
            listener = (AutoHideListener) getActivity();
        }

        view = activity.findViewById(getArguments().getInt(ARG_VIEW_ID));

        if(view == null)
            throw new NullPointerException("The view is null");

        hideTimeout = getArguments().getLong(ARG_HIDE_TIMEOUT);

        //inAnimation = AnimationUtils.loadAnimation(getActivity(),
        //                                           R.anim.show_from_bottom);
        //inAnimation.setAnimationListener(this);

        outAnimation = AnimationUtils.loadAnimation(activity,
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

        show();
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
        if(view == null)
        {
            logger.error("The view has not been created yet");
            return;
        }
        // This means that the View is hidden or animation is in progress
        if(autoHideTimer == null)
        {
            view.clearAnimation();
            // Need to re-layout the View
            view.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);

            if(listener != null)
            {
                listener.onAutoHideStateChanged(this, View.VISIBLE);
            }
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

            if(listener != null)
            {
                listener.onAutoHideStateChanged(this, View.GONE);
            }
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
     * Interface which can be used for listening to controlled view visibility
     * state changes.
     * Must be implemented by parent <tt>Activity</tt>, which will be registered
     * as a listener when this fragment is created.
     */
    public interface AutoHideListener
    {
        /**
         * Fired when controlled <tt>View</tt> visibility is changed by this
         * controller.
         * @param source the source <tt>AutoHideController</tt> of the event.
         * @param visibility controlled <tt>View</tt> visibility state.
         */
        void onAutoHideStateChanged(AutoHideController source, int visibility);
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
