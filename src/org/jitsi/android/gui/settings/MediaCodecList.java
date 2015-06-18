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

import android.content.res.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import org.jitsi.R;
import org.jitsi.impl.neomedia.codec.video.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * Activity that lists video <tt>MediaCodec</tt>s available in the system.
 * <p/>
 * Meaning of the colors:</br><br/>
 * blue - codec will be used in call<br/>
 * white - one of the codecs for particular media type, but it won't be used as
 * there is another codec before it on the list<br/>
 * grey - codec is banned and won't be used<br/>
 * <p/>
 * Click on codec to toggle it's banned state. Changes are not persistent
 * between Jitsi restarts so restarting jitsi restores default values.
 *
 * @author Pawel Domas
 */
public class MediaCodecList
    extends OSGiActivity
    implements AdapterView.OnItemClickListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_layout);

        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(new MediaCodecAdapter());

        list.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id)
    {
        MediaCodecAdapter adapter = (MediaCodecAdapter) parent.getAdapter();
        CodecInfo codec = (CodecInfo) adapter.getItem(position);

        // Toggle codec banned state
        codec.setBanned(!codec.isBanned());

        adapter.notifyDataSetChanged();
    }

    class MediaCodecAdapter
        extends BaseAdapter
    {
        private final ArrayList<CodecInfo> codecs;

        MediaCodecAdapter()
        {
            codecs = new ArrayList<CodecInfo>(CodecInfo.getSupportedCodecs());
        }

        @Override
        public int getCount()
        {
            return codecs.size();
        }

        @Override
        public Object getItem(int position)
        {
            return codecs.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            TextView row = (TextView) getLayoutInflater().inflate(
                android.R.layout.simple_list_item_1, parent, false);

            CodecInfo codec = codecs.get(position);
            String codecStr = codec.toString();
            row.setText(codecStr);

            Resources res = getResources();
            int color = codec.isBanned() ? R.color.grey : R.color.white;
            if(codec.isNominated())
            {
                color = R.color.blue;
            }
            row.setTextColor(res.getColor(color));

            return row;
        }
    }
}
