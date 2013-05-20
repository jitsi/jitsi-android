/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.app.*;
import android.view.*;
import android.widget.*;

import java.util.*;

/**
 * Convenience class wrapping set of elements into {@link Adapter}
 *
 * @param <T> class of the elements contained in this adapter
 *
 * @author Pawel Domas
 */
public abstract class CollectionAdapter<T>
        extends BaseAdapter
{
    /**
     * The {@link LayoutInflater} used to create the views
     */
    private final LayoutInflater layoutInflater;
    /**
     * List of elements handled by this adapter
     */
    private List<T> items;
    /**
     * The parent {@link Activity}
     */
    private final Activity parentActivity;

    /**
     * Creates a new instance of {@link CollectionAdapter}
     *
     * @param parent the parent {@link Activity}
     * @param layoutInflater the {@link LayoutInflater} for current context
     */
    public CollectionAdapter( Activity parent,
                              LayoutInflater layoutInflater )
    {
        this.parentActivity = parent;
        this.layoutInflater = layoutInflater;
    }

    /**
     * Creates new instance of {@link CollectionAdapter}
     *
     * @param parent the parent {@link Activity}
     * @param layoutInflater the {@link LayoutInflater} that will be used
     *  to create new {@link View}s
     * @param items iterator of {@link T} items
     */
    public CollectionAdapter( Activity parent,
                              LayoutInflater layoutInflater,
                              Iterator<T> items )
    {
        this.parentActivity = parent;
        this.layoutInflater = layoutInflater;

        setIterator(items);
    }

    /**
     * The method that accepts {@link Iterator} as a source set of objects
     *
     * @param iterator source of {@link T} instances that will be contained
     *  in this {@link CollectionAdapter}
     */
    protected void setIterator(Iterator<T> iterator)
    {
        items = new ArrayList<T>();
        while(iterator.hasNext())
            items.add(iterator.next());
    }

    /**
     * Accepts {@link List} as a source set of {@link T}
     *
     * @param collection the {@link List} that will be included
     *  in this {@link CollectionAdapter}
     */
    protected void setList(List<T> collection)
    {
        items = new ArrayList<T>();
        for(T item: collection)
            items.add(item);
    }

    /**
     * Returns total count of items contained in this adapter
     *
     * @return the count of {@link T} stored in this {@link CollectionAdapter}
     */
    public int getCount()
    {
        return items.size();
    }

    public Object getItem(int i)
    {
        return items.get(i);
    }

    public long getItemId(int i)
    {
        return i;
    }

    /**
     * Convenience method for retrieving {@link T} instances
     *
     * @param i the index of {@link T} that will be retrieved
     * @return the {@link T} object located at <tt>i</tt> position
     */
    public T getObject(int i)
    {
        return (T)items.get(i);
    }

    /**
     * Adds <tt>object</tt> to the adapter
     *
     * @param object instance of {@link T} that will be added to this adapter
     */
    public void add(T object)
    {
        if(!items.contains(object))
        {
            items.add(object);
            doRefreshList();
        }
    }

    /**
     * Removes the <tt>object</tt> from this adapter
     *
     * @param object instance of {@link T} that will be removed from the adapter
     */
    public void remove(T object)
    {
        if(items.remove(object))
        {
            doRefreshList();
        }
    }

    /**
     * Runs list change notification on the UI thread
     */
    protected void doRefreshList()
    {
        parentActivity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

    public View getView(int i, View view, ViewGroup viewGroup)
    {
        return getView(items.get(i), viewGroup, layoutInflater);
    }

    /**
     * Convenience method for creating new {@link View}s for each
     * adapter's object
     *
     * @param item the item for which a new View shall be created
     * @param parent {@link ViewGroup} parent View
     * @param inflater the {@link LayoutInflater} for creating new Views
     *
     * @return a {@link View} for given <tt>item</tt>
     */
    protected abstract View getView( T item,
                                     ViewGroup parent,
                                     LayoutInflater inflater);

    /**
     * The parent {@link android.app.Activity}
     */
    protected Activity getParentActivity()
    {
        return parentActivity;
    }
}
