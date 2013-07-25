/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.call;

import java.beans.*;
import java.util.*;

import android.content.*;
import android.graphics.Color;
import android.media.*;
import android.os.*;
import android.support.v4.app.DialogFragment;
import android.util.*;
import android.view.*;
import android.view.Menu; // Disambiguation
import android.view.MenuItem; // Disambiguation
import android.widget.*;

import org.jitsi.R;
import org.jitsi.android.*;
import org.jitsi.android.gui.call.notification.*;
import org.jitsi.android.gui.controller.*;
import org.jitsi.android.gui.fragment.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.widgets.*;
import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.jmfext.media.protocol.mediarecorder.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.event.*;

import net.java.sip.communicator.service.gui.call.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;
import net.java.sip.communicator.util.call.CallPeerAdapter;

/**
 * The <tt>VideoCallActivity</tt> corresponds the call screen.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class VideoCallActivity
    extends OSGiActivity
    implements  CallPeerRenderer,
                CallRenderer,
                CallChangeListener,
                PropertyChangeListener,
                ZrtpInfoDialog.SasVerificationListener
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(VideoCallActivity.class);

    /**
     * Tag name for fragment that handles proximity sensor in order to turn
     * the screen on and off.
     */
    private static final String PROXIMITY_FRAGMENT_TAG="proximity";

    /**
     * The remote video container.
     */
    private ViewGroup remoteVideoContainer;

    /**
     * The remote video view
     */
    private ViewAccessor remoteVideoAccessor;

    /**
     * The local video container.
     */
    private SurfaceView previewDisplay;

    /**
     * The callee avatar.
     */
    private ImageView calleeAvatar;

    /**
     * The call peer adapter that gives us access to all call peer events.
     */
    private CallPeerAdapter callPeerAdapter;

    /**
     * Instance of video listener that should be unregistered once this Activity
     * is destroyed
     */
    private VideoListener callPeerVideoListener;

    /**
     * The corresponding call.
     */
    private Call call;

    /**
     * Indicates if the call timer has been started.
     */
    private boolean isCallTimerStarted = false;

    /**
     * The start date time of the call.
     */
    private Date callStartDate;

    /**
     * A timer to count call duration.
     */
    private Timer callDurationTimer;

    /**
     * The {@link CallConference} instance depicted by this <tt>CallPanel</tt>.
     */
    private CallConference callConference;

    /**
     * The preview surface state handler
     */
    private CameraPreviewSurfaceHandler previewSurfaceHandler;

    /**
     * Flag indicates if the shutdown Thread has been started
     */
    private volatile boolean finishing = false;

    /**
     * The call identifier managed by {@link CallManager}
     */
    private String callIdentifier;

    /**
     * Stores the current local video state in case this <tt>Activity</tt> is
     * hidden during call.
     */
    private static boolean wasVideoEnabled = false;

    /**
     * The zrtp SAS verification toast controller.
     */
    private LegacyClickableToastCtrl sasToastController;

    /**
     * Called when the activity is starting. Initializes the corresponding
     * call interface.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_call);

        remoteVideoContainer
                = (ViewGroup) findViewById(R.id.remoteVideoContainer);

        callDurationTimer = new Timer();

        this.callIdentifier
                = getIntent().getExtras()
                        .getString(CallManager.CALL_IDENTIFIER);

        call = CallManager.getActiveCall(callIdentifier);

        if(call == null)
            throw new IllegalArgumentException(
                    "There's no call with id: "+callIdentifier);

        callConference = call.getConference();

        initMicrophoneView();
        initHangupView();

        calleeAvatar = (ImageView) findViewById(R.id.calleeAvatar);

        previewDisplay = (SurfaceView) findViewById(R.id.previewDisplay);
        
        // Creates and registers surface handler for events
        this.previewSurfaceHandler = new CameraPreviewSurfaceHandler();
        org.jitsi.impl.neomedia.jmfext.media
                .protocol.mediarecorder.DataSource
                .setPreviewSurfaceProvider(previewSurfaceHandler);
        previewDisplay.getHolder().addCallback(previewSurfaceHandler);

        // Preview display will be displayed on top of remote video
        previewDisplay.setZOrderMediaOverlay(true);

        // Makes the preview display draggable on the screen
        previewDisplay.setOnTouchListener(new SimpleDragController());

        // Registers as the call state listener
        call.addCallChangeListener(this);

        View toastView = findViewById(R.id.clickable_toast);
        View.OnClickListener toastclickHandler
                = new View.OnClickListener()
                        {
                            public void onClick(View v)
                            {
                                showZrtpInfoDialog();
                                sasToastController.hideToast(true);
                            }
                        };

        if(Build.VERSION.SDK_INT >= 11)
        {
            sasToastController
                    = new ClickableToastController( toastView,
                                                    toastclickHandler );
        }
        else
        {
            sasToastController
                    = new LegacyClickableToastCtrl( toastView,
                                                    toastclickHandler );
        }

        /**
         * Adds fragment that turns on and off the screen when proximity sensor
         * detects FAR/NEAR distance.
         */
        getSupportFragmentManager()
                .beginTransaction()
                .add(new ProximitySensorFragment(), PROXIMITY_FRAGMENT_TAG)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        sasToastController.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        sasToastController.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Called when an activity is destroyed.
     */
    @Override
    protected void onDestroy()
    {
        if(isCallTimerStarted())
        {
            stopCallTimer();
        }

        // Release shared video component
        if(remoteVideoAccessor != null)
        {
            remoteVideoContainer.removeView(remoteVideoAccessor.getView(this));
        }

        super.onDestroy();
    }

    /**
     * Initializes the hangup button view.
     */
    private void initHangupView()
    {
        ImageView hangupView = (ImageView) findViewById(R.id.callHangupButton);

        hangupView.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Start the hang up Thread, Activity will be closed later 
                // on call ended event
                CallManager.hangupCall(call);
            }
        });
    }

    /**
     * Called on call ended event. Runs on separate thread to release the EDT
     * Thread and preview surface can be hidden effectively.
     */
    private void doFinishActivity()
    {
        if(finishing)
            return;
        
        finishing = true;
        
        new Thread(new Runnable() 
        {
            public void run() 
            {
                // Waits for camera to be stopped
                previewSurfaceHandler.ensureCameraClosed();

                switchActivity(JitsiApplication.getHomeScreenActivityClass());
            }
        }).start();        
    }

    /**
     * Called when local video button is pressed.
     *
     * @param callVideoButton local video button <tt>View</tt>.
     */
    public void onLocalVideoButtonClicked(View callVideoButton)
    {
        boolean isEnable = !isLocalVideoEnabled();

        setLocalVideoEnabled(isEnable);

        if (isEnable)
            callVideoButton.setBackgroundColor(0x50000000);
        else
            callVideoButton.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * Checks local video status.
     *
     * @return <tt>true</tt> if local video is enabled.
     */
    private boolean isLocalVideoEnabled()
    {
        return CallManager.isLocalVideoEnabled(call);
    }

    /**
     * Sets local video status.
     *
     * @param enable flag indicating local video status to be set.
     */
    private void setLocalVideoEnabled(boolean enable)
    {
        CallManager.enableLocalVideo(
                call,
                enable);
    }

    /**
     * Initializes the microphone button view.
     */
    private void initMicrophoneView()
    {
        final ImageView microphoneButton
            = (ImageView) findViewById(R.id.callMicrophoneButton);

        microphoneButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                CallManager.setMute(call, !isMuted());
            }
        });
        microphoneButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            public boolean onLongClick(View view)
            {
                DialogFragment newFragment
                        = VolumeControlDialog.createInputVolCtrlDialog();
                newFragment.show( getSupportFragmentManager(),
                                  "vol_ctrl_dialog" );
                return true;
            }
        });
    }

    /**
     * Returns <tt>true</tt> if call is currently muted.
     *
     * @return <tt>true</tt> if call is currently muted.
     */
    private boolean isMuted()
    {
        return CallManager.isMute(call);
    }

    private void updateMuteStatus()
    {
        runOnUiThread(
        new Runnable()
        {
            public void run()
            {
                doUpdateMuteStatus();
            }
        });
    }

    private void doUpdateMuteStatus()
    {
        final ImageView microphoneButton
                = (ImageView) findViewById(R.id.callMicrophoneButton);

        if (isMuted())
        {
            microphoneButton.setBackgroundColor(0x50000000);
            microphoneButton.setImageResource(
                    R.drawable.callmicrophonemute);
        }
        else
        {
            microphoneButton.setBackgroundColor(Color.TRANSPARENT);
            microphoneButton.setImageResource(
                    R.drawable.callmicrophone);
        }
    }

    /**
     * Fired when call volume control button is clicked.
     * @param v the call volume control <tt>View</tt>.
     */
    public void onCallVolumeClicked(View v)
    {
        // Create and show the dialog.
        DialogFragment newFragment
                = VolumeControlDialog.createOutputVolCtrlDialog();
        newFragment.show(getSupportFragmentManager(), "vol_ctrl_dialog");
    }

    /**
     * Fired when speakerphone button is clicked.
     * @param v the speakerphone button <tt>View</tt>.
     */
    public void onSpeakerphoneClicked(View v)
    {
        AudioManager audioManager = JitsiApplication.getAudioManager();
        audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
        updateSpeakerphoneStatus();
    }

    /**
     * Updates speakerphone button status.
     */
    private void updateSpeakerphoneStatus()
    {
        final ImageView speakerPhoneButton
                = (ImageView) findViewById(R.id.speakerphoneButton);

        if (JitsiApplication.getAudioManager().isSpeakerphoneOn())
        {
            speakerPhoneButton.setBackgroundColor(0x50000000);
        }
        else
        {
            speakerPhoneButton.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        /**
         * The call to:
         * setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)
         * doesn't work when notification was being played during this Activity
         * creation, so the buttons must be captured and the voice call level
         * will be manipulated programmatically.
         */
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_UP)
                {
                    ((AudioManager)getSystemService(Context.AUDIO_SERVICE))
                            .adjustStreamVolume(AudioManager.STREAM_VOICE_CALL,
                                                AudioManager.ADJUST_RAISE,
                                                AudioManager.FLAG_SHOW_UI);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN)
                {
                    ((AudioManager)getSystemService(Context.AUDIO_SERVICE))
                            .adjustStreamVolume(AudioManager.STREAM_VOICE_CALL,
                                                AudioManager.ADJUST_LOWER,
                                                AudioManager.FLAG_SHOW_UI);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    /**
     * Initializes current video status for the call.
     *
     * @param callPeer owner of video object.
     */
    private void initRemoteVideo(CallPeer callPeer)
    {
        ProtocolProviderService pps = callPeer.getProtocolProvider();
        Component visualComponent = null;

        if (pps != null)
        {
            OperationSetVideoTelephony osvt
                    = pps.getOperationSet(OperationSetVideoTelephony.class);

            if (osvt != null)
                visualComponent = osvt.getVisualComponent(callPeer);
        }
        handleRemoteVideoEvent(visualComponent, null);
    }

    /**
     * Reinitialize the <tt>Activity</tt> to reflect current call status.
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        // Clears the in call notification
        if(CallNotificationManager.get().isNotificationRunning(callIdentifier))
        {
            CallNotificationManager.get()
                    .stopNotification(this, callIdentifier);

        }
        // Restores local video state
        if(wasVideoEnabled)
        {
            setLocalVideoEnabled(true);
        }
        // Registers as the call state listener
        call.addCallChangeListener(this);

        // Checks if call peer has video component
        Iterator<? extends CallPeer> peers = call.getCallPeers();
        if(peers.hasNext())
        {
            CallPeer callPeer = peers.next();
            addCallPeerUI(callPeer);
            addVideoListener(callPeer);
            initRemoteVideo(callPeer);
        }
        else
        {
            logger.error("There aren't any peers in the call");
            finish();
            return;
        }

        doUpdateHoldStatus();
        doUpdateCallDuration();
        doUpdateMuteStatus();
        updateSpeakerphoneStatus();
        initSecurityStatus();
    }

    /**
     * Called when this <tt>Activity</tt> is paused(hidden).
     * Releases all listeners and leaves the in call notification if the call is
     * in progress.
     */
    @Override
    protected void onPause()
    {
        call.removeCallChangeListener(this);

        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();
        if (callPeerIter.hasNext())
        {
            removeCallPeerUI(callPeerIter.next());
        }
        callPeerAdapter.dispose();
        callPeerAdapter = null;

        removeVideoListener();

        if(call.getCallState() != CallState.CALL_ENDED)
        {
            leaveNotification();

            wasVideoEnabled = isLocalVideoEnabled();
            logger.error("Was local enabled ? "+wasVideoEnabled);

            /**
             * Disables local video to stop the camera and release the surface.
             * Otherwise media recorder will crash on invalid preview surface.
             */
            setLocalVideoEnabled(false);
            previewSurfaceHandler.ensureCameraClosed();
        }

        super.onPause();
    }

    /**
     * Leaves the in call notification.
     */
    private void leaveNotification()
    {
        if(Build.VERSION.SDK_INT < 11)
        {
            // TODO: fix in call notifications for sdk < 11
            logger.warn("In call notifications not supported prior SDK 11");
            return;
        }

        String inCallStr = getString(R.string.in_call_with);

        Iterator<? extends CallPeer> callPeers = call.getCallPeers();
        if(callPeers.hasNext())
        {
            inCallStr += " " + callPeers.next().getDisplayName();
        }

        CallNotificationManager.get().showCallNotification(this, callIdentifier);
    }

    /**
     * Handles a video event.
     *
     * @param callPeer the corresponding call peer
     * @param event the <tt>VideoEvent</tt> that notified us
     */
    public void handleVideoEvent(CallPeer callPeer,
                                 final VideoEvent event)
    {
        if (event.isConsumed())
            return;

        event.consume();

        if (event.getOrigin() == VideoEvent.LOCAL)
        {
            // TODO: local video events are not used because the preview surface
            // is required for camera to start and it must not be removed until
            // is stopped, so it's handled by direct cooperation with 
            // .jmfext.media.protocol.mediarecorder.DataSource
            
            // Show/hide the local video.
            if (event.getType() == VideoEvent.VIDEO_ADDED)
            {
                
            }
            else if(event.getType() == VideoEvent.VIDEO_REMOVED)
            {
                
            }
            else if(event.getType() == SizeChangeVideoEvent.VIDEO_SIZE_CHANGE)
            {
                
            }
        }
        else if (event.getOrigin() == VideoEvent.REMOTE)
        {
            Component visualComponent
                    = (event.getType() == VideoEvent.VIDEO_ADDED
                      || event instanceof SizeChangeVideoEvent)
                    ? event.getVisualComponent()
                    : null;

            SizeChangeVideoEvent scve
                    = event instanceof SizeChangeVideoEvent
                    ? (SizeChangeVideoEvent) event
                    : null;

            handleRemoteVideoEvent(visualComponent, scve);
        }
    }

    /**
     * Handles remote video event arguments.
     *
     * @param visualComponent the remote video <tt>Component</tt> if available
     * or <tt>null</tt> otherwise.
     * @param scve the <tt>SizeChangeVideoEvent</tt> event if was supplied.
     */
    private void handleRemoteVideoEvent(final Component visualComponent,
                                        final SizeChangeVideoEvent scve)
    {
        if(visualComponent instanceof ViewAccessor)
        {
            logger.trace("Remote video added "+hashCode());
            this.remoteVideoAccessor = (ViewAccessor)visualComponent;
        }
        else
        {
            logger.trace("Remote video removed "+hashCode());
            this.remoteVideoAccessor = null;
            // null evaluates to false, so need to check here before warn
            if(visualComponent != null)
            {
                // Report component as not compatible
                logger.error(
                        "Remote video component is not Android compatible.");
            }
        }

        runOnUiThread(new Runnable()
        {
            public void run()
            {
                final View view
                        = remoteVideoAccessor != null
                        ? remoteVideoAccessor.getView(VideoCallActivity.this)
                        : null;

                Dimension preferredSize
                        = selectRemotePreferredSize(
                        visualComponent,
                        view,
                        scve);
                logger.info("Remote video size " + preferredSize.getWidth()
                                    + " : " + preferredSize.getHeight());
                doAlignRemoteVideo(view, preferredSize);
            }
        });
    }

    /**
     * Selected remote video preferred size based on current components and
     * event status.
     *
     * @param visualComponent remote video <tt>Component</tt>, <tt>null</tt>
     * if not available
     * @param remoteVideoView the remote video <tt>View</tt> if already created,
     * or <tt>null</tt> otherwise
     * @param scve the <tt>SizeChangeVideoEvent</tt> if was supplied during
     * event handling or <tt>null</tt> otherwise.
     *
     * @return selected preferred remote video size.
     */
    private Dimension selectRemotePreferredSize(Component visualComponent,
                                                View remoteVideoView,
                                                SizeChangeVideoEvent scve)
    {
        int width = -1;
        int height = -1;

        if(remoteVideoView == null || visualComponent == null)
        {
            // There's no remote video View, so returns invalid size
            return new Dimension(width, height);
        }

        Dimension preferredSize = visualComponent.getPreferredSize();
        if ((preferredSize != null)
                && (preferredSize.width > 0)
                && (preferredSize.height > 0))
        {
            /*
             * If the visualComponent displaying the
             * video of the remote callPeer has a
             * preferredSize, attempt to respect it.
             */
            width = preferredSize.width;
            height = preferredSize.height;
        }
        else if (scve != null)
        {
            /*
             * The SizeChangeVideoEvent may have
             * been delivered with a delay and thus
             * may not represent the up-to-date size
             * of the remote video. But since the
             * visualComponent does not have a
             * preferredSize, anything like the size
             * reported by the SizeChangeVideoEvent
             * may be used as a hint.
             */
            if ((scve.getHeight() > 0)
                    && (scve.getWidth() > 0))
            {
                height = scve.getHeight();
                width = scve.getWidth();
            }
        }

        return new Dimension(width, height);
    }

    /**
     * Aligns remote <tt>Video</tt> component if available.
     *
     * @param remoteVideoView the remote video <tt>View</tt> if available or
     * <tt>null</tt> otherwise.
     * @param preferredSize preferred size of remote video <tt>View</tt>.
     */
    private void doAlignRemoteVideo(View remoteVideoView,
                                    Dimension preferredSize)
    {
        double width = preferredSize.getWidth();
        double height = preferredSize.getHeight();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay()
                .getMetrics(displaymetrics);
        int viewHeight = displaymetrics.heightPixels;
        int viewWidth = displaymetrics.widthPixels;

        remoteVideoContainer.removeAllViews();

        if (remoteVideoView != null)
        {
            double ratio = width / height;

            if (height < viewHeight)
            {
                height = viewHeight;
                width = height*ratio;
            }
            remoteVideoContainer.addView(
                    remoteVideoView,
                    new ViewGroup.LayoutParams(
                            (int)width,
                            (int)height));

            calleeAvatar.setVisibility(View.GONE);

            // Show the local video in the center or in the
            // left corner depending on if we have a remote
            // video shown.
            realignPreviewDisplay();
        }
        else
            calleeAvatar.setVisibility(View.VISIBLE);
    }

    /**
     * Adds a video listener for the given call peer.
     *
     * @param callPeer the <tt>CallPeer</tt> to which we add a video listener
     */
    private void addVideoListener(final CallPeer callPeer)
    {
        ProtocolProviderService pps = callPeer.getProtocolProvider();

        if (pps == null)
            return;

        OperationSetVideoTelephony osvt
            = pps.getOperationSet(OperationSetVideoTelephony.class);

        if (osvt == null)
            return;

        if(callPeerVideoListener == null)
        {
            callPeerVideoListener = new VideoListener()
            {
                public void videoAdded(VideoEvent event)
                {
                    handleVideoEvent(callPeer, event);
                }

                public void videoRemoved(VideoEvent event)
                {
                    handleVideoEvent(callPeer, event);
                }

                public void videoUpdate(VideoEvent event)
                {
                    handleVideoEvent(callPeer, event);
                }
            };
        }
        osvt.addVideoListener(
                callPeer,
                callPeerVideoListener);
    }

    /**
     * Removes remote video listener.
     */
    private void removeVideoListener()
    {
        Iterator<? extends CallPeer> calPeers = call.getCallPeers();

        if(!calPeers.hasNext())
            return;

        CallPeer callPeer = calPeers.next();
        ProtocolProviderService pps = call.getProtocolProvider();

        if (pps == null)
            return;

        OperationSetVideoTelephony osvt
                = pps.getOperationSet(OperationSetVideoTelephony.class);

        if (osvt == null)
            return;

        if(callPeerVideoListener != null)
        {
            osvt.removeVideoListener(callPeer, callPeerVideoListener);
        }
    }

    /**
     * Re-aligns the preview display depending on the remote video visibility.
     */
    private void realignPreviewDisplay()
    {
        RelativeLayout.LayoutParams params
            = (RelativeLayout.LayoutParams) previewDisplay
            .getLayoutParams();

        if (remoteVideoContainer.getChildCount() > 0)
        {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
        }
        else
        {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        }

        previewDisplay.setLayoutParams(params);
    }

    /**
     * Sets the peer name.
     *
     * @param name the name of the call peer
     */
    public void setPeerName(final String name)
    {
        // ActionBar is not support prior 3.0
        if(Build.VERSION.SDK_INT < 11)
            return;

        runOnUiThread(new Runnable()
        {
            public void run()
            {
                ActionBarUtil.setTitle(VideoCallActivity.this,
                    getResources().getString(
                        R.string.service_gui_CALL_WITH) + ": ");
                ActionBarUtil.setSubtitle(VideoCallActivity.this, name);
            }
        });
    }

    /**
     * Sets the peer image.
     *
     * @param image the avatar of the call peer
     */
    public void setPeerImage(byte[] image)
    {

    }

    /**
     * Sets the peer state.
     *
     * @param oldState the old peer state
     * @param newState the new peer state
     * @param stateString the state of the call peer
     */
    public void setPeerState(CallPeerState oldState, CallPeerState newState,
        final String stateString)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                TextView statusName = (TextView) findViewById(R.id.callStatus);

                statusName.setText(stateString);
            }
        });
    }

    /**
     * Updates the call duration string. Invoked on UI thread.
     */
    public void updateCallDuration()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                doUpdateCallDuration();
            }
        });
    }

    /**
     * Updates the call duration string.
     */
    private void doUpdateCallDuration()
    {
        if(callStartDate == null)
            return;
        String timeStr = GuiUtils.formatTime(
                callStartDate.getTime(),
                System.currentTimeMillis());
        TextView callTime = (TextView) findViewById(R.id.callTime);
        callTime.setText(timeStr);
    }

    public void setErrorReason(String reason) {}

    public void setMute(boolean isMute)
    {
        // Just invoke mute UI refresh
        updateMuteStatus();
    }

    /**
     * Method mapped to hold button view on click event
     *
     * @param holdButtonView the button view that has been clicked
     */
    public void onHoldButtonClicked(View holdButtonView)
    {
        CallManager.putOnHold(call, !isOnHold());
    }

    private boolean isOnHold()
    {
        boolean onHold = false;
        Iterator<? extends CallPeer> peers = call.getCallPeers();
        if(peers.hasNext())
        {
            CallPeerState peerState = call.getCallPeers().next().getState();
            onHold = CallPeerState.ON_HOLD_LOCALLY.equals(peerState)
                    || CallPeerState.ON_HOLD_MUTUALLY.equals(peerState);
        }
        else 
        {
            logger.warn("No peer belongs to call: "+call.toString());    
        }

        return onHold;
    }

    public void setOnHold(boolean isOnHold){}

    /**
     * Updates on hold button to represent it's actual state
     */
    private void updateHoldStatus()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                doUpdateHoldStatus();
            }
        });
    }

    /**
     * Updates on hold button to represent it's actual state.
     * Called from {@link #updateHoldStatus()}.
     */
    private void doUpdateHoldStatus()
    {
        final ImageView holdButton
                = (ImageView) findViewById(R.id.callHoldButton);

        if (isOnHold())
        {
            holdButton.setBackgroundColor(0x50000000);
        }
        else
        {
            holdButton.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public void setCallPeerAdapter(CallPeerAdapter adapter)
    {
        this.callPeerAdapter = adapter;
    }

    public CallPeerAdapter getCallPeerAdapter()
    {
        return callPeerAdapter;
    }

    public void printDTMFTone(char dtmfChar)
    {

    }

    public CallRenderer getCallRenderer()
    {
        return this;
    }

    public void setLocalVideoVisible(final boolean isVisible)
    {
        // It can not be hidden here, because the preview surface will be
        // destroyed and camera recording system will crash     
    }

    /**
     * Sets {@link #previewDisplay} visibility state. As a result onCreate and
     * onDestroy events are produced when the surface used for camera display is
     * created/destroyed. 
     * 
     * @param isVisible flag indicating if it should be shown or hidden
     */
    private void setLocalVideoPreviewVisible(final boolean isVisible)
    {
        Handler previewDisplayHandler = previewDisplay.getHandler();
        if(previewDisplayHandler == null)
        {
            logger.warn("Preview surface is no longer attached");
            return;
        }
        previewDisplayHandler.post(new Runnable()
        {
            public void run() 
            {
                if (isVisible)
                {
                    // Show the local video in the center or in the left
                    // corner depending on if we have a remote video shown.
                    realignPreviewDisplay();
                    previewDisplay.setVisibility(View.VISIBLE);
                }
                else
                {
                    previewDisplay.setVisibility(View.GONE);
                }
            }
        });
    }

    public boolean isLocalVideoVisible()
    {
        return (previewDisplay.getVisibility() != View.VISIBLE)
                    ? false
                    : true;
    }

    public Call getCall()
    {
        return call;
    }

    public CallPeerRenderer getCallPeerRenderer(CallPeer callPeer)
    {
        return this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.video_call_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.low_resolution:
                return true;
            case R.id.high_resolution:
                return true;
            case R.id.call_info_item:
                showCallInfoDialog();
                return true;
            case R.id.call_zrtp_info_item:
                showZrtpInfoDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Displays technical call information dialog.
     */
    private void showCallInfoDialog()
    {
        CallInfoDialogFragment callInfo
                = CallInfoDialogFragment.newInstance(
                getIntent().getStringExtra(
                        CallManager.CALL_IDENTIFIER));

        callInfo.show(getSupportFragmentManager(), "callinfo");
    }

    /**
     * Displays ZRTP call information dialog.
     */
    private void showZrtpInfoDialog()
    {
        ZrtpInfoDialog zrtpInfo
            = ZrtpInfoDialog.newInstance(
                getIntent().getStringExtra(CallManager.CALL_IDENTIFIER));

        zrtpInfo.show(getSupportFragmentManager(), "zrtpinfo");
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        /*
         * If a Call is added to or removed from the CallConference depicted
         * by this CallPanel, an update of the view from its model will most
         * likely be required.
         */
        if (evt.getPropertyName().equals(CallConference.CALLS))
            onCallConferenceEventObject(evt);
    }

    public void callPeerAdded(CallPeerEvent evt)
    {
        CallPeer callPeer = evt.getSourceCallPeer();

        addCallPeerUI(callPeer);

        onCallConferenceEventObject(evt);
    }

    public void callPeerRemoved(CallPeerEvent evt)
    {
        CallPeer callPeer = evt.getSourceCallPeer();

        if (callPeerAdapter != null)
        {
            callPeer.addCallPeerListener(callPeerAdapter);
            callPeer.addCallPeerSecurityListener(callPeerAdapter);
            callPeer.addPropertyChangeListener(callPeerAdapter);
        }

        setPeerState(callPeer.getState(),
                     callPeer.getState(),
                     callPeer.getState().getLocalizedStateString());

        onCallConferenceEventObject(evt);
    }

    public void callStateChanged(CallChangeEvent evt)
    {
        onCallConferenceEventObject(evt);
    }

    /**
     * Invoked by {@link #callConferenceListener} to notify this instance about
     * an <tt>EventObject</tt> related to the <tt>CallConference</tt> depicted
     * by this <tt>CallPanel</tt>, the <tt>Call</tt>s participating in it,
     * the <tt>CallPeer</tt>s associated with them, the
     * <tt>ConferenceMember</tt>s participating in any telephony conferences
     * organized by them, etc. In other words, notifies this instance about
     * any change which may cause an update to be required so that this view
     * i.e. <tt>CallPanel</tt> depicts the current state of its model i.e.
     * {@link #callConference}.
     *
     * @param ev the <tt>EventObject</tt> this instance is being notified
     * about.
     */
    private void onCallConferenceEventObject(EventObject ev)
    {
        /*
         * The main task is to invoke updateViewFromModel() in order to make
         * sure that this view depicts the current state of its model.
         */

        try
        {
            /*
             * However, we seem to be keeping track of the duration of the call
             * (i.e. the telephony conference) in the user interface. Stop the
             * Timer which ticks the duration of the call as soon as the
             * telephony conference depicted by this instance appears to have
             * ended. The situation will very likely occur when a Call is
             * removed from the telephony conference or a CallPeer is removed
             * from a Call.
             */
            boolean tryStopCallTimer = false;

            if (ev instanceof CallPeerEvent)
            {
                tryStopCallTimer
                    = (CallPeerEvent.CALL_PEER_REMOVED
                            == ((CallPeerEvent) ev).getEventID());
            }
            else if (ev instanceof PropertyChangeEvent)
            {
                PropertyChangeEvent pcev = (PropertyChangeEvent) ev;

                tryStopCallTimer
                    = (CallConference.CALLS.equals(pcev)
                            && (pcev.getOldValue() instanceof Call)
                            && (pcev.getNewValue() == null));
            }

            if (tryStopCallTimer
                    && (callConference.isEnded()
                            || callConference.getCallPeerCount() == 0))
            {
                stopCallTimer();
                doFinishActivity();
            }
        }
        finally
        {
            updateViewFromModel(ev);
        }
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        if(callStartDate == null)
        {
            this.callStartDate = new Date();
        }

        this.callDurationTimer
            .schedule(new CallTimerTask(),
                new Date(System.currentTimeMillis()), 1000);

        this.isCallTimerStarted = true;
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer()
    {
        this.callDurationTimer.cancel();
    }

    /**
     * Returns <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>
     */
    public boolean isCallTimerStarted()
    {
        return isCallTimerStarted;
    }

    /**
     * {@inheritDoc}
     */
    public void onSasVerificationChanged(boolean isVerified)
    {
        doUpdatePadlockStatus(true, isVerified);
    }

    /**
     * Each second refreshes the time label to show to the user the exact
     * duration of the call.
     */
    private class CallTimerTask
        extends TimerTask
    {
        @Override
        public void run()
        {
            updateCallDuration();
        }
    }

    /**
     * The class exposes methods for managing preview surface state which must 
     * be synchronized with currently used {@link android.hardware.Camera} 
     * state.<br/>
     * The surface must be present before the camera is started and for this 
     * purpose {@link #obtainPreviewSurface()} method shall be used.
     * <br/>
     * When the call is ended, before the <tt>Activity</tt> is finished we
     * should ensure that the camera has been stopped(which is done by video
     * telephony internals), so we should wait for it to be disposed by 
     * invoking method {@link #ensureCameraClosed()}. It will block current 
     * <tt>Thread</tt> until it happens or an <tt>Exception</tt> will be thrown
     * if timeout occurs.
     * <br/>
     * It's a workaround which allows not changing the
     * OperationSetVideoTelephony and related APIs.
     *  
     * @see DataSource.PreviewSurfaceProvider
     * 
     */
    private class CameraPreviewSurfaceHandler
    implements DataSource.PreviewSurfaceProvider,
            SurfaceHolder.Callback    
    {

        /**
         * Timeout for dispose surface operation
         */
        private static final long REMOVAL_TIMEOUT=10000L;

        /**
         * Timeout for create surface operation
         */
        private static final long CREATE_TIMEOUT=10000L;

        /**
         * Pointer to the <tt>Surface</tt> used for preview
         */
        private Surface previewSurface;

        /**
         * Blocks until the {@link android.hardware.Camera} is stopped and 
         * {@link #previewDisplay} is hidden, or throws an <tt>Exception</tt>
         * if timeout occurs.
         */
        synchronized void ensureCameraClosed()
        {
            // If local video is visible wait until camera will be closed
            if(previewSurface != null)
            {
                try
                {
                    synchronized (this)
                    {
                        this.wait(REMOVAL_TIMEOUT);
                        if(previewSurface != null)
                        {
                            throw new RuntimeException(
                                    "Timeout waiting for" 
                                    + " preview surface removal");
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Blocks until {@link #previewDisplay} is shown and the surface is
         * created or throws en <tt>Exception</tt> if timeout occurs.
         * 
         * @return created <tt>Surface</tt> that shall be used for local camera
         * preview
         */
        synchronized public Surface obtainPreviewSurface()
        {
            setLocalVideoPreviewVisible(true);
            if(this.previewSurface == null)
            {
                try 
                {
                    this.wait(CREATE_TIMEOUT);
                    if(previewSurface == null)
                    {
                        throw new RuntimeException(
                                "Timeout waiting for surface");
                    }                    
                }
                catch (InterruptedException e) 
                {
                    throw new RuntimeException(e);
                }
            }
            return previewSurface;    
        }

        /**
         * {@inheritDoc}
         */
        public int getDisplayRotation()
        {
            return getWindowManager().getDefaultDisplay().getRotation();
        }

        /**
         * Hides the local video preview component causing the <tt>Surface</tt>
         * to be destroyed.
         */
        public void onPreviewSurfaceReleased()
        {
            setLocalVideoPreviewVisible(false);
            releasePreviewSurface();
        }

        /**
         * Releases the preview surface and notifies all threads waiting on
         * the lock.
         */
        synchronized private void releasePreviewSurface()
        {
            if(previewSurface == null)
                return;

            this.previewSurface = null;
            this.notifyAll();
        }
        
        synchronized public void surfaceCreated(SurfaceHolder holder)
        {
            this.previewSurface = holder.getSurface();
            this.notifyAll();
        }

        public void surfaceChanged( SurfaceHolder surfaceHolder, 
                                    int i, int i2, int i3 ) 
        {
            
        }

        synchronized public void surfaceDestroyed(SurfaceHolder holder)
        {
            releasePreviewSurface();
        }
    }

    private void addCallPeerUI(CallPeer callPeer)
    {
        callPeerAdapter
            = new CallPeerAdapter(callPeer, this);
        callPeer.addCallPeerListener(callPeerAdapter);
        callPeer.addCallPeerSecurityListener(callPeerAdapter);
        callPeer.addPropertyChangeListener(callPeerAdapter);

        setPeerState(   null,
                        callPeer.getState(),
                        callPeer.getState().getLocalizedStateString());
        setPeerName(callPeer.getDisplayName());

        CallPeerState currentState = callPeer.getState();
        if( (currentState == CallPeerState.CONNECTED
             || CallPeerState.isOnHold(currentState))
                 && !isCallTimerStarted())
        {
            callStartDate = new Date(callPeer.getCallDurationStartTime());
            startCallTimer();
        }
    }

    /**
     * Removes given <tt>callPeer</tt> from UI.
     *
     * @param callPeer the {@link CallPeer} to be removed from UI.
     */
    private void removeCallPeerUI(CallPeer callPeer)
    {
        callPeer.removeCallPeerListener(callPeerAdapter);
        callPeer.removeCallPeerSecurityListener(callPeerAdapter);
        callPeer.removePropertyChangeListener(callPeerAdapter);
    }

    private void updateViewFromModel(EventObject ev)
    {
    }

    public void updateHoldButtonState() 
    {
        updateHoldStatus();
    }

    public void dispose() {}

    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent securityStartedEvent) {}

    /**
     * Initializes current security status displays.
     */
    private void initSecurityStatus()
    {
        boolean isSecure=false;
        boolean isVerified=false;
        ZrtpControl zrtpCtrl = null;

        Iterator<? extends CallPeer> callPeers = call.getCallPeers();
        if(callPeers.hasNext())
        {
            CallPeer cpCandidate = callPeers.next();
            if(cpCandidate instanceof MediaAwareCallPeer<?, ?, ?>)
            {
                MediaAwareCallPeer<?, ?, ?> mediaAwarePeer
                        = (MediaAwareCallPeer<?, ?, ?>) cpCandidate;
                SrtpControl srtpCtrl = mediaAwarePeer.getMediaHandler()
                        .getEncryptionMethod(MediaType.AUDIO);
                isSecure = srtpCtrl != null
                        && srtpCtrl.getSecureCommunicationStatus();

                if(srtpCtrl instanceof ZrtpControl)
                {
                    zrtpCtrl = (ZrtpControl)srtpCtrl;
                    isVerified = zrtpCtrl.isSecurityVerified();
                }
                else
                {
                    isVerified = true;
                }
            }
        }

        // Protocol name label
        ViewUtil.setTextViewValue(
                findViewById(R.id.videoCallLayout),
                R.id.security_protocol,
                zrtpCtrl != null ? "zrtp" : "");

        doUpdatePadlockStatus(isSecure, isVerified);
    }

    /**
     * Updates padlock status text, icon and it's background color.
     *
     * @param isSecure <tt>true</tt> if the call is secured.
     * @param isVerified <tt>true</tt> if zrtp SAS string is verified.
     */
    private void doUpdatePadlockStatus(boolean isSecure, boolean isVerified)
    {
        if(isSecure)
        {
            if(isVerified)
            {
                // Security on
                setPadlockColor(R.color.green_padlock);
                setPadlockSecure(true);
            }
            else
            {
                // Security pending
                setPadlockColor(R.color.orange_padlock);
                setPadlockSecure(true);
            }
        }
        else
        {
            // Security off
            setPadlockColor(R.color.red_padlock);
            setPadlockSecure(false);
        }
    }

    /**
     * Sets the security padlock background color.
     *
     * @param colorId the color resource id that will be used.
     */
    private void setPadlockColor(int colorId)
    {
        View padlockGroup = findViewById(R.id.security_group);
        int color = getResources().getColor(colorId);
        padlockGroup.setBackgroundColor(color);
    }

    /**
     * Updates padlock icon based on security status.
     *
     * @param isSecure <tt>true</tt> if the call is secure.
     */
    private void setPadlockSecure(boolean isSecure)
    {
        ViewUtil.setImageViewIcon(
                findViewById(R.id.videoCallLayout),
                R.id.security_padlock,
                isSecure ? R.drawable.secure_on : R.drawable.secure_off);
    }

    /**
     * {@inheritDoc}
     */
    public void securityPending()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                doUpdatePadlockStatus(false, false);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {

    }

    /**
     * {@inheritDoc}
     */
    public void setSecurityPanelVisible(boolean visible) {}

    /**
     * {@inheritDoc}
     */
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                doUpdatePadlockStatus(false, false);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void securityOn(final CallPeerSecurityOnEvent evt)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                SrtpControl srtpCtrl = evt.getSecurityController();
                ZrtpControl zrtpControl = null;
                if(srtpCtrl instanceof ZrtpControl)
                {
                    zrtpControl = (ZrtpControl) srtpCtrl;
                }

                boolean isVerified
                        = zrtpControl != null
                                && zrtpControl.isSecurityVerified();

                doUpdatePadlockStatus(true, isVerified);

                // Protocol name label
                ViewUtil.setTextViewValue(
                        findViewById(R.id.videoCallLayout),
                        R.id.security_protocol,
                        zrtpControl != null ? "zrtp" : "");

                if(!isVerified)
                {
                    String toastMsg
                        = getString(R.string.service_gui_security_VERIFY_TOAST);
                    sasToastController.showToast(false, toastMsg);
                }
            }
        });
    }

    /**
     * Creates new video call intent for given <tt>callIdentifier</tt>.
     *
     * @param parent the parent <tt>Context</tt> that will be used to start new
     * <tt>Activity</tt>.
     * @param callIdentifier the call ID managed by {@link CallManager}.
     *
     * @return new video call <tt>Intent</tt> parametrized with given
     * <tt>callIdentifier</tt>.
     */
    static public Intent createVideoCallIntent(Context parent,
                                              String callIdentifier)
    {
        Intent videoCallIntent
                = new Intent( parent,
                              VideoCallActivity.class);

        videoCallIntent.putExtra(
                CallManager.CALL_IDENTIFIER,
                callIdentifier);

        wasVideoEnabled = false;

        return videoCallIntent;
    }
}