/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.login;

import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import org.jitsi.*;
import org.jitsi.android.gui.util.*;

/**
 * The credentials fragment can be used to retrieve username, password and the
 * "store password" status. Use the arguments to fill the fragment with default
 * values. Supported arguments are:<br/>
 * - {@link #ARG_LOGIN} login default text value<br/>
 * - {@link #ARG_LOGIN_EDITABLE} <tt>boolean</tt> flag indicating if the login
 * field is editable<br/>
 * - {@link #ARG_PASSWORD} password default text value<br/>
 * - {@link #ARG_STORE_PASSWORD} "store password" default <tt>boolean</tt> value
 *
 * @author Pawel Domas
 */
public class CredentialsFragment
    extends Fragment
{

    /**
     * Pre-entered login argument.
     */
    public static final String ARG_LOGIN="login";

    /**
     * Pre-entered password argument.
     */
    public static final String ARG_PASSWORD="password";

    /**
     * Argument indicating whether the login can be edited.
     */
    public static final String ARG_LOGIN_EDITABLE="login_editable";

    /**
     * Pre-entered "store password" <tt>boolean</tt> value.
     */
    public static final String ARG_STORE_PASSWORD = "store_pass";

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        View content = inflater.inflate(R.layout.credentials, container, false);

        ViewUtil.setTextViewValue(content,
                                  R.id.username,
                                  args.getString(ARG_LOGIN));

        boolean loginEditable = args.getBoolean(ARG_LOGIN_EDITABLE, true);
        content.findViewById(R.id.username).setEnabled(loginEditable);

        ViewUtil.setTextViewValue( content,
                                   R.id.password,
                                   args.getString(ARG_PASSWORD));

        ViewUtil.setCompoundChecked(content,
                                    R.id.store_password,
                                    args.getBoolean(ARG_STORE_PASSWORD, true));

        return content;
    }
}
