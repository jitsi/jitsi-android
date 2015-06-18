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
package org.jitsi.android.util;

import android.util.*;

import java.util.logging.*;
import java.util.logging.Handler;

/**
 * Android console handler that outputs to <tt>android.util.Log</tt>.
 *
 * @author Pawel Domas
 */
public class AndroidConsoleHandler
    extends Handler
{
    /**
     * Tag used for output(can be used to filter logcat).
     */
    private final static String TAG = "Jitsi";

    /**
     * Property indicates whether logger should translate logging levels to
     * logcat levels.
     */
    private boolean useAndroidLevels = true;

    static boolean isUseAndroidLevels()
    {
        String property = LogManager.getLogManager().getProperty(
            AndroidConsoleHandler.class.getName()+".useAndroidLevels");

        return property == null || property.equals("true");
    }

    public AndroidConsoleHandler()
    {
        //TODO: failed to set formatter through the properties
        setFormatter(new AndroidLogFormatter());

        useAndroidLevels = isUseAndroidLevels();
    }

    @Override
    public void close(){ }

    @Override
    public void flush(){ }

    @Override
    public void publish(LogRecord record)
    {
        try
        {
            if (this.isLoggable(record))
            {
                String msg = getFormatter().format(record);
                if(!useAndroidLevels)
                {
                    Log.w(TAG, msg);
                }
                else
                {
                    Level level = record.getLevel();
                    if(level == Level.INFO)
                    {
                        Log.i(TAG, msg);
                    }
                    else if(level == Level.SEVERE)
                    {
                        Log.e(TAG, msg);
                    }
                    else if(level == Level.FINE || level == Level.FINER)
                    {
                        Log.d(TAG, msg);
                    }
                    else if(level == Level.FINEST)
                    {
                        Log.v(TAG, msg);
                    }
                    else
                    {
                        Log.w(TAG, msg);
                    }
                }
            }
        }
        catch (Exception e)
        {
            // What a Terrible Failure :)
            Log.wtf(TAG, "Error publishing log output", e);
        }
    }
}
