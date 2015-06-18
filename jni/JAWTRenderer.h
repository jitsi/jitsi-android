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

#ifndef _JAWTRENDERER_H_
#define _JAWTRENDERER_H_

#ifndef __ANDROID__
    #include <jawt.h>
#else /* #ifndef __ANDROID__ */
    typedef void JAWT_DrawingSurfaceInfo;
#endif /* #ifndef __ANDROID__ */
#include <jni.h>

#ifndef NULL
#define NULL 0
#endif /* #ifndef NULL */

#ifdef __cplusplus
extern "C" {
#endif /* #ifdef __cplusplus */

void JAWTRenderer_close
    (JNIEnv *env, jclass clazz, jlong handle, jobject component);
jlong JAWTRenderer_open(JNIEnv *env, jclass clazz, jobject component);
jboolean JAWTRenderer_paint
    (jint version, JAWT_DrawingSurfaceInfo *dsi, jclass clazz, jlong handle,
        jobject g, jint zOrder);
jboolean JAWTRenderer_process
    (JNIEnv *env, jclass clazz,jlong handle, jobject component, jint *data,
        jint length, jint width, jint height);

#ifdef __APPLE__
jstring JAWTRenderer_sysctlbyname(JNIEnv *env, jstring name);
#endif /* #ifdef __APPLE__ */

#ifdef __cplusplus
} /* extern "C" { */
#endif /* #ifdef __cplusplus */

#endif /* _JAWTRENDERER_H_ */
