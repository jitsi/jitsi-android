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
import org.jitsi.android.gui.fragment.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;
import org.jitsi.service.resources.*;

/**
 * The screen that displays notification event details. It allows user to
 * enable/disable the whole event as well as adjust particular notification
 * handlers like popups, sound or vibration.
 *
 * @author Pawel Domas
 */
public class NotificationDetails
    extends OSGiActivity
    implements NotificationChangeListener,
               ActionBarToggleFragment.ActionBarToggleModel
{
    /**
     * Event type extra key
     */
    private final static String EVENT_TYPE_EXTRA="event_type";

    /**
     * The event type string that identifies the event
     */
    private String eventType;

    /**
     * Notification service instance
     */
    private NotificationService notificationService;

    /**
     * Resource service instance
     */
    private ResourceManagementService rms;

    /**
     * The description <tt>View</tt>
     */
    private TextView description;

    /**
     * Popup handler checkbox <tt>View</tt>
     */
    private CompoundButton popup;

    /**
     * Sound notification handler checkbox <tt>View</tt>
     */
    private CompoundButton soundNotification;

    /**
     * Sound playback handler checkbox <tt>View</tt>
     */
    private CompoundButton soundPlayback;

    /**
     * Vibrate handler checkbox <tt>View</tt>
     */
    private CompoundButton vibrate;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.eventType = getIntent().getStringExtra(EVENT_TYPE_EXTRA);

        if(eventType == null)
            throw new IllegalArgumentException();

        this.notificationService = ServiceUtils.getService(
                AndroidGUIActivator.bundleContext,
                NotificationService.class);
        this.rms = ServiceUtils.getService(
                AndroidGUIActivator.bundleContext,
                ResourceManagementService.class);

        setContentView(R.layout.notification_details);

        this.description = (TextView)findViewById(R.id.description);
        this.popup = (CompoundButton)findViewById(R.id.popup);
        this.soundNotification
                = (CompoundButton)findViewById(R.id.soundNotification);
        this.soundPlayback = (CompoundButton)findViewById(R.id.soundPlayback);
        this.vibrate = (CompoundButton)findViewById(R.id.vibrate);

        ActionBarUtil
            .setTitle(this,
                      rms.getI18NString(
                              "plugin.notificationconfig.event." + eventType));

        if(savedInstanceState == null)
        {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(ActionBarToggleFragment.create(""),
                         "action_bar_toggle")
                    .commit();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        updateDisplay();

        notificationService.addNotificationChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause()
    {
        super.onPause();

        notificationService.removeNotificationChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    private void updateDisplay()
    {
        boolean enable = notificationService.isActive(eventType);

        // Description
        description.setText(
            rms.getI18NString("plugin.notificationconfig.event."
                                      + eventType + ".description"));
        description.setEnabled(enable);

        // The popup
        NotificationAction popupHandler = notificationService
                .getEventNotificationAction(
                        eventType, NotificationAction.ACTION_POPUP_MESSAGE);

        popup.setEnabled(enable && popupHandler != null);
        if(popupHandler != null)
            popup.setChecked(popupHandler.isEnabled());

        // The sound
        SoundNotificationAction soundHandler
            = (SoundNotificationAction) notificationService
                .getEventNotificationAction(
                        eventType, NotificationAction.ACTION_SOUND);

        soundNotification.setEnabled(enable && soundHandler != null);
        soundPlayback.setEnabled(enable && soundHandler != null);

        if(soundHandler != null)
        {
            soundNotification.setChecked(
                    soundHandler.isSoundNotificationEnabled());
            soundPlayback.setChecked(
                    soundHandler.isSoundPlaybackEnabled());
        }

        // Vibrate action
        NotificationAction vibrateHandler = notificationService
                .getEventNotificationAction(
                        eventType, NotificationAction.ACTION_VIBRATE);

        vibrate.setEnabled(enable && vibrateHandler != null);

        if(vibrateHandler != null)
            vibrate.setChecked(vibrateHandler.isEnabled());
    }

    /**
     * Fired when popup checkbox is clicked.
     * @param v popup checkbox <tt>View</tt>
     */
    public void onPopupClicked(View v)
    {
        boolean enabled = ((CompoundButton)v).isChecked();

        NotificationAction action
            = notificationService.getEventNotificationAction(
                eventType, NotificationAction.ACTION_POPUP_MESSAGE);

        action.setEnabled(enabled);
    }

    /**
     * Fired when sound notification checkbox is clicked.
     * @param v sound notification checkbox <tt>View</tt>
     */
    public void onSoundNotificationClicked(View v)
    {
        boolean enabled = ((CompoundButton)v).isChecked();

        SoundNotificationAction action = (SoundNotificationAction)
            notificationService.getEventNotificationAction(
                    eventType, NotificationAction.ACTION_SOUND);

        action.setSoundNotificationEnabled(enabled);
    }

    /**
     * Fired when sound playback checkbox is clicked.
     * @param v sound playback checkbox <tt>View</tt>
     */
    public void onSoundPlaybackClicked(View v)
    {
        boolean enabled = ((CompoundButton)v).isChecked();

        SoundNotificationAction action = (SoundNotificationAction)
                notificationService.getEventNotificationAction(
                        eventType, NotificationAction.ACTION_SOUND);

        action.setSoundPlaybackEnabled(enabled);
    }

    /**
     * Fired when vibrate notification checkbox is clicked.
     * @param v vibrate notification checkbox <tt>View</tt>
     */
    public void onVibrateClicked(View v)
    {
        boolean enabled = ((CompoundButton)v).isChecked();

        NotificationAction action
                = notificationService.getEventNotificationAction(
                eventType, NotificationAction.ACTION_VIBRATE);

        action.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionAdded(NotificationActionTypeEvent event)
    {
        handleActionEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionRemoved(NotificationActionTypeEvent event)
    {
        handleActionEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionChanged(NotificationActionTypeEvent event)
    {
        handleActionEvent(event);
    }

    /**
     * Handles add/changed/removed notification action events by refreshing
     * the display if the event is related with the one currently displayed.
     * @param event the event object
     */
    private void handleActionEvent(NotificationActionTypeEvent event)
    {
        if(event.getEventType().equals(eventType))
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    updateDisplay();
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     * Not interested in type added event.
     */
    @Override
    public void eventTypeAdded(NotificationEventTypeEvent event){ }

    /**
     * {@inheritDoc}
     *
     * If removed event is the one currently displayed,
     * closes the <tt>Activity</tt>.
     */
    @Override
    public void eventTypeRemoved(NotificationEventTypeEvent event)
    {
        if(!event.getEventType().equals(eventType))
            return;

        // Event no longer exists
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                finish();
            }
        });
    }

    /**
     * Gets the <tt>Intent</tt> for starting <tt>NotificationDetails</tt>
     * <tt>Activity</tt>.
     * @param ctx the context
     * @param eventType name of the event that will be displayed by
     *                  <tt>NotificationDetails</tt>.
     * @return the <tt>Intent</tt> for starting <tt>NotificationDetails</tt>
     *         <tt>Activity</tt>.
     */
    public static Intent getIntent(Context ctx, String eventType)
    {
        Intent intent = new Intent(ctx, NotificationDetails.class);
        intent.putExtra(EVENT_TYPE_EXTRA, eventType);
        return intent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChecked()
    {
        return notificationService.isActive(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChecked(boolean isChecked)
    {
        notificationService.setActive(eventType, isChecked);
        updateDisplay();
    }
}
