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

import android.*;
import android.os.*;
import android.view.*;
import org.jitsi.android.gui.account.settings.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * @author Pawel Domas
 */
public class EncodingSettings
    extends OSGiActivity
{
    public static final String EXTRA_MEDIA_TYPE = "media_type";

    public static final String MEDIA_TYPE_AUDIO = "media_type.AUDIO";

    public static final String MEDIA_TYPE_VIDEO = "media_type.VIDEO";

    private EncodingsFragment encodingsFragment;

    private MediaType mediaType;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String mediaTypeStr = getIntent().getStringExtra(EXTRA_MEDIA_TYPE);
        if(mediaTypeStr.equals(MEDIA_TYPE_AUDIO))
        {
            this.mediaType = MediaType.AUDIO;
        }
        else if(mediaTypeStr.equals(MEDIA_TYPE_VIDEO))
        {
            this.mediaType = MediaType.VIDEO;
        }

        if(savedInstanceState == null)
        {
            MediaServiceImpl mediaSrvc
                    = NeomediaActivator.getMediaServiceImpl();
            EncodingConfiguration encConfig
                    = mediaSrvc.getCurrentEncodingConfiguration();

            List<MediaFormat> formats
                    = EncodingActivity.getEncodings(encConfig, mediaType);
            List<String> encodings
                    = EncodingActivity.getEncodingsStr(formats.iterator());
            List<Integer> priorities
                    = EncodingActivity.getPriorities(formats, encConfig);

            this.encodingsFragment
                    = EncodingsFragment.newInstance(encodings, priorities);
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, encodingsFragment)
                    .commit();
        }
        else
        {
            this.encodingsFragment
                    = (EncodingsFragment) getFragmentManager()
                    .findFragmentById(R.id.content);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            EncodingActivity.commitPriorities(
                    NeomediaActivator
                            .getMediaServiceImpl()
                            .getCurrentEncodingConfiguration(),
                    mediaType,
                    encodingsFragment);
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
