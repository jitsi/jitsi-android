/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
import java.util.regex.*;

/**
 * Android contact source implementation.
 *
 * @author Pawel Domas
 */
public class AndroidContactSource
    implements ExtendedContactSourceService
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
    private static final String SELECTION = DISPLAY_NAME_COLUMN+ " LIKE ?";

    /**
     * List of projection columns that will be returned
     */
    private static final String[] PROJECTION =
            {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    DISPLAY_NAME_COLUMN,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER,
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
     * Queries this search source for the given <tt>searchPattern</tt>.
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    @Override
    public ContactQuery queryContactSource(Pattern queryPattern)
    {
        String patternStr = "%"+queryPattern.toString()+"%";

        Context ctx = JitsiApplication.getGlobalContext();
        Cursor rCursor = ctx.getContentResolver()
                .query(CONTACTS_URI,
                       PROJECTION,
                       SELECTION,
                       new String[]{patternStr}, null);

        // Get projection column ids
        int ID = rCursor.getColumnIndex(ContactsContract.Contacts._ID);
        int LOOP_UP = rCursor.getColumnIndex(
                ContactsContract.Contacts.LOOKUP_KEY);
        int DISPLAY_NAME = rCursor.getColumnIndex(
                DISPLAY_NAME_COLUMN);
        int HAS_PHONE = rCursor.getColumnIndex(
                ContactsContract.Contacts.HAS_PHONE_NUMBER);
        int THUMBNAIL_URI = rCursor.getColumnIndex(
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
        int PHOTO_URI = rCursor.getColumnIndex(
                ContactsContract.Contacts.PHOTO_URI);
        int PHOTO_ID = rCursor.getColumnIndex(
                ContactsContract.Contacts.PHOTO_ID);

        // Create results
        List<SourceContact> results = new ArrayList<SourceContact>();
        while(rCursor.moveToNext())
        {
            long id = rCursor.getLong(ID);
            String lookUp = rCursor.getString(LOOP_UP);
            String displayName = rCursor.getString(DISPLAY_NAME);
            boolean hasPhone = rCursor.getInt(HAS_PHONE) == 1;
            String thumbnail = rCursor.getString(THUMBNAIL_URI);
            String photoUri = rCursor.getString(PHOTO_URI);
            String photoId = rCursor.getString(PHOTO_ID);

            results.add(new AndroidContact(this, id, lookUp,
                                           displayName,
                                           hasPhone,
                                           thumbnail,
                                           photoUri,
                                           photoId));
        }
        rCursor.close();

        return new AndroidContactQuery(this, patternStr, results);
    }

    /**
     * Queries this search source for the given <tt>query</tt>.
     *
     * @param query the string to search for
     * @return the created query
     */
    @Override
    public ContactQuery queryContactSource(String query)
    {
        return queryContactSource(
            Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
    }

    /**
     * Queries this search source for the given <tt>query</tt>.
     *
     * @param query the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    @Override
    public ContactQuery queryContactSource(String query, int contactCount)
    {
        return queryContactSource(
            Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
    }

    /**
     * Returns the global phone number prefix to be used when calling contacts
     * from this contact source.
     *
     * @return the global phone number prefix
     */
    @Override
    public String getPhoneNumberPrefix()
    {
        return null;
    }

    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    @Override
    public int getType()
    {
        return SEARCH_TYPE;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    @Override
    public String getDisplayName()
    {
        return "Android contacts";
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    @Override
    public int getIndex()
    {
        return -1;
    }
}
