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
package org.jitsi.android.gui.widgets;

import android.content.*;
import android.util.*;
import android.view.*;
import android.widget.*;

/*
 * This class implements <tt>Checkable</tt> interface in order to provide custom
 * <tt>ListView</tt> row layouts that can be checked. The layout retrieves
 * first child <tt>CheckedTextView</tt> and serves as a proxy between
 * the ListView.
 *
 * @author Pawel Domas
 */
public class CheckableRelativeLayout
        extends RelativeLayout
        implements Checkable
{

    /**
     * Instance of <tt>CheckedTextView</tt> to which this layout delegates
     * <tt>Checkable</tt> interface calls.
     */
    private CheckedTextView checkbox;

    /**
     * Creates new instance of <tt>CheckableRelativeLayout</tt>.
     *
     * @param context the context
     * @param attrs attributes set
     */
    public CheckableRelativeLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * Overrides in order to retrieve <tt>CheckedTextView</tt>.
     */
    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        int chCount = getChildCount();
        for (int i = 0; i < chCount; ++i)
        {
            View v = getChildAt(i);
            if (v instanceof CheckedTextView)
            {
                checkbox = (CheckedTextView)v;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isChecked()
    {
        return checkbox != null ? checkbox.isChecked() : false;
    }

    /**
     * {@inheritDoc}
     */
    public void setChecked(boolean checked)
    {
        if (checkbox != null)
        {
            checkbox.setChecked(checked);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toggle()
    {
        if (checkbox != null)
        {
            checkbox.toggle();
        }
    }
}
