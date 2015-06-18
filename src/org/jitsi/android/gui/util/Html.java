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
package org.jitsi.android.gui.util;

/**
 * Copy of <tt>android.text.Html</tt> utility methods to be used on lower API
 * levels.
 *
 * @author Pawel Domas
 */
public class Html
{
    /**
     * Returns an HTML escaped representation of the given plain text.
     */
    public static String escapeHtml(CharSequence text)
    {
        StringBuilder out = new StringBuilder();
        withinStyle(out, text, 0, text.length());
        return out.toString();
    }

    private static void withinStyle(StringBuilder out, CharSequence text,
                                    int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            char c = text.charAt(i);

            if (c == '<')
            {
                out.append("&lt;");
            }
            else if (c == '>')
            {
                out.append("&gt;");
            }
            else if (c == '&')
            {
                out.append("&amp;");
            }
            else if (c > 0x7E || c < ' ')
            {
                out.append("&#").append((int) c).append(";");
            }
            else if (c == ' ')
            {
                while (i + 1 < end && text.charAt(i + 1) == ' ')
                {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            }
            else
            {
                out.append(c);
            }
        }
    }
}
