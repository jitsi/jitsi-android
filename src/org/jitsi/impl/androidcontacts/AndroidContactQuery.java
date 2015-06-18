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
package org.jitsi.impl.androidcontacts;

import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import net.java.sip.communicator.service.contactsource.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;

import java.util.*;

/**
 * Android contact query.
 *
 * @author Pawel Domas
 */
public class AndroidContactQuery
    extends AbstractContactQuery<AndroidContactSource>
{
    /**
     * Key for display name varies on Android versions
     */
    private final static String DISPLAY_NAME_COLUMN
        = AndroidUtils.hasAPI(Build.VERSION_CODES.HONEYCOMB)
            ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            : ContactsContract.Contacts.DISPLAY_NAME;

    /**
     * Selection query
     */
    private static final String SELECTION
        = DISPLAY_NAME_COLUMN+ " LIKE ?"
            + " AND " +ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0";

    /**
     * List of projection columns that will be returned
     */
    private static final String[] PROJECTION =
        {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            DISPLAY_NAME_COLUMN,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.PHOTO_ID
        };

    /**
     * The uri that will be user for queries.
     */
    private static final Uri CONTACTS_URI
        = ContactsContract.Contacts.CONTENT_URI;


    /**
     * Query string
     */
    private final String queryString;

    /**
     * Results list
     */
    private final List<SourceContact> results = new ArrayList<SourceContact>();

    /**
     * The thread that runs the query
     */
    private Thread queryThread;

    /**
     * Flag used to cancel the query thread
     */
    private boolean cancel = false;

    //TODO: implement cancel, on API >= 16
    //private CancellationSignal cancelSignal = new CancellationSignal();


    /**
     * Creates new instance of <tt>AndroidContactQuery</tt>.
     * @param contactSource parent Android contact source.
     * @param queryString query string.
     */
    protected AndroidContactQuery(AndroidContactSource contactSource,
                                  String queryString)
    {
        super(contactSource);

        this.queryString = queryString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
    {
        if(queryThread != null)
            return;

        queryThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                doQuery();
            }
        });
        queryThread.start();
    }

    /**
     * Executes the query.
     */
    private void doQuery()
    {
        Context ctx = JitsiApplication.getGlobalContext();
        Cursor rCursor = ctx.getContentResolver()
            .query(CONTACTS_URI,
                   PROJECTION,
                   SELECTION,
                   new String[]{queryString}, null);
                   //new CancellationSignal());

        try
        {
            if(cancel)
                return;

            // Get projection column ids
            int ID = rCursor.getColumnIndex(ContactsContract.Contacts._ID);
            int LOOP_UP = rCursor.getColumnIndex(
                ContactsContract.Contacts.LOOKUP_KEY);
            int DISPLAY_NAME = rCursor.getColumnIndex(
                DISPLAY_NAME_COLUMN);
            //int HAS_PHONE = rCursor.getColumnIndex(
            //    ContactsContract.Contacts.HAS_PHONE_NUMBER);
            int THUMBNAIL_URI = rCursor.getColumnIndex(
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
            int PHOTO_URI = rCursor.getColumnIndex(
                ContactsContract.Contacts.PHOTO_URI);
            int PHOTO_ID = rCursor.getColumnIndex(
                ContactsContract.Contacts.PHOTO_ID);

            // Create results
            while(rCursor.moveToNext())
            {
                if(cancel)
                    break;

                long id = rCursor.getLong(ID);
                String lookUp = rCursor.getString(LOOP_UP);
                String displayName = rCursor.getString(DISPLAY_NAME);
                String thumbnail = rCursor.getString(THUMBNAIL_URI);
                String photoUri = rCursor.getString(PHOTO_URI);
                String photoId = rCursor.getString(PHOTO_ID);

                // Loop on all phones
                Cursor result = null;
                try
                {
                    result = ctx.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DATA},
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY+"=?",
                    new String[]{String.valueOf(lookUp)},null);

                    while (result.moveToNext() && !cancel)
                    {
                        int adrIdx = result.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DATA);
                        String phone = result.getString(adrIdx);
                        results.add(
                            new AndroidContact( getContactSource(),
                                                id, lookUp,
                                                displayName,
                                                thumbnail,
                                                photoUri,
                                                photoId,
                                                phone) );
                    }
                }
                finally
                {
                    if(result != null)
                        result.close();
                }
            }

            if(!cancel)
            {
                setStatus(ContactQuery.QUERY_COMPLETED);
            }
        }
        finally
        {
            rCursor.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel()
    {
        cancel = true;
        try
        {
            queryThread.join();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        super.cancel();
    }

    /**
     * Returns the query string, this query was created for.
     * @return the query string, this query was created for
     */
    @Override
    public String getQueryString()
    {
        return queryString;
    }

    /**
     * Returns the list of <tt>SourceContact</tt>s returned by this query.
     * @return the list of <tt>SourceContact</tt>s returned by this query
     */
    @Override
    public List<SourceContact> getQueryResults()
    {
        return results;
    }
}
