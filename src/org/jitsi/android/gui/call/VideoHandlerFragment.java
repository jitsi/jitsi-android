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
package org.jitsi.android.gui.call;

import android.annotation.*;
import android.app.*;
import android.hardware.*;
import android.opengl.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;

import org.jitsi.*;
import org.jitsi.android.gui.controller.*;
import org.jitsi.android.gui.util.*;
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
     * Container used for local preview
     */
    protected ViewGroup localPreviewContainer;

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
     * Call info group
     */
    private ViewGroup callInfoGroup;

    /**
     * Call control buttons group.
     */
    private View ctrlButtonsGroup;

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

        localPreviewContainer
            = (ViewGroup) activity.findViewById(R.id.localPreviewContainer);

        callInfoGroup
            = (ViewGroup) activity.findViewById(R.id.callInfoGroup);

        ctrlButtonsGroup
            = activity.findViewById(R.id.buttonContainer);

        // (must be done after layout or 0 sizes will be returned)
        ctrlButtonsGroup.getViewTreeObserver()
            .addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener()
                {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    @SuppressWarnings("deprecation")
                    @Override
                    public void onGlobalLayout()
                    {
                        // We know the size of all components at this point,
                        // so we can init layout dependent stuff

                        // Initial call info margin adjustment
                        updateCallInfoMargin();

                        // Remove the listener, as it has to be called only once
                        if (AndroidUtils.hasAPI(16))
                        {
                            ctrlButtonsGroup.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        }
                        else
                        {
                            ctrlButtonsGroup.getViewTreeObserver()
                                .removeGlobalOnLayoutListener(this);
                        }
                    }
                });

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
            = new PreviewSurfaceProvider(
                (OSGiActivity) activity, localPreviewContainer, true);
        CameraUtils.setPreviewSurfaceProvider(previewSurfaceHandler);

        // Makes the preview display draggable on the screen
        localPreviewContainer.setOnTouchListener(new SimpleDragController());

        this.call = ((VideoCallActivity)activity).getCall();

        AndroidDecoder.renderSurfaceProvider
            = new PreviewSurfaceProvider(
                    (OSGiActivity) activity, remoteVideoContainer, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(call == null)
        {
            logger.error("Call is null");
            return;
        }

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

        if(call == null)
        {
            logger.error("Call is null");
            return;
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
            //TODO: release object on rotation, but the data source
            // have to be paused
            //remoteSurfaceHandler.waitForObjectRelease();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // Release shared video component
        remoteVideoContainer.removeAllViews();
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
        if(call == null)
        {
            logger.error("Call instance is null(the call has ended already ?)");
            return;
        }

        CallManager.enableLocalVideo(call, enable);
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
    @SuppressWarnings("deprecation")
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

        /*if (event.getOrigin() == VideoEvent.LOCAL)
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
        }*/
        if (event.getOrigin() == VideoEvent.REMOTE)
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
                if (getActivity() == null)
                {
                    logger.warn(
                        "Remote video event when getActivity() returned null");
                    return;
                }

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
        if (remoteVideoView != null)
        {
            ((RemoteVideoLayout)remoteVideoContainer)
                .setVideoPreferredSize(preferredSize);

            // Hack only for GLSurfaceView
            // Remote video view will match parents width and height,
            // but renderer object is properly updated only when removed
            // and added back again.
            if(remoteVideoView instanceof GLSurfaceView)
            {
                remoteVideoContainer.removeAllViews();
                remoteVideoContainer.addView(remoteVideoView);
            }

            calleeAvatar.setVisibility(View.GONE);

            // When remote video is visible then the call info is positioned
            // in the bottom part of the screen
            RelativeLayout.LayoutParams params
                = (RelativeLayout.LayoutParams)
                        callInfoGroup.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_VERTICAL, 0);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            callInfoGroup.setLayoutParams(params);
        }
        else
        {
            remoteVideoContainer.removeAllViews();

            // When remote video is hidden then the call info is centered
            // below the avatar
            RelativeLayout.LayoutParams params
                = (RelativeLayout.LayoutParams)
                        callInfoGroup.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            callInfoGroup.setLayoutParams(params);

            calleeAvatar.setVisibility(View.VISIBLE);
        }

        // Update call info group margin based on control buttons group
        // visibility state
        updateCallInfoMargin();
    }

    /**
     * Returns <tt>true</tt> if local video is currently visible.
     * @return <tt>true</tt> if local video is currently visible.
     */
    public boolean isLocalVideoVisible()
    {
        return localPreviewContainer.getChildCount() > 0;
    }

    /**
     * Block the program until camera is stopped to prevent from crashing on
     * not existing preview surface.
     */
    void ensureCameraClosed()
    {
        previewSurfaceHandler.waitForObjectRelease();
        //TODO: remote display must be released too
        // (but the DataSource must be paused)
        //remoteVideoSurfaceHandler.waitForObjectRelease();
    }

    /**
     * Positions call info group buttons.
     */
    void updateCallInfoMargin()
    {
        RelativeLayout.LayoutParams params
            = (RelativeLayout.LayoutParams)
                    callInfoGroup.getLayoutParams();

        // If we have remote video
        if(remoteVideoContainer.getChildCount() > 0)
        {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager()
                .getDefaultDisplay().getMetrics(displaymetrics);

            int ctrlButtonsHeight = ctrlButtonsGroup.getHeight();

            int marginBottom = (int) (0.10*displaymetrics.heightPixels);

            if(marginBottom < ctrlButtonsHeight
                && ctrlButtonsGroup.getVisibility() == View.VISIBLE)
            {
                marginBottom = ctrlButtonsHeight + AndroidUtils.pxToDp(10);
            }

            // This can be used if we want to keep it on the same height
            if(ctrlButtonsGroup.getVisibility() == View.VISIBLE)
            {
                marginBottom -= ctrlButtonsHeight;
            }

            params.setMargins(0, 0, 0, marginBottom);
            callInfoGroup.setLayoutParams(params);
        }
        else
        {
            params.setMargins(0, 0, 0, 0);
            callInfoGroup.setLayoutParams(params);
        }
    }
}
