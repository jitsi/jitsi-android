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
#include "org_jitsi_impl_neomedia_codec_video_h264_OMXDecoder.h"

#include <android/log.h>
#include <dlfcn.h> /* dlclose, dlopen, dlsym */
#include <OMX_Component.h> /* OMX_PARAM_PORTDEFINITIONTYPE */
#include <OMX_Core.h>
#include <OMX_Index.h> /* OMX_INDEXTYPE */
#include <stdint.h> /* intptr_t */
#include <stdlib.h> /* calloc, free */
#include <string.h> /* memset */

typedef OMX_ERRORTYPE (*OMXDeinitFunc)();
typedef OMX_ERRORTYPE (*OMXFreeHandleFunc)(OMX_HANDLETYPE);
typedef OMX_ERRORTYPE (*OMXGetHandleFunc)(OMX_HANDLETYPE *, OMX_STRING, OMX_PTR, OMX_CALLBACKTYPE *);
typedef OMX_ERRORTYPE (*OMXInitFunc)();

#define DEFAULT_OUTPUT_BUFFER_COUNT 4
#define LOG_TAG "jnopenmax"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#ifndef NULL
#define NULL 0
#endif /* #ifndef NULL */

typedef struct _OMXDecoder
{
    OMX_HANDLETYPE component;
    OMX_U32 inputBufferCount;
    OMX_BUFFERHEADERTYPE **inputBuffers;
    OMX_U32 inputBufferSize;
    OMX_U32 inputPortIndex;
    OMX_U32 outputBufferCount;
    OMX_BUFFERHEADERTYPE **outputBuffers;
    OMX_U32 outputBufferSize;
    OMX_U32 outputPortIndex;
}
OMXDecoder;

static OMX_ERRORTYPE OMXDecoder_allocateBuffers(OMXDecoder *thiz, OMX_BUFFERHEADERTYPE ***buffers, OMX_U32 count, OMX_U32 portIndex, OMX_U32 size);
static OMX_ERRORTYPE OMXDecoder_emptyBufferDone(OMX_HANDLETYPE component, OMX_PTR appData, OMX_BUFFERHEADERTYPE *buffer);
static OMX_ERRORTYPE OMXDecoder_eventHandler(OMX_HANDLETYPE component, OMX_PTR appData, OMX_EVENTTYPE event, OMX_U32 data1, OMX_U32 data2, OMX_PTR eventData);
static OMX_ERRORTYPE OMXDecoder_fillBufferDone(OMX_HANDLETYPE component, OMX_PTR appData, OMX_BUFFERHEADERTYPE *buffer);
static OMX_ERRORTYPE OMXDecoder_freeBuffers(OMXDecoder *thiz, OMX_BUFFERHEADERTYPE **buffers, OMX_U32 count, OMX_U32 portIndex);
static void OMXDecoder_onCmdStateSetComplete(OMXDecoder *thiz, OMX_STATETYPE reached);

static void *dlHandle = NULL;
static OMXDeinitFunc omxDeinit = NULL;
static OMXFreeHandleFunc omxFreeHandle = NULL;
static OMXGetHandleFunc omxGetHandle = NULL;

JNIEXPORT void JNICALL
Java_org_jitsi_impl_neomedia_codec_video_h264_OMXDecoder_close
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    OMXDecoder *thiz = (OMXDecoder *) (intptr_t) ptr;

    if (thiz->component)
    {
        if (thiz->inputBuffers)
        {
            OMXDecoder_freeBuffers(
                    thiz,
                    thiz->inputBuffers, thiz->inputBufferCount,
                    thiz->inputPortIndex);
        }
        if (thiz->outputBuffers)
        {
            OMXDecoder_freeBuffers(
                    thiz,
                    thiz->outputBuffers, thiz->outputBufferCount,
                    thiz->outputPortIndex);
        }
        omxFreeHandle(thiz->component);
    }
    free(thiz);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_impl_neomedia_codec_video_h264_OMXDecoder_open
    (JNIEnv *jniEnv, jclass clazz, jobject reserved)
{
    OMXDecoder *thiz = calloc(1, sizeof(OMXDecoder));

    if (thiz)
    {
        OMX_HANDLETYPE component = NULL;
        OMX_CALLBACKTYPE callbacks
            = {
                OMXDecoder_eventHandler,
                OMXDecoder_emptyBufferDone,
                OMXDecoder_fillBufferDone
            };

        if (OMX_ErrorNone
                == omxGetHandle(
                        &component,
                        "OMX.Nvidia.h264.decode",
                        thiz,
                        &callbacks))
        {
            OMX_PORT_PARAM_TYPE videoInitParam;
            OMX_ERRORTYPE omxerr;

            thiz->component = component;

            memset(&videoInitParam, 0, sizeof(OMX_PORT_PARAM_TYPE));
            videoInitParam.nSize = sizeof(OMX_PORT_PARAM_TYPE);
            videoInitParam.nVersion.nVersion = 0x00000101;
            omxerr
                = OMX_GetParameter(
                        component,
                        OMX_IndexParamVideoInit,
                        &videoInitParam);
            if (OMX_ErrorNone == omxerr)
            {
                OMX_PARAM_PORTDEFINITIONTYPE portDefinitionParam;
                OMX_U32 inputPortIndex = videoInitParam.nStartPortNumber;

                memset(
                        &portDefinitionParam,
                        0,
                        sizeof(OMX_PARAM_PORTDEFINITIONTYPE));
                portDefinitionParam.nPortIndex = inputPortIndex;
                portDefinitionParam.nSize
                    = sizeof(OMX_PARAM_PORTDEFINITIONTYPE);
                portDefinitionParam.nVersion.nVersion = 0x00000101;
                omxerr
                    = OMX_GetParameter(
                            component,
                            OMX_IndexParamPortDefinition,
                            &portDefinitionParam);
                if (OMX_ErrorNone == omxerr)
                {
                    portDefinitionParam.nBufferSize = 4 * 1024;
                    omxerr
                        = OMX_SetParameter(
                                component,
                                OMX_IndexParamPortDefinition,
                                &portDefinitionParam);
                    if (OMX_ErrorNone == omxerr)
                    {
                        OMX_U32 outputPortIndex = inputPortIndex + 1;

                        thiz->inputBufferCount
                            = portDefinitionParam.nBufferCountActual;
                        thiz->inputBufferSize = portDefinitionParam.nBufferSize;
                        LOGD(
                                "%s:%d: nPortIndex= %d; nBufferCountActual= %d; nBufferSize= %d",
                                __func__, (int) __LINE__,
                                portDefinitionParam.nPortIndex,
                                portDefinitionParam.nBufferCountActual,
                                portDefinitionParam.nBufferSize);

                        memset(
                                &portDefinitionParam,
                                0,
                                sizeof(OMX_PARAM_PORTDEFINITIONTYPE));
                        portDefinitionParam.nPortIndex = outputPortIndex;
                        portDefinitionParam.nSize
                            = sizeof(OMX_PARAM_PORTDEFINITIONTYPE);
                        portDefinitionParam.nVersion.nVersion = 0x00000101;
                        omxerr
                            = OMX_GetParameter(
                                    component,
                                    OMX_IndexParamPortDefinition,
                                    &portDefinitionParam);
                        if (OMX_ErrorNone == omxerr)
                        {
                            OMX_U32 minOutputBufferCount
                                = portDefinitionParam.nBufferCountMin;

                            if (minOutputBufferCount)
                            {
                                portDefinitionParam.nBufferCountActual
                                    = (minOutputBufferCount
                                            > DEFAULT_OUTPUT_BUFFER_COUNT)
                                        ? minOutputBufferCount
                                        : DEFAULT_OUTPUT_BUFFER_COUNT;
                            }
                            else
                            {
                                portDefinitionParam.nBufferCountActual
                                    = DEFAULT_OUTPUT_BUFFER_COUNT;
                                portDefinitionParam.nBufferCountMin
                                    = DEFAULT_OUTPUT_BUFFER_COUNT;
                            }
                            portDefinitionParam.format.video.nFrameHeight = 0;
                            portDefinitionParam.format.video.nFrameWidth = 0;
                            portDefinitionParam.nBufferSize = 4 * 1024;
                            omxerr
                                = OMX_SetParameter(
                                        component,
                                        OMX_IndexParamPortDefinition,
                                        &portDefinitionParam);
                            if (OMX_ErrorNone == omxerr)
                            {
                                thiz->outputBufferCount
                                    = portDefinitionParam.nBufferCountActual;
                                thiz->outputBufferSize
                                    = portDefinitionParam.nBufferSize;
                                LOGD(
                                        "%s:%d: nPortIndex= %d; nBufferCountActual= %d; nBufferSize= %d",
                                        __func__, (int) __LINE__,
                                        portDefinitionParam.nPortIndex,
                                        portDefinitionParam.nBufferCountActual,
                                        portDefinitionParam.nBufferSize);

                                thiz->inputPortIndex = inputPortIndex;
                                thiz->outputPortIndex = outputPortIndex;

                                omxerr
                                    = OMXDecoder_allocateBuffers(
                                            thiz,
                                            &(thiz->outputBuffers),
                                            thiz->outputBufferCount,
                                            outputPortIndex,
                                            thiz->outputBufferSize);
                                if (OMX_ErrorNone == omxerr)
                                {
                                    omxerr
                                        = OMX_SendCommand(
                                                component,
                                                OMX_CommandStateSet,
                                                OMX_StateIdle, NULL);
                                    if (OMX_ErrorNone == omxerr)
                                    {
                                        omxerr
                                            = OMXDecoder_allocateBuffers(
                                                    thiz,
                                                    &(thiz->inputBuffers),
                                                    thiz->inputBufferCount,
                                                    inputPortIndex,
                                                    thiz->inputBufferSize);
                                        if (OMX_ErrorNone == omxerr)
                                        {
                                            /* TODO Auto-generated method stub */
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (OMX_ErrorNone != omxerr)
                component = NULL;
        }

        if (!component)
        {
            Java_org_jitsi_impl_neomedia_codec_video_h264_OMXDecoder_close(
                    jniEnv, clazz,
                    (jlong) (intptr_t) thiz);
            thiz = NULL;
        }
    }
    return (jlong) (intptr_t) thiz;
}

jint
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    jint ret = JNI_ERR;

    if (dlHandle)
        ret = JNI_VERSION_1_6;
    else
    {
        dlHandle = dlopen("libnvomx.so", RTLD_NOW);
        if (dlHandle)
        {
            OMXInitFunc omxInit = (OMXInitFunc) dlsym(dlHandle, "OMX_Init");

            if (omxInit)
                omxDeinit = (OMXDeinitFunc) dlsym(dlHandle, "OMX_Deinit");
            if (omxDeinit)
                omxGetHandle
                    = (OMXGetHandleFunc) dlsym(dlHandle, "OMX_GetHandle");
            if (omxGetHandle)
                omxFreeHandle
                    = (OMXFreeHandleFunc) dlsym(dlHandle, "OMX_FreeHandle");
            if (omxFreeHandle && (OMX_ErrorNone == omxInit()))
                ret = JNI_VERSION_1_6;
            else
            {
                omxDeinit = NULL;
                omxFreeHandle = NULL;
                omxGetHandle = NULL;

                dlclose(dlHandle);
                dlHandle = NULL;
            }
        }
    }
    return ret;
}

void
JNI_OnUnload(JavaVM *vm, void *reserved)
{
    if (dlHandle)
    {
        omxFreeHandle = NULL;
        omxGetHandle = NULL;
        if (omxDeinit)
        {
            omxDeinit();
            omxDeinit = NULL;
        }

        dlclose(dlHandle);
        dlHandle = NULL;
    }
}

static OMX_ERRORTYPE
OMXDecoder_allocateBuffers
    (OMXDecoder *thiz,
        OMX_BUFFERHEADERTYPE ***buffers, OMX_U32 count,
        OMX_U32 portIndex, OMX_U32 size)
{
    OMX_BUFFERHEADERTYPE **buffers_;
    OMX_ERRORTYPE omxerr;

    buffers_ = calloc(count, sizeof(OMX_BUFFERHEADERTYPE *));
    if (buffers_)
    {
        OMX_U32 i;
        OMX_HANDLETYPE component = thiz->component;

        omxerr = OMX_ErrorNone;
        for (i = 0; i < count; i++)
        {
            OMX_U8 *buffer = malloc(size * sizeof(OMX_U8));

            if (buffer)
            {
                omxerr
                    = OMX_UseBuffer(
                            component,
                            buffers_ + i,
                            portIndex, NULL, size, buffer);
                if (OMX_ErrorNone != omxerr)
                    free(buffer);
            }
            else
                omxerr = OMX_ErrorInsufficientResources;
            if (OMX_ErrorNone != omxerr)
                break;
        }
    }
    else
        omxerr = OMX_ErrorInsufficientResources;
    *buffers = buffers_;
    return omxerr;
}

static OMX_ERRORTYPE
OMXDecoder_emptyBufferDone
    (OMX_HANDLETYPE component, OMX_PTR appData, OMX_BUFFERHEADERTYPE *buffer)
{
    /* TODO Auto-generated method stub */
    return OMX_ErrorNone;
}

static OMX_ERRORTYPE
OMXDecoder_eventHandler
    (OMX_HANDLETYPE component, OMX_PTR appData, OMX_EVENTTYPE event,
        OMX_U32 data1, OMX_U32 data2, OMX_PTR eventData)
{
    switch (event)
    {
    case OMX_EventCmdComplete:
        switch (data1)
        {
        case OMX_CommandStateSet:
            OMXDecoder_onCmdStateSetComplete(
                    (OMXDecoder *) appData,
                    (OMX_STATETYPE) data2);
            break;
        }
        break;
    case OMX_EventError:
        LOGD("%s:%d: OMX_ERRORTYPE 0x%x", __func__, (int) __LINE__, data1);
        break;
    }
    return OMX_ErrorNone;
}

static OMX_ERRORTYPE
OMXDecoder_fillBufferDone
    (OMX_HANDLETYPE component, OMX_PTR appData, OMX_BUFFERHEADERTYPE *buffer)
{
    /* TODO Auto-generated method stub */
    return OMX_ErrorNone;
}

static OMX_ERRORTYPE
OMXDecoder_freeBuffers
    (OMXDecoder *thiz,
        OMX_BUFFERHEADERTYPE **buffers, OMX_U32 count,
        OMX_U32 portIndex)
{
    OMX_U32 i;
    OMX_HANDLETYPE component = thiz->component;
    OMX_ERRORTYPE ret = OMX_ErrorNone;

    for (i = 0; i < count; i++)
    {
        OMX_BUFFERHEADERTYPE *buffer = *(buffers + i);

        if (buffer)
        {
            OMX_U8 *buffer_;
            OMX_ERRORTYPE omxerr;

            buffer_ = buffer->pBuffer;
            omxerr = OMX_FreeBuffer(component, portIndex, buffer);
            if (OMX_ErrorNone == omxerr)
            {
                free(buffer_);
                *(buffers + i) = NULL;
            }
            else
                ret = omxerr;
        }
    }
    if (OMX_ErrorNone == ret)
        free(buffers);
    return ret;
}

static void
OMXDecoder_onCmdStateSetComplete(OMXDecoder *thiz, OMX_STATETYPE reached)
{
    const char *s;

    switch (reached)
    {
    case OMX_StateExecuting:
        s = "OMX_StateExecuting";
        break;
    case OMX_StateIdle:
        s = "OMX_StateIdle";
        break;
    case OMX_StateInvalid:
        s = "OMX_StateInvalid";
        break;
    case OMX_StateKhronosExtensions:
        s = "OMX_StateKhronosExtensions";
        break;
    case OMX_StateLoaded:
        s = "OMX_StateLoaded";
        break;
    case OMX_StatePause:
        s = "OMX_StatePause";
        break;
    case OMX_StateWaitForResources:
        s = "OMX_StateWaitForResources";
        break;
    case OMX_StateVendorStartUnused:
        s = "OMX_StateVendorStartUnused";
        break;
    default:
        s = "OMX_StateMax";
        break;
    }

    LOGD("%s:%d: %s", __func__, (int) __LINE__, s);
}
