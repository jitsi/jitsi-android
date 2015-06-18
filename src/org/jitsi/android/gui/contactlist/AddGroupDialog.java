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
package org.jitsi.android.gui.contactlist;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.*;
import org.jitsi.service.osgi.*;

/**
 * Dialog allowing user to create new contact group.
 *
 * @author Pawel Domas
 */
public class AddGroupDialog
    extends OSGiFragment
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(AddGroupDialog.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.create_group, container, false);
    }

    /**
     * Displays create contact group dialog. If the source wants to be notified
     * about the result should pass the listener here or <tt>null</tt>
     * otherwise.
     *
     * @param parent the parent <tt>Activity</tt>
     * @param createListener listener for contact group created event that will
     *                       receive newly created instance of the contact group
     *                       or <tt>null</tt> in case user cancels the dialog.
     */
    public static void showCreateGroupDialog(
            Activity parent,
            EventListener<MetaContactGroup> createListener)
    {
        DialogActivity.showCustomDialog(
                parent,
                parent.getString(
                    R.string.service_gui_CREATE_GROUP),
                AddGroupDialog.class.getName(),
                null,
                parent.getString(
                    R.string.service_gui_CREATE),
                new DialogListenerImpl(createListener),
                null);
    }

    /**
     * Implements <tt>DialogActivity.DialogListener</tt> interface and handles
     * contact group creation process.
     */
    static class DialogListenerImpl
        implements DialogActivity.DialogListener
    {
        /**
         * Contact created event listener.
         */
        private final EventListener<MetaContactGroup> listener;

        /**
         * Newly created contact group.
         */
        private MetaContactGroup newMetaGroup;

        /**
         * Thread that runs create group process.
         */
        private Thread createThread;

        /**
         * Creates new instance of <tt>DialogListenerImpl</tt>.
         * @param createListener create group listener if any.
         */
        public DialogListenerImpl(
                EventListener<MetaContactGroup> createListener)
        {
            this.listener = createListener;
        }

        //private ProgressDialog progressDialog;

        @Override
        public boolean onConfirmClicked(DialogActivity dialog)
        {
            if(createThread != null)
                return false;

            AddGroupDialog content
                = (AddGroupDialog) dialog.getContentFragment();
            EditText editText
                = (EditText) content.getView().findViewById(R.id.editText);

            String groupName = editText.getText().toString().trim();
            if (groupName.length() == 0)
            {
                showErrorMessage(
                        dialog.getString(
                                R.string.service_gui_ADD_GROUP_EMPTY_NAME,
                                groupName));
                return false;
            }
            else
            {
                // TODO: in progress dialog removed for simplicity
                // Add it here if operation will be taking too much time
                // (seems to finish fast for now)
                //displayOperationInProgressDialog(dialog);

                this.createThread
                    = new CreateGroup(
                        AndroidGUIActivator.getContactListService(),
                        groupName);
                createThread.start();

                try
                {
                    // Wait for create group thread to finish
                    createThread.join();
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }

                if(listener != null)
                    listener.onChangeEvent(newMetaGroup);

                return true;
            }
        }

        /**
         * Shows given error message as an alert.
         * @param errorMessage the error message to show.
         */
        private void showErrorMessage(String errorMessage)
        {
            Context ctx = JitsiApplication.getGlobalContext();
            AndroidUtils.showAlertDialog(
                    ctx,
                    ctx.getString(R.string.service_gui_ERROR),
                    errorMessage);
        }

        @Override
        public void onDialogCancelled(DialogActivity dialog){ }

        /**
         * Shows the "in progress" dialog
         */
        /*private void displayOperationInProgressDialog(DialogActivity dialog)
        {
            Context context = dialog;
            CharSequence title = context.getText(
                    R.string.service_gui_COMMIT_PROGRESS_TITLE);
            CharSequence msg = context.getText(
                    R.string.service_gui_COMMIT_PROGRESS_MSG);

            this.progressDialog = ProgressDialog.show(
                    context, title, msg, true, false);
            // Display the progress dialog
            progressDialog.show();
        }*/

        /**
         * Hides the "in progress" dialog
         */
        /*private void hideOperationInProgressDialog()
        {
            if(progressDialog != null)
            {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }*/

        /**
         * Creates a new meta contact group in a separate thread.
         */
        private class CreateGroup extends Thread
        {
            /**
             * Contact list instance.
             */
            MetaContactListService mcl;

            /**
             * Name of the contact group to create.
             */
            String groupName;

            /**
             * Creates new instance of <tt>AddGroupDialog</tt>.
             * @param mcl contact list service instance.
             * @param groupName name of the contact group to create.
             */
            CreateGroup(MetaContactListService mcl, String groupName)
            {
                this.mcl = mcl;
                this.groupName = groupName;
            }

            @Override
            public void run()
            {
                try
                {
                    newMetaGroup
                        = mcl.createMetaContactGroup(mcl.getRoot(), groupName);
                }
                catch (MetaContactListException ex)
                {
                    logger.error(ex);
                    Context ctx = JitsiApplication.getGlobalContext();

                    int errorCode = ex.getErrorCode();

                    if (errorCode
                            == MetaContactListException
                            .CODE_GROUP_ALREADY_EXISTS_ERROR)
                    {
                        showErrorMessage(
                            ctx.getString(
                                    R.string.service_gui_ADD_GROUP_EXIST_ERROR,
                                    groupName));
                    }
                    else if (errorCode
                            == MetaContactListException.CODE_LOCAL_IO_ERROR)
                    {
                        showErrorMessage(
                            ctx.getString(
                                    R.string.service_gui_ADD_GROUP_LOCAL_ERROR,
                                    groupName));
                    }
                    else if (errorCode
                            == MetaContactListException.CODE_NETWORK_ERROR)
                    {
                        showErrorMessage(
                            ctx.getString(
                                    R.string.service_gui_ADD_GROUP_NET_ERROR,
                                    groupName));
                    }
                    else
                    {
                        showErrorMessage(
                            ctx.getString(
                                    R.string.service_gui_ADD_GROUP_ERROR,
                                    groupName));
                    }
                }
                /*finally
                {
                    hideOperationInProgressDialog();
                }*/
            }
        }
    }
}
