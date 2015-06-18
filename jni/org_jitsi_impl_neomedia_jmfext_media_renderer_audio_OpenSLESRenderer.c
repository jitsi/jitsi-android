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
#include "org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer.h"

#include <android/log.h>
#include <pthread.h>
#include <SLES/OpenSLES.h>
#include <stdint.h>
#include <stdlib.h>

#define BUFFER_CAPACITYINMILLIS 20
#define BUFFERQUEUE_NUMBUFFERS 5
#define LOG_TAG "jnopensles"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define PLUGIN_BUFFERPROCESSEDFAILED 1
#define PLUGIN_BUFFERPROCESSEDOK 0

typedef struct _OpenSLESRenderer
{
    jint bufferCapacity;
    SLuint32 bufferCount;
    SLuint32 *bufferPlayIndexes;
    jbyte *buffers;
    pthread_cond_t *cond;
    SLEngineItf engine_EngineItf;
    SLObjectItf engine_ObjectItf;
    pthread_mutex_t *mutex;
    SLuint32 nextBufferPlayIndex;
    SLObjectItf outputMix_ObjectItf;
    SLBufferQueueItf player_BufferQueueItf;
    SLObjectItf player_ObjectItf;
    SLPlayItf player_PlayItf;
    SLuint32 playIndex;
}
OpenSLESRenderer;

static SLresult OpenSLESRenderer_createAudioPlayer
    (OpenSLESRenderer *thiz,
     JNIEnv *jniEnv, jclass clazz,
     jstring encoding, jdouble sampleRate, jint sampleSizeInBits, jint channels,
     jint endian, jint zigned, jclass dataType);
static void OpenSLESRenderer_player_BufferQueueItfCallback
    (SLBufferQueueItf caller, void *context);

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    OpenSLESRenderer *thiz = (OpenSLESRenderer *) (intptr_t) ptr;
    pthread_mutex_t *mutex = thiz->mutex;

    if (mutex && (pthread_mutex_lock(mutex) == 0))
    {
        SLObjectItf player_ObjectItf = thiz->player_ObjectItf;
        SLObjectItf outputMix_ObjectItf = thiz->outputMix_ObjectItf;
        SLObjectItf engine_ObjectItf = thiz->engine_ObjectItf;
        pthread_cond_t *cond = thiz->cond;

        if (player_ObjectItf)
        {
            SLPlayItf player_PlayItf = thiz->player_PlayItf;

            if (player_PlayItf)
                (*player_PlayItf)->SetPlayState(
                    player_PlayItf,
                    SL_PLAYSTATE_STOPPED);
            (*player_ObjectItf)->Destroy(player_ObjectItf);
        }
        if (outputMix_ObjectItf)
            (*outputMix_ObjectItf)->Destroy(outputMix_ObjectItf);
        if (engine_ObjectItf)
            (*engine_ObjectItf)->Destroy(engine_ObjectItf);
        if (thiz->buffers)
            free(thiz->buffers);
        if (thiz->bufferPlayIndexes)
            free(thiz->bufferPlayIndexes);

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

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer_open
    (JNIEnv *jniEnv, jclass clazz,
     jstring encoding, jdouble sampleRate, jint sampleSizeInBits, jint channels,
     jint endian, jint zigned, jclass dataType)
{
    OpenSLESRenderer *thiz = calloc(1, sizeof(OpenSLESRenderer));

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
                            = OpenSLESRenderer_createAudioPlayer(
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
            Java_org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer_close(
                jniEnv, clazz,
                (jlong) (intptr_t) thiz);
            thiz = NULL;
        }
    }
    return (jlong) (intptr_t) thiz;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong ptr, jobject data, jint offset, jint length)
{
    OpenSLESRenderer *thiz = (OpenSLESRenderer *) (intptr_t) ptr;
    jint processed;

    while (length)
    {
        pthread_mutex_t *mutex = thiz->mutex;

        if (mutex && (pthread_mutex_lock(mutex) == 0))
        {
            SLPlayItf player_PlayItf = thiz->player_PlayItf;
            SLuint32 playState;

            if (SL_RESULT_SUCCESS
                        == (*player_PlayItf)->GetPlayState(
                            player_PlayItf,
                            &playState))
            {
                SLuint32 bufferIndex;
                SLuint32 bufferCount = thiz->bufferCount;
                SLuint32 *bufferPlayIndexes = thiz->bufferPlayIndexes;
                SLuint32 playIndex = thiz->playIndex;
                jboolean duplicatePlayIndex = JNI_FALSE;

                processed = PLUGIN_BUFFERPROCESSEDOK;
                for (bufferIndex = 0; bufferIndex < bufferCount; bufferIndex++)
                {
                    SLuint32 *bufferPlayIndexPtr
                        = bufferPlayIndexes + bufferIndex;
                    SLuint32 bufferPlayIndex = *bufferPlayIndexPtr;

                    if ((bufferPlayIndex < playIndex)
                            || ((bufferPlayIndex == playIndex)
                                    && ((duplicatePlayIndex = JNI_TRUE))))
                    {
                        jint bufferCapacity = thiz->bufferCapacity;
                        jbyte *buffer
                            = thiz->buffers
                                + (bufferIndex * bufferCapacity);
                        SLuint32 size
                            = (bufferCapacity < length)
                                ? bufferCapacity
                                : length;

                        (*jniEnv)->GetByteArrayRegion(
                            jniEnv,
                            (jbyteArray) data, offset, size,
                            buffer);
                        if ((*jniEnv)->ExceptionCheck(jniEnv))
                        {
                            processed = PLUGIN_BUFFERPROCESSEDFAILED;
                            LOGD(
                                "%s:%d: PlugIn.BUFFER_PROCESSED_FAILED",
                                __func__,
                                (int) __LINE__);
                        }
                        else
                        {
                            SLBufferQueueItf player_BufferQueueItf
                                = thiz->player_BufferQueueItf;

                            if (SL_RESULT_SUCCESS
                                    == (*player_BufferQueueItf)->Enqueue(
                                            player_BufferQueueItf,
                                            buffer, size))
                            {
                                *bufferPlayIndexPtr
                                    = thiz->nextBufferPlayIndex++;
                                offset += size;
                                length -= size;
                            }
                            else
                            {
                                processed = PLUGIN_BUFFERPROCESSEDFAILED;
                                LOGD(
                                    "%s:%d: PlugIn.BUFFER_PROCESSED_FAILED",
                                    __func__,
                                    (int) __LINE__);
                            }
                        }

                        break;
                    }
                }
                if ((PLUGIN_BUFFERPROCESSEDOK == processed)
                        && (bufferIndex == bufferCount)
                        && (pthread_cond_wait(thiz->cond, mutex) != 0))
                {
                    processed = PLUGIN_BUFFERPROCESSEDFAILED;
                    LOGD(
                        "%s:%d: PlugIn.BUFFER_PROCESSED_FAILED",
                        __func__,
                        (int) __LINE__);
                }
            }
            else
            {
                processed = PLUGIN_BUFFERPROCESSEDFAILED;
                LOGD(
                    "%s:%d: PlugIn.BUFFER_PROCESSED_FAILED",
                    __func__,
                    (int) __LINE__);
            }

            if (pthread_mutex_unlock(mutex) == 0)
            {
                if (PLUGIN_BUFFERPROCESSEDFAILED == processed)
                    break;
            }
            else
            {
                processed = PLUGIN_BUFFERPROCESSEDFAILED;
                LOGD(
                    "%s:%d: PlugIn.BUFFER_PROCESSED_FAILED",
                    __func__,
                    (int) __LINE__);
                break;
            }
        }
        else
        {
            processed = PLUGIN_BUFFERPROCESSEDFAILED;
            LOGD(
                "%s:%d: PlugIn.BUFFER_PROCESSED_FAILED",
                __func__,
                (int) __LINE__);
            break;
        }
    }
    return processed;
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer_start
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    OpenSLESRenderer *thiz = (OpenSLESRenderer *) (intptr_t) ptr;
    SLPlayItf player_PlayItf = thiz->player_PlayItf;

    if (player_PlayItf)
    {
        SLresult SLresult_
            = (*player_PlayItf)->SetPlayState(
                    player_PlayItf,
                    SL_PLAYSTATE_PLAYING);
    }
}

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_jmfext_media_renderer_audio_OpenSLESRenderer_stop
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    OpenSLESRenderer *thiz = (OpenSLESRenderer *) (intptr_t) ptr;
    pthread_mutex_t *mutex = thiz->mutex;

    if (mutex && (pthread_mutex_lock(mutex) == 0))
    {
        SLPlayItf player_PlayItf = thiz->player_PlayItf;

        if (player_PlayItf)
            (*player_PlayItf)->SetPlayState(
                player_PlayItf,
                SL_PLAYSTATE_STOPPED);
        pthread_mutex_unlock(mutex);
    }
}

static SLresult
OpenSLESRenderer_createAudioPlayer
    (OpenSLESRenderer *thiz,
     JNIEnv *jniEnv, jclass clazz,
     jstring encoding, jdouble sampleRate, jint sampleSizeInBits, jint channels,
     jint endian, jint zigned, jclass dataType)
{
    SLDataLocator_BufferQueue bufferQueue;
    SLDataFormat_PCM pcm;
    SLDataSource audioSource;
    SLresult SLresult_;
    SLEngineItf engine_EngineItf = thiz->engine_EngineItf;
    SLObjectItf outputMix_ObjectItf;

    bufferQueue.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
    bufferQueue.numBuffers = BUFFERQUEUE_NUMBUFFERS;
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
    audioSource.pLocator = &bufferQueue;
    audioSource.pFormat = &pcm;

    SLresult_
        = (*engine_EngineItf)->CreateOutputMix(
            engine_EngineItf,
            &outputMix_ObjectItf,
            0, NULL, NULL);
    if (SL_RESULT_SUCCESS == SLresult_)
    {
        thiz->outputMix_ObjectItf = outputMix_ObjectItf;

        SLresult_
            = (*outputMix_ObjectItf)->Realize(
                outputMix_ObjectItf,
                SL_BOOLEAN_FALSE);
        if (SL_RESULT_SUCCESS == SLresult_)
        {
            SLDataLocator_OutputMix outputMix;
            SLDataSink audioSink;
            SLObjectItf player_ObjectItf;
            SLInterfaceID interfaceIds[] = { SL_IID_BUFFERQUEUE, SL_IID_PLAY };
            SLboolean interfaceRequired[]
                = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };

            outputMix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
            outputMix.outputMix = outputMix_ObjectItf;
            audioSink.pLocator = &outputMix;
            audioSink.pFormat = NULL;

            SLresult_
                = (*engine_EngineItf)->CreateAudioPlayer(
                    engine_EngineItf,
                    &player_ObjectItf,
                    &audioSource, &audioSink,
                    sizeof(interfaceIds) / sizeof(SLInterfaceID),
                    interfaceIds, interfaceRequired);
            if (SL_RESULT_SUCCESS == SLresult_)
            {
                thiz->player_ObjectItf = player_ObjectItf;

                SLresult_
                    = (*player_ObjectItf)->Realize(
                        player_ObjectItf,
                        SL_BOOLEAN_FALSE);
                if (SL_RESULT_SUCCESS == SLresult_)
                {
                    SLBufferQueueItf player_BufferQueueItf;

                    SLresult_
                        = (*player_ObjectItf)->GetInterface(
                            player_ObjectItf,
                            SL_IID_BUFFERQUEUE,
                            &player_BufferQueueItf);
                    if (SL_RESULT_SUCCESS == SLresult_)
                    {
                        SLPlayItf player_PlayItf;

                        thiz->player_BufferQueueItf = player_BufferQueueItf;

                        SLresult_
                            = (*player_ObjectItf)->GetInterface(
                                player_ObjectItf,
                                SL_IID_PLAY,
                                &player_PlayItf);
                        if (SL_RESULT_SUCCESS == SLresult_)
                        {
                            thiz->player_PlayItf = player_PlayItf;

                            thiz->bufferCapacity
                                = (jint)
                                    ((sampleRate / 1000)
                                        * BUFFER_CAPACITYINMILLIS
                                        * channels
                                        * (sampleSizeInBits / 8));
                            thiz->bufferCount = bufferQueue.numBuffers;
                            thiz->buffers
                                = malloc(
                                    thiz->bufferCapacity * thiz->bufferCount);
                            thiz->bufferPlayIndexes
                                = calloc(thiz->bufferCount, sizeof(SLuint32));
                            if (thiz->buffers && thiz->bufferPlayIndexes)
                            {
                                thiz->nextBufferPlayIndex = 1;

                                SLresult_
                                    = (*player_BufferQueueItf)
                                        ->RegisterCallback(
                                            player_BufferQueueItf,
                                            OpenSLESRenderer_player_BufferQueueItfCallback,
                                            thiz);
                            }
                            else
                                SLresult_ = SL_RESULT_MEMORY_FAILURE;
                        }
                    }
                }
            }
        }
    }
    return SLresult_;
}

static void
OpenSLESRenderer_player_BufferQueueItfCallback
    (SLBufferQueueItf caller, void *context)
{
    OpenSLESRenderer *thiz = (OpenSLESRenderer *) context;
    pthread_mutex_t *mutex = thiz->mutex;

    if (mutex && (pthread_mutex_lock(mutex) == 0))
    {
        SLBufferQueueState state;

        if ((SL_RESULT_SUCCESS == (*caller)->GetState(caller, &state)))
        {
            thiz->playIndex = state.playIndex;
            if (0 == state.count)
                *(thiz->bufferPlayIndexes) = 0;
            pthread_cond_signal(thiz->cond);
        }
        pthread_mutex_unlock(mutex);
    }
}
