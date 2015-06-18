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
package org.jitsi.android.gui.settings.notification;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.notification.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.service.osgi.*;
import org.jitsi.service.resources.*;

import java.util.*;

/**
 * The <tt>Activity</tt> lists all notification events. When user selects one of
 * them the details screen is opened.
 *
 * @author Pawel Domas
 */
public class NotificationSettings
    extends OSGiActivity
{
    /**
     * Notifications adapter.
     */
    private NotificationsAdapter adapter;

    /**
     * Notification service instance.
     */
    private NotificationService notificationService;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.notificationService
                = ServiceUtils.getService(AndroidGUIActivator.bundleContext,
                                          NotificationService.class);

        setContentView(R.layout.list_layout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        // Refresh the list each time is displayed
        adapter = new NotificationsAdapter();
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(adapter);
        // And start listening for updates
        notificationService.addNotificationChangeListener(adapter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        // Do not listen for changes when paused
        notificationService.removeNotificationChangeListener(adapter);
        adapter = null;
    }

    /**
     * Adapter lists all notification events.
     */
    class NotificationsAdapter
        extends BaseAdapter
        implements NotificationChangeListener
    {
        /**
         * List of event types
         */
        private final ArrayList<String> events;

        /**
         * Resources service instance
         */
        private final ResourceManagementService rms;

        /**
         * Creates new instance of <tt>NotificationsAdapter</tt>.
         */
        NotificationsAdapter()
        {
            Iterator<String> eventsIter
                    = notificationService.getRegisteredEvents().iterator();

            this.events = new ArrayList<String>();

            while (eventsIter.hasNext())
            {
                events.add(eventsIter.next());
            }

            this.rms = AndroidGUIActivator.getResourcesService();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount()
        {
            return events.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position)
        {
            return rms.getI18NString("plugin.notificationconfig.event."
                                             + events.get(position));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getItemId(int position)
        {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent)
        {
            View row = getLayoutInflater().inflate(R.layout.notification_item,
                                                   parent, false);
            row.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent details
                        = NotificationDetails.getIntent(
                            NotificationSettings.this, events.get(position));

                    startActivity(details);
                }
            });

            TextView textView = (TextView)row.findViewById(R.id.text1);
            textView.setText((String)getItem(position));

            CompoundButton enableBtn
                    = (CompoundButton) row.findViewById(R.id.enable);

            enableBtn.setChecked(
                    notificationService.isActive(events.get(position)));

            enableBtn.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked)
                {
                    notificationService.setActive(events.get(position),
                                                  isChecked);
                }
            });

            return row;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void eventTypeAdded(final NotificationEventTypeEvent event)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    events.add(event.getEventType());
                    notifyDataSetChanged();
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void eventTypeRemoved(final NotificationEventTypeEvent event)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    events.remove(event.getEventType());
                    notifyDataSetChanged();
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionAdded(NotificationActionTypeEvent event){ }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionRemoved(NotificationActionTypeEvent event){ }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionChanged(NotificationActionTypeEvent event){ }

    }
}
