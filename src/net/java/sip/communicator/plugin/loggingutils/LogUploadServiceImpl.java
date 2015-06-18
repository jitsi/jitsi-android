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
package net.java.sip.communicator.plugin.loggingutils;

import android.content.*;
import android.net.*;
import android.os.*;

import org.jitsi.android.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.log.*;

import net.java.sip.communicator.util.*;

import java.io.*;
import java.util.*;

/**
 * Send/upload logs, to specified destination.
 *
 * @author Damian Minkov
 * @author Pawel Domas
 */
public class LogUploadServiceImpl
    implements LogUploadService
{
    /**
     * The logger.
     */
    private Logger logger = Logger.getLogger(
        LogUploadServiceImpl.class.getName());

    /**
     * List of log files created for sending logs purpose.
     * There is no easy way of waiting until email is sent and deleting temp log
     * file, so they are cached and removed on OSGI service stop action.
     */
    private List<File> storedLogFiles = new ArrayList<File>();

    /**
     * The path pointing to directory used to store temporary log archives.
     */
    private static final String storagePath
        = Environment.getExternalStorageDirectory().getPath()+"/jitsi-logs/";

    /**
     * Send the log files.
     * @param destinations array of destination addresses
     * @param subject the subject if available
     * @param title the title for the action, used any intermediate
     *              dialogs that need to be shown, like "Choose action:".
     */
    public void sendLogs(String[] destinations, String subject, String title)
    {
        try
        {
            File storageDir = new File(storagePath);

            if(!storageDir.exists())
                storageDir.mkdir();

            File logcatFile = null;
            try
            {
                logcatFile = LoggingUtilsActivatorEx.getFileAccessService()
                    .getPrivatePersistentFile(
                        new File("log", "jitsi-current-logcat.txt").toString(),
                        FileCategory.LOG);

                Runtime.getRuntime()
                    .exec("logcat -f " + logcatFile.getAbsolutePath());
            }
            catch (Exception e)
            {
                logger.error("Couldn't save logcat file.");
            }
System.err.println("STORAGE DIR======" + storageDir);
            File externalStorageFile
                = LogsCollector.collectLogs(storageDir, null);

             // Stores file name to remove it on service shutdown
            storedLogFiles.add(externalStorageFile);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, destinations);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.setType("application/zip");
            sendIntent.putExtra(Intent.EXTRA_STREAM,
                                Uri.fromFile(externalStorageFile));

            // we are starting this activity from context
            // that is most probably not from the current activity
            // and this flag is needed in this situation
            Intent chooserIntent =
                Intent.createChooser(sendIntent, title);
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            JitsiApplication.getGlobalContext().startActivity(chooserIntent);
        }
        catch(Exception e)
        {
            logger.error("Error sending files", e);
        }
    }

    /**
     * Frees resources allocated by this service.
     */
    public void dispose()
    {
        System.err.println("DISPOSE!!!!!!!!!!!!!");
        for(File logFile : storedLogFiles)
        {
            logFile.delete();
        }

        storedLogFiles.clear();
    }
}
