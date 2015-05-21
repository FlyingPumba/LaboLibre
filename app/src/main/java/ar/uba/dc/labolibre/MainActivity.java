package ar.uba.dc.labolibre;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, WeekView.MonthChangeListener, CalendarEvents.NewEventsListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};
    GoogleAccountCredential credential;

    private WeekView mCalendarView;
    private boolean calendarEventsOutdated = true;
    private int initial3Months = 0;

    // cache of monthly events
    CalendarEvents calendarEvents;
    Calendar currentMonthTime;
    Calendar prevMonthTime;
    Calendar nextMonthTime;
    private List<WeekViewEvent> prevMonth = new ArrayList<WeekViewEvent>();
    private List<WeekViewEvent> currentMonth = new ArrayList<WeekViewEvent>();
    private List<WeekViewEvent> nextMonth = new ArrayList<WeekViewEvent>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mCalendarView = (WeekView)findViewById(R.id.calendarView);
        mCalendarView.setMonthChangeListener(this);

        // Initialize credentials and calendar service.
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);

        credential = GoogleAccountCredential.usingOAuth2(
                this, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));


        // calculate month time
        Calendar aux = Calendar.getInstance();
        currentMonthTime = Calendar.getInstance();
        currentMonthTime.clear();
        currentMonthTime.set(aux.get(Calendar.YEAR), aux.get(Calendar.MONTH), 1);
        currentMonthTime.getTime(); // needed to repopulate values

        // Populate initial events
        populateEvents();
    }

    private void populateEvents() {
        // fetch events for this month, the previous one and the next one
        prevMonthTime = Calendar.getInstance();
        nextMonthTime = Calendar.getInstance();
        prevMonthTime.setTime(currentMonthTime.getTime());
        nextMonthTime.setTime(currentMonthTime.getTime());
        if (currentMonthTime.get(Calendar.MONTH) == Calendar.JANUARY) {
            prevMonthTime.set(Calendar.MONTH, Calendar.DECEMBER);
            prevMonthTime.set(Calendar.YEAR, currentMonthTime.get(Calendar.YEAR) - 1);
            nextMonthTime.set(Calendar.MONTH, Calendar.FEBRUARY);
        } else if (currentMonthTime.get(Calendar.MONTH) == Calendar.DECEMBER) {
            prevMonthTime.set(Calendar.MONTH, Calendar.NOVEMBER);
            nextMonthTime.set(Calendar.MONTH, Calendar.JANUARY);
            nextMonthTime.set(Calendar.YEAR, currentMonthTime.get(Calendar.YEAR) + 1);
        } else {
            prevMonthTime.set(Calendar.MONTH, currentMonthTime.get(Calendar.MONTH) - 1);
            nextMonthTime.set(Calendar.MONTH, currentMonthTime.get(Calendar.MONTH) + 1);
        }

        prevMonthTime.getTime(); // needed to repopulate values
        nextMonthTime.getTime(); // needed to repopulate values

        Calendar endTime = Calendar.getInstance();
        endTime.setTime(nextMonthTime.getTime());

        // compute endTime
        if (nextMonthTime.get(Calendar.MONTH) == Calendar.DECEMBER) {
            endTime.set(Calendar.MONTH, Calendar.JANUARY);
            endTime.set(Calendar.YEAR, nextMonthTime.get(Calendar.YEAR) + 1);
        } else {
            endTime.set(Calendar.MONTH, nextMonthTime.get(Calendar.MONTH) + 1);
        }
        endTime.getTime(); // needed to repopulate values

        // prepare calendar ids
        List<String> cids = new ArrayList<String>();
        cids.add(getResources().getString(R.string.labo1_cid));
        cids.add(getResources().getString(R.string.labo2_cid));
        cids.add(getResources().getString(R.string.labo3_cid));
        cids.add(getResources().getString(R.string.labo4_cid));
        cids.add(getResources().getString(R.string.labo5_cid));
        cids.add(getResources().getString(R.string.labo6_cid));
        cids.add(getResources().getString(R.string.laboTuring_cid));

        // prepare calendar names
        List<String> names = new ArrayList<String>();
        names.add(getResources().getString(R.string.labo1_name));
        names.add(getResources().getString(R.string.labo2_name));
        names.add(getResources().getString(R.string.labo3_name));
        names.add(getResources().getString(R.string.labo4_name));
        names.add(getResources().getString(R.string.labo5_name));
        names.add(getResources().getString(R.string.labo6_name));
        names.add(getResources().getString(R.string.laboTuring_name));

        // prepare calendar colors
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(getResources().getColor(R.color.md_light_blue_400));
        colors.add(getResources().getColor(R.color.md_red_400));
        colors.add(getResources().getColor(R.color.md_light_green_400));
        colors.add(getResources().getColor(R.color.md_amber_400));
        colors.add(getResources().getColor(R.color.md_purple_400));
        colors.add(getResources().getColor(R.color.md_pink_400));
        colors.add(getResources().getColor(R.color.md_indigo_400));

        // fetch !
        if (credential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                calendarEvents = new CalendarEvents(this, credential);
                calendarEvents.fetchEventsfromCalendars(cids, names, colors, prevMonthTime, endTime);
            } else {
                // yield: no connection
            }
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == RESULT_OK) {
                    populateEvents();
                } else {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        populateEvents();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    //mStatusText.setText("Account unspecified.");
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    populateEvents();
                } else {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onNavigationDrawerItemChecked(int position, boolean checked) {
        // refresh event list
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        newMonth--; // gil el que hizo WeekView

        //  devolver el newMonth, y pedir el mes siguiente y anterior a ese
        if ((currentMonthTime.get(Calendar.YEAR) == newYear && currentMonthTime.get(Calendar.MONTH) == newMonth + 1)
                || (currentMonthTime.get(Calendar.YEAR) == newYear+1 && currentMonthTime.get(Calendar.MONTH) == Calendar.JANUARY)) {
            // we moved to next month
            if(initial3Months >= 3) {
                currentMonthTime = nextMonthTime;
                populateEvents();
            } else {
                initial3Months++;
            }
            return nextMonth;
        } else if ((currentMonthTime.get(Calendar.YEAR) == newYear && currentMonthTime.get(Calendar.MONTH) == newMonth - 1)
                || (currentMonthTime.get(Calendar.YEAR) == newYear-1 && currentMonthTime.get(Calendar.MONTH) == Calendar.DECEMBER)) {
            // we moved to prev month
            if(initial3Months >= 3) {
                currentMonthTime = prevMonthTime;
                populateEvents();
            } else {
                initial3Months++;
            }
            return prevMonth;
        } else if (currentMonthTime.get(Calendar.YEAR) == newYear && currentMonthTime.get(Calendar.MONTH) == newMonth) {
            // we are in current month
            if(initial3Months < 3) {
                initial3Months++;
            }
            return currentMonth;

        } else {
            // completely different month to the cached ones
            currentMonthTime.clear();
            currentMonthTime.set(newYear, newMonth, 1);
            currentMonthTime.getTime(); // needed to repopulate values
            calendarEventsOutdated = true;

            // triger some kind of notification to the user, to inform that we are fetching the events
            populateEvents();
            return new ArrayList<WeekViewEvent>();
        }
    }

    @Override
    public void onNewEvents(final List<WeekViewEvent> events) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(WeekViewEvent e : events) {
                    if (e.getStartTime().before(currentMonthTime)) {
                        // event from previous month
                        prevMonth.add(e);
                    } else if (e.getStartTime().after(nextMonthTime)) {
                        // event from next month
                        nextMonth.add(e);
                    } else {
                        // event from current month
                        currentMonth.add(e);
                    }
                }
                if (calendarEventsOutdated) {
                    mCalendarView.notifyDatasetChanged();
                    calendarEventsOutdated = false;
                }
            }
        });
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    /**
     * Starts an activity in Google Play Services so the user can pick an
     * account.
     */
    private void chooseAccount() {
        startActivityForResult(
                credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode,
                        MainActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

}
