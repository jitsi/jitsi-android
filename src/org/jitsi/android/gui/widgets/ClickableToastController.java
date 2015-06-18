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
package org.jitsi.android.gui.widgets;

import android.animation.*;
import android.view.*;

import org.jitsi.*;

/**
 * Animated version of {@link LegacyClickableToastCtrl}
 *
 * @author Pawel Domas
 */
public class ClickableToastController
    extends LegacyClickableToastCtrl
{
    /**
     * Show animation length
     */
    private static final long SHOW_DURATION = 2000;

    /**
     * Hide animation length
     */
    private static final long HIDE_DURATION = 2000;

    /**
     * The animator object used to animate toast <tt>View</tt> alpha property.
     */
    private ObjectAnimator toastAnimator;

    /**
     * Creates new instance of <tt>ClickableToastController</tt>.
     *
     * @param toastView the <tt>View</tt> that will be animated. Must contain
     * <tt>R.id.toast_msg</tt> <tt>TextView</tt>.
     * @param clickListener the click listener that will be notified when the
     * toast is clicked.
     */
    public ClickableToastController(View toastView,
                                    View.OnClickListener clickListener)
    {
        this(toastView, clickListener, R.id.toast_msg);
    }

    /**
     * Creates new instance of <tt>ClickableToastController</tt>.
     *
     * @param toastView the <tt>View</tt> that will be animated. Must contain
     * <tt>R.id.toast_msg</tt> <tt>TextView</tt>.
     * @param clickListener the click listener that will be notified when the
     * toast is clicked.
     * @param toastButtonId the id of <tt>View</tt> contained in <tt>toastView
     * </tt> that will be used as a button.
     */
    public ClickableToastController(View toastView,
                                    View.OnClickListener clickListener,
                                    int toastButtonId)
    {
        super(toastView, clickListener, toastButtonId);

        // Initialize animator
        toastAnimator = new ObjectAnimator();
        toastAnimator.setPropertyName("alpha");
        toastAnimator.setTarget(toastView);
    }

    /**
     * Shows the toast.
     *
     * @param immediate if <tt>true</tt> there wil be no animation.
     * @param message the toast text to use.
     */
    public void showToast(boolean immediate, CharSequence message)
    {
        super.showToast(immediate, message);
        if (!immediate)
        {
            toastAnimator.cancel();
            toastAnimator.setFloatValues(0, 1);
            toastAnimator.setDuration(SHOW_DURATION);
            toastAnimator.start();
        }
    }

    /**
     * Hides the toast.
     *
     * @param immediate if <tt>true</tt> no animation will be used.
     */
    public void hideToast(boolean immediate)
    {
        super.hideToast(immediate);
        if (!immediate)
        {
            toastAnimator.cancel();
            toastAnimator.setFloatValues(1, 0);
            toastAnimator.setDuration(HIDE_DURATION);
            toastAnimator.start();
            toastAnimator.addListener(new Animator.AnimatorListener()
            {
                public void onAnimationStart(Animator animation)
                {

                }

                public void onAnimationEnd(Animator animation)
                {
                    onHide();
                }

                public void onAnimationCancel(Animator animation)
                {

                }

                public void onAnimationRepeat(Animator animation)
                {

                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onHide()
    {
        super.onHide();
        toastView.setAlpha(0);
    }
}
