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
import android.content.res.*;
import android.graphics.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;

/**
 * Modified version of class of the same name from android source of the music
 * app. The widget displays a list of items. User can set order of items by 
 * dragging them on the screen.<br/>
 * This <tt>View</tt> requires following XML attributes:<br/> 
 * - <tt>itemHeight</tt> the height of list item<br/>
 * - <tt>itemExpandedHeight</tt> the height that will be set to expanded 
 * item(the one tha makes space for dragged item)<br/>
 * - <tt>dragRegionStartX</tt> and <tt>dragRegionEndX</tt> item can be
 * grabbed when start x coordinate is between them
 * 
 * @author Pawel Domas
 */
public class TouchInterceptor 
        extends ListView
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(TouchInterceptor.class);
    /**
     * The view representing dragged item 
     */
    private ImageView dragView;
    /**
     * The {@link android.view.WindowManager} used to display dragged view
     */
    private WindowManager windowManager;
    /**
     * Layout parameters of dragged view
     */
    private WindowManager.LayoutParams windowParams;
    /**
     * At which position is the item currently being dragged. Note that this
     * takes in to account header items.
     */
    private int dragPos;
    /**
     * At which position was the item being dragged originally
     */
    private int srcDragPos;
    /**
     * At what x offset inside the item did the user grab it
     */
    private int dragPointX;
    /**
     * At what y offset inside the item did the user grab it
     */
    private int dragPointY;
    /**
     * The difference between screen coordinates and coordinates in this view
     */
    private int xOffset;
    /**
     * The difference between screen coordinates and coordinates in this view
     */
    private int yOffset;
    /**
     * The {@link DragListener}
     */
    private DragListener dragListener;
    /**
     * The {@link DropListener}
     */
    private DropListener dropListener;
    /**
     * Upper boundary, when reached the view is scrolled up
     */
    private int upperBound;
    /**
     * Lower boundary, when reached the view is scrolled down
     */
    private int lowerBound;
    /**
     * View's height
     */
    private int height;
    /**
     *
     */
    private Rect tempRect = new Rect();
    /**
     * The background of dragged <tt>View</tt>
     */
    private Bitmap dragBitmap;
    /**
     * The touch slop
     */
    private final int touchSlop;
    /**
     * Normal list item's height
     */
    private int itemHeightNormal;
    /**
     * The height of expanded item
     */
    private int itemHeightExpanded;
    /**
     * Half of the normal item's height
     */
    private int itemHeightHalf;
    /**
     * Start x coordinate of draggable area
     */
    private int dragRegionStartX;
    /**
     * End x coordinate of draggable area
     */
    private int dragRegionEndX;

    /**
     * Creates new instance of {@link TouchInterceptor}.
     * The attribute set must contain:</br>
     * - <tt>itemHeight</tt> the height of list item<br/>
     * - <tt>itemExpandedHeight</tt> the height that will be set to expanded
     * item<br/>
     * - <tt>dragRegionStartX</tt> and <tt>dragRegionEndX</tt> item can be
     * grabbed when start x coordinate is between them
     *
     * @param context the {@link android.content.Context}
     * @param attrs the {@link android.util.AttributeSet}
     */
    public TouchInterceptor(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TouchInterceptor,
                0, 0);
        try
        {
            itemHeightNormal = a.getDimensionPixelSize(
                    R.styleable.TouchInterceptor_itemHeight, -1);
            itemHeightHalf = itemHeightNormal / 2;

            if(itemHeightNormal == -1)
                throw new IllegalArgumentException(
                        "Item height attribute unspecified");

            itemHeightExpanded = a.getDimensionPixelSize(
                    R.styleable.TouchInterceptor_itemExpandedHeight, -1);

            if(itemHeightExpanded == -1)
                throw new IllegalArgumentException(
                        "Expanded item height attribute unspecified");

            dragRegionStartX = a.getDimensionPixelSize(
                    R.styleable.TouchInterceptor_dragRegionStartX, -1);
            dragRegionEndX = a.getDimensionPixelSize(
                    R.styleable.TouchInterceptor_dragRegionEndX, -1);

            if(dragRegionStartX == -1 || dragRegionEndX == -1)
                throw new IllegalArgumentException(
                        "Drag region attributes unspecified");
        }
        finally
        {
            a.recycle();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if (dragListener != null || dropListener != null)
        {
            if(!isEnabled())
            {
                return true;
            }
            switch (ev.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    int itemnum = pointToPosition(x, y);
                    if (itemnum == AdapterView.INVALID_POSITION)
                    {
                        break;
                    }
                    View item =
                            getChildAt(itemnum - getFirstVisiblePosition());
                    dragPointX = x - item.getLeft();
                    dragPointY = y - item.getTop();
                    xOffset = ((int)ev.getRawX()) - x;
                    yOffset = ((int)ev.getRawY()) - y;
                    if(logger.isDebugEnabled())
                    {
                        logger.debug(
                                "Dragging "+x+","+y
                                + " sxy: "+dragRegionStartX
                                + ","+dragRegionEndX);
                    }
                    if (x >= dragRegionStartX && x <= dragRegionEndX)
                    {
                        item.setDrawingCacheEnabled(true);
                        // Create a copy of the drawing cache so that it does
                        // not get recycled by the framework when the list tries
                        // to clean up memory
                        Bitmap bitmap =
                                Bitmap.createBitmap(item.getDrawingCache());
                        startDragging(bitmap, x, y);
                        dragPos = itemnum;
                        srcDragPos = dragPos;
                        height = getHeight();
                        int touchSlop = this.touchSlop;
                        upperBound = Math.min(y - touchSlop, height / 3);
                        lowerBound = Math.max(y + touchSlop, height * 2 /3);
                        return false;
                    }
                    stopDragging();
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    /*
     * pointToPosition() doesn't consider invisible views, but we
     * need to, so implement a slightly different version.
     */
    private int myPointToPosition(int x, int y)
    {

        if (y < 0)
        {
            // when dragging off the top of the screen, calculate position
            // by going back from a visible item
            int pos = myPointToPosition(x, y + itemHeightNormal);
            if (pos > 0)
            {
                return pos - 1;
            }
        }

        Rect frame = tempRect;
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--)
        {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y))
            {
                return getFirstVisiblePosition() + i;
            }
        }
        return INVALID_POSITION;
    }

    private int getItemForPosition(int y)
    {
        int adjustedy = y - dragPointY - itemHeightHalf;
        int pos = myPointToPosition(0, adjustedy);
        if (pos >= 0)
        {
            if (pos <= srcDragPos)
            {
                pos += 1;
            }
        }
        else if (adjustedy < 0)
        {
            // this shouldn't happen anymore now that myPointToPosition deals
            // with this situation
            pos = 0;
        }
        return pos;
    }

    private void adjustScrollBounds(int y)
    {
        if (y >= height / 3)
        {
            upperBound = height / 3;
        }
        if (y <= height * 2 / 3)
        {
            lowerBound = height * 2 / 3;
        }
    }

    /*
     * Restore size and visibility for all listitems
     */
    private void unExpandViews(boolean deletion)
    {
        for (int i = 0;; i++)
        {
            View v = getChildAt(i);
            if (v == null)
            {
                if (deletion)
                {
                    // HACK force update of mItemCount
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                    // end hack
                }
                try
                {
                    // force children to be recreated where needed
                    layoutChildren();
                    v = getChildAt(i);
                }
                catch (IllegalStateException ex)
                {
                    // layoutChildren throws this sometimes, presumably because
                    // we're in the process of being torn down but are still
                    // getting touch events
                }
                if (v == null)
                {
                    return;
                }
            }
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = itemHeightNormal;
            v.setLayoutParams(params);
            v.setVisibility(View.VISIBLE);
        }
    }

    /* Adjust visibility and size to make it appear as though
     * an item is being dragged around and other items are making
     * room for it:
     * If dropping the item would result in it still being in the
     * same place, then make the dragged listitem's size normal,
     * but make the item invisible.
     * Otherwise, if the dragged listitem is still on screen, make
     * it as small as possible and expand the item below the insert
     * point.
     * If the dragged item is not on screen, only expand the item
     * below the current insertpoint.
     */
    private void doExpansion()
    {
        int childnum = dragPos - getFirstVisiblePosition();
        if (dragPos > srcDragPos)
        {
            childnum++;
        }
        int numheaders = getHeaderViewsCount();

        View first = getChildAt(srcDragPos - getFirstVisiblePosition());
        for (int i = 0;; i++)
        {
            View vv = getChildAt(i);
            if (vv == null)
            {
                break;
            }

            int height = itemHeightNormal;
            int visibility = View.VISIBLE;
            if (dragPos < numheaders && i == numheaders)
            {
                // dragging on top of the header item, so adjust the item below
                // instead
                if (vv.equals(first))
                {
                    visibility = View.INVISIBLE;
                } else
                {
                    height = itemHeightExpanded;
                }
            }
            else if (vv.equals(first))
            {
                // processing the item that is being dragged
                if ( dragPos == srcDragPos
                  || getPositionForView(vv) == getCount() - 1)
                {
                    // hovering over the original location
                    visibility = View.INVISIBLE;
                }
                else
                {
                    // not hovering over it
                    // Ideally the item would be completely gone, but neither
                    // setting its size to 0 nor settings visibility to GONE
                    // has the desired effect.
                    height = 1;
                }
            }
            else if (i == childnum)
            {
                if (dragPos >= numheaders && dragPos < getCount() - 1)
                {
                    height = itemHeightExpanded;
                }
            }
            ViewGroup.LayoutParams params = vv.getLayoutParams();
            params.height = height;
            vv.setLayoutParams(params);
            vv.setVisibility(visibility);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        if ((dragListener != null || dropListener != null) && dragView != null)
        {
            int action = ev.getAction();
            switch (action)
            {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Rect r = tempRect;
                    dragView.getDrawingRect(r);
                    stopDragging();
                    if ( dropListener != null
                         && dragPos >= 0
                         && dragPos < getCount() )
                    {
                        dropListener.drop(srcDragPos, dragPos);
                    }
                    unExpandViews(false);
                    break;

                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    dragView(x, y);
                    int itemnum = getItemForPosition(y);
                    if (itemnum >= 0)
                    {
                        if ( action == MotionEvent.ACTION_DOWN
                             || itemnum != dragPos )
                        {
                            if (dragListener != null)
                            {
                                dragListener.drag(dragPos, itemnum);
                            }
                            dragPos = itemnum;
                            doExpansion();
                        }
                        int speed = 0;
                        adjustScrollBounds(y);
                        if (y > lowerBound)
                        {
                            // scroll the list up a bit
                            if (getLastVisiblePosition() < getCount() - 1)
                            {
                                speed = y > (height + lowerBound) / 2 ? 16 : 4;
                            }
                            else
                            {
                                speed = 1;
                            }
                        }
                        else if (y < upperBound)
                        {
                            // scroll the list down a bit
                            speed = y < upperBound / 2 ? -16 : -4;
                            if ( getFirstVisiblePosition() == 0
                                 && getChildAt(0).getTop() >= getPaddingTop() )
                            {
                                // if we're already at the top, don't try to
                                // scroll, because it causes the framework to
                                // do some extra drawing that messes up our
                                // animation
                                speed = 0;
                            }
                        }
                        if (speed != 0)
                        {
                            smoothScrollBy(speed, 30);
                        }
                    }
                    break;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void startDragging(Bitmap bm, int x, int y)
    {
        stopDragging();

        windowParams = new WindowManager.LayoutParams();
        windowParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowParams.x = xOffset;
        windowParams.y = y - dragPointY + yOffset;

        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        windowParams.format = PixelFormat.TRANSLUCENT;
        windowParams.windowAnimations = 0;

        Context context = getContext();
        ImageView v = new ImageView(context);
        int backGroundColor = context.getResources().getColor(R.color.blue);
        v.setBackgroundColor(backGroundColor);
        v.setPadding(0, 0, 0, 0);
        v.setImageBitmap(bm);
        dragBitmap = bm;

        windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(v, windowParams);
        dragView = v;
    }

    private void dragView(int x, int y)
    {
        windowParams.x = xOffset;
        windowParams.y = y - dragPointY + yOffset;
        windowManager.updateViewLayout(dragView, windowParams);
    }

    private void stopDragging()
    {
        if (dragView != null)
        {
            dragView.setVisibility(GONE);
            WindowManager wm = (WindowManager)getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(dragView);
            dragView.setImageDrawable(null);
            dragView = null;
        }
        if (dragBitmap != null)
        {
            dragBitmap.recycle();
            dragBitmap = null;
        }
    }

    /**
     * Sets the {@link DragListener} that will be notified about drag events.
     * @param l the drag listener
     */
    public void setDragListener(DragListener l)
    {
        dragListener = l;
    }

    /**
     * Sets the {@link DropListener} that will be notified about drop events.
     * @param l the drop listener
     */
    public void setDropListener(DropListener l)
    {
        dropListener = l;
    }

    /**
     * Drag events occur when currently dragged item moves around the screen
     * over other items.
     */
    public interface DragListener
    {
        /**
         * Fired when item is dragged over other list item.
         * 
         * @param from the position of dragged item
         * @param to the position of currently selected item
         */
        void drag(int from, int to);
    }

    /**
     * Drop events occur when the dragged item is finally dropped.
     * 
     */
    public interface DropListener
    {
        /**
         * Fired when dragged item is dropped on other list item.
         * 
         * @param from the position of source item
         * @param to the destination drop position
         */
        void drop(int from, int to);
    }
}
