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
package org.jitsi.android.plugin.errorhandler;

import java.io.*;

import android.content.*;
import net.java.sip.communicator.util.*;
import org.jitsi.android.*;
import org.jitsi.service.fileaccess.*;

/**
 * The <tt>ExceptionHandler</tt> is used to catch unhandled exceptions which
 * occur on the UI <tt>Thread</tt>. Those exceptions normally cause current
 * <tt>Activity</tt> to freeze and the process usually must be killed after
 * the Application Not Responding dialog is displayed. This handler kills
 * Jitsi process at the moment when the exception occurs, so that user don't
 * have to wait for ANR dialog. It also marks in <tt>SharedPreferences</tt>
 * that such crash has occurred. Next time the Jitsi is started it will ask
 * the user if he wants to send the logs.<br/>
 *
 * Usually system restarts Jitsi and it's service automatically after the
 * process was killed. That's because the service was still bound to some
 * <tt>Activities</tt> at the moment when the exception occurred.<br/>
 *
 * The handler is bound to the <tt>Thread</tt> in every <tt>OSGiActivity</tt>.
 *
 * @author Pawel Domas
 */
public class ExceptionHandler
    implements Thread.UncaughtExceptionHandler
{
    /**
     * Parent exception handler(system default).
     */
    private final Thread.UncaughtExceptionHandler parent;

    /**
     * Creates new instance of <tt>ExceptionHandler</tt> bound to given
     * <tt>Thread</tt>.
     *
     * @param t the <tt>Thread</tt> which will be handled.
     */
    private ExceptionHandler(Thread t)
    {
        parent = t.getUncaughtExceptionHandler();

        t.setUncaughtExceptionHandler(this);
    }

    /**
     * Checks and attaches the <tt>ExceptionHandler</tt> if it hasn't been
     * bound already.
     */
    public static void checkAndAttachExceptionHandler()
    {
        Thread current = Thread.currentThread();
        if(current.getUncaughtExceptionHandler() instanceof ExceptionHandler)
        {
            return;
        }

        // Creates and binds new handler instance
        new ExceptionHandler(current);
    }

    /**
     * Marks the crash in <tt>SharedPreferences</tt> and kills the process.
     *
     * {@inheritDoc}
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        markCrashedEvent();

        parent.uncaughtException(thread, ex);

        Logger logger = Logger.getLogger(ExceptionHandler.class);
        logger.fatal("Uncaught exception occurred, killing the process...",
                       ex);

        // Save logcat for more information.
        File logFile;
        try
        {
            logFile = ExceptionHandlerActivator.getFileAccessService()
                .getPrivatePersistentFile(
                    new File("log", "jitsi-crash-logcat.txt").toString(),
                    FileCategory.LOG);

            Runtime.getRuntime().exec("logcat -f " + logFile.getAbsolutePath());
        }
        catch (Exception e)
        {
            logger.error("Couldn't save logcat file.");
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);

    }

    /**
     * Returns <tt>SharedPreferences</tt> used to mark the crash event.
     * @return <tt>SharedPreferences</tt> used to mark the crash event.
     */
    private static SharedPreferences getStorage()
    {
        return JitsiApplication.getGlobalContext()
                .getSharedPreferences("crash", Context.MODE_PRIVATE);
    }

    /**
     * Marks that the crash has occurred in <tt>SharedPreferences</tt>.
     */
    private static void markCrashedEvent()
    {
        getStorage()
                .edit()
                .putBoolean("crash", true)
                .commit();
    }

    /**
     * Returns <tt>true</tt> if Jitsi crash was detected.
     * @return <tt>true</tt> if Jitsi crash was detected.
     */
    public static boolean hasCrashed()
    {
        return getStorage().getBoolean("crash", false);
    }

    /**
     * Clears the "crashed" flag.
     */
    public static void resetCrashedStatus()
    {
        getStorage()
                .edit()
                .putBoolean("crash", false)
                .commit();
    }
}
