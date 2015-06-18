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

import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import org.jitsi.*;

/**
 * The controller used for displaying a custom toast that can be clicked.
 *
 *  @author Pawel Domas
 */
public class LegacyClickableToastCtrl
{
    /**
     * How long the toast will be displayed.
     */
    private static final long DISPLAY_DURATION = 10000;

    /**
     * The toast <tt>View</tt> container.
     */
    protected View toastView;

    /**
     * The <tt>TextView</tt> displaying message text.
     */
    private TextView messageView;

    /**
     * Handler object used for hiding the toast if it's not clicked.
     */
    private Handler hideHandler = new Handler();

    /**
     * The listener that will be notified when the toast is clicked.
     */
    private View.OnClickListener clickListener;

    /**
     * State object for message text.
     */
    protected CharSequence toastMessage;

    /**
     * Creates new instance of <tt>ClickableToastController</tt>.
     *
     * @param toastView the <tt>View</tt> that will be animated. Must contain
     * <tt>R.id.toast_msg</tt> <tt>TextView</tt>.
     * @param clickListener the click listener that will be notified when the
     * toast is clicked.
     */
    public LegacyClickableToastCtrl(View toastView,
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
    public LegacyClickableToastCtrl(View toastView,
                                    View.OnClickListener clickListener,
                                    int toastButtonId)
    {
        this.toastView = toastView;

        this.clickListener = clickListener;

        messageView = (TextView) toastView.findViewById(R.id.toast_msg);

        toastView.findViewById(toastButtonId)
            .setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View view)
                {
                    hideToast(false);
                    LegacyClickableToastCtrl.this.clickListener.onClick(view);
                }
            });

        hideToast(true);
    }

    /**
     * Shows the toast.
     *
     * @param immediate if <tt>true</tt> there wil be no animation.
     * @param message the toast text to use.
     */
    public void showToast(boolean immediate, CharSequence message)
    {
        toastMessage = message;
        messageView.setText(toastMessage);

        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, DISPLAY_DURATION);

        toastView.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the toast.
     *
     * @param immediate if <tt>true</tt> no animation will be used.
     */
    public void hideToast(boolean immediate)
    {
        hideHandler.removeCallbacks(hideRunnable);
        if(immediate)
        {
            onHide();
        }
    }

    /**
     * Performed to hide the toast view.
     */
    protected void onHide()
    {
        toastView.setVisibility(View.GONE);
        toastMessage = null;
    }

    /**
     * {@inheritDoc}
     */
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putCharSequence("toast_message", toastMessage);
    }

    /**
     * {@inheritDoc}
     */
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {
            toastMessage = savedInstanceState.getCharSequence("toast_message");

            if (!TextUtils.isEmpty(toastMessage))
            {
                showToast(true, toastMessage);
            }
        }
    }

    /**
     * Hides the toast after delay.
     */
    private Runnable hideRunnable = new Runnable()
    {
        public void run()
        {
            hideToast(false);
        }
    };
}
