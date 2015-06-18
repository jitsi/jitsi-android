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
#include "org_jitsi_impl_neomedia_device_OpenSLESSystem.h"

#include <SLES/OpenSLES.h>
#include <stdlib.h>

static void
OpenSLESSystem_queryAudioInputCapabilities
    (JNIEnv *jniEnv,
        SLAudioIODeviceCapabilitiesItf engine_AudioIODeviceCapabilitiesItf,
        jlong deviceID, SLAudioInputDescriptor descriptor,
        jdoubleArray sampleRates, jintArray sampleSizesInBits, jintArray channels,
        jintArray audioInputCapabilities, jint *audioInputCapabilitiesIndex);
static void
OpenSLESSystem_queryAudioInputCapabilitiesByChannel
    (JNIEnv *jniEnv,
        SLAudioIODeviceCapabilitiesItf engine_AudioIODeviceCapabilitiesItf,
        jlong deviceID, SLAudioInputDescriptor descriptor,
        jdoubleArray sampleRates, jintArray sampleSizesInBits, jint channel, jint channelIndex,
        jintArray audioInputCapabilities, jint *audioInputCapabilitiesIndex);
static void
OpenSLESSystem_queryAudioInputCapabilitiesBySampleRate
    (JNIEnv *jniEnv,
        SLAudioIODeviceCapabilitiesItf engine_AudioIODeviceCapabilitiesItf,
        jlong deviceID, SLAudioInputDescriptor descriptor,
        jdouble sampleRate, jint sampleRateIndex, jintArray sampleSizesInBits, jint channel, jint channelIndex,
        jintArray audioInputCapabilities, jint *audioInputCapabilitiesIndex);

JNIEXPORT jintArray JNICALL
Java_org_jitsi_impl_neomedia_device_OpenSLESSystem_queryAudioInputCapabilities
    (JNIEnv *jniEnv, jclass clazz,
        jlong deviceID,
        jdoubleArray sampleRates, jintArray sampleSizesInBits, jintArray channels)
{
    SLObjectItf engine_ObjectItf;
    SLInterfaceID interfaceIds[]
        = { SL_IID_AUDIOIODEVICECAPABILITIES };
    SLboolean interfaceRequired[] = { SL_BOOLEAN_TRUE };
    SLresult SLresult_
        = slCreateEngine(
                &engine_ObjectItf,
                0,
                NULL,
                sizeof(interfaceIds) / sizeof(SLInterfaceID),
                interfaceIds,
                interfaceRequired);
    jintArray audioInputCapabilities = NULL;

    if (SL_RESULT_SUCCESS == SLresult_)
    {
        SLresult_
            = (*engine_ObjectItf)->Realize(engine_ObjectItf, SL_BOOLEAN_FALSE);
        if (SL_RESULT_SUCCESS == SLresult_)
        {
            SLAudioIODeviceCapabilitiesItf engine_AudioIODeviceCapabilitiesItf;

            SLresult_
                = (*engine_ObjectItf)->GetInterface(
                        engine_ObjectItf,
                        SL_IID_AUDIOIODEVICECAPABILITIES,
                        &engine_AudioIODeviceCapabilitiesItf);
            if (SL_RESULT_SUCCESS == SLresult_)
            {
                SLAudioInputDescriptor descriptor;

                SLresult_
                    = (*engine_AudioIODeviceCapabilitiesItf)
                        ->QueryAudioInputCapabilities(
                            engine_AudioIODeviceCapabilitiesItf,
                            (SLuint32) deviceID,
                            &descriptor);
                if (SL_RESULT_SUCCESS == SLresult_)
                {
                    jsize sampleRateCount
                        = (*jniEnv)->GetArrayLength(jniEnv, sampleRates);

                    if (!((*jniEnv)->ExceptionCheck(jniEnv)))
                    {
                        jsize sampleSizeInBitsCount
                            = (*jniEnv)->GetArrayLength(jniEnv, sampleSizesInBits);

                        if (!((*jniEnv)->ExceptionCheck(jniEnv)))
                        {
                            jsize channelCount
                                = (*jniEnv)->GetArrayLength(jniEnv, channels);

                            if (!((*jniEnv)->ExceptionCheck(jniEnv)))
                            {
                                audioInputCapabilities
                                    = (*jniEnv)->NewIntArray(
                                            jniEnv,
                                            (sampleRateCount
                                                        * sampleSizeInBitsCount
                                                        * channelCount
                                                    + 1)
                                                * 3);
                                if (audioInputCapabilities)
                                {
                                    jsize audioInputCapabilitiesIndex = 0;

                                    OpenSLESSystem_queryAudioInputCapabilities(
                                            jniEnv,
                                            engine_AudioIODeviceCapabilitiesItf,
                                            deviceID, descriptor,
                                            sampleRates, sampleSizesInBits, channels,
                                            audioInputCapabilities, &audioInputCapabilitiesIndex);
                                    if (!((*jniEnv)->ExceptionCheck(jniEnv)))
                                    {
                                        jint minus1s[] = { -1, -1, -1 };

                                        (*jniEnv)->SetIntArrayRegion(
                                                jniEnv,
                                                audioInputCapabilities,
                                                audioInputCapabilitiesIndex,
                                                sizeof(minus1s) / sizeof(jint),
                                                minus1s);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        (*engine_ObjectItf)->Destroy(engine_ObjectItf);
    }
    return audioInputCapabilities;
}

static void
OpenSLESSystem_queryAudioInputCapabilities
    (JNIEnv *jniEnv,
        SLAudioIODeviceCapabilitiesItf engine_AudioIODeviceCapabilitiesItf,
        jlong deviceID, SLAudioInputDescriptor descriptor,
        jdoubleArray sampleRates, jintArray sampleSizesInBits, jintArray channels,
        jintArray audioInputCapabilities, jint *audioInputCapabilitiesIndex)
{
    jsize channelIndex;
    jsize channelCount = (*jniEnv)->GetArrayLength(jniEnv, channels);

    if ((*jniEnv)->ExceptionCheck(jniEnv))
        channelCount = 0;
    for (channelIndex = 0; channelIndex < channelCount; channelIndex++)
    {
        jint channel;

        (*jniEnv)->GetIntArrayRegion(
                jniEnv,
                channels, channelIndex, 1,
                &channel);
        if ((*jniEnv)->ExceptionCheck(jniEnv))
            break;

        if (channel <= descriptor.maxChannels)
        {
            OpenSLESSystem_queryAudioInputCapabilitiesByChannel(
                    jniEnv,
                    engine_AudioIODeviceCapabilitiesItf,
                    deviceID, descriptor,
                    sampleRates, sampleSizesInBits, channel, channelIndex,
                    audioInputCapabilities, audioInputCapabilitiesIndex);
            if ((*jniEnv)->ExceptionCheck(jniEnv))
                break;
        }
    }
}

static void
OpenSLESSystem_queryAudioInputCapabilitiesByChannel
    (JNIEnv *jniEnv,
        SLAudioIODeviceCapabilitiesItf engine_AudioIODeviceCapabilitiesItf,
        jlong deviceID, SLAudioInputDescriptor descriptor,
        jdoubleArray sampleRates, jintArray sampleSizesInBits, jint channel, jint channelIndex,
        jintArray audioInputCapabilities, jint *audioInputCapabilitiesIndex)
{
    jsize sampleRateIndex;
    jsize sampleRateCount = (*jniEnv)->GetArrayLength(jniEnv, sampleRates);

    if ((*jniEnv)->ExceptionCheck(jniEnv))
        sampleRateCount = 0;
    for (sampleRateIndex = 0;
            sampleRateIndex < sampleRateCount;
            sampleRateIndex++)
    {
        jdouble sampleRate;
        jboolean sampleRateIsSupported;

        (*jniEnv)->GetDoubleArrayRegion(
                jniEnv,
                sampleRates, sampleRateIndex, 1,
                &sampleRate);
        if ((*jniEnv)->ExceptionCheck(jniEnv))
            break;

        sampleRate *= 1000;

        if (SL_BOOLEAN_TRUE == descriptor.isFreqRangeContinuous)
        {
            sampleRateIsSupported
                = (descriptor.minSampleRate <= sampleRate)
                    && (sampleRate <= descriptor.maxSampleRate);
        }
        else
        {
            SLint16 supportedSampleRateCount
                = descriptor.numOfSamplingRatesSupported;

            sampleRateIsSupported = JNI_FALSE;
            if (supportedSampleRateCount)
            {
                SLint16 supportedSampleRateIndex;
                SLmilliHertz *supportedSampleRates
                    = descriptor.samplingRatesSupported;

                for (supportedSampleRateIndex = 0;
                        supportedSampleRateIndex < supportedSampleRateCount;
                        supportedSampleRateIndex++)
                {
                    if (sampleRate == *supportedSampleRates++)
                    {
                        sampleRateIsSupported = JNI_TRUE;
                        break;
                    }
                }
            }
        }

        if (sampleRateIsSupported)
        {
            OpenSLESSystem_queryAudioInputCapabilitiesBySampleRate(
                    jniEnv,
                    engine_AudioIODeviceCapabilitiesItf,
                    deviceID, descriptor,
                    sampleRate, sampleRateIndex, sampleSizesInBits, channel, channelIndex,
                    audioInputCapabilities, audioInputCapabilitiesIndex);
            if ((*jniEnv)->ExceptionCheck(jniEnv))
                break;
        }
    }
}

static void
OpenSLESSystem_queryAudioInputCapabilitiesBySampleRate
    (JNIEnv *jniEnv,
        SLAudioIODeviceCapabilitiesItf engine_AudioIODeviceCapabilitiesItf,
        jlong deviceID, SLAudioInputDescriptor descriptor,
        jdouble sampleRate, jint sampleRateIndex, jintArray sampleSizesInBits, jint channel, jint channelIndex,
        jintArray audioInputCapabilities, jint *audioInputCapabilitiesIndex)
{
    SLint32 sampleFormatCount;
    SLresult SLresult_
        = (*engine_AudioIODeviceCapabilitiesItf)->QuerySampleFormatsSupported(
                engine_AudioIODeviceCapabilitiesItf,
                deviceID,
                sampleRate,
                NULL, &sampleFormatCount);
    if ((SL_RESULT_SUCCESS == SLresult_) && sampleFormatCount)
    {
        SLint32 *sampleFormats = malloc(sizeof(SLint32) * sampleFormatCount);

        if (sampleFormats)
        {
            SLresult_
                = (*engine_AudioIODeviceCapabilitiesItf)
                    ->QuerySampleFormatsSupported(
                        engine_AudioIODeviceCapabilitiesItf,
                        deviceID,
                        sampleRate,
                        sampleFormats, &sampleFormatCount);
            if (SL_RESULT_SUCCESS == SLresult_)
            {
                int sampleSizeInBitsIndex;
                jsize sampleSizeInBitsCount
                    = (*jniEnv)->GetArrayLength(jniEnv, sampleSizesInBits);

                if ((*jniEnv)->ExceptionCheck(jniEnv))
                    sampleSizeInBitsCount = 0;
                for (sampleSizeInBitsIndex = 0;
                        sampleSizeInBitsIndex < sampleSizeInBitsCount;
                        sampleSizeInBitsIndex++)
                {
                    jint sampleSizeInBits;
                    jboolean sampleSizeInBitsIsSupported;
                    SLint32 sampleFormatIndex;

                    (*jniEnv)->GetIntArrayRegion(
                            jniEnv,
                            sampleSizesInBits, sampleSizeInBitsIndex, 1,
                            &sampleSizeInBits);
                    if ((*jniEnv)->ExceptionCheck(jniEnv))
                        break;

                    sampleSizeInBitsIsSupported = JNI_FALSE;
                    for (sampleFormatIndex = 0;
                            sampleFormatIndex < sampleFormatCount;
                            sampleFormatIndex++)
                    {
                        switch (*(sampleFormats + sampleFormatIndex))
                        {
                        case SL_PCMSAMPLEFORMAT_FIXED_8:
                            if (8 == sampleSizeInBits)
                                sampleSizeInBitsIsSupported = JNI_TRUE;
                            break;
                        case SL_PCMSAMPLEFORMAT_FIXED_16:
                            if (16 == sampleSizeInBits)
                                sampleSizeInBitsIsSupported = JNI_TRUE;
                            break;
                        default:
                            break;
                        }
                        if (sampleSizeInBitsIsSupported)
                            break;
                    }

                    if (sampleSizeInBitsIsSupported)
                    {
                        jint audioInputCapability[]
                            = {
                                sampleRateIndex,
                                sampleSizeInBitsIndex,
                                channelIndex
                            };
                        jint _audioInputCapabilitiesIndex
                            = *audioInputCapabilitiesIndex;
                        jsize audioInputCapabilityLength
                            = sizeof(audioInputCapability) / sizeof(jint);

                        (*jniEnv)->SetIntArrayRegion(
                                jniEnv,
                                audioInputCapabilities,
                                _audioInputCapabilitiesIndex,
                                audioInputCapabilityLength,
                                audioInputCapability);
                        if ((*jniEnv)->ExceptionCheck(jniEnv))
                            break;
                        *audioInputCapabilitiesIndex
                            = _audioInputCapabilitiesIndex
                                + audioInputCapabilityLength;
                    }
                }
            }
            free(sampleFormats);
        }
    }
}
