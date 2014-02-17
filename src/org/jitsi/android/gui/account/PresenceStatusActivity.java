/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import android.app.*;
import android.content.*;
import android.database.*;
import android.graphics.*;
import android.net.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.account.settings.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.EventListener;/*Disambiguation*/
import org.jitsi.service.osgi.*;

import org.osgi.framework.*;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Activity allows user to set presence status, status message and
 * change the avatar for the {@link #account}.
 *
 * The {@link #account} is retrieved from the {@link Intent} extra by it's
 * {@link AccountID#getAccountUniqueID()}
 *
 * @author Pawel Domas
 */
public class PresenceStatusActivity
    extends OSGiActivity
    implements EventListener<AccountEvent>,
        AdapterView.OnItemSelectedListener
{
    /**
     * Intent's extra's key for account ID property of this activity
     */
    static public final String INTENT_ACCOUNT_ID = "account_id";

    /**
     * The id for the "select image from gallery" intent result
     */
    static private final int SELECT_IMAGE = 1;

    /**
     * The logger used by this class
     */
    static final private Logger logger =
            Logger.getLogger(PresenceStatusActivity.class);

    /**
     * The account's {@link OperationSetPresence}
     * used to perform presence operations
     */
    private OperationSetPresence accountPresence;

    /**
     * The instance of {@link Account} used for operations on the account
     */
    private Account account;

    /**
     * Flag indicates if there were any uncommitted changes that
     * shall be applied on exit
     */
    private boolean hasChanges = false;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Set the main layout
        setContentView(R.layout.presence_status);
    }

    protected void start(BundleContext bundleContext)
            throws Exception
    {
        super.start(bundleContext);
        // Get account ID from intent extras
        String accountIDStr = getIntent().getStringExtra(INTENT_ACCOUNT_ID);
        // Find account for given account ID
        AccountID accountID = AccountUtils.getAccountForID(accountIDStr);
        if(accountID == null)
        {
            logger.error("No account found for: "+accountIDStr);
            finish();
            return;
        }
        this.account = new Account(accountID, bundleContext, getBaseContext());
        
        initPresenceStatus();

        account.addAccountEventListener(this);
    }

    /**
     * Create and initialize the view with actual values
     */
    private void initPresenceStatus()
    {
        this.accountPresence = account.getPresenceOpSet();

        // Check for presence support
        if(accountPresence == null)
        {
            Toast t = Toast.makeText(
                this,
                getString( R.string.service_gui_PRESENCE_NOT_SUPPORTED,
                           account.getAccountName() ),
                Toast.LENGTH_LONG);
            t.show();

            finish();
            return;
        }

        // Account properties
        String titlePattern = getResources().getString(
                R.string.service_gui_PRESENCE_EDIT_TITLE);

        String title
                = MessageFormat.format( titlePattern, account.getAccountName());

        ActionBarUtil.setTitle(this, title);

        // Create spinner with status list
        Spinner statusSpinner = (Spinner) findViewById(
                R.id.presenceStatusSpinner);

        // Create list adapter
        Iterator<PresenceStatus> statusIter =
                accountPresence.getSupportedStatusSet();

        StatusListAdapter statusAdapter = new StatusListAdapter(statusIter);

        statusSpinner.setAdapter(statusAdapter);

        // Selects current status
        statusSpinner.setSelection(
                statusAdapter.getPositionForItem(
                    accountPresence.getPresenceStatus()));

        statusSpinner.setOnItemSelectedListener(this);

        // Status edit
        EditText statusEdit = (EditText) findViewById(
                R.id.presenceStatusMessageEdit);

        // Sets current status message
        statusEdit.setText(accountPresence.getCurrentStatusMessage());

        // Watch the text for any changes
        statusEdit.addTextChangedListener( new TextWatcher()
        {

            public void beforeTextChanged( CharSequence charSequence,
                                           int i, int i2, int i3)
            {
                // Ignore
            }

            public void onTextChanged( CharSequence charSequence,
                                       int i, int i2, int i3)
            {
                // Ignore
            }

            public void afterTextChanged(Editable editable)
            {
                hasChanges = true;
            }
        });

        // Avatar icon
        updateAvatar(this.account);
    }

    @Override
    protected void stop(BundleContext bundleContext)
        throws Exception
    {
        super.stop(bundleContext);

        commitStatusChanges();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.presence_status_menu, menu);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.remove)
        {
            RemoveAccountDialog
                    .create(this, account.getAccountID(),
                            new RemoveAccountDialog.OnAccountRemovedListener()
                            {
                                @Override
                                public void onAccountRemoved(AccountID accID)
                                {
                                    // Prevent from submitting status
                                    hasChanges = false;
                                    finish();
                                }
                            })
                    .show();
            return true;
        }
        else if(id == R.id.account_settings)
        {
            Intent preferences
                    = AccountPreferencesActivity
                            .getIntent(this, account.getAccountID());
            startActivity(preferences);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Method mapped to the avatar image clicked event.
     * It starts the select image {@link Intent}
     *
     * @param avatarView the {@link View} that has been clicked
     */
    @SuppressWarnings("unused")
    public void onAvatarClicked(View avatarView)
    {
        if(account.getAvatarOpSet() == null)
        {
            logger.warn("Avatar operation set is not supported by "
                        + account.getAccountName());
            return;
        }
        logger.error("Starting selected avatar activity!");
        //Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        Intent gallIntent=new Intent(Intent.ACTION_GET_CONTENT);
        gallIntent.setType("image/*");
        //Intent camIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        gallIntent.putExtra( Intent.EXTRA_TITLE,
                             R.string.service_gui_SELECT_AVATAR);
        startActivityForResult(gallIntent, SELECT_IMAGE);
    }

    /**
     * Gets the real path for the image referenced from the {@link Uri}
     *
     * @param activity the parent {@link Activity}
     *
     * @param contentUri the {@link Uri} that points to the image
     *
     * @return the real path that can be opened by
     *  for instance {@link FileInputStream}
     */
    public static String getRealPathFromUri(Activity activity, Uri contentUri)
    {
        String[] proj = { MediaStore.Images.Media.DATA };

        Cursor cursor =
                activity.managedQuery(contentUri, proj, null, null, null);

        int column_index =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    /**
     * Method handles callbacks from external {@link Intent} that retrieve
     * avatar image
     *
     * @param requestCode the request code {@link #SELECT_IMAGE}
     *
     * @param resultCode  the result code
     *
     * @param data the source {@link Intent} that returns the result
     */
    protected void onActivityResult( int requestCode, int resultCode,
                                     Intent data )
    {
        if (requestCode == SELECT_IMAGE)
        {
            if (resultCode == RESULT_OK)
            {
                // A contact was picked.  Here we will just display it
                // to the user.
                Uri imageUri = data.getData();
                String resolvedPath = getRealPathFromUri(this,imageUri);
                File f = new File(resolvedPath);
                FileInputStream fin = null;
                try
                {
                    final OperationSetAvatar avatarOpSet =
                            account.getAvatarOpSet();
                    if(avatarOpSet == null)
                    {
                        logger.warn("No avatar operation set found for "
                                    + account.getAccountName());
                        return;
                    }

                    fin = new FileInputStream(f);

                    final ByteArrayOutputStream bout =
                            new ByteArrayOutputStream(fin.available());
                    int read;
                    byte[] buffer = new byte[2048];
                    do
                    {
                        read = fin.read(buffer);
                        if(read > 0)
                            bout.write(buffer, 0, read);
                    }while (read > 0);

                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            logger.trace("Trying to set avatar");
                            avatarOpSet.setAvatar(bout.toByteArray());
                            logger.trace("Avatar set");
                        }
                    }).start();
                }
                catch (IOException e)
                {
                    logger.error("Error reading the avatar: "+e);
                }
				finally
				{
				if(fin != null)
					try
					{
						fin.close();
					} catch (IOException e)
					{
						logger.error("Unable to close FileInputStream: " + e);
					}
				}
            }
        }
    }

    /**
     * Method starts a new Thread and publishes the status
     *
     * @param status {@link PresenceStatus} to be set
     * @param text the status message
     */
    private void publishStatus(final PresenceStatus status, final String text)
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    // Try to publish selected status
                    logger.trace("Publishing status "+status+" msg: "+text);
                    GlobalStatusService globalStatus
                            = ServiceUtils.getService(
                                    AndroidGUIActivator.bundleContext,
                                    GlobalStatusService.class);
                    globalStatus.publishStatus(account.getProtocolProvider(),
                                               status,
                                               true);
                    accountPresence.publishPresenceStatus(status, text);
                }
                catch (Exception e)
                {
                    logger.error(e);

                    // TODO: fix me
                    // This is annoying and removed for now. It is displayed for
                    // XMPP provider even when switching from offline to any
                    // other status.
                    /*AndroidUtils.showAlertDialog(
                            getBaseContext(),
                            "Error",
                            "An error occurred while setting the status: "
                            + e.getLocalizedMessage());*/
                }
            }
        }).start();
    }

    /**
     * Updates the avatar image
     *
     * @param account the {@link Account} that contains the image data
     */
    private void updateAvatar(Account account)
    {
        ImageView avatarView = (ImageView) findViewById(R.id.accountAvatar);

        avatarView.setImageDrawable(account.getAvatarIcon());
    }

    /**
     * Fired when the {@link #account} has changed and the UI need to be updated
     *
     * @param eventObject the instance that has been changed
     */
    public void onChangeEvent(final AccountEvent eventObject)
    {
        if(eventObject.getEventType() != AccountEvent.AVATAR_CHANGE)
        {
            return;
        }
        
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                updateAvatar(eventObject.getSource());
            }
        });
    }

    /**
     * Checks if there are any uncommitted changes and applies them eventually
     */
    private void commitStatusChanges()
    {
        if(!hasChanges)
            return;

        Spinner statusSpinner = (Spinner) findViewById(
                R.id.presenceStatusSpinner);
        EditText statusMessageEdit = (EditText) findViewById(
                R.id.presenceStatusMessageEdit);

        PresenceStatus selectedStatus =
                (PresenceStatus) statusSpinner.getSelectedItem();
        String statusText = statusMessageEdit.getText().toString();

        // Publish status in new thread
        publishStatus(selectedStatus, statusText);
    }

    /**
     * Fired when new presence status is selected
     *
     * @param adapterView clicked adapter's View
     * @param view selected item's View
     * @param i items position
     * @param l item's id
     */
    public void onItemSelected( AdapterView<?> adapterView,
                                View view, int i, long l)
    {
        hasChanges = true;
    }

    public void onNothingSelected(AdapterView<?> adapterView)
    {
       // Should not happen in single selection mode
    }

    /**
     * Class responsible for creating the Views
     * for a given set of {@link PresenceStatus}
     */
    class StatusListAdapter
        extends CollectionAdapter<PresenceStatus>
    {
        /**
         * Creates new instance of {@link StatusListAdapter}
         *
         * @param objects {@link Iterator} for a set of {@link PresenceStatus}
         *
         */
        public StatusListAdapter( Iterator<PresenceStatus> objects)
        {
            super(PresenceStatusActivity.this, objects);
        }

        @Override
        protected View getView( boolean isDropDown,
                                PresenceStatus item,
                                ViewGroup parent,
                                LayoutInflater inflater)
        {

            // Retrieve views
            View statusItemView = inflater.inflate(
                    R.layout.presence_status_row, parent, false);
            TextView statusNameView = (TextView) statusItemView.findViewById(
                    R.id.presenceStatusNameView);
            ImageView statusIconView = (ImageView) statusItemView.findViewById(
                    R.id.presenceStatusIconView);

            // Set status name
            String statusName = item.getStatusName();
            statusNameView.setText(statusName);

            // Set status icon
            Bitmap presenceIcon =
                    AndroidImageUtil.bitmapFromBytes(
                            item.getStatusIcon());
            statusIconView.setImageBitmap(presenceIcon);

            return statusItemView;
        }

        /**
         * Find the position of <tt>status</tt> in adapter's list
         *
         * @param status the {@link PresenceStatus} for which
         *  the position is returned
         *
         * @return index of <tt>status</tt> in adapter's list
         */
        int getPositionForItem(PresenceStatus status)
        {
            for(int i=0; i < getCount(); i++)
            {
                PresenceStatus other = (PresenceStatus) getItem(i);
                if(other.equals(status))
                    return i;
            }
            return -1;
        }
    }
}
