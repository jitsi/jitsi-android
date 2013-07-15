/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.loggingutils;

import android.content.*;
import android.net.*;
import org.jitsi.android.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.log.*;
import org.jitsi.service.osgi.*;
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
    private List<String> storedLogFiles = new ArrayList<String>();

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

            File tempLogArchive
                = LogsCollector.collectLogs(fs.getTemporaryDirectory(), null);

            // Copies the archive to world readable file
            String worldReadableFname = tempLogArchive.getName();
            Context ctx = JitsiApplication.getGlobalContext();
            FileOutputStream fout
                    = ctx.openFileOutput( worldReadableFname,
                                          Context.MODE_WORLD_READABLE );
            FileInputStream fin = new FileInputStream(tempLogArchive);
            byte[] buffer = new byte[2048];
            int read;
            while(true)
            {
                read = fin.read(buffer);
                if(read > 0)
                    fout.write(buffer, 0, read);
                else
                    break;
            }
            fin.close();
            // Removes temp logs archive
            tempLogArchive.delete();
            // Stores file name to remove it on service shutdown
            storedLogFiles.add(worldReadableFname);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, destinations);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.setType("application/zip");
            sendIntent.putExtra(Intent.EXTRA_STREAM,
                    Uri.fromFile(ctx.getFileStreamPath(worldReadableFname)));

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
        Context ctx = JitsiApplication.getGlobalContext();
        for(String logFileName : storedLogFiles)
        {
            ctx.deleteFile(logFileName);
        }
    }
}
