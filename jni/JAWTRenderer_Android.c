/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "JAWTRenderer.h"

#include <android/log.h>
#include <EGL/egl.h>
#include <GLES/gl.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define JAWT_RENDERER_TEXTURE GL_TEXTURE_2D
#define JAWT_RENDERER_TEXTURE_FORMAT GL_RGBA
#define JAWT_RENDERER_TEXTURE_TYPE GL_UNSIGNED_BYTE
#define LOG_TAG "jnawtrenderer"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

static const GLfloat vertexPointer[]
    = {
        -1.0, 1.0, 0.0,
        1.0, 1.0, 0.0,
        1.0, -1.0, 0.0,
        -1.0, 1.0, 0.0,
        -1.0, -1.0, 0.0,
        1.0, -1.0, 0.0
    };

typedef struct _JAWTRenderer
{
    jint *data;

    /** The number of <tt>jint</tt> elements allocated in <tt>data</tt>. */
    jint dataCapacity;

    /** The height in pixels of the video frame represented by <tt>data</tt>. */
    jint dataHeight;

    /**
     * The number of <tt>jint</tt> elements in <tt>data</tt> which contain
     * actual/valid data.
     */
    jint dataLength;

    /** The width in pixels of the video frame represented by <tt>data</tt>. */
    jint dataWidth;

    EGLContext eglContext;

    /**
     * The indicator which determines whether one-time initialization is to be
     * performed for <tt>eglContext</tt>.
     */
    jboolean prepareOpenGL;

    /**
     * The texture generated in <tt>eglContext</tt> which is drawn/rendered to
     * depict <tt>data</tt>.
     */
    GLuint tex;

    GLfloat texCoordPointer[12];

    /**
     * The height in pixels of <tt>tex</tt> which contains actual/valid video
     * frame data. If non-power-of-two textures are not supported,
     * <tt>texEffectiveHeight</tt> will be less than or equal to
     * <tt>texRealHeight</tt>.
     */
    jint texEffectiveHeight;

    /**
     * The width in pixels of <tt>tex</tt> which contains actual/valid video
     * frame data. If non-power-of-two textures are not supported,
     * <tt>texEffectiveWidth</tt> will be less than or equal to
     * <tt>texRealHeight</tt>.
     */
    jint texEffectiveWidth;

    /**
     * The height in pixels of <tt>tex</tt>. If non-power-of-two textures are
     * not supported, <tt>texRealHeight</tt> will be the least power of two that
     * is not less than <tt>texEffectiveHeight</tt>.
     */
    jint texRealHeight;

    /**
     * The width in pixels of <tt>tex</tt>. If non-power-of-two textures are not
     * supported, <tt>texRealWidth</tt> will be the least power of two that is
     * not less than <tt>texEffectiveWidth</tt>.
     */
    jint texRealWidth;
}
JAWTRenderer;

/**
 * Determines whether a specific GL error has occurred since the last call to
 * the function <tt>JAWTRenderer_checkGLError</tt> or since the GL was
 * initialized.
 *
 * @param func the name of the last GL function which was invoked prior to the
 * invocation of <tt>JAWTRenderer_checkGLError</tt> and which is to be printed
 * in the log if an error has occurred
 * @param line the number of the source code line to be printed in the log if an
 * error has occurred
 * @param err the GL error to be checked whether it has occurred since the last
 * call to the function <tt>JAWTRenderer_checkGLError</tt> or since the GL was
 * initialized. May be <tt>GL_NO_ERROR</tt> to determine whether any GL error
 * has occurred.
 * @return <tt>GL_TRUE</tt> if the specified <tt>err</tt> has occurred since the
 * last call to the function <tt>JAWTRenderer_checkGLError</tt> or since the GL
 * was initialized; otherwise, <tt>GL_FALSE</tt>
 */
static GLboolean
JAWTRenderer_checkGLError(const char *func, int line, GLenum err);

/**
 * Finds the least power of two that is not less than a specific integer
 * <tt>i</tt>.
 *
 * @param i an integer for which the least power of two that is not less than it
 * is to be found
 */
static GLsizei JAWTRenderer_roundUpToPowerOfTwo(GLsizei i);

/**
 * Determines whether a specific GL error has occurred since the last call to
 * the function <tt>JAWTRenderer_checkGLError</tt> or since the GL was
 * initialized.
 *
 * @param func the name of the last GL function which was invoked prior to the
 * invocation of <tt>JAWTRenderer_checkGLError</tt> and which is to be printed
 * in the log if an error has occurred
 * @param line the number of the source code line to be printed in the log if an
 * error has occurred
 * @param err the GL error to be checked whether it has occurred since the last
 * call to the function <tt>JAWTRenderer_checkGLError</tt> or since the GL was
 * initialized. May be <tt>GL_NO_ERROR</tt> to determine whether any GL error
 * has occurred.
 * @return <tt>GL_TRUE</tt> if the specified <tt>err</tt> has occurred since the
 * last call to the function <tt>JAWTRenderer_checkGLError</tt> or since the GL
 * was initialized; otherwise, <tt>GL_FALSE</tt>
 */
static GLboolean
JAWTRenderer_checkGLError(const char *func, int line, GLenum err)
{
    GLenum e;
    GLboolean ret = (GL_NO_ERROR == err) ? GL_TRUE : GL_FALSE;

    while ((e = glGetError()))
    {
        const char *s;

        switch (e)
        {
        case GL_INVALID_ENUM: s = "GL_INVALID_ENUM"; break;
        case GL_INVALID_OPERATION: s = "GL_INVALID_OPERATION"; break;
        case GL_INVALID_VALUE: s = "GL_INVALID_VALUE"; break;
        case GL_OUT_OF_MEMORY: s = "GL_OUT_OF_MEMORY"; break;
        case GL_STACK_OVERFLOW: s = "GL_STACK_OVERFLOW"; break;
        case GL_STACK_UNDERFLOW: s = "GL_STACK_UNDERFLOW"; break;
        default: s = 0; break;
        }
        if (s)
            LOGD("%s:%d: %s\n", func, line, s);
        else
            LOGD("%s:%d: 0x%x\n", func, line, (unsigned int) e);

        if (GL_NO_ERROR == err)
            ret = GL_FALSE;
        else if ((GL_FALSE == ret) && (e == err))
            ret = GL_TRUE;
    }

    return ret;
}

void
JAWTRenderer_close(JNIEnv *env, jclass clazz, jlong handle, jobject component)
{
    JAWTRenderer *thiz = (JAWTRenderer *) (intptr_t) handle;

    if (thiz->data)
        free(thiz->data);
    free(thiz);
}

jlong
JAWTRenderer_open(JNIEnv *env, jclass clazz, jobject component)
{
    JAWTRenderer *thiz;

    thiz = calloc(1, sizeof(JAWTRenderer));
    if (thiz)
    {
        thiz->eglContext = EGL_NO_CONTEXT;
        thiz->prepareOpenGL = JNI_TRUE;
    }
    return (intptr_t) thiz;
}

jboolean
JAWTRenderer_paint
    (jint version, JAWT_DrawingSurfaceInfo *dsi, jclass clazz, jlong handle,
        jobject g, jint zOrder)
{
    JAWTRenderer *thiz = (JAWTRenderer *) (intptr_t) handle;
    EGLContext eglContext;

    /*
     * The texture of this JAWTRenderer, if any, has been created in a specific
     * OpenGL context. But the current OpenGL context may be changed (e.g.
     * GLSurfaceView#onResume() is documented to "recreate the OpenGL display
     * and resume the rendering thread.") Consequently, there is a need to
     * recreate the texture of this JAWTRenderer after the current OpenGL
     * context has been changed.
     */
    if (thiz->eglContext != (eglContext = eglGetCurrentContext()))
    {
        thiz->eglContext = eglContext;
        thiz->prepareOpenGL = JNI_TRUE;
        /*
         * The documentation on GLSurfaceView.Renderer says that, when the
         * EGLContext is lost, the tex will be automatically deleted.
         */
        thiz->tex = 0;
    }

    if (JNI_TRUE == thiz->prepareOpenGL)
    {
        const char *extensions;

        thiz->prepareOpenGL = JNI_FALSE;

        /* For the purposes of debugging, log GL_EXTENSIONS. */
        if ((extensions = (const char *) glGetString(GL_EXTENSIONS)))
        {
            LOGD(
                    "%s:%d: GL_EXTENSIONS= %s\n",
                    __func__, (int) __LINE__,
                    extensions);
        }

        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDisable(GL_CULL_FACE);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    /*
     * If dataLength is positive, it means that new data has been processed and
     * the tex represents old data. Consequently, we have to reflect the new
     * data upon the tex.
     */
    if (thiz->dataLength)
    {
        jint dataHeight = thiz->dataHeight;
        jint dataWidth = thiz->dataWidth;
        jboolean texCoordPointerIsOutOfDate = JNI_FALSE;

        /*
         * If the tex will not accommodate the data, delete it. A new suitable
         * one will be initialized afterwards.
         */
        if (thiz->tex
                && ((thiz->texRealHeight < dataHeight)
                        || (thiz->texRealWidth < dataWidth)))
        {
            glDeleteTextures(1, &(thiz->tex));
            thiz->tex = 0;
        }

        if (thiz->tex)
        {
            glBindTexture(JAWT_RENDERER_TEXTURE, thiz->tex);
            glTexSubImage2D(
                    JAWT_RENDERER_TEXTURE,
                    0,
                    0, 0, dataWidth, dataHeight,
                    JAWT_RENDERER_TEXTURE_FORMAT,
                    JAWT_RENDERER_TEXTURE_TYPE,
                    thiz->data);
        }
        else
        {
            GLsizei texRealHeight, texRealWidth;

            glGenTextures(1, &(thiz->tex));
            glBindTexture(JAWT_RENDERER_TEXTURE, thiz->tex);

            //glTexParameterf(JAWT_RENDERER_TEXTURE, GL_TEXTURE_PRIORITY, 1.0);
            glTexParameteri(
                    JAWT_RENDERER_TEXTURE,
                    GL_TEXTURE_WRAP_S,
                    GL_CLAMP_TO_EDGE);
            glTexParameteri(
                    JAWT_RENDERER_TEXTURE,
                    GL_TEXTURE_WRAP_T,
                    GL_CLAMP_TO_EDGE);
            glTexParameteri(
                    JAWT_RENDERER_TEXTURE,
                    GL_TEXTURE_MAG_FILTER,
                    GL_LINEAR);
            glTexParameteri(
                    JAWT_RENDERER_TEXTURE,
                    GL_TEXTURE_MIN_FILTER,
                    GL_LINEAR);

            glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

            /*
             * OpenGL ES 2.0 should provide enough support for non-power-of-two
             * textures for the purposes of JAWTRenderer. Unfortunately, it does
             * not seem to be the case on Samsung Galaxy Tab 2 10.1"/GT-P5100.
             * Consequently, we will first try to create a non-power-of-two
             * texture and, if that fails, we will then try to create a
             * power-of-two texture.
             */

            /*
             * The lack of support for non-power-of-two textures will be
             * signaled by the error GL_INVALID_VALUE returned by the function
             * glTexImage2D. Consequently, we have to clear all errors just
             * before calling the function in question.
             */
            JAWTRenderer_checkGLError("glTexEnvf", __LINE__, GL_NO_ERROR);
            glTexImage2D(
                    JAWT_RENDERER_TEXTURE,
                    0,
                    GL_RGBA,
                    dataWidth, dataHeight,
                    0,
                    JAWT_RENDERER_TEXTURE_FORMAT,
                    JAWT_RENDERER_TEXTURE_TYPE,
                    thiz->data);
            if (GL_TRUE
                    == JAWTRenderer_checkGLError(
                            "glTexImage2D", __LINE__,
                            GL_INVALID_VALUE))
            {
                /*
                 * There is likely no support for non-power-of-two textures. Of
                 * course, if dataWidth and dataHeight are powers of two, there
                 * is a completely different problem.
                 */
                texRealHeight = JAWTRenderer_roundUpToPowerOfTwo(dataHeight);
                texRealWidth = JAWTRenderer_roundUpToPowerOfTwo(dataWidth);
                if ((dataHeight != texRealHeight)
                        || (dataWidth != texRealWidth))
                {
                    glTexImage2D(
                            JAWT_RENDERER_TEXTURE,
                            0,
                            GL_RGBA,
                            texRealWidth, texRealHeight,
                            0,
                            JAWT_RENDERER_TEXTURE_FORMAT,
                            JAWT_RENDERER_TEXTURE_TYPE,
                            0);
                    if (GL_TRUE
                            == JAWTRenderer_checkGLError(
                                    "glTexImage2D", __LINE__,
                                    GL_NO_ERROR))
                    {
                        glTexSubImage2D(
                                JAWT_RENDERER_TEXTURE,
                                0,
                                0, 0, dataWidth, dataHeight,
                                JAWT_RENDERER_TEXTURE_FORMAT,
                                JAWT_RENDERER_TEXTURE_TYPE,
                                thiz->data);
                    }
                }
            }
            else
            {
                texRealHeight = dataHeight;
                texRealWidth = dataWidth;
            }
            if (thiz->texRealHeight != texRealHeight)
            {
                thiz->texRealHeight = texRealHeight;
                texCoordPointerIsOutOfDate = JNI_TRUE;
            }
            if (thiz->texRealWidth != texRealWidth)
            {
                thiz->texRealWidth = texRealWidth;
                texCoordPointerIsOutOfDate = JNI_TRUE;
            }
        }
        if (thiz->texEffectiveHeight != thiz->dataHeight)
        {
            thiz->texEffectiveHeight = thiz->dataHeight;
            texCoordPointerIsOutOfDate = JNI_TRUE;
        }
        if (thiz->texEffectiveWidth != thiz->dataWidth)
        {
            thiz->texEffectiveWidth = thiz->dataWidth;
            texCoordPointerIsOutOfDate = JNI_TRUE;
        }

        thiz->dataLength = 0;

        /*
         * We may have just changed the effective and/or real sizes of tex. Such
         * a change affects texCoordPointer.
         */
        if (JNI_TRUE == texCoordPointerIsOutOfDate)
        {
            GLfloat *texCoordPointer = thiz->texCoordPointer;
            GLfloat x
                = (thiz->texEffectiveWidth == thiz->texRealWidth)
                    ? 1.0
                    : (thiz->texEffectiveWidth
                            / (GLfloat) (thiz->texRealWidth));
            GLfloat y
                = (thiz->texEffectiveHeight == thiz->texRealHeight)
                    ? 1.0
                    : (thiz->texEffectiveHeight
                            / (GLfloat) (thiz->texRealHeight));

            texCoordPointer[2] = x;
            texCoordPointer[4] = x;
            texCoordPointer[5] = y;
            texCoordPointer[9] = y;
            texCoordPointer[10] = x;
            texCoordPointer[11] = y;
        }
    }

    /* At long last, do paint this JAWTRenderer i.e. render the tex. */
    if (thiz->tex)
    {
        glEnable(JAWT_RENDERER_TEXTURE);

        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(2, GL_FLOAT, 0, thiz->texCoordPointer);
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(3, GL_FLOAT, 0, vertexPointer);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);

        glDisable(JAWT_RENDERER_TEXTURE);
    }

    return JNI_TRUE;
}

jboolean
JAWTRenderer_process
    (JNIEnv *env, jclass clazz, jlong handle, jobject component, jint *data,
        jint length, jint width, jint height)
{
    if (data && length)
    {
        JAWTRenderer *thiz = (JAWTRenderer *) (intptr_t) handle;
        jint *rendererData = thiz->data;
        size_t dataSize = length * sizeof(jint);

        if (!rendererData || (thiz->dataCapacity < length))
        {
            jint *newData;

            newData = realloc(rendererData, dataSize);
            if (newData)
            {
                thiz->data = rendererData = newData;
                thiz->dataCapacity = length;
            }
            else
                rendererData = NULL;
        }
        if (rendererData)
        {
            memcpy(rendererData, data, dataSize);
            thiz->dataHeight = height;
            thiz->dataLength = length;
            thiz->dataWidth = width;
        }
        else
        {
            /* We seem to have run out of memory. */
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

/**
 * Finds the least power of two that is not less than a specific integer
 * <tt>i</tt>.
 *
 * @param i an integer for which the least power of two that is not less than it
 * is to be found
 * @see http://en.wikipedia.org/wiki/Power_of_two#Algorithm_to_round_up_to_power_of_two
 */
static GLsizei
JAWTRenderer_roundUpToPowerOfTwo(GLsizei i)
{
    i--;
    i |= i >> 1;
    i |= i >> 2;
    i |= i >> 4;
    i |= i >> 8;
    i |= i >> 16;
    i++;
    return i;
}
