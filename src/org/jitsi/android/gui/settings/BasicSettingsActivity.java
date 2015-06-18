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
package org.jitsi.android.gui.settings;

import android.content.pm.*;
import android.os.*;

import org.jitsi.service.osgi.*;

/**
 * Base class for settings screens which only adds preferences
 * from XML resource. By default preference resource id is obtained from
 * <tt>Activity</tt> meta-data, resource key: "android.preference".
 *
 * @author Pawel Domas
 */
public class BasicSettingsActivity
    extends OSGiPreferenceActivity
{

    /**
     * Returns preference XML resource ID.
     * @return preference XML resource ID.
     */
    protected int getPreferencesXmlId()
    {
        // Cant' find custom preference classes using:
        //addPreferencesFromIntent(getActivity().getIntent());
        try
        {
            ActivityInfo app = getPackageManager()
                    .getActivityInfo(
                            getComponentName(),
                            PackageManager.GET_ACTIVITIES
                                    | PackageManager.GET_META_DATA);
            return app.metaData.getInt("android.preference");
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(getPreferencesXmlId());
    }
}
