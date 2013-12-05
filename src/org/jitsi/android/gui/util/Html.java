/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
