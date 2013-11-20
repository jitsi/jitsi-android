/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device.util;

import android.opengl.*;
import net.java.sip.communicator.util.*;

/**
 * Code for EGL context handling
 */
public class OpenGLContext
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(OpenGLContext.class);

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

    /**
     * Prepares EGL.
     * We want a GLES 2.0 context and a surface that supports recording.
     */
    public OpenGLContext(boolean recorder, Object window,
                         EGLContext sharedContext)
    {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY)
        {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] majorVersion = new int[1];
        int[] minorVersion = new int[1];
        if (!EGL14.eglInitialize(eglDisplay, majorVersion, 0, minorVersion, 0))
        {
            throw new RuntimeException("unable to initialize EGL14");
        }
        logger.info("EGL version: "+majorVersion[0]+"."+minorVersion[0]);


        EGLConfig config = chooseEglConfig(eglDisplay, recorder);

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        eglContext = EGL14.eglCreateContext(eglDisplay, config,
                                             sharedContext,
                                             attrib_list, 0);
        checkEglError("eglCreateContext");

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config,
                                                   window,
                                                   surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
    }

    private EGLConfig chooseEglConfig(EGLDisplay eglDisplay, boolean recorder)
    {
        EGLConfig[] configs = new EGLConfig[1];
        int[] attribList;
        if(recorder)
        {
            // Configure EGL for recording and OpenGL ES 2.0.
            attribList= new int[]
                    {
                            EGL14.EGL_RED_SIZE, 8,
                            EGL14.EGL_GREEN_SIZE, 8,
                            EGL14.EGL_BLUE_SIZE, 8,
                            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                            EGL_RECORDABLE_ANDROID, 1,
                            EGL14.EGL_NONE
                    };
        }
        else
        {
            // Configure EGL for OpenGL ES 2.0 only.
            attribList= new int[]
                    {
                            EGL14.EGL_RED_SIZE, 8,
                            EGL14.EGL_GREEN_SIZE, 8,
                            EGL14.EGL_BLUE_SIZE, 8,
                            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                            EGL14.EGL_NONE
                    };
        }
        int[] numconfigs = new int[1];

        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0,
                                   configs, 0, configs.length,
                                   numconfigs, 0))
        {
            throw new IllegalArgumentException(
                    "eglChooseConfig failed "
                            + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        else if (numconfigs[0] <= 0)
        {
            throw new IllegalArgumentException(
                    "eglChooseConfig failed "
                            + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        return configs[0];
    }

    /**
     * Discards all resources held by this class, notably the EGL context.
     * Also releases the Surface that was passed to our constructor.
     */
    public void release()
    {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY)
        {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE,
                                 EGL14.EGL_NO_SURFACE,
                                 EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(eglDisplay);
        }

        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglContext = EGL14.EGL_NO_CONTEXT;
        eglSurface = EGL14.EGL_NO_SURFACE;
    }

    public void ensureIsCurrentCtx()
    {
        EGLContext ctx = EGL14.eglGetCurrentContext();
        EGLSurface surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        if (!eglContext.equals(ctx) || !eglSurface.equals(surface))
        {
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface,
                                      eglContext))
            {
                throw new RuntimeException(
                        "eglMakeCurrent failed "
                            + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            }
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    public void swapBuffers()
    {
        if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface))
        {
            throw new RuntimeException("Cannot swap buffers");
        }
        checkEglError("opSwapBuffers");
    }

    /**
     * Sends the presentation time stamp to EGL.
     * Time is expressed in nanoseconds.
     */
    public void setPresentationTime(long nsecs)
    {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }

    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    private void checkEglError(String msg)
    {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS)
        {
            throw new RuntimeException(
                    msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    public EGLContext getContext()
    {
        return eglContext;
    }
}
