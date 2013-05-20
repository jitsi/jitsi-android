/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.widgets;

import android.animation.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.jitsi.*;

/**
 * The controller used for displaying a custom toast that can be clicked.
 *
 */
public class ClickableToastController
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
     * How long the toast will be displayed.
     */
    private static final long DISPLAY_DURATION = 10000;

    /**
     * The toast <tt>View</tt> container.
     */
    private View toastView;

    /**
     * The <tt>TextView</tt> displaying message text.
     */
    private TextView messageView;

    /**
     * The animator object used to animate toast <tt>View</tt> alpha property.
     */
    private ObjectAnimator toastAnimator;

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
    private CharSequence toastMessage;

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
        this.toastView = toastView;

        // Initialize animator
        toastAnimator = new ObjectAnimator();
        toastAnimator.setPropertyName("alpha");
        toastAnimator.setTarget(toastView);

        this.clickListener = clickListener;

        messageView = (TextView) toastView.findViewById(R.id.toast_msg);

        toastView.findViewById(toastButtonId)
                .setOnClickListener(new View.OnClickListener()
                {

                    public void onClick(View view)
                    {
                        hideToast(false);
                        ClickableToastController.this
                                .clickListener.onClick(view);
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
        if (immediate)
        {
            toastView.setAlpha(1);
        }
        else
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
        hideHandler.removeCallbacks(hideRunnable);
        if (immediate)
        {
            toastView.setVisibility(View.GONE);
            toastView.setAlpha(0);
            toastMessage = null;
        }
        else
        {
            toastAnimator.cancel();
            toastAnimator.setFloatValues(1, 0);
            toastAnimator.setDuration(HIDE_DURATION);
            toastAnimator.start();
        }
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
