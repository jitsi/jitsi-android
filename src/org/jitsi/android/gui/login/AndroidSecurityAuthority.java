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

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.LauncherActivity;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.resources.*;

/**
 * Default <tt>SecurityAuthority</tt> Android implementation.
 *
 * @author Pawel Domas
 */
public class AndroidSecurityAuthority
    implements SecurityAuthority
{
    /**
     * The logger.
     */
    private Logger logger = Logger.getLogger(AndroidSecurityAuthority.class);

    /**
     * If user name should be editable when asked for credentials.
     */
    private boolean isUserNameEditable = false;

    /**
     * Returns a UserCredentials object associated with the specified realm,
     * by specifying the reason of this operation.
     * <p/>
     *
     * @param realm         The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @param reasonCode    indicates the reason for which we're obtaining
     *                      the credentials.
     * @return The credentials associated with the specified realm or null
     *         if none could be obtained.
     */
    public UserCredentials obtainCredentials(String realm,
                                             UserCredentials defaultValues,
                                             int reasonCode)
    {
        if(reasonCode == SecurityAuthority.AUTHENTICATION_REQUIRED
                || reasonCode == WRONG_PASSWORD
                || reasonCode == WRONG_USERNAME)
            return obtainCredentials(realm, defaultValues);

        Context ctx = JitsiApplication.getGlobalContext();
        ResourceManagementService resources
                = AndroidGUIActivator.getResourcesService();

        String errorMessage
                = resources.getI18NString(
                            "service.gui.CONNECTION_FAILED_MSG",
                            new String[]{ defaultValues.getUserName(), realm });

        DialogActivity.showDialog(
                ctx,
                resources.getI18NString("service.gui.LOGIN.FAILED"),
                errorMessage);

        return defaultValues;
    }

    /**
     * Returns a UserCredentials object associated with the specified realm,
     * by specifying the reason of this operation.<p/>
     *
     * @param realm         The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @return The credentials associated with the specified realm or null
     *         if none could be obtained.
     */
    public UserCredentials obtainCredentials(String realm,
                                             final UserCredentials defaultValues)
    {
        if(Looper.myLooper() == Looper.getMainLooper())
        {
            logger.error("Can not obtain credentials from the main thread!");
            return defaultValues;
        }

        // Insert DialogActivity arguments
        Bundle args = new Bundle();

        // Login argument
        args.putString( CredentialsFragment.ARG_LOGIN,
                        defaultValues.getUserName());

        // Login editable ?
        args.putBoolean( CredentialsFragment.ARG_LOGIN_EDITABLE,
                         isUserNameEditable );

        // Password argument
        char[] password = defaultValues.getPassword();
        if(password != null)
        {
            args.putString( CredentialsFragment.ARG_PASSWORD,
                            defaultValues.getPasswordAsString() );
        }

        // Persistent password argument
        args.putBoolean( CredentialsFragment.ARG_STORE_PASSWORD,
                         defaultValues.isPasswordPersistent() );

        // If OSGi has not started wait for
        // the <tt>LauncherActivity</tt> to finish
        if(AndroidGUIActivator.bundleContext == null)
        {
            final Object currentActivityMonitor
                = JitsiApplication.getCurrentActivityMonitor();
            synchronized (currentActivityMonitor)
            {
                while (true)
                {
                    Activity currentActivity
                        = JitsiApplication.getCurrentActivity();
                    if(currentActivity != null
                        && !(currentActivity instanceof LauncherActivity))
                    {
                        break;
                    }
                    try
                    {
                        logger.warn(
                            "Enter password dialog waiting for any Activity");
                        currentActivityMonitor.wait();
                    }
                    catch (InterruptedException e)
                    {
                        logger.error(e, e);
                    }
                }
            }
        }

        ResourceManagementService resources
                = AndroidGUIActivator.getResourcesService();

        // Obtain credentials lock
        final Object credentialsLock = new Object();

        // Displays the credentials dialog and waits for it to complete
        DialogActivity.showCustomDialog(
                JitsiApplication.getGlobalContext(),
                resources.getI18NString("service.gui.LOGIN_FAILED"),
                CredentialsFragment.class.getName(),
                args,
                resources.getI18NString("sign_in"),
                new DialogActivity.DialogListener()
                {
                    public boolean onConfirmClicked(DialogActivity dialog)
                    {
                        View dialogContent
                                = dialog.findViewById(R.id.alertContent);

                        String userName = ViewUtil.
                                getTextViewValue(dialogContent, R.id.username);
                        String password = ViewUtil.
                                getTextViewValue(dialogContent, R.id.password);
                        boolean storePassword = ViewUtil.
                                isCompoundChecked( dialogContent,
                                                   R.id.store_password);

                        defaultValues.setUserName(userName);
                        defaultValues.setPassword(password.toCharArray());
                        defaultValues.setPasswordPersistent(storePassword);

                        synchronized (credentialsLock)
                        {
                            credentialsLock.notify();
                        }

                        return true;
                    }

                    public void onDialogCancelled(DialogActivity dialog)
                    {
                        synchronized (credentialsLock)
                        {
                            credentialsLock.notify();
                        }
                    }
                }, null);
        try
        {
            synchronized (credentialsLock)
            {
                // Wait for the credentials
                credentialsLock.wait();
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        return defaultValues;
    }

    /**
     * Sets the userNameEditable property, which should indicate to the
     * implementations of this interface if the user name could be changed
     * by user or not.
     *
     * @param isUserNameEditable indicates if the user name could be changed
     *        by user in the implementation of this interface.
     */
    public void setUserNameEditable(boolean isUserNameEditable)
    {
        this.isUserNameEditable = isUserNameEditable;
    }

    /**
     * Indicates if the user name is currently editable, i.e. could be
     * changed by user or not.
     *
     * @return <code>true</code> if the user name could be changed,
     *         <code>false</code> - otherwise.
     */
    public boolean isUserNameEditable()
    {
        return isUserNameEditable;
    }
}
