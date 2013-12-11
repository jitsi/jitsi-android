/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidupdate;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;

import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.update.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.resources.*;
import org.jitsi.service.version.*;

import java.io.*;
import java.util.*;

/**
 * Android update service implementation. It checks for update and schedules
 * .apk download using <tt>DownloadManager</tt>.
 *
 * @author Pawel Domas
 */
public class UpdateServiceImpl
    implements UpdateService
{
    /**
     * The logger
     */
    private final static Logger logger
        = Logger.getLogger(UpdateServiceImpl.class);

    /**
     * The name of the property which specifies the update link in the
     * configuration file.
     */
    private static final String PROP_UPDATE_LINK
        = "net.java.sip.communicator.UPDATE_LINK";

    /**
     * Apk mime type constant.
     */
    private static final String APK_MIME_TYPE
        = "application/vnd.android.package-archive";

    /**
     * Latest version string
     */
    private String latestVersion;
    /**
     * The download link
     */
    private String downloadLink;
    //private String changesLink;

    /**
     * Checks for updates.
     *
     * @param notifyAboutNewestVersion <tt>true</tt> if the user is to be
     *        notified if they have the newest version already; otherwise,
     *        <tt>false</tt>
     */
    @Override
    public void checkForUpdates(boolean notifyAboutNewestVersion)
    {
        boolean isLatest = isLatestVersion();
        logger.info("Is latest: " + isLatest);
        logger.info("Latest version: " + latestVersion);
        logger.info("Download link: " + downloadLink);
        //logger.info("Changes link: " + changesLink);

        ResourceManagementService R
            = ServiceUtils.getService(
                    UpdateActivator.bundleContext,
                    ResourceManagementService.class);

        if(!isLatest && downloadLink != null)
        {
            AndroidUtils.showAlertConfirmDialog(
                JitsiApplication.getGlobalContext(),
                R.getI18NString("plugin.updatechecker.DIALOG_TITLE"),
                R.getI18NString("plugin.updatechecker.DIALOG_MESSAGE",
                    new String[]{R.getI18NString("app.name")}),
                R.getI18NString("plugin.updatechecker.BUTTON_DOWNLOAD"),
                new DialogActivity.DialogListener()
                {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog)
                    {
                        downloadApk();
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogActivity dialog){ }
                }
            );
        }
        else if(notifyAboutNewestVersion)
        {
            // Notify that running version is up to date
            AndroidUtils.showAlertDialog(
                JitsiApplication.getGlobalContext(),
                R.getI18NString("plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                R.getI18NString("plugin.updatechecker.DIALOG_NOUPDATE"));
        }
    }

    /**
     * Schedules .apk download.
     */
    private void downloadApk()
    {
        Uri uri = Uri.parse(downloadLink);
        String fileName = uri.getLastPathSegment();

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(
            DownloadManager
                .Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType(APK_MIME_TYPE);
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager)
            JitsiApplication.getGlobalContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        /*JitsiApplication.getGlobalContext()
            .registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context,
                                  Intent intent)
            {
                Uri fileUri
                    = downloadManager.getUriForDownloadedFile(jobId);
                String mime
                    = downloadManager.getMimeTypeForDownloadedFile(jobId);
                logger.info("Completed: "+fileUri+" mime: "+mime);
                JitsiApplication.getGlobalContext().unregisterReceiver(this);

                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(fileUri, APK_MIME_TYPE");
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                JitsiApplication.getGlobalContext().startActivity(install);
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));*/
    }

    /**
     * Gets the current (software) version.
     *
     * @return the current (software) version
     */
    private static Version getCurrentVersion()
    {
        return getVersionService().getCurrentVersion();
    }

    /**
     * Returns the currently registered instance of version service.
     * @return the current version service.
     */
    private static VersionService getVersionService()
    {
        return ServiceUtils.getService(
            UpdateActivator.bundleContext,
            VersionService.class);
    }

    /**
     * Determines whether we are currently running the latest version.
     *
     * @return <tt>true</tt> if we are currently running the latest version;
     * otherwise, <tt>false</tt>
     */
    private boolean isLatestVersion()
    {
        try
        {
            String updateLink
                = UpdateActivator.getConfiguration().getString(
                    PROP_UPDATE_LINK);

            if(updateLink == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "Updates are disabled, faking latest version.");
            }
            else
            {
                HttpUtils.HTTPResponseResult res
                    = HttpUtils.openURLConnection(updateLink);

                if (res != null)
                {
                    InputStream in = null;
                    Properties props = new Properties();

                    try
                    {
                        in = res.getContent();
                        props.load(in);
                    }
                    finally
                    {
                        if(in != null)
                            in.close();
                    }

                    latestVersion = props.getProperty("last_version");
                    downloadLink = props.getProperty("download_link");

                    /*changesLink
                        = updateLink.substring(
                        0,
                        updateLink.lastIndexOf("/") + 1)
                        + props.getProperty("changes_html");*/

                    try
                    {
                        VersionService versionService = getVersionService();

                        Version latestVersionObj =
                            versionService.parseVersionString(latestVersion);

                        if(latestVersionObj != null)
                            return latestVersionObj.compareTo(
                                getCurrentVersion()) <= 0;
                        else
                            logger.error(
                                "Version obj not parsed("+latestVersion+")");
                    }
                    catch(Throwable t)
                    {
                        logger.error("Error parsing version string", t);
                    }

                    // fallback to lexicographically compare
                    // of version strings in case of an error
                    return latestVersion.compareTo(
                        getCurrentVersion().toString()) <= 0;
                }
            }
        }
        catch (Exception e)
        {
            logger.warn(
                "Could not retrieve latest version or compare it to current"
                    + " version", e);
        }
        return true;
    }
}
