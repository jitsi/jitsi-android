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
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.util.*;

import org.jitsi.impl.configuration.*;
import org.jitsi.service.osgi.*;
import net.java.sip.communicator.util.*;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;

/**
 * Implements a <tt>ConfigurationStore</tt> which stores property name-value
 * associations in an SQLite database.
 *
 * @author Lyubomir Marinov
 */
public class SQLiteConfigurationStore
    extends DatabaseConfigurationStore
{
    private static final String NAME_COLUMN_NAME = "Name";

    private static final String TABLE_NAME = "Properties";

    private static final String VALUE_COLUMN_NAME = "Value";

    private final SQLiteOpenHelper openHelper;

    /**
     * Initializes a new <tt>SQLiteConfigurationStore</tt> instance.
     */
    public SQLiteConfigurationStore()
    {
        Context context
            = ServiceUtils.getService(
                    ConfigurationActivator.getBundleContext(),
                    OSGiService.class);

        openHelper
            = new SQLiteOpenHelper(
                    context,
                    SQLiteConfigurationStore.class.getName() + ".db",
                    null /* factory */,
                    1 /* version */)
            {
                public void onCreate(SQLiteDatabase db)
                {
                    db.execSQL(
                            "CREATE TABLE " + TABLE_NAME + " ("
                                + NAME_COLUMN_NAME + " TEXT PRIMARY KEY,"
                                + VALUE_COLUMN_NAME + " TEXT"
                                + ");");
                }

                public void onUpgrade(
                        SQLiteDatabase db,
                        int oldVersion, int newVersion)
                {
                    // TODO Auto-generated method stub
                }
            };
    }

    /**
     * Overrides {@link HashtableConfigurationStore#getProperty(String)}. If
     * this <tt>ConfigurationStore</tt> contains a value associated with the
     * specified property name, returns it. Otherwise, searches for a system
     * property with the specified name and returns its value.
     *
     * @param name the name of the property to get the value of
     * @return the value in this <tt>ConfigurationStore</tt> of the property
     * with the specified name; <tt>null</tt> if the property with the specified
     * name does not have an association with a value in this
     * <tt>ConfigurationStore</tt>
     * @see ConfigurationStore#getProperty(String)
     */
    @Override
    public Object getProperty(String name)
    {
        Object value = properties.get(name);

        if (value == null)
        {
            synchronized (openHelper)
            {
                SQLiteDatabase db = openHelper.getReadableDatabase();
                Cursor cursor
                    = db.query(
                        TABLE_NAME,
                        new String[] { VALUE_COLUMN_NAME },
                        NAME_COLUMN_NAME + " = ?",
                        new String[] { name },
                        null /* groupBy */,
                        null /* having */,
                        null /* orderBy */,
                        "1");

                try
                {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst())
                        value = cursor.getString(0);
                }
                finally
                {
                    cursor.close();
                }
            }

            if (value == null)
                value = System.getProperty(name);
        }
        return value;
    }

    /**
     * Overrides {@link HashtableConfigurationStore#getPropertyNames()}. Gets
     * the names of the properties which have values associated in this
     * <tt>ConfigurationStore</tt>.
     *
     * @return an array of <tt>String</tt>s which specify the names of the
     * properties that have values associated in this
     * <tt>ConfigurationStore</tt>; an empty array if this instance contains no
     * property values
     * @see ConfigurationStore#getPropertyNames()
     */
    @Override
    public String[] getPropertyNames()
    {
        List<String> propertyNames = new ArrayList<String>();

        synchronized (openHelper)
        {
            SQLiteDatabase db = openHelper.getReadableDatabase();
            Cursor cursor
                = db.query(
                        TABLE_NAME,
                        new String[] { NAME_COLUMN_NAME },
                        null /* selection */,
                        null /* selectionArgs */,
                        null /* groupBy */,
                        null /* having */,
                        NAME_COLUMN_NAME + " ASC");

            try
            {
                while (cursor.moveToNext())
                    propertyNames.add(cursor.getString(0));
            }
            finally
            {
                cursor.close();
            }
        }

        return propertyNames.toArray(new String[propertyNames.size()]);
    }

    /**
     * Removes all property name-value associations currently present in this
     * <tt>ConfigurationStore</tt> instance and deserializes new property
     * name-value associations from its underlying database (storage).
     *
     * @throws IOException if there is an input error while reading from the
     * underlying database (storage)
     */
    protected void reloadConfiguration()
        throws IOException
    {
        // TODO Auto-generated method stub
    }

    /**
     * Overrides {@link HashtableConfigurationStore#removeProperty(String)}.
     * Removes the value association in this <tt>ConfigurationStore</tt> of the
     * property with a specific name. If the property with the specified name is
     * not associated with a value in this <tt>ConfigurationStore</tt>, does
     * nothing.
     *
     * @param name the name of the property which is to have its value
     * association in this <tt>ConfigurationStore</tt> removed
     * @see ConfigurationStore#removeProperty(String)
     */
    public void removeProperty(String name)
    {
        super.removeProperty(name);

        synchronized (openHelper)
        {
            SQLiteDatabase db = openHelper.getWritableDatabase();

            db.delete(
                    TABLE_NAME,
                    NAME_COLUMN_NAME + " = ?",
                    new String[] { name });
        }
    }

    /**
     * Overrides
     * {@link HashtableConfigurationStore#setNonSystemProperty(String, Object)}.
     *
     * @param name the name of the non-system property to be set to the
     * specified value in this <tt>ConfigurationStore</tt>
     * @param value the value to be assigned to the non-system property with the
     * specified name in this <tt>ConfigurationStore</tt>
     * @see ConfigurationStore#setNonSystemProperty(String, Object)
     */
    @Override
    public void setNonSystemProperty(String name, Object value)
    {
        synchronized (openHelper)
        {
            ContentValues initialValues = new ContentValues();

            initialValues.put(NAME_COLUMN_NAME, name);
            initialValues.put(VALUE_COLUMN_NAME, value.toString());

            SQLiteDatabase db = openHelper.getWritableDatabase();

            if (db.replace(TABLE_NAME, null /* nullColumnHack */, initialValues)
                    == -1)
                throw new RuntimeException("Failed to set non-system property");
        }

        super.setNonSystemProperty(name, value);
    }
}
