package org.jitsi.android.gui;

import org.jitsi.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.service.osgi.*;

import android.app.*;
import android.os.*;
import android.view.*;

public class MainViewFragment
    extends OSGiFragmentV4
{
    private ContactListFragment contactListFragment;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(   LayoutInflater inflater,
                                ViewGroup container,
                                Bundle savedInstanceState)
    {
        View content = inflater.inflate( R.layout.main_view,
                                         container,
                                         false);

        showContactsFragment();

        return content;
    }

    /**
     * Displays contacts fragment(currently <tt>CallContactFragment</tt>.
     */
    private void showContactsFragment()
    {
        contactListFragment = new ContactListFragment();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.contactListFragment, contactListFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    public void filterContactList(String query)
    {
        if (contactListFragment == null)
            return;

        contactListFragment.filterContactList(query);
    }
}
