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
package org.jitsi.impl.androidversion;

import net.java.sip.communicator.util.*;
import org.jitsi.service.resources.*;
import org.jitsi.service.version.util.*;

/**
 * Android version service implementation.
 *
 * @author Pawel Domas
 */
public class VersionImpl
    extends AbstractVersion
{
    /**
     * Default application name.
     */
    private static final String DEFAULT_APPLICATION_NAME = "Jitsi";

    /**
     * The name of this application.
     */
    private static String applicationName = null;

    /**
     * Indicates if this Jitsi version corresponds to a nightly build
     * of a repository snapshot or to an official Jitsi release.
     */
    public static final boolean IS_NIGHTLY_BUILD = true;

    /**
     * Creates new instance of <tt>VersionImpl</tt> with given major, minor and
     * nightly build id parameters.
     * @param major the major version number.
     * @param minor the minor version number.
     * @param nightBuildID the nightly build id.
     */
    public VersionImpl(int major, int minor, String nightBuildID)
    {
        super(major, minor, nightBuildID);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNightly()
    {
        return IS_NIGHTLY_BUILD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPreRelease()
    {
        return false;
    }

    /**
     * Returns the version prerelease ID of the current Jitsi version
     * and null if this version is not a prerelease.
     *
     * @return a String containing the version prerelease ID.
     */
    public String getPreReleaseID()
    {
        return null;
    }

    /**
     * Returns the name of the application that we're currently running. Default
     * MUST be Jitsi.
     *
     * @return the name of the application that we're currently running. Default
     * MUST be Jitsi.
     */
    public String getApplicationName()
    {
        if (applicationName == null)
        {
            try
            {
                /*
                 * XXX There is no need to have the ResourceManagementService
                 * instance as a static field of the VersionImpl class because
                 * it will be used once only anyway.
                 */
                ResourceManagementService resources
                    = ServiceUtils.getService(
                        VersionActivator.bundleContext,
                        ResourceManagementService.class);

                if (resources != null)
                {
                    applicationName
                        = resources.getSettingsString(
                            "service.gui.APPLICATION_NAME");
                }
            }
            catch (Exception e)
            {
                // if resource bundle is not found or the key is missing
                // return the default name
            }
            finally
            {
                if (applicationName == null)
                    applicationName = DEFAULT_APPLICATION_NAME;
            }
        }
        return applicationName;
    }
}
