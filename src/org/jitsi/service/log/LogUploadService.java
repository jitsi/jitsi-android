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
package org.jitsi.service.log;

/**
 * Send/upload logs, to specified destination.
 *
 * @author Damian Minkov
 */
public interface LogUploadService
{
    /**
     * Send the log files.
     * @param destinations array of destination addresses
     * @param subject the subject if available
     * @param title the title for the action, used any intermediate
     *              dialogs that need to be shown, like "Choose action:".
     */
    public void sendLogs(String[] destinations, String subject, String title);
}
