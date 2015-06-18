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
package org.jitsi.android.gui.contactlist.model;

import android.os.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.util.*;

import org.jitsi.android.gui.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.service.osgi.*;

import org.osgi.framework.*;

import java.util.*;

/**
 * Class implements adapter that can be used to search contact sources and the
 * contact list. Meta contact list is a base for this adapter and queries
 * returned from contact sources are appended as next contact groups.
 *
 * @author Pawel Domas
 */
public class QueryContactListAdapter
    extends BaseContactListAdapter
    implements UIGroupRenderer,
               ContactQueryListener
{
    /**
     * The logger
     */
    private final Logger logger
        = Logger.getLogger(QueryContactListAdapter.class);

    /**
     * Handler used to execute stuff on UI thread.
     */
    private Handler uiHandler = OSGiActivity.uiHandler;

    /**
     * The meta contact list used as a base contact source.
     * It is capable of filtering contacts itself without queries.
     */
    private MetaContactListAdapter metaContactList;

    /**
     * List of contact sources of type {@link ContactSourceService#SEARCH_TYPE}.
     */
    private List<ContactSourceService> sources;

    /**
     * List of results groups. Each group corresponds to results from one
     * contact source.
     */
    private List<ResultGroup> results = new ArrayList<ResultGroup>();

    /**
     * List of queries currently handled.
     */
    private List<ContactQuery> queries = new ArrayList<ContactQuery>();

    /**
     * Creates new instance of <tt>QueryContactListAdapter</tt>.
     * @param fragment parent fragment.
     * @param contactListModel meta contact list model used as a base data model
     */
    public QueryContactListAdapter(ContactListFragment fragment,
                                   MetaContactListAdapter contactListModel)
    {
        super(fragment);

        this.metaContactList = contactListModel;
    }

    private List<ContactSourceService> getSources()
    {
        ServiceReference<?>[] sources
            = ServiceUtils.getServiceReferences(
                AndroidGUIActivator.bundleContext,
                ContactSourceService.class);

        List<ContactSourceService> list = new ArrayList<ContactSourceService>(sources.length);
        for(ServiceReference ref : sources)
        {
            ContactSourceService css = (ContactSourceService)
                AndroidGUIActivator.bundleContext.getService(ref);

            if(css.getType() == ContactSourceService.SEARCH_TYPE)
            {
                list.add(css);
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    public void initModelData()
    {
        this.sources = getSources();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
    {
        super.dispose();

        cancelQueries();
    }

    @Override
    public int getGroupCount()
    {
        return metaContactList.getGroupCount() + results.size();
    }

    @Override
    public Object getGroup(int position)
    {
        int metaGroupCount = metaContactList.getGroupCount();
        if(position < metaGroupCount)
        {
            return metaContactList.getGroup(position);
        }
        else
        {
            return results.get(position-metaGroupCount);
        }
    }

    @Override
    public UIGroupRenderer getGroupRenderer(int groupPosition)
    {
        if(groupPosition < metaContactList.getGroupCount())
        {
            return metaContactList.getGroupRenderer(groupPosition);
        }
        else
        {
            return this;
        }
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        int metaGroupCount = metaContactList.getGroupCount();
        if(groupPosition < metaGroupCount)
        {
            return metaContactList.getChildrenCount(groupPosition);
        }
        else
        {
            return results.get(groupPosition-metaGroupCount).getCount();
        }
    }

    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        int metaGroupCount = metaContactList.getGroupCount();
        if(groupPosition < metaGroupCount)
        {
            return metaContactList.getChild(groupPosition, childPosition);
        }
        else
        {
            return results.get(groupPosition-groupPosition)
                .contacts.get(childPosition);
        }
    }

    @Override
    public UIContactRenderer getContactRenderer(int groupPosition)
    {
        if(groupPosition < metaContactList.getGroupCount())
        {
            return metaContactList.getContactRenderer(groupPosition);
        }
        else
        {
            return SourceContactRenderer.instance;
        }
    }

    @Override
    public void filterData(String queryStr)
    {
        cancelQueries();

        for(ContactSourceService css : sources)
        {
            ContactQuery query = css.createContactQuery(queryStr);
            queries.add(query);
            query.addContactQueryListener(this);

            query.start();
        }

        metaContactList.filterData(queryStr);

        results = new ArrayList<ResultGroup>();
        notifyDataSetChanged();
    }

    private void cancelQueries()
    {
        for(ContactQuery query : queries)
        {
            query.cancel();
        }
        queries.clear();
    }

    @Override
    public String getDisplayName(Object groupImpl)
    {
        return ((ResultGroup) groupImpl).source.getDisplayName();
    }

    @Override
    public void contactReceived(
        ContactReceivedEvent contactReceivedEvent)
    {

    }

    @Override
    public void queryStatusChanged(
        ContactQueryStatusEvent contactQueryStatusEvent)
    {
        if(contactQueryStatusEvent.getEventType()
            == ContactQuery.QUERY_COMPLETED)
        {
            final ContactQuery query
                = contactQueryStatusEvent.getQuerySource();

            final ResultGroup resultGroup
                = new ResultGroup( query.getContactSource(),
                                   query.getQueryResults() );

            if(resultGroup.getCount() == 0)
            {
                return;
            }

            uiHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    if(!queries.contains(query))
                    {
                        logger.warn(
                            "Received event for cancelled query: "+query);
                        return;
                    }

                    results.add(resultGroup);

                    notifyDataSetChanged();

                    expandAllGroups();
                }
            });
        }
    }

    @Override
    public void contactRemoved(ContactRemovedEvent contactRemovedEvent)
    {
        logger.error("CONTACT REMOVED NOT IMPLEMENTED");
    }

    @Override
    public void contactChanged(ContactChangedEvent contactChangedEvent)
    {
        logger.error("CONTACT CHANGED NOT IMPLEMENTED");
    }

    private class ResultGroup
    {
        private final List<SourceContact> contacts;
        private final ContactSourceService source;

        public ResultGroup(ContactSourceService source,
                           List<SourceContact> results)
        {
            this.source = source;
            this.contacts = results;
        }

        int getCount()
        {
            return contacts.size();
        }
    }
}
