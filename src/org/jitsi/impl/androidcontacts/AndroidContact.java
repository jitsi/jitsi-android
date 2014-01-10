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
import android.provider.*;

import net.java.sip.communicator.service.contactsource.*;
import org.jitsi.android.*;

import java.util.*;

/**
 * Android source contact class.
 *
 * @author Pawel Domas
 */
public class AndroidContact
    extends GenericSourceContact
{
    private final long id;

    private final String lookUpKey;

    private final String thumbnailUri;

    private final String photoUri;

    private final String photoId;

    public AndroidContact(ContactSourceService contactSource,
                          long id, String loopUpKey, String displayName,
                          String thumbnail, String photoUri,
                          String photoId)
    {
        super(contactSource, displayName, new ArrayList<ContactDetail>());

        this.id = id;
        this.lookUpKey = loopUpKey;
        this.thumbnailUri = thumbnail;
        this.photoUri = photoUri;
        this.photoId = photoId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContactAddress()
    {
        // Return the phone number
        return getDisplayDetails();

        /*String address = super.getContactAddress();
        if(address == null)
        {
            Context ctx = JitsiApplication.getGlobalContext();

            Cursor result = ctx.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Email.DATA},
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID+"=?",
                    new String[]{String.valueOf(id)},null);

            if(result.moveToNext())
            {
                int adrIdx = result.getColumnIndex(
                        ContactsContract.CommonDataKinds.Email.DATA);
                address = result.getString(adrIdx);
                setContactAddress(address);
            }
            result.close();
        }
        return address;*/
    }

    @Override
    public String getDisplayDetails()
    {
        String details = super.getDisplayDetails();
        if(details == null)
        {
            Context ctx = JitsiApplication.getGlobalContext();

            Cursor result = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DATA},
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY+"=?",
                new String[]{String.valueOf(lookUpKey)},null);

            if(result.moveToNext())
            {
                int adrIdx = result.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DATA);
                details = result.getString(adrIdx);
                setDisplayDetails(details);
            }
            result.close();
        }
        return details;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getImage()
    {
        byte[] image = super.getImage();

        if(image == null)
        {
            Context ctx = JitsiApplication.getGlobalContext();

            Uri contactUri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI, id);

            Uri photoUri = Uri.withAppendedPath(
                    contactUri,
                    ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

            Cursor cursor = ctx.getContentResolver()
                    .query(photoUri,
                           new String[] {ContactsContract.Contacts.Photo.PHOTO},
                           null, null, null);

            if (cursor == null)
                return null;

            try
            {
                if (cursor.moveToFirst())
                {
                    image = cursor.getBlob(0);
                    setImage(image);
                }
            }
            finally
            {
                cursor.close();
            }
            /*if (thumbnailUri != null) {
                Uri uri = Uri.parse(thumbnailUri);
                System.out.println(uri);
                try
                {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            ctx.getContentResolver(), uri);
                    System.err.println("BITMAP: " + bitmap);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

            }*/
        }
        return image;
    }
}
