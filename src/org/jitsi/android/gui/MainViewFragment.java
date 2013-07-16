package org.jitsi.android.gui;

import org.jitsi.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.service.osgi.*;

import android.app.*;
import android.os.*;
import android.view.*;

public class MainViewFragment
    extends OSGiFragment
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
        /**
         * TODO: Extract splash screen to separate activity from Jitsi
         * and merge this fragment
         *
         * Here we pass chat contact arguments to ContactListFragment.
         * Once the splash screen will be extracted from Jitsi Activity
         * this fragment could be merged with it and arguments will be passed
         * directly.
         */
        contactListFragment.setArguments(getArguments());
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
