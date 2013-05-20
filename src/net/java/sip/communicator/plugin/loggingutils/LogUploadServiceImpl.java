/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.loggingutils;

import android.content.*;
import android.net.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.log.*;
import org.jitsi.service.osgi.*;
import net.java.sip.communicator.util.*;

import java.io.*;

/**
 * Send/upload logs, to specified destination.
 *
 * @author Damian Minkov
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
            FileAccessService fs = ServiceUtils.getService(
                LoggingUtilsActivatorEx.bundleContext,
                FileAccessService.class);

            File tempFolder = fs.getTemporaryDirectory();

            File tempLogArchive = LogsCollector.collectLogs(tempFolder, null);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, destinations);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.setType("application/zip");
            sendIntent.putExtra(Intent.EXTRA_STREAM,
                Uri.fromFile(tempLogArchive));

            // we are starting this activity from context
            // that is most probably not from the current activity
            // and this flag is needed in this situation
            Intent chooserIntent =
                Intent.createChooser(sendIntent, title);
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Context context = ServiceUtils.getService(
                LoggingUtilsActivatorEx.bundleContext,
                OSGiService.class);

            context.startActivity(chooserIntent);
        }
        catch(Throwable t)
        {
            logger.error("Error sending files", t);
        }
    }
}
