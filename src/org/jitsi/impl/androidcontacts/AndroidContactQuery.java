/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidcontacts;

import net.java.sip.communicator.service.contactsource.*;

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
     * Query string
     */
    private final String queryString;

    /**
     * Results list
     */
    private final List<SourceContact> results;

    /**
     * Creates new instance of <tt>AndroidContactQuery</tt>.
     * @param contactSource parent Android contact source.
     * @param queryString query string.
     * @param results search results list.
     */
    protected AndroidContactQuery(AndroidContactSource contactSource,
                                  String queryString,
                                  List<SourceContact> results)
    {
        super(contactSource);

        this.queryString = queryString;
        this.results = results;
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
