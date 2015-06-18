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
package org.jitsi.android.gui.fragment;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.globaldisplaydetails.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.menu.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.EventListener;
import org.jitsi.android.gui.widgets.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.*;

import java.util.*;

/**
 * Fragment when added to Activity will display global display details like
 * avatar, display name and status. When status is clicked a popup menu is
 * displayed allowing user to set global presence status.
 *
 * @author Pawel Domas
 */
public class ActionBarStatusFragment
    extends OSGiFragment
    implements EventListener<PresenceStatus>,
               GlobalDisplayDetailsListener
{
    /**
     * The online status.
     */
    private static final int ONLINE = 1;

    /**
     * The offline status.
     */
    private static final int OFFLINE = 2;

    /**
     * The free for chat status.
     */
    private static final int FFC = 3;

    /**
     * The away status.
     */
    private static final int AWAY = 4;

    /**
     * The do not disturb status.
     */
    private static final int DND = 5;

    /**
     * The global status menu.
     */
    private GlobalStatusMenu globalStatusMenu;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Create custom ActionBar View
        getActivity().getActionBar().setCustomView(R.layout.action_bar);

        this.globalStatusMenu = createGlobalStatusMenu();

        final RelativeLayout actionBarView
                = (RelativeLayout) getActivity()
                .findViewById(R.id.actionBarView);

        actionBarView.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                globalStatusMenu.show(actionBarView);
                globalStatusMenu.setAnimStyle(GlobalStatusMenu.ANIM_REFLECT);
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        AndroidLoginRenderer loginRenderer
                = AndroidGUIActivator.getLoginRenderer();
        loginRenderer.addGlobalStatusListener(this);

        onChangeEvent(loginRenderer.getGlobalStatus());

        GlobalDisplayDetailsService displayDetailsService
                = AndroidGUIActivator.getGlobalDisplayDetailsService();

        displayDetailsService.addGlobalDisplayDetailsListener(this);

        setGlobalAvatar(displayDetailsService.getGlobalDisplayAvatar());
        setGlobalDisplayName(displayDetailsService.getGlobalDisplayName());

    }

    @Override
    public void onPause()
    {
        AndroidGUIActivator.getLoginRenderer().removeGlobalStatusListener(this);

        GlobalDisplayDetailsService displayDetailsService
                = AndroidGUIActivator.getGlobalDisplayDetailsService();

        displayDetailsService.removeGlobalDisplayDetailsListener(this);

        super.onPause();
    }

    /**
     * Creates the <tt>GlobalStatusMenu</tt>.
     *
     * @return the newly created <tt>GlobalStatusMenu</tt>
     */
    private GlobalStatusMenu createGlobalStatusMenu()
    {
        ActionMenuItem ffcItem = new ActionMenuItem(FFC,
                    getResources().getString(R.string.service_gui_FFC_STATUS),
                    getResources().getDrawable(R.drawable.global_ffc));
        ActionMenuItem onlineItem = new ActionMenuItem(ONLINE,
                    getResources().getString(R.string.service_gui_ONLINE),
                    getResources().getDrawable(R.drawable.global_online));
        ActionMenuItem offlineItem = new ActionMenuItem(OFFLINE,
                    getResources().getString(R.string.service_gui_OFFLINE),
                    getResources().getDrawable(R.drawable.global_offline));
        ActionMenuItem awayItem = new ActionMenuItem(AWAY,
                    getResources().getString(R.string.service_gui_AWAY_STATUS),
                    getResources().getDrawable(R.drawable.global_away));
        ActionMenuItem dndItem = new ActionMenuItem(DND,
                    getResources().getString(R.string.service_gui_DND_STATUS),
                    getResources().getDrawable(R.drawable.global_dnd));

        final GlobalStatusMenu globalStatusMenu
                = new GlobalStatusMenu(getActivity());

        globalStatusMenu.addActionItem(ffcItem);
        globalStatusMenu.addActionItem(onlineItem);
        globalStatusMenu.addActionItem(offlineItem);
        globalStatusMenu.addActionItem(awayItem);
        globalStatusMenu.addActionItem(dndItem);

        globalStatusMenu.setOnActionItemClickListener(
                new GlobalStatusMenu.OnActionItemClickListener()
                {
                    @Override
                    public void onItemClick(GlobalStatusMenu source,
                                            int pos,
                                            int actionId)
                    {
                        publishGlobalStatus(actionId);
                    }
                });

        globalStatusMenu.setOnDismissListener(
                new GlobalStatusMenu.OnDismissListener()
                {
                    public void onDismiss()
                    {
                        //TODO: Add a dismiss action.
                    }
                });

        return globalStatusMenu;
    }

    /**
     * Publishes global status on separate thread to prevent
     * <tt>NetworkOnMainThreadException</tt>.
     *
     * @param newStatus new global status to set.
     */
    private void publishGlobalStatus(final int newStatus)
    {
        /**
         * Runs publish status on separate thread to prevent
         * NetworkOnMainThreadException
         */
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                GlobalStatusService globalStatusService
                        = AndroidGUIActivator.getGlobalStatusService();

                switch (newStatus)
                {
                    case ONLINE:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.ONLINE);
                        break;
                    case OFFLINE:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.OFFLINE);
                        break;
                    case FFC:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.FREE_FOR_CHAT);
                        break;
                    case AWAY:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.AWAY);
                        break;
                    case DND:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.DO_NOT_DISTURB);
                        break;
                }
            }
        }).start();
    }

    @Override
    public void onChangeEvent(final PresenceStatus presenceStatus)
    {
        final Activity activity = getActivity();

        if(presenceStatus == null || activity == null)
            return;

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ActionBarUtil.setSubtitle(
                        activity,
                        presenceStatus.getStatusName());

                ActionBarUtil.setStatus(
                        activity,
                        StatusUtil.getContactStatusIcon(presenceStatus));
            }
        });
    }

    /**
     * Indicates that the global avatar has been changed.
     */
    @Override
    public void globalDisplayAvatarChanged(final GlobalAvatarChangeEvent evt)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setGlobalAvatar(evt.getNewAvatar());
            }
        });

    }

    /**
     * Indicates that the global display name has been changed.
     */
    @Override
    public void globalDisplayNameChanged(final GlobalDisplayNameChangeEvent evt)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setGlobalDisplayName(evt.getNewDisplayName());
            }
        });
    }

    /**
     * Sets the global avatar in the action bar.
     *
     * @param avatar the byte array representing the avatar to set
     */
    private void setGlobalAvatar(final byte[] avatar)
    {
        if (avatar != null && avatar.length > 0)
        {
            ActionBarUtil.setAvatar(getActivity(), avatar);
        }
    }

    /**
     * Sets the global display name in the action bar.
     *
     * @param name the display name to set
     */
    private void setGlobalDisplayName(final String name)
    {
        String displayName = name;

        if (StringUtils.isNullOrEmpty(displayName))
        {
            Collection<ProtocolProviderService> pProviders
                = AccountUtils.getRegisteredProviders();

            if (pProviders.size() > 0)
                displayName = pProviders.iterator().next()
                                .getAccountID().getUserID();
        }

        ActionBarUtil.setTitle(getActivity(), displayName);
    }
}
