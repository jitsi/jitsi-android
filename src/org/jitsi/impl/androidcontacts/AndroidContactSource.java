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
    public ContactQuery createContactQuery(Pattern queryPattern)
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
    public ContactQuery createContactQuery(String query)
    {
        return createContactQuery(
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
    public ContactQuery createContactQuery(String query, int contactCount)
    {
        return createContactQuery(
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
