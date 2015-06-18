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
package org.jitsi.impl.osgi.framework.launch;

import java.util.*;

import org.osgi.framework.launch.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class FrameworkFactoryImpl
    implements FrameworkFactory
{
    public Framework newFramework(Map<String, String> configuration)
    {
        return new FrameworkImpl(configuration);
    }
}
