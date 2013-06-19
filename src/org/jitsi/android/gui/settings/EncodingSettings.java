/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
