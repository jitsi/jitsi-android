/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import net.java.sip.communicator.impl.configuration.SQLiteConfigurationStore;
import net.java.sip.communicator.util.*;
import org.jitsi.android.gui.*;

/**
 * Called when the Android boot process finished and Jitsi can be started.
 */
public class BootReceiver
    extends BroadcastReceiver
{
    /**
     * The logger.
     */
    private static final Logger logger
            = Logger.getLogger(BootReceiver.class);

    /**
     * Name of config property that indicates whether Jitsi should be started
     * on boot.
     */
    public static final String START_ON_BOOT_PROPERTY
        = "org.jitsi.android.start_on_boot";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean start = false;

        /*
        The following code determines whether Jitsi should be started by
        directly examining the SQLite-Database in which the config ist stored.
        It's an ugly hack, but it gets the job done.
        Better suggestions are appreciated.
         */
        SQLiteOpenHelper openHelper = new SQLiteOpenHelper(
                context,
                SQLiteConfigurationStore.class.getName() + ".db",
                null,
                1
        )
        {
            @Override
            public void onCreate(SQLiteDatabase db) {}

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
        };

        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor
                = db.query(
                "Properties",
                new String[] { "Value" },
                "Name = ?",
                new String[] { START_ON_BOOT_PROPERTY },
                null,
                null,
                null,
                "1");

        try
        {
            if ((cursor.getCount() == 1) && cursor.moveToFirst())
                start = Boolean.parseBoolean(cursor.getString(0));
        }
        finally
        {
            cursor.close();
        }
        db.close();
        openHelper.close();


        if (logger.isDebugEnabled())
        {
            logger.debug("Will start Jitsi: " + start);
        }

        if (start)
        {
            Intent i = new Intent(context, LauncherActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

}
