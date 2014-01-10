/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidcontacts;

import net.java.sip.communicator.service.contactsource.*;

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
     * Queries this search source for the given <tt>searchPattern</tt>.
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    @Override
    public ContactQuery queryContactSource(Pattern queryPattern)
    {
        return new AndroidContactQuery(this,
                                       "%"+queryPattern.toString()+"%");
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
        return "Phone book";
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
