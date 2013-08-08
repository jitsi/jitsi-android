/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.util.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;
import org.jitsi.util.*;

import android.graphics.drawable.*;
import android.os.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.widget.LinearLayout.*;

/**
 * The <tt>ChatFragment</tt> is responsible for chat interface.
 * 
 * @author Yana Stamcheva
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
     * The chat typing view.
     */
    private LinearLayout typingView;

    /**
     * The current position of this chat in the chat pager.
     */
    private int position;

    /**
     * The task that loads history.
     */
    private LoadHistoryTask loadHistoryTask;

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
     * Returns the chat position.
     *
     * @return the chat position
     */
    public int getChatPosition()
    {
        return position;
    }

    /**
     * Sets this chat position in the view pager.
     *
     * @param pos the position of this chat
     */
    public void setChatPosition(int pos)
    {
        this.position = pos;
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

        typingView = (LinearLayout) content.findViewById(R.id.typingView);

        chatListView.setAdapter(chatListAdapter);

        chatListView.setSelector(R.drawable.contact_list_selector);

        // Chat intent handling
        Bundle arguments = getArguments();
        String chatId
                = arguments.getString(ChatSessionManager.CHAT_IDENTIFIER);
        if (chatId != null && chatId.length() > 0)
        {
            chatSession = ChatSessionManager.getActiveChat(chatId);

            initAdapter();
        }

        return content;
    }

    /**
     * Initializes the chat list adapter.
     */
    private void initAdapter()
    {
        loadHistoryTask = new LoadHistoryTask();

        loadHistoryTask.execute();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        chatSession.addMessageListener(chatListAdapter);
        chatSession.addContactStatusListener(chatListAdapter);
        chatSession.addTypingListener(chatListAdapter);
    }

    @Override
    public void onPause()
    {
        chatSession.removeMessageListener(chatListAdapter);
        chatSession.removeContactStatusListener(chatListAdapter);
        chatSession.removeTypingListener(chatListAdapter);

        super.onPause();
    }

    /**
     * Creates new parametrized instance of <tt>CallContactFragment</tt>.
     *
     * @param chatId optional phone number that will be filled.
     * @return new parametrized instance of <tt>CallContactFragment</tt>.
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

        chatListAdapter = null;

        if (loadHistoryTask != null)
        {
            loadHistoryTask.cancel(true);
            loadHistoryTask = null;
        }
    }

    /**
     * Sends the given message to the chat.
     *
     * @param message the message to send
     */
    public void sendMessage(final String message)
    {
        if (!StringUtils.isNullOrEmpty(message))
            new Thread(){
                public void run()
                {
                    chatSession.sendMessage(message);
                }
            }.start();
    }

    class ChatListAdapter
        extends BaseAdapter
        implements  MessageListener,
                    ContactPresenceStatusListener,
                    TypingNotificationsListener
    {
        /**
         * The list of chat messages.
         */
        private final List<ChatMessage> messages
            = new LinkedList<ChatMessage>();

        /**
         * The type of the incoming message view.
         */
        final int INCOMING_MESSAGE_VIEW = 0;

        /**
         * The type of the outgoing message view.
         */
        final int OUTGOING_MESSAGE_VIEW = 1;

        /**
         * Passes the message to the contained <code>ChatConversationPanel</code>
         * for processing and appends it at the end of the conversationPanel
         * document.
         *
         * @param contactName the name of the contact sending the message
         * @param displayName the display name of the contact
         * @param date the time at which the message is sent or received
         * @param messageType the type of the message. One of OUTGOING_MESSAGE
         * or INCOMING_MESSAGE
         * @param message the message text
         * @param contentType the content type
         */
        public void addMessage( final String contactName,
                                final String displayName,
                                final Date date,
                                final int messageType,
                                final String message,
                                final String contentType,
                                final String messageUID,
                                final String correctedMessageUID)
        {
            ChatMessage chatMessage = null;

            synchronized (messages)
            {
                if (!isConsecutiveMessage(  contactName,
                                            messageType,
                                            date.getTime()))
                {
                     chatMessage
                         = new ChatMessage(contactName, displayName,
                            date, messageType, null, message,
                            contentType, messageUID,
                            correctedMessageUID);

                     messages.add(chatMessage);
                }
            }

            // A consecutive message.
            if (chatMessage == null)
            {
                // Return the last message.
                chatMessage = getMessage(getCount() - 1);

                chatMessage.setMessage(
                    chatMessage.getMessage() + " \n"
                    + message);
            }

            dataChanged();
        }

        /**
         * {@inheritDoc}
         */
        public int getCount()
        {
            synchronized (messages)
            {
                return messages.size();
            }
        }

        /**
         * {@inheritDoc}
         */
        public Object getItem(int position)
        {
            synchronized (messages)
            {
                if (logger.isDebugEnabled())
                    logger.debug("OBTAIN CHAT ITEM ON POSITION: " + position);
                return messages.get(position);
            }
        }

        ChatMessage getMessage(int pos)
        {
            return (ChatMessage) getItem(pos);
        }
    
        /**
         * {@inheritDoc}
         */
        public long getItemId(int pos)
        {
            return pos;
        }

        public int getViewTypeCount()
        {
            return 2;
        }

        public int getItemViewType(int position)
        {
            ChatMessage message = getMessage(position);
            int messageType = message.getMessageType();

            if (messageType == ChatMessage.INCOMING_MESSAGE)
                return INCOMING_MESSAGE_VIEW;
            else if (messageType == ChatMessage.OUTGOING_MESSAGE)
                return OUTGOING_MESSAGE_VIEW;

            return 0;
        }

        /**
         * {@inheritDoc}
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // Keeps reference to avoid future findViewById()
            MessageViewHolder messageViewHolder;

            if (convertView == null)
            {
                LayoutInflater inflater = getActivity().getLayoutInflater();

                messageViewHolder = new MessageViewHolder();

                int viewType = getItemViewType(position);

                messageViewHolder.viewType = viewType;

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
                else
                {
                    convertView = inflater.inflate( R.layout.chat_outgoing_row,
                                                    parent,
                                                    false);

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
                }

                convertView.setTag(messageViewHolder);
            }
            else
            {
                messageViewHolder = (MessageViewHolder) convertView.getTag();
            }

            ChatMessage message = getMessage(position);

            if (message != null)
            {
                Drawable avatar = null;
                Drawable status = null;
                if (messageViewHolder.viewType == INCOMING_MESSAGE_VIEW)
                {
                    avatar = ContactListAdapter.getAvatarDrawable(
                        chatSession.getMetaContact());

                    status = ContactListAdapter.getStatusDrawable(
                        chatSession.getMetaContact());
                }
                else if (messageViewHolder.viewType == OUTGOING_MESSAGE_VIEW)
                {
                    avatar = getLocalAvatarDrawable();
                    status = getLocalStatusDrawable();
                }

                if (avatar == null)
                    avatar = AccountUtil.getDefaultAvatarIcon(getActivity());

                setAvatar(messageViewHolder.avatarView, avatar);
                setStatus(messageViewHolder.statusView, status);

                messageViewHolder.messageView.setText(message.getMessage());
                messageViewHolder.timeView.setText(
                    GuiUtils.formatTime(message.getDate()));
            }

            return convertView;
        }

        private void dataChanged()
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    notifyDataSetChanged();
                }
            });
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
                final Message msg = evt.getSourceMessage();

                if (logger.isTraceEnabled())
                    logger.trace(
                    "MESSAGE DELIVERED: process message to chat for contact: "
                    + contact.getAddress()
                    + " MESSAGE: " + msg.getContent());

                getActivity().runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        addMessage(
                            contact.getProtocolProvider().getAccountID()
                                .getAccountAddress(),
                            getAccountDisplayName(
                                contact.getProtocolProvider()),
                            evt.getTimestamp(),
                            ChatMessage.OUTGOING_MESSAGE,
                            msg.getContent(),
                            msg.getContentType(),
                            msg.getMessageUID(),
                            evt.getCorrectedMessageUID());
                    }
                });
            }
        }

        @Override
        public void messageDeliveryFailed(MessageDeliveryFailedEvent arg0)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void messageReceived(final MessageReceivedEvent evt)
        {
            if (logger.isTraceEnabled())
                logger.trace("MESSAGE RECEIVED from contact: "
                + evt.getSourceContact().getAddress());

            final Contact protocolContact = evt.getSourceContact();
            final ContactResource contactResource = evt.getContactResource();
            final Message message = evt.getSourceMessage();
            final int eventType = evt.getEventType();
            final MetaContact metaContact
                = AndroidGUIActivator.getContactListService()
                    .findMetaContactByContact(protocolContact);

            if(metaContact != null
                && chatSession.getMetaContact().equals(metaContact))
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        addMessage( protocolContact.getAddress(),
                                    metaContact.getDisplayName(),
                                    evt.getTimestamp(),
                                    ChatMessage.getMessageType(evt),
                                    message.getContent(),
                                    message.getContentType(),
                                    message.getMessageUID(),
                                    evt.getCorrectedMessageUID());
                    }
                });
            }
            else
            {
                if (logger.isTraceEnabled())
                    logger.trace("MetaContact not found for protocol contact: "
                        + protocolContact + ".");
            }
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
    }

    static class MessageViewHolder
    {
        ImageView avatarView;
        ImageView statusView;
        ImageView typeIndicator;
        TextView messageView;
        TextView timeView;
        ImageView typingView;
        int viewType;
        int position;
    }

    /**
     * Returns the account user display name for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user display name for the given protocol provider.
     */
    private static String getAccountDisplayName(
        ProtocolProviderService protocolProvider)
    {
        final OperationSetServerStoredAccountInfo accountInfoOpSet
            = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

        try
        {
            if (accountInfoOpSet != null)
            {
                String displayName
                    = AccountInfoUtils.getDisplayName(accountInfoOpSet);
                if(displayName != null && displayName.length() > 0)
                    return displayName;
            }
        }
        catch(Throwable e)
        {
            logger.error("Cannot obtain display name through OPSet");
        }

        return protocolProvider.getAccountID().getDisplayName();
    }

    /**
     * Indicates if this is a consecutive message.
     *
     * @param messageContactAddress the address of the contact associated with
     * the message
     * @param messageType the type of the message. One of the message types
     * defined in the <tt>ChatMessage</tt> class
     * @param messageTime the time at which the message was received/sent
     * @return <tt>true</tt> if the given message is a consecutive message,
     * <tt>false</tt> - otherwise
     */
    private boolean isConsecutiveMessage(   String messageContactAddress,
                                            int messageType,
                                            long messageTime)
    {
        ChatMessage lastMessage = null;
        int messageCount = chatListAdapter.getCount();
        if (messageCount > 0)
            lastMessage = chatListAdapter.getMessage(messageCount - 1);

        if (lastMessage == null)
            return false;

        String contactAddress = lastMessage.getContactName();

        if (contactAddress != null
                && (messageType == ChatMessage.INCOMING_MESSAGE
                    || messageType == ChatMessage.OUTGOING_MESSAGE)
                && contactAddress.equals(messageContactAddress)
                // And if the new message is within a minute from the last one.
                && ((messageTime - lastMessage.getDate().getTime()) < 60000))
        {
            return true;
        }

        return false;
    }

    /**
     * Loads the history in an asynchronous thread and then adds the history
     * messages to the user interface.
     */
    private class LoadHistoryTask
        extends AsyncTask<Void, Void, Collection<Object>>
    {
        @Override
        protected Collection<Object> doInBackground(Void... params)
        {
            return chatSession.getHistory(10);
        }

        @Override
        protected void onPostExecute(Collection<Object> result)
        {
            super.onPostExecute(result);

            loadHistory((Collection<Object>) result, false);
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

            for (int i = 0;
                i <= chatListView.getLastVisiblePosition(); i++)
            {
                RelativeLayout chatRowView
                    = (RelativeLayout) chatListView.getChildAt(
                        i - chatListView.getFirstVisiblePosition());

                if (chatRowView != null
                    && chatListAdapter.getItemViewType(i)
                        == chatListAdapter.INCOMING_MESSAGE_VIEW)
                {
                    Drawable status = ContactListAdapter
                        .getStatusDrawable(
                            chatSession.getMetaContact());

                    ImageView statusView
                        = (ImageView) chatRowView
                            .findViewById(R.id.incomingStatusIcon);

                    setStatus(statusView, status);
                }
            }
        }
    }

    /**
     * Process history messages.
     *
     * @param historyList the collection of messages coming from history
     * @param dataChanged The incoming message needed to be ignored if
     * contained in history.
     */
    private void loadHistory( Collection<Object> historyList,
                              boolean dataChanged)
    {
        Iterator<Object> iterator = historyList.iterator();

        while (iterator.hasNext())
        {
            Object o = iterator.next();

            if(o instanceof MessageDeliveredEvent)
            {
                chatListAdapter.messageDelivered((MessageDeliveredEvent) o);
            }
            else if(o instanceof MessageReceivedEvent)
            {
                chatListAdapter.messageReceived((MessageReceivedEvent) o);
            }
        }

        if(dataChanged)
            chatListAdapter.dataChanged();
    }

    /**
     * Returns the local user avatar drawable.
     *
     * @return the local user avatar drawable
     */
    private static Drawable getLocalAvatarDrawable()
    {
        GlobalDisplayDetailsService displayDetailsService
            = AndroidGUIActivator.getGlobalDisplayDetailsService();

        byte[] avatarImage = displayDetailsService.getGlobalDisplayAvatar();

        if (avatarImage != null)
            return AndroidImageUtil.drawableFromBytes(avatarImage);

        return null;
    }

    /**
     * Returns the local user status drawable.
     *
     * @return the local user status drawable
     */
    private static Drawable getLocalStatusDrawable()
    {
        GlobalStatusService globalStatusService
            = AndroidGUIActivator.getGlobalStatusService();

        byte[] statusImage
            = StatusUtil.getContactStatusIcon(
                globalStatusService.getGlobalPresenceStatus());

        return AndroidImageUtil.drawableFromBytes(statusImage);
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
            avatarDrawable = AccountUtil.getDefaultAvatarIcon(getActivity());

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
                + getResources()
                    .getString(R.string.service_gui_CONTACT_TYPING));
            setVisible = true;
        }
        else if (typingState
                == OperationSetTypingNotifications.STATE_PAUSED)
        {
            typingImgView.setImageResource(R.drawable.typing1);
            typingTextView.setText(
                chatSession.getShortDisplayName()
                + " "
                + getResources()
                    .getString(R.string.service_gui_CONTACT_PAUSED_TYPING));
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
