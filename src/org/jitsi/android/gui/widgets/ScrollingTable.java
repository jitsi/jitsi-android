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
import org.jitsi.*;

/**
 * Custom layout that handles fixes table header, by measuring maximum column
 * widths in both header and table body. Then synchronizes those maximum
 * values in header and body columns widths.
 *
 * @author Pawel Domas
 */
public class ScrollingTable
    extends LinearLayout
{
    /**
     * Create new instance of <tt>ScrollingTable</tt>
     * @param context the context
     */
    public ScrollingTable(Context context)
    {
        super(context);
    }

    /**
     * Creates new instance of <tt>ScrollingTable</tt>.
     * @param context the context
     * @param attrs the attribute set
     */
    public ScrollingTable(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top,
                            int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        TableLayout header = (TableLayout) findViewById( R.id.table_header );
        TableLayout body = (TableLayout) findViewById( R.id.table_body );

        // Find max column widths
        int[] headerWidths = findMaxWidths(header);
        int[] bodyWidths = findMaxWidths(body);

        if(bodyWidths == null)
        {
            // Table is empty
            return;
        }

        int[] maxWidths = new int[bodyWidths.length];

        for(int i=0; i<headerWidths.length;i++)
        {
            maxWidths[i] = headerWidths[i] > bodyWidths[i]
                    ? headerWidths[i] : bodyWidths[i];
        }

        //Set column widths to max values
        setColumnWidths(header, maxWidths);
        setColumnWidths(body, maxWidths);
    }

    /**
     * Finds maximum columns widths in given table layout.
     * @param table table layout that will be examined for max column widths.
     * @return array of max columns widths for given table, it's length is
     *         equal to table's column count.
     */
    private int[] findMaxWidths(TableLayout table)
    {
        int[] colWidths = null;

        for ( int rowNum = 0; rowNum < table.getChildCount(); rowNum++ )
        {
            TableRow row = (TableRow) table.getChildAt( rowNum );

            if(colWidths == null)
                colWidths = new int[row.getChildCount()];

            for ( int colNum = 0; colNum < row.getChildCount(); colNum++ )
            {
                int cellWidth = row.getChildAt( colNum ).getWidth();
                if( cellWidth > colWidths[colNum] )
                {
                    colWidths[colNum] = cellWidth;
                }
            }
        }

        return colWidths;
    }

    /**
     * Adjust given table columns width to sizes given in <tt>widths</tt> array.
     * @param table the table layout which columns will be adjusted
     * @param widths array of columns widths to set
     */
    private void setColumnWidths(TableLayout table, int[] widths)
    {
        for ( int rowNum = 0; rowNum < table.getChildCount(); rowNum++ )
        {
            TableRow row = (TableRow) table.getChildAt( rowNum );

            for ( int colNum = 0; colNum < row.getChildCount(); colNum++ )
            {
                View column = row.getChildAt( colNum );
                TableRow.LayoutParams params
                        = (TableRow.LayoutParams)column.getLayoutParams();
                params.width = widths[colNum];
            }
        }
    }
}
