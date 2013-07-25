/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.content.*;
import android.os.Bundle;
import android.preference.*;

import java.util.*;
import javax.media.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.settings.util.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.osgi.*;

import org.osgi.framework.*;

/**
 * <tt>Activity</tt> implements Jitsi settings.
 *
 * @author Pawel Domas
 */
public class SettingsActivity
    extends OSGiActivity
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(SettingsActivity.class);

    // Message section
    static private final String P_KEY_LOG_CHAT_HISTORY
        = JitsiApplication.getResString(R.string.pref_key_history_logging);
    static private final String P_KEY_SHOW_HISTORY
        = JitsiApplication.getResString(R.string.pref_key_show_history);
    static private final String P_KEY_HISTORY_SIZE
        = JitsiApplication.getResString(R.string.pref_key_chat_history_size);
    static private final String P_KEY_TYPING_NOTIFICATIONS
        = JitsiApplication.getResString(R.string.pref_key_typing_notifications);
    static private final String P_KEY_CHAT_ALERTS
        = JitsiApplication.getResString(R.string.pref_key_chat_alerts);

    // Notifications
    static private final String P_KEY_POPUP_HANDLER
        = JitsiApplication.getResString(R.string.pref_key_popup_handler);

    // Call section
    static private final String P_KEY_NORMALIZE_PNUMBER
        = JitsiApplication.getResString(R.string.pref_key_normalize_pnumber);
    static private final String P_KEY_ACCEPT_ALPHA_PNUMBERS
        = JitsiApplication.getResString(
            R.string.pref_key_accept_alpha_pnumbers);

    // Audio settings
    static private final String P_KEY_AUDIO_ECHO_CANCEL
        = JitsiApplication.getResString(R.string.pref_key_audio_echo_cancel);
    static private final String P_KEY_AUDIO_DENOISE
        = JitsiApplication.getResString(R.string.pref_key_audio_denoise);

    // Video settings
    static private final String P_KEY_VIDEO_CAMERA
        = JitsiApplication.getResString(R.string.pref_key_video_camera);
    // Video resolutions
    static private final String P_KEY_VIDEO_RES
        = JitsiApplication.getResString(R.string.pref_key_video_resolution);
    // Video advanced settings
    static private final String P_KEY_VIDEO_LIMIT_FPS
        = JitsiApplication.getResString(R.string.pref_key_video_limit_fps);
    static private final String P_KEY_VIDEO_TARGET_FPS
        = JitsiApplication.getResString(R.string.pref_key_video_target_fps);
    static private final String P_KEY_VIDEO_MAX_BANDWIDTH
        = JitsiApplication.getResString(R.string.pref_key_video_max_bandwidth);
    static private final String P_KEY_VIDEO_BITRATE
        = JitsiApplication.getResString(R.string.pref_key_video_bitrate);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null)
        {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * The preferences fragment implements Jitsi settings.
     */
    public static class SettingsFragment
        extends OSGiPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        /**
         * The device configuration
         */
        private DeviceConfiguration deviceConfig;

        private AudioSystem audioSystem;

        /**
         * Summary mapper used to display preferences values as summaries.
         */
        private SummaryMapper summaryMapper = new SummaryMapper();

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStart()
        {
            super.onStart();
            SharedPreferences shPrefs = getPreferenceManager()
                    .getSharedPreferences();

            shPrefs.registerOnSharedPreferenceChangeListener(this);
            shPrefs.registerOnSharedPreferenceChangeListener(summaryMapper);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStop()
        {
            SharedPreferences shPrefs = getPreferenceManager()
                    .getSharedPreferences();

            shPrefs.unregisterOnSharedPreferenceChangeListener(this);
            shPrefs.unregisterOnSharedPreferenceChangeListener(summaryMapper);

            super.onStop();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onOSGiConnected()
        {
            super.onOSGiConnected();

            /*ListPreference localePreference
                    = (ListPreference) findPreference(
                            getString(R.string.pref_key_locale));

            ResourceManagementService resources
                    = ServiceUtils.getService( osgiContext,
                                               ResourceManagementService.class);

            ArrayList<CharSequence> localeStrs = new ArrayList<CharSequence>();
            Iterator<Locale> iter = resources.getAvailableLocales();
            while (iter.hasNext())
            {
                Locale locale = iter.next();
                localeStrs.add(locale.getDisplayLanguage(locale));
            }
            CharSequence[] locales = localeStrs.toArray(
                    new CharSequence[localeStrs.size()]);
            localePreference.setEntries(locales);
            localePreference.setEntryValues(locales);

            Locale currLocale =
                    ConfigurationUtils.getCurrentLanguage();
            localePreference.setValue(currLocale.getDisplayLanguage());*/

            // Messages section
            initMessagesPreferences();

            // Notifications section
            initNotificationPreferences();

            // Call section
            initCallPreferences();

            // Audio section
            initAudioPreferences();

            // Video section
            initVideoPreferences();
        }

        /**
         * Initializes messages section
         */
        private void initMessagesPreferences()
        {
            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_LOG_CHAT_HISTORY,
                    ConfigurationUtils.isHistoryLoggingEnabled());

            PreferenceUtil.setCheckboxVal(
                    this,P_KEY_SHOW_HISTORY,
                    ConfigurationUtils.isHistoryShown());

            EditTextPreference historySizePref
                    = (EditTextPreference) findPreference(P_KEY_HISTORY_SIZE);
            historySizePref.setText(""+ConfigurationUtils.getChatHistorySize());
            updateHistorySizeSummary();

            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_TYPING_NOTIFICATIONS,
                    ConfigurationUtils.isSendTypingNotifications());

            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_CHAT_ALERTS,
                    ConfigurationUtils.isAlerterEnabled());
        }

        /**
         * Updates displayed history size summary.
         */
        private void updateHistorySizeSummary()
        {
            EditTextPreference historySizePref
                    = (EditTextPreference) findPreference(P_KEY_HISTORY_SIZE);
            historySizePref.setSummary(
                    getString(
                            R.string.service_gui_settings_CHAT_HISTORY_SUMMARY,
                            ConfigurationUtils.getChatHistorySize()));
        }

        /**
         * Initializes notifications section
         */
        private void initNotificationPreferences()
        {
            BundleContext bc = AndroidGUIActivator.bundleContext;

            ServiceReference[] handlerRefs = ServiceUtils
                    .getServiceReferences(bc, PopupMessageHandler.class);
            if (handlerRefs == null)
            {
                logger.warn("No popup handlers found");
                handlerRefs = new ServiceReference[0];
            }

            String[] names = new String[handlerRefs.length+1]; // +1 Auto
            String[] values = new String[handlerRefs.length+1];
            names[0] = "Auto";
            values[0] = "Auto";
            int selectedIdx = 0;// Auto by default

            String configuredHandler
                    = (String) AndroidGUIActivator
                    .getConfigurationService()
                    .getProperty("systray.POPUP_HANDLER");
            int idx=1;
            for (ServiceReference ref : handlerRefs)
            {
                PopupMessageHandler handler =
                        (PopupMessageHandler) bc.getService(ref);

                names[idx] = handler.toString();
                values[idx] = handler.getClass().getName();

                if (configuredHandler != null &&
                        configuredHandler.equals(handler.getClass().getName()))
                {
                    selectedIdx = idx;
                }
            }

            // Configures ListPreference
            ListPreference handlerList
                    = (ListPreference) findPreference(P_KEY_POPUP_HANDLER);
            handlerList.setEntries(names);
            handlerList.setEntryValues(values);
            handlerList.setValueIndex(selectedIdx);
            // Summaries mapping
            summaryMapper.includePreference(handlerList, "Auto");
        }

        /**
         * Initializes call section
         */
        private void initCallPreferences()
        {
            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_NORMALIZE_PNUMBER,
                    ConfigurationUtils.isNormalizePhoneNumber());

            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_ACCEPT_ALPHA_PNUMBERS,
                    ConfigurationUtils.acceptPhoneNumberWithAlphaChars());

            this.deviceConfig
                    = NeomediaActivator.getMediaServiceImpl()
                    .getDeviceConfiguration();

            this.audioSystem = deviceConfig.getAudioSystem();
        }

        /**
         * Initializes video preferences part.
         */
        private void initVideoPreferences()
        {
            List<CaptureDeviceInfo> videoDevices = getCameras();
            String[] names = new String[videoDevices.size()];
            String[] values = new String[videoDevices.size()];
            for(int i=0; i<videoDevices.size(); i++)
            {
                CaptureDeviceInfo device = videoDevices.get(i);
                String devName = device.getName();
                String displayName
                    = devName
                        .contains(MediaRecorderSystem.CAMERA_FACING_FRONT)
                        ? getString(R.string.service_gui_settings_FRONT_CAMERA)
                        : getString(R.string.service_gui_settings_BACK_CAMERA);
                values[i] = devName;
                names[i] = displayName;
            }

            ListPreference cameraList
                    = (ListPreference) findPreference(P_KEY_VIDEO_CAMERA);
            cameraList.setEntries(names);
            cameraList.setEntryValues(values);

            // Set the first one as default camera
            String currentCamera = cameraList.getValue();
            if(currentCamera == null || currentCamera.isEmpty())
            {
                if(values.length > 0)
                    cameraList.setValueIndex(0);
            }

            // Resolutions
            String[] resolutionValues
                = new String[
                    DeviceConfiguration.SUPPORTED_RESOLUTIONS.length+1];
            String autoResStr
                    = getString(R.string.service_gui_settings_AUTO_RESOLUTION);
            resolutionValues[0] = autoResStr;
            for(int i=0; i<resolutionValues.length-1; i++)
            {
                resolutionValues[i+1]
                        = resToStr(
                                DeviceConfiguration.SUPPORTED_RESOLUTIONS[i]);
            }

            ListPreference resList
                    = (ListPreference) findPreference(P_KEY_VIDEO_RES);
            resList.setEntries(resolutionValues);
            resList.setEntryValues(resolutionValues);

            // Init current resolution
            Dimension currentResDim = deviceConfig.getVideoSize();
            Dimension autoResDim = new Dimension(
                    DeviceConfiguration.DEFAULT_VIDEO_WIDTH,
                    DeviceConfiguration.DEFAULT_VIDEO_HEIGHT);
            if(currentResDim.equals(autoResDim))
            {
                resList.setValue(autoResStr);
            }
            else
            {
                resList.setValue(resToStr(deviceConfig.getVideoSize()));
            }

            //Frame rate
            String defaultFpsStr = "20";
            CheckBoxPreference limitFpsPref
                    = (CheckBoxPreference) findPreference(P_KEY_VIDEO_LIMIT_FPS);
            int targetFps = deviceConfig.getFrameRate();
            limitFpsPref.setChecked(targetFps != -1);

            EditTextPreference targetFpsPref
                    = (EditTextPreference) findPreference(
                            P_KEY_VIDEO_TARGET_FPS);
            targetFpsPref.setText(
                    targetFps != DeviceConfiguration.DEFAULT_VIDEO_FRAMERATE
                            ? Integer.toString(targetFps)
                            : defaultFpsStr  );

            // Max bandwidth
            int videoMaxBandwith = deviceConfig.getVideoRTPPacingThreshold();
            // Accord the current value with the maximum allowed value. Fixes
            // existing configurations that have been set to a number larger
            // than the advised maximum value.
            videoMaxBandwith
                    = ((videoMaxBandwith > 999) ? 999 : videoMaxBandwith);

            EditTextPreference maxBandwidthPref
                    = (EditTextPreference) findPreference(
                            P_KEY_VIDEO_MAX_BANDWIDTH);
            maxBandwidthPref.setText(Integer.toString(videoMaxBandwith));

            // Video bitrate
            int bitrate = deviceConfig.getVideoBitrate();
            EditTextPreference bitratePref
                    = (EditTextPreference) findPreference(P_KEY_VIDEO_BITRATE);
            bitratePref.setText(Integer.toString(bitrate));

            // Summaries mapping
            summaryMapper.includePreference(
                    cameraList,
                    getString(R.string.service_gui_settings_NO_CAMERA));
            summaryMapper.includePreference(
                    resList,
                    getString(R.string.service_gui_settings_AUTO_RESOLUTION));
            summaryMapper.includePreference(
                    targetFpsPref,
                    defaultFpsStr);
            summaryMapper.includePreference(
                    maxBandwidthPref,
                    DeviceConfiguration.DEFAULT_VIDEO_RTP_PACING_THRESHOLD+"");
            summaryMapper.includePreference(
                    bitratePref,
                    DeviceConfiguration.DEFAULT_VIDEO_BITRATE+"");
        }

        /**
         * Converts resolution to string.
         * @param d resolution as <tt>Dimension</tt>
         * @return resolution string.
         */
        private static String resToStr(Dimension d)
        {
            return ((int) d.getWidth()) + "x" + ((int) d.getHeight());
        }

        /**
         * Selects resolution from supported resolutions list for given string.
         * @param resStr resolution string created with method
         *        {@link #resToStr(Dimension)}.
         * @return resolution <tt>Dimension</tt> for given string representation
         *         created with method {@link #resToStr(Dimension)}
         */
        private static Dimension resoultionForStr(String resStr)
        {
            Dimension[] resolutions = DeviceConfiguration.SUPPORTED_RESOLUTIONS;
            for (Dimension resolution : resolutions)
            {
                if (resToStr(resolution).equals(resStr))
                    return resolution;
            }
            // "Auto" string won't match none of "120x320" strings
            // so will return default for auto
            return new Dimension(
                    DeviceConfiguration.DEFAULT_VIDEO_WIDTH,
                    DeviceConfiguration.DEFAULT_VIDEO_HEIGHT);
        }

        /**
         * Returns all camera devices info.
         * @return list of available cameras.
         */
        private List<CaptureDeviceInfo> getCameras()
        {
            return deviceConfig.getAvailableVideoCaptureDevices(
                    MediaUseCase.CALL);
        }

        /**
         * Initializes audio settings.
         */
        private void initAudioPreferences()
        {
            AudioSystem audioSystem = deviceConfig.getAudioSystem();

            int audioSystemFeatures = audioSystem.getFeatures();

            // Echo cancellation
            CheckBoxPreference echoCancelPRef
                = (CheckBoxPreference) findPreference(P_KEY_AUDIO_ECHO_CANCEL);
            boolean hasEchoFeature
                    = (AudioSystem.FEATURE_ECHO_CANCELLATION
                            & audioSystemFeatures) != 0;
            echoCancelPRef.setEnabled( hasEchoFeature );
            echoCancelPRef.setChecked( hasEchoFeature
                                       &&audioSystem.isEchoCancel() );

            // Denoise
            CheckBoxPreference denoisePref
                    = (CheckBoxPreference) findPreference(P_KEY_AUDIO_DENOISE);
            boolean hasDenoiseFeature
                    = (AudioSystem.FEATURE_DENOISE
                            & audioSystemFeatures) != 0;
            denoisePref.setEnabled( hasDenoiseFeature );
            denoisePref.setChecked( hasDenoiseFeature
                                    && audioSystem.isDenoise() );
        }

        /**
         * Retrieves currently registered <tt>PopupMessageHandler</tt> for given
         * <tt>clazz</tt> name.
         * @param clazz the class name of <tt>PopupMessageHandler</tt>
         *              implementation.
         * @return implementation of <tt>PopupMessageHandler</tt> for given
         *         class name registered in OSGI context.
         */
        private PopupMessageHandler getHandlerForClassName(String clazz)
        {
            BundleContext bc = AndroidGUIActivator.bundleContext;
            ServiceReference[] handlerRefs = ServiceUtils
                    .getServiceReferences(bc, PopupMessageHandler.class);

            if (handlerRefs == null)
                return null;

            for(ServiceReference sRef : handlerRefs)
            {
                PopupMessageHandler handler
                        = (PopupMessageHandler) bc.getService(sRef);
                if(handler.getClass().getName().equals(clazz))
                    return handler;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public void onSharedPreferenceChanged( SharedPreferences shPreferences,
                                               String            key )
        {
            if(key.equals(P_KEY_LOG_CHAT_HISTORY))
            {
                ConfigurationUtils.setHistoryLoggingEnabled(
                        shPreferences.getBoolean(
                                P_KEY_LOG_CHAT_HISTORY,
                                ConfigurationUtils.isHistoryLoggingEnabled()));
            }
            else if(key.equals(P_KEY_SHOW_HISTORY))
            {
                ConfigurationUtils.setHistoryShown(
                        shPreferences.getBoolean(
                                P_KEY_SHOW_HISTORY,
                                ConfigurationUtils.isHistoryShown()));
            }
            else if(key.equals(P_KEY_HISTORY_SIZE))
            {
                String intStr = shPreferences.getString(
                        P_KEY_HISTORY_SIZE,
                        ""+ConfigurationUtils.getChatHistorySize());
                ConfigurationUtils.setChatHistorySize(Integer.parseInt(intStr));
                updateHistorySizeSummary();
            }
            else if(key.equals(P_KEY_TYPING_NOTIFICATIONS))
            {
                ConfigurationUtils.setSendTypingNotifications(
                        shPreferences.getBoolean(
                                P_KEY_TYPING_NOTIFICATIONS,
                                ConfigurationUtils.isSendTypingNotifications())
                );
            }
            else if(key.equals(P_KEY_POPUP_HANDLER))
            {
                String handler = shPreferences.getString( P_KEY_POPUP_HANDLER,
                                                          "Auto" );
                SystrayService systray
                        = AndroidGUIActivator.getSystrayService();
                if(handler.equals("Auto"))
                {
                    // "Auto" selected. Delete the user's preference and
                    // select the best available handler.
                    ConfigurationUtils.setPopupHandlerConfig(null);
                    systray.selectBestPopupMessageHandler();
                }
                else
                {
                    ConfigurationUtils.setPopupHandlerConfig(handler);
                    PopupMessageHandler handlerInstance
                            = getHandlerForClassName(handler);
                    if(handlerInstance == null)
                    {
                        logger.warn("No handler found for name: "+handler);
                    }
                    else
                    {
                        systray.setActivePopupMessageHandler(handlerInstance);
                    }
                }
            }
            else if(key.equals(P_KEY_NORMALIZE_PNUMBER))
            {
                // Normalize phone number
                ConfigurationUtils.setNormalizePhoneNumber(
                        shPreferences.getBoolean(
                                P_KEY_NORMALIZE_PNUMBER, true));
            }
            else if(key.equals(P_KEY_ACCEPT_ALPHA_PNUMBERS))
            {
                // Accept alphanumeric characters in phone number
                ConfigurationUtils.setAcceptPhoneNumberWithAlphaChars(
                        shPreferences.getBoolean(
                                P_KEY_ACCEPT_ALPHA_PNUMBERS, true));
            }
            else if(key.equals(P_KEY_AUDIO_ECHO_CANCEL))
            {
                // Echo cancellation
                audioSystem.setEchoCancel(
                        shPreferences.getBoolean(
                                P_KEY_AUDIO_ECHO_CANCEL, true));
            }
            else if(key.equals(P_KEY_AUDIO_DENOISE))
            {
                // Noise reduction
                audioSystem.setDenoise(
                        shPreferences.getBoolean(
                                P_KEY_AUDIO_DENOISE, true));
            }
            else if(key.equals(P_KEY_VIDEO_CAMERA))
            {
                // Camera
                String cameraName
                        = shPreferences.getString(P_KEY_VIDEO_CAMERA, null);
                List<CaptureDeviceInfo> cameras = getCameras();
                CaptureDeviceInfo selectedCamera = null;
                for(CaptureDeviceInfo camera : cameras)
                {
                    if(camera.getName().equals(cameraName))
                    {
                        selectedCamera = camera;
                        break;
                    }
                }
                if(selectedCamera != null)
                {
                    deviceConfig.setVideoCaptureDevice(selectedCamera, true);
                }
                else
                {
                    logger.warn("No camera found for name: "+cameraName);
                }
            }
            else if(key.equals(P_KEY_VIDEO_RES))
            {
                // Video resolution
                String resStr = shPreferences.getString(P_KEY_VIDEO_RES, null);
                Dimension videoRes = resoultionForStr(resStr);
                deviceConfig.setVideoSize(videoRes);
            }
            else if( key.equals(P_KEY_VIDEO_LIMIT_FPS)
                     || key.equals(P_KEY_VIDEO_TARGET_FPS) )
            {
                // Frame rate
                boolean isLimitOn
                        = shPreferences.getBoolean( P_KEY_VIDEO_LIMIT_FPS,
                                                    false );
                if(isLimitOn)
                {
                    EditTextPreference fpsPref
                            = (EditTextPreference) findPreference(
                                    P_KEY_VIDEO_TARGET_FPS);
                    String fpsStr = fpsPref.getText();
                    if(!fpsStr.isEmpty())
                    {
                        int fps = Integer.parseInt(fpsStr);
                        if(fps > 30)
                        {
                            fps = 30;
                        }
                        else if(fps < 5)
                        {
                            fps = 5;
                        }
                        deviceConfig.setFrameRate(fps);
                        fpsPref.setText(Integer.toString(fps));
                    }
                }
                else
                {
                    deviceConfig.setFrameRate(
                            DeviceConfiguration.DEFAULT_VIDEO_FRAMERATE);
                }
            }
            else if(key.equals(P_KEY_VIDEO_MAX_BANDWIDTH))
            {
                // Max bandwidth
                String resStr
                        = shPreferences.getString(
                                P_KEY_VIDEO_MAX_BANDWIDTH, null);
                if(resStr != null && !resStr.isEmpty())
                {
                    int maxBw = Integer.parseInt(resStr);
                    if(maxBw > 999)
                    {
                        maxBw = 999;
                    }
                    else if(maxBw < 1)
                    {
                        maxBw = 1;
                    }
                    deviceConfig.setVideoRTPPacingThreshold(maxBw);
                }
                else
                {
                    deviceConfig.setVideoRTPPacingThreshold(
                            DeviceConfiguration
                                    .DEFAULT_VIDEO_RTP_PACING_THRESHOLD);
                }

                ((EditTextPreference)findPreference(P_KEY_VIDEO_MAX_BANDWIDTH))
                        .setText(deviceConfig.getVideoRTPPacingThreshold()+"");
            }
            else if(key.equals(P_KEY_VIDEO_BITRATE))
            {
                String bitrateStr
                        = shPreferences.getString(P_KEY_VIDEO_BITRATE, "");
                int bitrate = !bitrateStr.isEmpty()
                        ? Integer.parseInt(bitrateStr)
                        : DeviceConfiguration.DEFAULT_VIDEO_BITRATE;
                if(bitrate < 1)
                {
                    bitrate = 1;
                }
                deviceConfig.setVideoBitrate(bitrate);

                ((EditTextPreference)findPreference(P_KEY_VIDEO_BITRATE))
                        .setText(bitrate + "");
            }
        }
    }
}
