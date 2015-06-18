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
