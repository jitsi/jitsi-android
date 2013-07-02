/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.content.*;
import android.os.*;
import android.preference.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.settings.util.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.osgi.*;

import javax.media.*;
import java.awt.*;
import java.util.*;

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

    // Call section
    static private final String P_KEY_NORMALIZE_PNUMBER =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_normalize_pnumber);

    static private final String P_KEY_ACCEPT_ALPHA_PNUMBERS =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_accept_alpha_pnumbers);
    // Audio settings
    static private final String P_KEY_AUDIO_ECHO_CANCEL =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_audio_echo_cancel);
    static private final String P_KEY_AUDIO_DENOISE =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_audio_denoise);
    // Video settings
    static private final String P_KEY_VIDEO_CAMERA =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_video_camera);
    // Video resolutions
    static private final String P_KEY_VIDEO_RES =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_video_resolution);
    // Video advanced settings
    static private final String P_KEY_VIDEO_LIMIT_FPS =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_video_limit_fps);
    static private final String P_KEY_VIDEO_TARGET_FPS =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_video_target_fps);
    static private final String P_KEY_VIDEO_MAX_BANDWIDTH =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_video_max_bandwidth);
    static private final String P_KEY_VIDEO_BITRATE =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_video_bitrate);

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

            // Call section
            PreferenceUtil.setCheckboxVal(
                    this,
                    P_KEY_NORMALIZE_PNUMBER,
                    ConfigurationUtils.isNormalizePhoneNumber());
            PreferenceUtil.setCheckboxVal(
                    this,
                    P_KEY_ACCEPT_ALPHA_PNUMBERS,
                    ConfigurationUtils.acceptPhoneNumberWithAlphaChars());

            this.deviceConfig
                    = NeomediaActivator.getMediaServiceImpl()
                            .getDeviceConfiguration();

            // Audio section
            initAudioPreferences();

            // Video section
            initVideoPreferences();
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
         *        {@link #resToStr(java.awt.Dimension)}.
         * @return resolution <tt>Dimension</tt> for given string representation
         *         created with method {@link #resToStr(java.awt.Dimension)}
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
                                       &&deviceConfig.isEchoCancel() );

            // Denoise
            CheckBoxPreference denoisePref
                    = (CheckBoxPreference) findPreference(P_KEY_AUDIO_DENOISE);
            boolean hasDenoiseFeature
                    = (AudioSystem.FEATURE_DENOISE
                            & audioSystemFeatures) != 0;
            denoisePref.setEnabled( hasDenoiseFeature );
            denoisePref.setChecked( hasDenoiseFeature
                                    && deviceConfig.isDenoise() );
        }

        /**
         * {@inheritDoc}
         */
        public void onSharedPreferenceChanged( SharedPreferences shPreferences,
                                               String            key )
        {
            if(key.equals(P_KEY_NORMALIZE_PNUMBER))
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
                deviceConfig.setEchoCancel(
                        shPreferences.getBoolean(
                                P_KEY_AUDIO_ECHO_CANCEL, true));
            }
            else if(key.equals(P_KEY_AUDIO_DENOISE))
            {
                // Noise reduction
                deviceConfig.setDenoise(
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
