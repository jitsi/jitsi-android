/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.os.*;
import android.view.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

import java.io.*;
import java.util.*;

/**
 * Fragment can be used to display indeterminate progress dialogs.
 *
 * @author Pawel Domas
 */
public class ProgressDialogFragment
    extends OSGiFragment
{
    /**
     * Argument used to retrieve the message that will be displayed next to
     * the progress bar.
     */
    private static final String ARG_MESSAGE = "progress_dialog_message";

    public ProgressDialogFragment(){ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View progressView
            = inflater.inflate(R.layout.progress_dialog, container, false);

        ViewUtil.setTextViewValue(
            progressView, R.id.textView,
            getArguments().getString(ARG_MESSAGE));

        return progressView;
    }

    /**
     * Displays indeterminate progress dialog.
     * @param title dialog's title
     * @param message the message to be displayed next to the progress bar.
     * @return dialog id that can be used to close the dialog
     * {@link DialogActivity#closeDialog(android.content.Context, long)}.
     */
    public static long showProgressDialog(String title, String message)
    {
        Map<String, Serializable> extras
            = new HashMap<String, Serializable>();
        extras.put(DialogActivity.EXTRA_CANCELABLE, false);
        extras.put(DialogActivity.EXTRA_REMOVE_BUTTONS, true);

        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);

        return DialogActivity.showCustomDialog(
            JitsiApplication.getGlobalContext(),
            title,
            ProgressDialogFragment.class.getName(),
            args,
            null, null, extras);
    }
}
