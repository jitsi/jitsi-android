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
package org.jitsi.impl.androidupdate;

import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;

import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.update.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.*;
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
     * <tt>SharedPreferences</tt> used to store download ids.
     */
    private SharedPreferences store;

    /**
     * Name of <tt>SharedPreferences</tt> entry used to store old download ids.
     * Ids are stored in single string separated by ",".
     */
    private static final String ENTRY_NAME = "apk_ids";

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
            // Check old or scheduled downloads
            List<Long> previousDownloads = getOldDownloads();
            if(previousDownloads.size() > 0)
            {
                long lastDownload
                    = previousDownloads.get(previousDownloads.size()-1);

                int lastJobStatus = checkDownloadStatus(lastDownload);
                if(lastJobStatus == DownloadManager.STATUS_SUCCESSFUL)
                {
                    // Ask the use if he wants to install
                    askInstallDownloadedApk(lastDownload);
                    return;
                }
                else if(lastJobStatus != DownloadManager.STATUS_FAILED)
                {
                    // Download is in progress or scheduled for retry
                    AndroidUtils.showAlertDialog(
                        JitsiApplication.getGlobalContext(),
                        R.getI18NString(
                            "plugin.updatechecker.DIALOG_IN_PROGRESS_TITLE"),
                        R.getI18NString(
                            "plugin.updatechecker.DIALOG_IN_PROGRESS"));
                    return;
                }
            }

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
     * Asks the user whether to install downloaded .apk.
     * @param apkDownloadId download id of the apk to install.
     */
    private void askInstallDownloadedApk(final long apkDownloadId)
    {
        AndroidUtils.showAlertConfirmDialog(
            JitsiApplication.getGlobalContext(),
            JitsiApplication.getResString(
                R.string.plugin_updatechecker_DIALOG_DOWNLOADED_TITLE),
            JitsiApplication.getResString(
                R.string.plugin_updatechecker_DIALOG_DOWNLOADED),
            JitsiApplication.getResString(
                R.string.plugin_updatechecker_BUTTON_INSTALL),
            new DialogActivity.DialogListener()
            {

                @Override
                public boolean onConfirmClicked(DialogActivity dialog)
                {
                    DownloadManager downloadManager
                        = JitsiApplication.getDownloadManager();
                    Uri fileUri = downloadManager
                        .getUriForDownloadedFile(apkDownloadId);

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, APK_MIME_TYPE);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    JitsiApplication.getGlobalContext().startActivity(intent);

                    return true;
                }

                @Override
                public void onDialogCancelled(DialogActivity dialog){ }
            }
        );
    }

    /**
     * Queries the <tt>DownloadManager</tt> for the status of download job
     * identified by given <tt>id</tt>.
     * @param id download identifier which status will be returned.
     * @return download status of the job identified by given id. If given job
     * is not found {@link DownloadManager#STATUS_FAILED} will be returned.
     */
    private int checkDownloadStatus(long id)
    {
        DownloadManager downloadManager
            = JitsiApplication.getDownloadManager();

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        Cursor cursor = downloadManager.query(query);
        try
        {
            if(!cursor.moveToFirst())
                return DownloadManager.STATUS_FAILED;
            else
                return cursor.getInt(
                    cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        }
        finally
        {
            cursor.close();
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

        DownloadManager downloadManager
            = JitsiApplication.getDownloadManager();

        long jobId = downloadManager.enqueue(request);

        rememberDownloadId(jobId);
    }

    private void rememberDownloadId(long id)
    {
        SharedPreferences store = getStore();

        String storeStr = store.getString(ENTRY_NAME, "");
        storeStr += id + ",";

        store.edit().putString(ENTRY_NAME, storeStr).commit();
    }

    private SharedPreferences getStore()
    {
        if(store == null)
        {
            store
                = JitsiApplication.getGlobalContext()
                    .getSharedPreferences("store", Context.MODE_PRIVATE);
        }
        return store;
    }

    private List<Long> getOldDownloads()
    {
        String storeStr = getStore().getString(ENTRY_NAME, "");
        String[] idStrs = storeStr.split(",");
        List<Long> apkIds = new ArrayList<Long>(idStrs.length);
        for(String idStr : idStrs)
        {
            try
            {
                if(!idStr.isEmpty())
                    apkIds.add(Long.parseLong(idStr));
            }
            catch (NumberFormatException e)
            {
                logger.error(
                    "Error parsing apk id for string: " + idStr
                        + " [" + storeStr + "]");
            }
        }
        return apkIds;
    }

    /**
     * Removes old downloads.
     */
    void removeOldDownloads()
    {
        List<Long> apkIds = getOldDownloads();

        DownloadManager downloadManager
            = JitsiApplication.getDownloadManager();
        for(long id : apkIds)
        {
            logger.debug("Removing .apk for id "+id);
            downloadManager.remove(id);
        }

        getStore().edit().remove(ENTRY_NAME).commit();
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
