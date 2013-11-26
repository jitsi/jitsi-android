/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import android.content.*;

import net.java.sip.communicator.service.contactlist.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;

/**
 * The <tt>MetaContactListManager</tt> is the class through which we make
 * operations with the <tt>MetaContactList</tt>. All methods in this class are
 * static.
 *
 * @author Pawel Domas
 */
public class MetaContactListManager
{
    /**
     * Removes given <tt>contact</tt> from the contact list.
     * Asks the user for confirmation, before it's done.
     *
     * @param contact the contact to be removed from the contact list.
     */
    public static void removeMetaContact(final MetaContact contact)
    {
        Context ctx = JitsiApplication.getGlobalContext();
        DialogActivity.showConfirmDialog(
                ctx,
                ctx.getString(R.string.service_gui_REMOVE_CONTACT),
                ctx.getString(R.string.service_gui_REMOVE_CONTACT_TEXT,
                              contact.getDisplayName()),
                ctx.getString(R.string.service_gui_REMOVE),
                new DialogActivity.DialogListener()
                {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog)
                    {
                        MetaContactListService mls
                                = AndroidGUIActivator.getContactListService();
                        mls.removeMetaContact(contact);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogActivity dialog){ }
                }
        );
    }

    /**
     * Removes the given <tt>MetaContactGroup</tt> from the list.
     * @param group the <tt>MetaContactGroup</tt> to remove
     */
    public static void removeMetaContactGroup(final MetaContactGroup group)
    {
        Context ctx = JitsiApplication.getGlobalContext();
        String message = ctx.getString(
                R.string.service_gui_REMOVE_CONTACT_TEXT,
                group.getGroupName());

        DialogActivity.showConfirmDialog(
                ctx,
                ctx.getString(R.string.service_gui_REMOVE),
                message,
                ctx.getString(R.string.service_gui_REMOVE_GROUP),
                new DialogActivity.DialogListener()
                {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog)
                    {
                        doRemoveGroup(group);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogActivity dialog){ }
                });
        ///int returnCode = dialog.showDialog();

        //if (returnCode == MessageDialog.OK_RETURN_CODE)
        //{
          //  GuiActivator.getContactListService()
            //        .removeMetaContactGroup(group);
        //}
        //else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
        //{
          //  GuiActivator.getContactListService()
            //        .removeMetaContactGroup(group);

            //Constants.REMOVE_CONTACT_ASK = false;
        //}
    }

    /**
     * Removes given group from the contact list. Catches any exceptions and
     * shows error alert.
     *
     * @param group the group to remove from the contact list.
     */
    private static void doRemoveGroup(MetaContactGroup group)
    {
        Context ctx = JitsiApplication.getGlobalContext();
        try
        {
            AndroidGUIActivator.getContactListService()
                    .removeMetaContactGroup(group);
        }
        catch (Exception ex)
        {
            AndroidUtils.showAlertDialog(
                    ctx,
                    ctx.getString(R.string.service_gui_REMOVE_GROUP),
                    ex.getMessage());
        }
    }
}
