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
package org.jitsi.service.neomedia;

import android.content.*;
import android.view.*;

/**
 * Declares the interface to be supported by providers of access to
 * {@link View}s.
 *
 * @author Lyubomir Marinov
 */
public interface ViewAccessor
{
    /**
     * Gets the {@link View} provided by this instance which is to be used in a
     * specific {@link Context}.
     *
     * @param context the <tt>Context</tt> in which the provided <tt>View</tt>
     * will be used
     * @return the <tt>View</tt> provided by this instance which is to be used
     * in a specific <tt>Context</tt>
     */
    public View getView(Context context);
}