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
package org.jitsi.android.gui.chat;

import java.util.*;

import android.app.*;
import android.content.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.*;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.method.*;
import android.text.util.*;
import android.view.*;
import android.widget.*;
import android.widget.LinearLayout.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.contactlist.model.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.EventListener;
import org.jitsi.service.osgi.*;

/**
 * The <tt>ChatFragment</tt> is responsible for chat interface.
 * 
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ChatFragment
    extends OSGiFragment
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(ChatFragment.class);

    /**
     * The session adapter for the contained <tt>ChatSession</tt>.
     */
    private ChatListAdapter chatListAdapter;

    /**
     * The corresponding <tt>ChatSession</tt>.
     */
    private ChatSession chatSession;

    /**
     * The chat list view representing the chat.
     */
    private ListView chatListView;

    /**
     * List header used to display progress bar when history is being loaded.
     */
    private View header;

    /**
     * Remembers first visible view to scroll the list after new portion of
     * history messages is added.
     */
    public int scrollFirstVisible;

    /**
     * Remembers top position to add to the scrolling offset after new portion
     * of history messages is added.
     */
    public int scrollTopOffset;

    /**
     * The chat typing view.
     */
    private LinearLayout typingView;

    /**
     * The task that loads history.
     */
    private LoadHistoryTask loadHistoryTask;

    /**
     * Indicates that this fragment is visible to the user.
     * This is important, because of PagerAdapter being used on phone layouts,
     * which doesn't properly call onResume() when switched page fragment is
     * displayed.
     */
    private boolean visibleToUser = false;

    /**
     * The chat controller used to handle operations like editing and sending
     * messages used by this fragment.
     */
    private ChatController chatController;

    /**
     * Refresh avatar and globals status display on change.
     */
    private EventListener<PresenceStatus> globalStatusListener
            = new EventListener<PresenceStatus>()
    {
        @Override
        public void onChangeEvent(PresenceStatus eventObject)
        {
            if(chatListAdapter != null)
                chatListAdapter.localAvatarOrStatusChanged();
        }
    };

    /**
     * Returns the corresponding <tt>ChatSession</tt>.
     *
     * @return the corresponding <tt>ChatSession</tt>
     */
    public ChatSession getChatSession()
    {
        return chatSession;
    }

    /**
     * Returns the underlying chat list view.
     *
     * @return the underlying chat list view
     */
    public ListView getChatListView()
    {
        return chatListView;
    }

    /**
     * Returns the underlying chat list view.
     *
     * @return the underlying chat list view
     */
    public ChatListAdapter getChatListAdapter()
    {
        return chatListAdapter;
    }

    /**
     * Flag indicates that we have loaded the history for the first time.
     */
    private boolean historyLoaded = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(   LayoutInflater inflater,
                                ViewGroup container,
                                Bundle savedInstanceState)
    {
        View content = inflater.inflate( R.layout.chat_conversation,
                                         container,
                                         false);

        chatListAdapter = new ChatListAdapter();
        chatListView = (ListView) content.findViewById(R.id.chatListView);

        // Inflates and adds the header, hidden by default
        this.header = inflater.inflate(R.layout.progressbar,
                                       chatListView, false);
        header.setVisibility(View.GONE);
        chatListView.addHeaderView(header);

        // Registers for chat message context menu
        registerForContextMenu(chatListView);

        typingView = (LinearLayout) content.findViewById(R.id.typingView);

        chatListView.setAdapter(chatListAdapter);

        chatListView.setSelector(R.drawable.contact_list_selector);

        chatListView.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
                // Detects event when user scrolls to the top of the list
                if(scrollState == 0)
                {
                    if(chatListView.getChildAt(0).getTop()==0)
                    {
                        // Loads some more history
                        // if there's no loading task in progress
                        if(loadHistoryTask == null)
                        {
                            loadHistoryTask = new LoadHistoryTask(false);
                            loadHistoryTask.execute();
                        }
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount)
            {
                // Remembers scrolling position to restore after new history
                // messages are loaded
                scrollFirstVisible = firstVisibleItem;
                View firstVisible = view.getChildAt(0);
                scrollTopOffset
                        = firstVisible != null ? firstVisible.getTop() : 0;
            }
        });

        // Chat intent handling
        Bundle arguments = getArguments();
        String chatId
                = arguments.getString(ChatSessionManager.CHAT_IDENTIFIER);

        if(chatId == null)
            throw new IllegalArgumentException();

        chatSession = ChatSessionManager.getActiveChat(chatId);
        if(chatSession == null)
        {
            logger.error("Chat for given id: " + chatId + " not exists");
            return null;
        }

        chatSession.addMessageListener(chatListAdapter);

        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        this.chatController = new ChatController(activity, this);
    }

    /**
     * This method must be called by parent <tt>Activity</tt> or
     * <tt>Fragment</tt> in order to register the chat controller.
     *
     * @param isVisible <tt>true</tt> if the fragment is now visible to
     *                  the user.
     * @see ChatController
     */
    public void setVisibleToUser(boolean isVisible)
    {
        logger.debug("View visible to user: " + hashCode()+" "+isVisible);
        this.visibleToUser = isVisible;
        checkInitController();
    }

    /**
     * Checks for <tt>ChatController</tt> initialization. To init the controller
     * fragment must be visible and it's View must be created.
     *
     * If fragment is no longer visible the controller will be uninitialized.
     */
    private void checkInitController()
    {
        if(visibleToUser && chatListView != null)
        {
            logger.debug("Init controller: " + hashCode());
            chatController.onShow();
            // Also register global status listener
            AndroidGUIActivator.getLoginRenderer()
                    .addGlobalStatusListener(globalStatusListener);
            // Init the history
            initAdapter();
        }
        else if(!visibleToUser)
        {
            chatController.onHide();
            // Also remove global status listener
            AndroidGUIActivator.getLoginRenderer()
                    .removeGlobalStatusListener(globalStatusListener);
        }
        else
        {
            logger.debug("Skipping controller init... " + hashCode());
        }
    }

    /**
     * Initializes the chat list adapter.
     */
    private void initAdapter()
    {
        /**
         * Initial history load is delayed until the chat is displayed
         * to the user. We previously relyed on onCreate, but it will be called
         * too early on phone layouts where ChatPagerAdapter is used. It creates
         * ChatFragment too early that is before the first message is added to
         * the history and we are unable to retrieve it without hacks.
         */
        if(!historyLoaded)
        {
            loadHistoryTask
                = new LoadHistoryTask(chatListAdapter.isEmpty());

            loadHistoryTask.execute();

            historyLoaded = true;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // If added to the pager adapter for the first time it is required
        // to check again, because it's marked visible when the Views
        // are not created yet
        checkInitController();

        chatSession.addContactStatusListener(chatListAdapter);
        chatSession.addTypingListener(chatListAdapter);
    }

    @Override
    public void onPause()
    {
        chatSession.removeContactStatusListener(chatListAdapter);
        chatSession.removeTypingListener(chatListAdapter);

        /*
         * Indicates that this fragment is no longer visible,
         * because of this call parent <tt>Activities don't have to call it
         * in onPause().
         */
        setVisibleToUser(false);

        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v,menuInfo);
        // Creates chat message context menu
        getActivity().getMenuInflater().inflate(R.menu.chat_msg_ctx_menu, menu);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.copy_to_clipboard)
        {
            AdapterView.AdapterContextMenuInfo info
                    = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            // Clicked position must be aligned to list headers count
            int position = info.position - chatListView.getHeaderViewsCount();
            // Gets clicked message
            ChatMessage clickedMsg = chatListAdapter.getMessage(position);
            // Copy message content to clipboard
            ClipboardManager clipboardManager
                    = (ClipboardManager) getActivity()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(clickedMsg.getContentForClipboard());
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Creates new parametrized instance of <tt>ChatFragment</tt>.
     *
     * @param chatId optional phone number that will be filled.
     * @return new parametrized instance of <tt>ChatFragment</tt>.
     */
    public static ChatFragment newInstance(String chatId)
    {
        if (logger.isDebugEnabled())
            logger.debug("CHAT FRAGMENT NEW INSTANCE: " + chatId);

        ChatFragment chatFragment = new ChatFragment();

        Bundle args = new Bundle();
        args.putString(ChatSessionManager.CHAT_IDENTIFIER, chatId);

        chatFragment.setArguments(args);

        return chatFragment;
    }

    /**
     * 
     */
    public void onDetach()
    {
        if (logger.isDebugEnabled())
            logger.debug("DETACH CHAT FRAGMENT: " + this);

        super.onDetach();

        if(chatSession != null)
        {
            chatSession.removeMessageListener(chatListAdapter);
        }

        chatListAdapter = null;

        if (loadHistoryTask != null)
        {
            loadHistoryTask.cancel(true);
            loadHistoryTask = null;
        }
    }

    class ChatListAdapter
        extends BaseAdapter
        implements  ChatSession.ChatSessionListener,
                    ContactPresenceStatusListener,
                    TypingNotificationsListener
    {
        /**
         * The list of chat message displays. All access and modification of
         * this list must be done on the UI thread.
         */
        private final List<MessageDisplay> messages
                = new ArrayList<MessageDisplay>();

        /**
         * The type of the incoming message view.
         */
        static final int INCOMING_MESSAGE_VIEW = 0;

        /**
         * The type of the outgoing message view.
         */
        static final int OUTGOING_MESSAGE_VIEW = 1;

        /**
         * The type of the system message view.
         */
        static final int SYSTEM_MESSAGE_VIEW = 2;

        /**
         * The type of the error message view.
         */
        static final int ERROR_MESSAGE_VIEW = 3;

        /**
         * The type for corrected message view.
         */
        static final int CORRECTED_MESSAGE_VIEW = 4;

        /**
         * Counter used to generate row ids.
         */
        private long idGenerator=0;

        /**
         * HTML image getter.
         */
        private final Html.ImageGetter imageGetter = new HtmlImageGetter();

        /**
         * Passes the message to the <tt>ChatListAdapter</tt>
         * for processing and appends it at the end.
         *
         * @param newMessage the message to add.
         * @param update if set to <tt>true</tt> will notify the UI about update
         *               immediately.
         */
        public void addMessage( final ChatMessage newMessage,
                                final boolean update )
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if(chatListAdapter == null)
                    {
                        logger.warn(
                            "Add message handled, when there's no adapter" +
                                "-possibly after onDetach()");
                        return;
                    }
                    addMessageImpl(newMessage, update);
                }
            });
        }

        /**
         * This method must be called on the UI thread(as well as any other that
         * access the {@link #messages} list).
         *
         */
        private void addMessageImpl( ChatMessage newMessage, boolean update)
        {
            int msgIdx;
            int lastMsgIdx = getLastMessageIdx(newMessage);
            ChatMessage lastMsg = lastMsgIdx != -1
                        ? chatListAdapter.getMessage(lastMsgIdx)
                        : null;

            if(lastMsg == null || !lastMsg.isConsecutiveMessage(newMessage))
            {
                messages.add(new MessageDisplay(newMessage));
                msgIdx = messages.size()-1;
            }
            else
            {
                // Merge the message and update the object in the list
                messages.get(lastMsgIdx)
                        .update(lastMsg.mergeMessage(newMessage));
                msgIdx = lastMsgIdx;
            }

            if(update)
            {
                chatListAdapter.notifyDataSetChanged();
                // List must be scrolled manually, when
                // android:transcriptMode="normal" is set
                chatListView.setSelection(
                        msgIdx + chatListView.getHeaderViewsCount());
            }
        }

        /**
         * Inserts given <tt>Collection</tt> of <tt>ChatMessage</tt> at
         * the beginning of the list.
         *
         * @param collection the collection of <tt>ChatMessage</tt> to prepend.
         */
        public void prependMessages(Collection<ChatMessage> collection)
        {
            List<MessageDisplay> newMsgs = new ArrayList<MessageDisplay>();
            Iterator<ChatMessage> iterator = collection.iterator();
            MessageDisplay previous = null;
            while(iterator.hasNext())
            {
                ChatMessage next = iterator.next();
                if(previous == null || !previous.msg.isConsecutiveMessage(next))
                {
                    previous = new MessageDisplay(next);
                    newMsgs.add(previous);
                }
                else
                {
                    // Merge the message and update the object in the list
                    previous.update(previous.msg.mergeMessage(next));
                }
            }
            messages.addAll(0, newMsgs);
        }

        /**
         * Finds index of the message that will handle <tt>newMessage</tt>
         * merging process(usually just the last one). If the
         * <tt>newMessage</tt> is a correction message, then the last message
         * of the same type will be returned.
         *
         * @param newMessage the next message to be merged into the adapter.
         *
         * @return index of the message that will handle <tt>newMessage</tt>
         * merging process. If <tt>newMessage</tt> is a correction message,
         * then the last message of the same type will be returned.
         */
        private int getLastMessageIdx(ChatMessage newMessage)
        {
            // If it's not a correction message then jus return the last one
            if(newMessage.getCorrectedMessageUID() == null)
                return chatListAdapter.getCount()-1;

            // Search for the same type
            int msgType = newMessage.getMessageType();
            for(int i=getCount()-1; i>= 0; i--)
            {
                ChatMessage candidate = getMessage(i);
                if(candidate.getMessageType() == msgType)
                {
                    return i;
                }
            }
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        public int getCount()
        {
            return messages.size();
        }

        /**
         * {@inheritDoc}
         */
        public Object getItem(int position)
        {
            if (logger.isDebugEnabled())
                logger.debug("OBTAIN CHAT ITEM ON POSITION: " + position);

            return messages.get(position);
        }

        ChatMessage getMessage(int pos)
        {
            return ((MessageDisplay) getItem(pos)).msg;
        }

        MessageDisplay getMessageDisplay(int pos)
        {
            return (MessageDisplay) getItem(pos);
        }
    
        /**
         * {@inheritDoc}
         */
        public long getItemId(int pos)
        {
            return messages.get(pos).id;
        }

        public int getViewTypeCount()
        {
            return 5;
        }

        public int getItemViewType(int position)
        {
            ChatMessage message = getMessage(position);
            int messageType = message.getMessageType();

            if (messageType == ChatMessage.INCOMING_MESSAGE)
                return INCOMING_MESSAGE_VIEW;
            else if (messageType == ChatMessage.OUTGOING_MESSAGE)
            {
                String sessionCorrUID = chatSession.getCorrectionUID();
                String msgCorrUID = message.getUidForCorrection();

                /*logger.error( "pos: " + position
                                 + "S UID: " + sessionCorrUID
                                 + "M UID: " + msgCorrUID );*/

                if( sessionCorrUID != null
                    && sessionCorrUID.equals(msgCorrUID) )
                {
                    //logger.error("pos: "+position+" corrected");
                    return CORRECTED_MESSAGE_VIEW;
                }
                else
                {
                    return OUTGOING_MESSAGE_VIEW;
                }

            }
            else if (messageType == ChatMessage.SYSTEM_MESSAGE)
                return SYSTEM_MESSAGE_VIEW;
            else if(messageType == ChatMessage.ERROR_MESSAGE)
                return ERROR_MESSAGE_VIEW;

            return 0;
        }

        /**
         * Hack required to capture TextView(message body) clicks,
         * when <tt>LinkMovementMethod</tt> is set.
         */
        private final View.OnClickListener msgClickAdapter
            = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(chatController != null
                    && v.getTag() instanceof Integer)
                {
                    Integer position = (Integer) v.getTag();
                    chatController.onItemClick(
                        chatListView, v, position, -1/* id not used */);
                }
            }
        };

        /**
         * {@inheritDoc}
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // Keeps reference to avoid future findViewById()
            MessageViewHolder messageViewHolder;

            if (convertView == null)
            {
                messageViewHolder = new MessageViewHolder();

                int viewType = getItemViewType(position);

                convertView
                    = inflateViewForType(viewType, messageViewHolder, parent);
            }
            else
            {
                messageViewHolder = (MessageViewHolder) convertView.getTag();

                // Convert between OUTGOING and CORRECTED
                if(messageViewHolder.viewType == CORRECTED_MESSAGE_VIEW
                    || messageViewHolder.viewType == OUTGOING_MESSAGE_VIEW)
                {
                    int currentViewType = getItemViewType(position);
                    // Convert if has changed
                    if(messageViewHolder.viewType != currentViewType)
                    {
                        messageViewHolder = new MessageViewHolder();

                        convertView = inflateViewForType(
                            currentViewType, messageViewHolder, parent);
                    }
                }
            }

            // Set position used for click handling from click adapter
            int clickedPos = position + chatListView.getHeaderViewsCount();
            messageViewHolder.messageView.setTag(clickedPos);
            if(messageViewHolder.outgoingMessageHolder != null)
                messageViewHolder.outgoingMessageHolder.setTag(clickedPos);

            MessageDisplay message = getMessageDisplay(position);

            if (message != null)
            {
                if(messageViewHolder.viewType == INCOMING_MESSAGE_VIEW
                    || messageViewHolder.viewType == OUTGOING_MESSAGE_VIEW
                    || messageViewHolder.viewType == CORRECTED_MESSAGE_VIEW)
                {
                    updateStatusAndAvatarView(messageViewHolder);

                    messageViewHolder.timeView.setText(message.getDateStr());
                }

                messageViewHolder.messageView.setText(message.getBody());
            }

            return convertView;
        }

        private View inflateViewForType( int viewType,
                                         MessageViewHolder messageViewHolder,
                                         ViewGroup parent )
        {
            messageViewHolder.viewType = viewType;

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View convertView;

            if (viewType == INCOMING_MESSAGE_VIEW)
            {
                convertView = inflater.inflate( R.layout.chat_incoming_row,
                                                parent,
                                                false);

                messageViewHolder.avatarView
                    = (ImageView) convertView.findViewById(
                    R.id.incomingAvatarIcon);

                messageViewHolder.statusView
                    = (ImageView) convertView.findViewById(
                    R.id.incomingStatusIcon);

                messageViewHolder.messageView
                    = (TextView) convertView.findViewById(
                    R.id.incomingMessageView);

                messageViewHolder.timeView
                    = (TextView) convertView.findViewById(
                    R.id.incomingTimeView);

                messageViewHolder.typingView
                    = (ImageView) convertView.findViewById(
                    R.id.typingImageView);
            }
            else if( viewType == OUTGOING_MESSAGE_VIEW
                     || viewType == CORRECTED_MESSAGE_VIEW )
            {
                if(viewType == OUTGOING_MESSAGE_VIEW)
                {
                    convertView
                        = inflater.inflate( R.layout.chat_outgoing_row,
                                            parent,
                                            false);
                }
                else
                {
                    convertView
                        = inflater.inflate( R.layout.chat_corrected_row,
                                            parent,
                                            false);
                }

                messageViewHolder.avatarView
                    = (ImageView) convertView.findViewById(
                    R.id.outgoingAvatarIcon);

                messageViewHolder.statusView
                    = (ImageView) convertView.findViewById(
                    R.id.outgoingStatusIcon);

                messageViewHolder.messageView
                    = (TextView) convertView.findViewById(
                    R.id.outgoingMessageView);

                messageViewHolder.timeView
                    = (TextView) convertView.findViewById(
                        R.id.outgoingTimeView);

                messageViewHolder.outgoingMessageHolder
                    = convertView.findViewById(
                        R.id.outgoingMessageHolder);
            }
            else
            {
                // System or error view
                convertView = inflater.inflate(
                    viewType == SYSTEM_MESSAGE_VIEW
                        ? R.layout.chat_system_row : R.layout.chat_error_row,
                    parent, false );

                messageViewHolder.messageView
                    = (TextView) convertView.findViewById(R.id.messageView);
            }

            // Set link movement method
            messageViewHolder
                .messageView
                .setMovementMethod(LinkMovementMethod.getInstance());
            // Set clicks adapter
            messageViewHolder
                .messageView.setOnClickListener(msgClickAdapter);
            if(messageViewHolder.outgoingMessageHolder != null)
            {
                messageViewHolder.outgoingMessageHolder
                    .setOnClickListener(msgClickAdapter);
            }

            convertView.setTag(messageViewHolder);

            return convertView;
        }

        /**
         * Updates status and avatar views on given <tt>MessageViewHolder</tt>.
         * @param viewHolder the <tt>MessageViewHolder</tt> to update.
         */
        private void updateStatusAndAvatarView(MessageViewHolder viewHolder)
        {
            Drawable avatar;
            Drawable status;
            if (viewHolder.viewType == INCOMING_MESSAGE_VIEW)
            {
                avatar = MetaContactRenderer.getAvatarDrawable(
                    chatSession.getMetaContact());

                status = MetaContactRenderer.getStatusDrawable(
                    chatSession.getMetaContact());
            }
            else if (viewHolder.viewType == OUTGOING_MESSAGE_VIEW
                     || viewHolder.viewType == CORRECTED_MESSAGE_VIEW)
            {
                AndroidLoginRenderer loginRenderer
                        = AndroidGUIActivator.getLoginRenderer();
                avatar = loginRenderer.getLocalAvatarDrawable();
                status = loginRenderer.getLocalStatusDrawable();
            }
            else
            {
                // Avatar and status are present only in outgoing or incoming
                // message views
                return;
            }

            setAvatar(viewHolder.avatarView, avatar);
            setStatus(viewHolder.statusView, status);
        }

        @Override
        public void messageDelivered(final MessageDeliveredEvent evt)
        {
            final Contact contact = evt.getDestinationContact();
            final MetaContact metaContact
                = AndroidGUIActivator.getContactListService()
                    .findMetaContactByContact(contact);

            if (logger.isTraceEnabled())
                logger.trace("MESSAGE DELIVERED to contact: "
                    + contact.getAddress());

            if (metaContact != null
                && chatSession.getMetaContact().equals(metaContact))
            {
                final ChatMessageImpl msg = ChatMessageImpl.getMsgForEvent(evt);

                if (logger.isTraceEnabled())
                    logger.trace(
                    "MESSAGE DELIVERED: process message to chat for contact: "
                    + contact.getAddress()
                    + " MESSAGE: " + msg.getMessage());

                addMessage(msg, true);
            }
        }

        @Override
        public void messageDeliveryFailed(MessageDeliveryFailedEvent arg0)
        {
            // Do nothing, handled in ChatSession
        }

        @Override
        public void messageReceived(final MessageReceivedEvent evt)
        {
            if (logger.isTraceEnabled())
                logger.trace("MESSAGE RECEIVED from contact: "
                + evt.getSourceContact().getAddress());

            final Contact protocolContact = evt.getSourceContact();
            final MetaContact metaContact
                = AndroidGUIActivator.getContactListService()
                    .findMetaContactByContact(protocolContact);

            if(metaContact != null
                && chatSession.getMetaContact().equals(metaContact))
            {
                final ChatMessageImpl msg = ChatMessageImpl.getMsgForEvent(evt);

                addMessage(msg, true);
            }
            else
            {
                if (logger.isTraceEnabled())
                    logger.trace("MetaContact not found for protocol contact: "
                        + protocolContact + ".");
            }
        }

        @Override
        public void messageAdded(ChatMessage msg)
        {
            addMessage(msg, true);
        }

        /**
         * Indicates a contact has changed its status.
         */
        @Override
        public void contactPresenceStatusChanged(
            ContactPresenceStatusChangeEvent evt)
        {
            Contact sourceContact = evt.getSourceContact();

            if (logger.isDebugEnabled())
                logger.debug("Contact presence status changed: "
                    + sourceContact.getAddress());

            if (!chatSession.getMetaContact().containsContact(sourceContact))
                return;

            new UpdateStatusTask().execute();
        }

        @Override
        public void typingNotificationDeliveryFailed(
            TypingNotificationEvent evt)
        {
            
        }

        @Override
        public void typingNotificationReceived(TypingNotificationEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("Typing notification received: "
                    + evt.getSourceContact().getAddress());

            TypingNotificationHandler
                .handleTypingNotificationReceived(evt, ChatFragment.this);
        }

        /**
         * Updates all avatar and status on outgoing messages rows.
         */
        public void localAvatarOrStatusChanged()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for(int i=0; i<chatListView.getChildCount(); i++)
                    {
                        View row = chatListView.getChildAt(i);

                        MessageViewHolder viewHolder
                                = (MessageViewHolder) row.getTag();
                        if(viewHolder == null)
                            continue;

                        updateStatusAndAvatarView(viewHolder);
                    }
                }
            });
        }

        /**
         * Class used to cache processed message contents. Prevents from
         * re-processing on each View display.
         */
        class MessageDisplay
        {
            /**
             * Row identifier.
             */
            private final long id;
            /**
             * Displayed <tt>ChatMessage</tt>
             */
            private ChatMessage msg;

            /**
             * Date string cache
             */
            private String dateStr;

            /**
             * Message body cache
             */
            private Spanned body;

            /**
             * Creates new instance of <tt>MessageDisplay</tt> that will be used
             * for displaying given <tt>ChatMessage</tt>.
             *
             * @param msg the <tt>ChatMessage</tt> that will be displayed by
             *            this instance.
             */
            MessageDisplay(ChatMessage msg)
            {
                this.msg = msg;
                this.id = idGenerator++;
            }

            /**
             * Returns formatted date string for the <tt>ChatMessage</tt>.
             * @return formatted date string for the <tt>ChatMessage</tt>.
             */
            public String getDateStr()
            {
                if(dateStr == null)
                {
                    Date date = msg.getDate();
                    dateStr = GuiUtils.formatDate(date)
                            + " " + GuiUtils.formatTime(date);

                }
                return dateStr;
            }

            /**
             * Returns <tt>Spanned</tt> message body processed for HTML tags.
             * @return <tt>Spanned</tt> message body.
             */
            public Spanned getBody()
            {
                if(body == null)
                {
                    body = Html.fromHtml(msg.getMessage(), imageGetter, null);

                    //TODO: adding links will destroy our links in system msgs
                    final int msgType = msg.getMessageType();
                    if(msgType == ChatMessage.OUTGOING_MESSAGE
                        || msgType == ChatMessage.INCOMING_MESSAGE)
                    {
                        Linkify.addLinks((Spannable) body, Linkify.ALL);
                    }
                }
                return body;
            }

            /**
             * Updates this display instance with new message causing display
             * contents to be invalidated.
             * @param chatMessage new message content
             */
            public void update(ChatMessage chatMessage)
            {
                dateStr = null;
                body = null;
                msg = chatMessage;
            }
        }
    }

    static class MessageViewHolder
    {
        ImageView avatarView;
        ImageView statusView;
        TextView messageView;
        TextView timeView;
        ImageView typingView;
        int viewType;
        // Outgoing message background area
        // (instance used to capture clicks and trigger message correction)
        View outgoingMessageHolder;
    }

    /**
     * Loads the history in an asynchronous thread and then adds the history
     * messages to the user interface.
     */
    private class LoadHistoryTask
        extends AsyncTask<Void, Void, Collection<ChatMessage>>
    {
        /**
         * Indicates that history is being loaded for the first time.
         */
        private final boolean init;
        /**
         * Remembers adapter size before new messages were added.
         */
        private int preSize;

        LoadHistoryTask(boolean init)
        {
            this.init = init;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            header.setVisibility(View.VISIBLE);

            this.preSize = chatListAdapter.getCount();
        }

        @Override
        protected Collection<ChatMessage> doInBackground(Void... params)
        {
            return chatSession.getHistory(init);
        }

        @Override
        protected void onPostExecute(Collection<ChatMessage> result)
        {
            super.onPostExecute(result);

            chatListAdapter.prependMessages(result);

            header.setVisibility(View.GONE);
            chatListAdapter.notifyDataSetChanged();
            int loaded = chatListAdapter.getCount() - preSize;
            int scrollTo = loaded + scrollFirstVisible;
            chatListView.setSelectionFromTop(scrollTo, scrollTopOffset);

            loadHistoryTask = null;
        }
    }

    /**
     * Updates the status user interface.
     */
    private class UpdateStatusTask
        extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... params)
        {
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if(chatListView == null || chatSession == null)
            {
                return;
            }

            for (int i = 0; i < chatListView.getChildCount(); i++)
            {
                View chatRowView = chatListView.getChildAt(i);

                MessageViewHolder viewHolder
                    = (MessageViewHolder) chatRowView.getTag();

                if (viewHolder != null
                    && viewHolder.viewType
                        == ChatListAdapter.INCOMING_MESSAGE_VIEW)
                {
                    Drawable status
                        = MetaContactRenderer.getStatusDrawable(
                            chatSession.getMetaContact());

                    ImageView statusView = viewHolder.statusView;

                    setStatus(statusView, status);
                }
            }
        }
    }

    /**
     * Sets the avatar icon for the given avatar view.
     *
     * @param avatarView the avatar image view
     * @param avatarDrawable the avatar drawable to set
     */
    public void setAvatar(  ImageView avatarView,
                            Drawable avatarDrawable)
    {
        if (avatarDrawable == null)
        {
            avatarDrawable = JitsiApplication.getAppResources()
                .getDrawable(R.drawable.avatar);
        }

        avatarView.setImageDrawable(avatarDrawable);
    }

    /**
     * Sets the status of the given view.
     *
     * @param statusView the status icon view
     * @param statusDrawable the status drawable
     */
    public void setStatus(  ImageView statusView,
                            Drawable statusDrawable)
    {
        statusView.setImageDrawable(statusDrawable);
    }

    /**
     * Sets the appropriate typing notification interface.
     *
     * @param typingState the typing state that should be represented in the
     * view
     */
    public void setTypingState(int typingState)
    {
        if (typingView == null)
            return;

        TextView typingTextView
            = (TextView) typingView.findViewById(R.id.typingTextView);
        ImageView typingImgView
            = (ImageView) typingView.findViewById(R.id.typingImageView);

        boolean setVisible = false;
        if (typingState == OperationSetTypingNotifications.STATE_TYPING)
        {
            Drawable typingDrawable = typingImgView.getDrawable();
            if (!(typingDrawable instanceof AnimationDrawable))
            {
                typingImgView.setImageResource(R.drawable.typing_drawable);
                typingDrawable = typingImgView.getDrawable();
            }

            if(!((AnimationDrawable) typingDrawable).isRunning())
            {
                AnimationDrawable animatedDrawable
                    = (AnimationDrawable) typingDrawable;
                animatedDrawable.setOneShot(false);
                animatedDrawable.start();
            }

            typingTextView.setText(chatSession.getShortDisplayName()
                + " "
                + JitsiApplication.getResString(
                    R.string.service_gui_CONTACT_TYPING));
            setVisible = true;
        }
        else if (typingState
                == OperationSetTypingNotifications.STATE_PAUSED)
        {
            typingImgView.setImageResource(R.drawable.typing1);
            typingTextView.setText(
                chatSession.getShortDisplayName()
                + " "
                + JitsiApplication.getResString(
                    R.string.service_gui_CONTACT_PAUSED_TYPING));
            setVisible = true;
        }

        if (setVisible)
        {
            typingImgView.getLayoutParams().height
                = LayoutParams.WRAP_CONTENT;
            typingImgView.setPadding(7, 0, 7, 7);

            typingView.setVisibility(View.VISIBLE);
        }
        else
            typingView.setVisibility(View.INVISIBLE);
    }


}
