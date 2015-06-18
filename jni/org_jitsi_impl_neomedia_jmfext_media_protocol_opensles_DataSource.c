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
#include "org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource.h"

#include <android/log.h>
#include <errno.h>
#include <pthread.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>

#define ANDROIDSIMPLEBUFFERQUEUE_NUMBUFFERS 5
#define LOG_TAG "jnopensles"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define RECORD_POSITIONUPDATEPERIOD 20

typedef struct _DataSource
{
    SLuint32 bufferCapacity;
    SLuint32 bufferCount;
    jbyte *buffers;
    pthread_cond_t *cond;
    SLEngineItf engine_EngineItf;
    SLObjectItf engine_ObjectItf;
    pthread_mutex_t *mutex;
    SLAndroidSimpleBufferQueueItf recorder_AndroidSimpleBufferQueueItf;
    SLObjectItf recorder_ObjectItf;
    SLRecordItf recorder_RecordItf;

    /**
     * The number of #buffers containing (valid) data written by the
     * AudioRecorder.
     */
    SLuint32 writtenBufferCount;

    /**
     * The index in #buffers of the first buffer containing (valid) data written
     * by the AudioRecorder.
     */
    SLuint32 writtenBufferIndex;
}
DataSource;

static SLresult DataSource_createAudioRecorder
    (DataSource *thiz,
     JNIEnv *jniEnv, jclass clazz,
     jstring encoding, jdouble sampleRate, jint sampleSizeInBits, jint channels,
     jint endian, jint zigned, jclass dataType);
static void DataSource_recorder_AndroidSimpleBufferQueueItfCallback
    (SLAndroidSimpleBufferQueueItf caller, void *context);
static void DataSource_setRecordState(DataSource *thiz, SLuint32 recordState);

static SLresult
DataSource_createAudioRecorder
    (DataSource *thiz,
     JNIEnv *jniEnv, jclass clazz,
     jstring encoding, jdouble sampleRate, jint sampleSizeInBits, jint channels,
     jint endian, jint zigned, jclass dataType)
{
    SLDataLocator_IODevice ioDevice;
    SLDataSource audioSource;
    SLDataLocator_AndroidSimpleBufferQueue androidSimpleBufferQueue;
    SLDataFormat_PCM pcm;
    SLDataSink audioSink;
    SLresult SLresult_;
    SLEngineItf engine_EngineItf = thiz->engine_EngineItf;
    SLObjectItf recorder_ObjectItf;
    SLInterfaceID interfaceIds[]
        = { SL_IID_RECORD, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_VOLUME };
    SLboolean interfaceRequired[]
        = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_FALSE };

    ioDevice.locatorType = SL_DATALOCATOR_IODEVICE;
    ioDevice.deviceType = SL_IODEVICE_AUDIOINPUT;
    ioDevice.deviceID = SL_DEFAULTDEVICEID_AUDIOINPUT;
    ioDevice.device = NULL;
    audioSource.pLocator = &ioDevice;
    audioSource.pFormat = NULL;

    androidSimpleBufferQueue.locatorType
        = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE;
    androidSimpleBufferQueue.numBuffers = ANDROIDSIMPLEBUFFERQUEUE_NUMBUFFERS;
    pcm.bitsPerSample = sampleSizeInBits;
    pcm.channelMask = 0;
    pcm.containerSize = sampleSizeInBits;
    pcm.endianness
        = (0 /* AudioFormat.LITTLE_ENDIAN */ == endian)
            ? SL_BYTEORDER_LITTLEENDIAN
            : SL_BYTEORDER_BIGENDIAN;
    pcm.formatType = SL_DATAFORMAT_PCM;
    pcm.numChannels = channels;
    pcm.samplesPerSec = (SLuint32) (sampleRate * 1000);
    audioSink.pLocator = &androidSimpleBufferQueue;
    audioSink.pFormat = &pcm;

    SLresult_
        = (*engine_EngineItf)->CreateAudioRecorder(
            engine_EngineItf,
            &recorder_ObjectItf,
            &audioSource, &audioSink,
            sizeof(interfaceIds) / sizeof(SLInterfaceID),
            interfaceIds, interfaceRequired);
    if (SL_RESULT_SUCCESS == SLresult_)
    {
        thiz->recorder_ObjectItf = recorder_ObjectItf;

        SLresult_
            = (*recorder_ObjectItf)->Realize(
                recorder_ObjectItf,
                SL_BOOLEAN_FALSE);
        if (SL_RESULT_SUCCESS == SLresult_)
        {
            SLRecordItf recorder_RecordItf;

            SLresult_
                = (*recorder_ObjectItf)->GetInterface(
                    recorder_ObjectItf,
                    SL_IID_RECORD,
                    &recorder_RecordItf);
            if (SL_RESULT_SUCCESS == SLresult_)
            {
                thiz->recorder_RecordItf = recorder_RecordItf;

                SLresult_
                    = (*recorder_RecordItf)->SetPositionUpdatePeriod(
                        recorder_RecordItf,
                        RECORD_POSITIONUPDATEPERIOD);
                if (SL_RESULT_SUCCESS == SLresult_)
                {
                    SLAndroidSimpleBufferQueueItf
                        recorder_AndroidSimpleBufferQueueItf;

                    SLresult_
                        = (*recorder_ObjectItf)->GetInterface(
                            recorder_ObjectItf,
                            SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                            &recorder_AndroidSimpleBufferQueueItf);
                    if (SL_RESULT_SUCCESS == SLresult_)
                    {
                        thiz->recorder_AndroidSimpleBufferQueueItf
                            = recorder_AndroidSimpleBufferQueueItf;

                        thiz->bufferCapacity
                            = (SLuint32)
                                ((sampleRate / 1000)
                                    * RECORD_POSITIONUPDATEPERIOD
                                    * channels
                                    * (sampleSizeInBits / 8));
                        thiz->bufferCount = androidSimpleBufferQueue.numBuffers;
                        thiz->buffers
                            = malloc(thiz->bufferCapacity * thiz->bufferCount);
                        if (thiz->buffers)
                        {
                            SLVolumeItf recorder_VolumeItf;

                            thiz->writtenBufferCount = 0;
                            thiz->writtenBufferIndex = 0;

                            /*
                             * Attempt to set the volume of the audio input
                             * device to its maximum.
                             */
                            if (SL_RESULT_SUCCESS
                                    == (*recorder_ObjectItf)->GetInterface(
                                            recorder_ObjectItf,
                                            SL_IID_VOLUME,
                                            &recorder_VolumeItf))
                            {
                                SLmillibel maxLevel;

                                if (SL_RESULT_SUCCESS
                                        == (*recorder_VolumeItf)
                                                ->GetMaxVolumeLevel(
                                                        recorder_VolumeItf,
                                                        &maxLevel))
                                {
                                    SLresult SLresult_SetVolumeLevel
                                        = (*recorder_VolumeItf)->SetVolumeLevel(
                                                recorder_VolumeItf,
                                                maxLevel);

                                    if (SL_RESULT_SUCCESS
                                            != SLresult_SetVolumeLevel)
                                        LOGD(
                                            "%s:%d: SLVolumeItf::SetVolumeLevel=%d",
                                            __func__, (int) __LINE__,
                                            (int) SLresult_SetVolumeLevel);
                                }
                            }

                            SLresult_
                                = (*recorder_AndroidSimpleBufferQueueItf)
                                    ->RegisterCallback(
                                        recorder_AndroidSimpleBufferQueueItf,
                                        DataSource_recorder_AndroidSimpleBufferQueueItfCallback,
                                        thiz);
                            if (SL_RESULT_SUCCESS == SLresult_)
                            {
                                SLuint32 bufferIndex;
                                SLuint32 bufferCount = thiz->bufferCount;
                                jbyte *buffers = thiz->buffers;
                                SLuint32 bufferCapacity = thiz->bufferCapacity;

                                for (bufferIndex = 0;
                                        bufferIndex < bufferCount;
                                        bufferIndex++)
                                {
                                    SLresult_
                                        = (*recorder_AndroidSimpleBufferQueueItf)
                                            ->Enqueue(
                                                recorder_AndroidSimpleBufferQueueItf,
                                                buffers,
                                                bufferCapacity);
                                    if (SL_RESULT_SUCCESS == SLresult_)
                                        buffers += bufferCapacity;
                                    else
                                        break;
                                }
                            }
                        }
                        else
                            SLresult_ = SL_RESULT_MEMORY_FAILURE;
                    }
                }
            }
        }
    }
    return SLresult_;
}

static void
DataSource_recorder_AndroidSimpleBufferQueueItfCallback
    (SLAndroidSimpleBufferQueueItf caller, void *context)
{
    DataSource *thiz = (DataSource *) context;
    pthread_mutex_t *mutex = thiz->mutex;

    if (mutex && (pthread_mutex_lock(mutex) == 0))
    {
        SLAndroidSimpleBufferQueueState state;

        if (SL_RESULT_SUCCESS == (*caller)->GetState(caller, &state))
        {
            SLuint32 bufferCount = thiz->bufferCount;
            SLuint32 unqueuedCount;

            thiz->writtenBufferCount++;
            if (thiz->writtenBufferCount == thiz->bufferCount)
            {
                thiz->writtenBufferIndex
                    = (thiz->writtenBufferIndex + 1) % bufferCount;
                thiz->writtenBufferCount--;
            }

            /*
             * Notify that the information about the buffers containing (valid)
             * data written by the AudioRecorder (i.e. writtenBufferIndex and
             * writtenBufferCount) has been updated.
             */
            pthread_cond_signal(thiz->cond);

            unqueuedCount
                = bufferCount - thiz->writtenBufferCount - state.count;
            if (unqueuedCount > 0)
            {
                SLuint32 bufferIndex
                    = (thiz->writtenBufferIndex + thiz->writtenBufferCount)
                        % bufferCount;
                SLuint32 bufferCapacity = thiz->bufferCapacity;

                do
                {
                    SLresult SLresult_
                        = (*caller)->Enqueue(
                            caller,
                            thiz->buffers + bufferIndex * bufferCapacity,
                            bufferCapacity);

                    if (SL_RESULT_SUCCESS != SLresult_)
                    {
                        LOGD(
                            "%s:%d: SLAndroidSimpleBufferQueueItf::Enqueue=%d",
                            __func__, (int) __LINE__,
                            (int) SLresult_);
                    }
                    bufferIndex = (bufferIndex + 1) % bufferCount;
                    unqueuedCount--;
                }
                while (unqueuedCount);
            }
        }
        pthread_mutex_unlock(mutex);
    }
}

static void
DataSource_setRecordState(DataSource *thiz, SLuint32 recordState)
{
    SLRecordItf recorder_RecordItf = thiz->recorder_RecordItf;

    if (recorder_RecordItf)
        (*recorder_RecordItf)->SetRecordState(recorder_RecordItf, recordState);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource_connect
    (JNIEnv *jniEnv, jclass clazz,
     jstring encoding, jdouble sampleRate, jint sampleSizeInBits, jint channels,
     jint endian, jint zigned, jclass dataType)
{
    DataSource *thiz = calloc(1, sizeof(DataSource));

    if (thiz)
    {
        SLresult SLresult_ = SL_RESULT_SUCCESS;

        {
            pthread_mutex_t *mutex = malloc(sizeof(pthread_mutex_t));

            if (mutex)
            {
                pthread_mutexattr_t attr;
                pthread_mutexattr_t *pattr = &attr;

                if ((pthread_mutexattr_init(pattr) == 0)
                    && (pthread_mutexattr_settype(pattr, PTHREAD_MUTEX_RECURSIVE) == 0)
                    && (pthread_mutex_init(mutex, pattr) == 0))
                {
                    thiz->mutex = mutex;

                    if (pthread_mutexattr_destroy(pattr) == 0)
                    {
                        pthread_cond_t *cond = malloc(sizeof(pthread_cond_t));

                        if (cond)
                        {
                            if (pthread_cond_init(cond, NULL) == 0)
                                thiz->cond = cond;
                            else
                            {
                                free(cond);
                                SLresult_ = SL_RESULT_UNKNOWN_ERROR;
                            }
                        }
                        else
                            SLresult_ = SL_RESULT_MEMORY_FAILURE;
                    }
                    else
                        SLresult_ = SL_RESULT_UNKNOWN_ERROR;
                }
                else
                {
                    free(mutex);
                    SLresult_ = SL_RESULT_UNKNOWN_ERROR;
                }
            }
            else
                SLresult_ = SL_RESULT_MEMORY_FAILURE;
        }

        if (SL_RESULT_SUCCESS == SLresult_)
        {
            SLObjectItf engine_ObjectItf;

            SLresult_
                = slCreateEngine(&engine_ObjectItf, 0, NULL, 0, NULL, NULL);
            if (SL_RESULT_SUCCESS == SLresult_)
            {
                thiz->engine_ObjectItf = engine_ObjectItf;

                SLresult_
                    = (*engine_ObjectItf)->Realize(
                        engine_ObjectItf,
                        SL_BOOLEAN_FALSE);
                if (SL_RESULT_SUCCESS == SLresult_)
                {
                    SLEngineItf engine_EngineItf;

                    SLresult_
                        = (*engine_ObjectItf)->GetInterface(
                            engine_ObjectItf,
                            SL_IID_ENGINE,
                            &engine_EngineItf);
                    if (SL_RESULT_SUCCESS == SLresult_)
                    {
                        thiz->engine_EngineItf = engine_EngineItf;

                        SLresult_
                            = DataSource_createAudioRecorder(
                                thiz,
                                jniEnv, clazz,
                                encoding, sampleRate, sampleSizeInBits, channels,
                                endian, zigned, dataType);
                    }
                }
            }
        }

        if (SL_RESULT_SUCCESS != SLresult_)
        {
            Java_org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource_disconnect(
                jniEnv, clazz,
                (jlong) (intptr_t) thiz);
            thiz = NULL;
        }
    }
    return (jlong) (intptr_t) thiz;
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource_disconnect
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    DataSource *thiz = (DataSource *) (intptr_t) ptr;
    pthread_mutex_t *mutex = thiz->mutex;

    if (mutex && (pthread_mutex_lock(mutex) == 0))
    {
        SLObjectItf recorder_ObjectItf = thiz->recorder_ObjectItf;
        SLObjectItf engine_ObjectItf = thiz->engine_ObjectItf;
        pthread_cond_t *cond = thiz->cond;

        if (recorder_ObjectItf)
        {
            DataSource_setRecordState(thiz, SL_RECORDSTATE_STOPPED);
            (*recorder_ObjectItf)->Destroy(recorder_ObjectItf);
        }
        if (engine_ObjectItf)
            (*engine_ObjectItf)->Destroy(engine_ObjectItf);
        if (thiz->buffers)
            free(thiz->buffers);

        if (cond)
        {
            thiz->cond = NULL;
            if ((pthread_cond_broadcast(cond) == 0)
                    && (pthread_cond_destroy(cond) == 0))
                free(cond);
        }
        thiz->mutex = NULL;
        if ((pthread_mutex_unlock(mutex) == 0)
                && (pthread_mutex_destroy(mutex) == 0))
            free(mutex);
    }
    free(thiz);
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource_read
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jobject data, jint offset, jint length)
{
    DataSource *thiz = (DataSource *) (intptr_t) ptr;
    pthread_mutex_t *mutex = thiz->mutex;
    int err;

    if (mutex && ((err = pthread_mutex_lock(mutex)) == 0))
    {
        SLuint32 writtenBufferCount = thiz->writtenBufferCount;

        if (writtenBufferCount)
        {
            SLuint32 writtenBufferIndex = thiz->writtenBufferIndex;

            length = thiz->bufferCapacity;
            (*jniEnv)->SetByteArrayRegion(
                jniEnv,
                (jbyteArray) data, offset, length,
                thiz->buffers + (writtenBufferIndex * length));
            thiz->writtenBufferIndex
                = (writtenBufferIndex + 1) % thiz->bufferCount;
            thiz->writtenBufferCount = writtenBufferCount - 1;
        }
        else
        {
            struct timespec timeout;

            if ((err = clock_gettime(CLOCK_REALTIME, &timeout)) == 0)
            {
                long nsec
                    = timeout.tv_nsec
                        + (RECORD_POSITIONUPDATEPERIOD * 1000 * 1000);

                timeout.tv_sec += (nsec / (1000 * 1000 * 1000));
                timeout.tv_nsec = (nsec % (1000 * 1000 * 1000));
                if (((err = pthread_cond_timedwait(thiz->cond, mutex, &timeout))
                            != 0)
                        && (err != ETIMEDOUT))
                    LOGD(
                        "%s:%d: pthread_cond_timedwait=%d",
                        __func__, (int) __LINE__,
                        err);
            }
            else
                LOGD("%s:%d: clock_gettime=%d", __func__, (int) __LINE__, err);
            length = 0;
        }
        if ((err = pthread_mutex_unlock(mutex)) != 0)
            LOGD(
                "%s:%d: pthread_mutex_unlock=%d",
                __func__, (int) __LINE__,
                err);
    }
    else
    {
        length = 0;
        if (!mutex)
            LOGD("%s:%d: mutex=0x0", __func__, (int) __LINE__);
    }
    return length;
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource_start
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    DataSource_setRecordState(
        (DataSource *) (intptr_t) ptr,
        SL_RECORDSTATE_RECORDING);
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_protocol_opensles_DataSource_stop
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    DataSource_setRecordState(
        (DataSource *) (intptr_t) ptr,
        SL_RECORDSTATE_STOPPED);
}
