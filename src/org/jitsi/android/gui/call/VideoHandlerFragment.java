/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.call;

import android.app.*;
import android.hardware.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;

import org.jitsi.*;
import org.jitsi.android.gui.controller.*;
import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.codec.video.*;
import org.jitsi.impl.neomedia.device.util.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.event.*;

import java.util.*;

/**
 * Fragment takes care of handling call UI parts related to the video - both
 * local and remote.
 *
 * @author Pawel Domas
 */
public class VideoHandlerFragment
    extends OSGiFragment
{
    /**
     * The logger
     */
    protected final static Logger logger
            = Logger.getLogger(VideoHandlerFragment.class);

    /**
     * The callee avatar.
     */
    private ImageView calleeAvatar;

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
     * Instance of video listener that should be unregistered once this Activity
     * is destroyed
     */
    private VideoListener callPeerVideoListener;

    /**
     * The preview surface state handler
     */
    private PreviewSurfaceProvider previewSurfaceHandler;

    /**
     * Stores the current local video state in case this <tt>Activity</tt> is
     * hidden during call.
     */
    static boolean wasVideoEnabled = false;

    /**
     * The call for which this fragment is handling video events.
     */
    private Call call;

    /**
     * Menu object used by this fragment.
     */
    private Menu menu;

    /**
     * The thread that switches the camera.
     */
    private Thread cameraSwitchThread;

    /**
     * Surface provider used for displaying remote video in hw decoding mode.
     */
    private PreviewSurfaceProvider remoteSurfaceHandler;
    /**
     * Remote video view.
     */
    private SurfaceView remoteVideo;

    /**
     * Creates new instance of <tt>VideoHandlerFragment</tt>.
     */
    public VideoHandlerFragment()
    {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();

        remoteVideoContainer
                = (ViewGroup) activity.findViewById(R.id.remoteVideoContainer);

        previewDisplay
                = (SurfaceView) activity.findViewById(R.id.previewDisplay);

        calleeAvatar = (ImageView) activity.findViewById(R.id.calleeAvatar);

        activity.findViewById(R.id.callVideoButton).setOnClickListener(
                new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onLocalVideoButtonClicked(v);
            }
        });

        // Creates and registers surface handler for events
        this.previewSurfaceHandler
            = new PreviewSurfaceProvider(activity, previewDisplay);
        CameraUtils.setPreviewSurfaceProvider(previewSurfaceHandler);

        // Preview display will be displayed on top of remote video
        previewDisplay.setZOrderMediaOverlay(true);

        // Makes the preview display draggable on the screen
        previewDisplay.setOnTouchListener(new SimpleDragController());

        this.call = ((VideoCallActivity)activity).getCall();

        this.remoteVideo
                = (SurfaceView) activity.findViewById(R.id.remoteDisplay);
        this.remoteSurfaceHandler
                = new PreviewSurfaceProvider(activity, remoteVideo);

        AndroidDecoder.renderSurfaceProvider = remoteSurfaceHandler;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Restores local video state
        if(wasVideoEnabled)
        {
            setLocalVideoEnabled(true);
        }
        // Checks if call peer has video component
        Iterator<? extends CallPeer> peers = call.getCallPeers();
        if(peers.hasNext())
        {
            CallPeer callPeer = peers.next();
            addVideoListener(callPeer);
            initRemoteVideo(callPeer);
        }
        else
        {
            logger.error("There aren't any peers in the call");
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // Make sure to join the switch camera thread
        if(cameraSwitchThread != null)
        {
            try
            {
                cameraSwitchThread.join();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        removeVideoListener();

        if(call.getCallState() != CallState.CALL_ENDED)
        {
            wasVideoEnabled = isLocalVideoEnabled();
            logger.error("Was local enabled ? "+wasVideoEnabled);

            /**
             * Disables local video to stop the camera and release the surface.
             * Otherwise media recorder will crash on invalid preview surface.
             */
            setLocalVideoEnabled(false);
            previewSurfaceHandler.waitForObjectRelease();
            //TODO: release object on rotation ?
            //remoteSurfaceHandler.waitForObjectRelease();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // Release shared video component
        if(remoteVideoAccessor != null)
        {
            remoteVideoContainer.removeView(
                    remoteVideoAccessor.getView(getActivity()));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);

        AndroidCamera currentCamera = AndroidCamera.getSelectedCameraDevInfo();
        if(currentCamera == null)
        {
            return;
        }

        // Check for camera with other facing from current system
        int otherFacing
            = currentCamera.getCameraFacing() == AndroidCamera.FACING_BACK
                ? AndroidCamera.FACING_FRONT
                : AndroidCamera.FACING_BACK;
        if(AndroidCamera.getCameraFromCurrentDeviceSystem(otherFacing) == null)
        {
            return;
        }

        inflater.inflate(R.menu.camera_menu, menu);
        this.menu = menu;

        updateMenu();
    }

    /**
     * Updates menu status.
     */
    private void updateMenu()
    {
        if(menu == null)
            return;

        AndroidCamera currentCamera = AndroidCamera.getSelectedCameraDevInfo();
        boolean isFrontCamera
            = currentCamera.getCameraFacing() == AndroidCamera.FACING_FRONT;

        String displayName
            = isFrontCamera
                    ? getString(R.string.service_gui_settings_USE_BACK_CAMERA)
                    : getString(R.string.service_gui_settings_USE_FRONT_CAMERA);

        menu.findItem(R.id.switch_camera).setTitle(displayName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.switch_camera)
        {
            // Ignore action if camera switching is in progress
            if(cameraSwitchThread != null)
                return true;

            String back
                = getString(R.string.service_gui_settings_USE_BACK_CAMERA);
            String front
                = getString(R.string.service_gui_settings_USE_FRONT_CAMERA);
            String newTitle;

            final AndroidCamera newDevice;

            if(item.getTitle().equals(back))
            {
                // Switch to back camera
                newDevice
                    = AndroidCamera.getCameraFromCurrentDeviceSystem(
                            Camera.CameraInfo.CAMERA_FACING_BACK);
                // Set opposite title
                newTitle = front;
            }
            else
            {
                // Switch to front camera
                newDevice
                    = AndroidCamera.getCameraFromCurrentDeviceSystem(
                            Camera.CameraInfo.CAMERA_FACING_FRONT);
                // Set opposite title
                newTitle = back;
            }
            item.setTitle(newTitle);

            // Switch the camera in separate thread
            this.cameraSwitchThread = new Thread()
            {
                @Override
                public void run()
                {
                    AndroidCamera.setSelectedCamera(newDevice.getLocator());
                    // Keep track of created threads
                    cameraSwitchThread = null;
                }
            };
            cameraSwitchThread.start();

            return true;
        }
        return super.onOptionsItemSelected(item);
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
            callVideoButton.setBackgroundColor(
                    android.graphics.Color.TRANSPARENT);
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
        CallManager.enableLocalVideo(call, enable);
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
                } else
                {
                    previewDisplay.setVisibility(View.GONE);
                }
            }
        });
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
                        ? remoteVideoAccessor.getView(getActivity())
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
        getActivity().getWindowManager()
                .getDefaultDisplay().getMetrics(displaymetrics);
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
     * Returns <tt>true</tt> if local video is currently visible.
     * @return <tt>true</tt> if local video is currently visible.
     */
    public boolean isLocalVideoVisible()
    {
        if(previewDisplay == null)
            return false;

        return previewDisplay.getVisibility() == View.VISIBLE;
    }

    /**
     * Block the program until camera is stopped to prevent from crashing on
     * not existing preview surface.
     */
    public void ensureCameraClosed()
    {
        previewSurfaceHandler.waitForObjectRelease();
        //TODO: remote display must be released too
        //remoteSurfaceHandler.waitForObjectRelease();
    }
}
