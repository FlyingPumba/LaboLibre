package ar.uba.dc.labolibre;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;

import java.util.Calendar;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, WeekView.MonthChangeListener, CalendarEventsManager.EventsManagerListener {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    private WeekView mCalendarView;
    private SmoothProgressBar progressBar;

    Calendar currentMonthTime;
    CalendarEventsManager eventsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calculateCurrentMonth();

        initHelpers();

        initViews();

        populateInitialEvents(currentMonthTime);
    }

    private void calculateCurrentMonth() {
        // calculate month time
        Calendar aux = Calendar.getInstance();
        currentMonthTime = Calendar.getInstance();
        currentMonthTime.clear();
        currentMonthTime.set(aux.get(Calendar.YEAR), aux.get(Calendar.MONTH), 1);
        currentMonthTime.getTime(); // needed to repopulate values
    }

    private void initHelpers() {
        eventsManager = new CalendarEventsManager(this, this);
    }

    private void initViews() {
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        progressBar = (SmoothProgressBar)findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        mCalendarView = (WeekView)findViewById(R.id.calendarView);
        mCalendarView.setMonthChangeListener(this);

        // focus current hour
        Calendar aux = Calendar.getInstance();
        mCalendarView.goToHour(aux.get(Calendar.HOUR_OF_DAY));

        eventsManager.setShowingCalendars(mNavigationDrawerFragment.getSelectedItems());
    }

    private void populateInitialEvents(Calendar time) {
        // fetch events for this month, the previous one and the next one
        Calendar prevMonthTime = Calendar.getInstance();
        Calendar nextMonthTime = Calendar.getInstance();
        prevMonthTime.setTime(time.getTime());
        nextMonthTime.setTime(time.getTime());
        if (time.get(Calendar.MONTH) == Calendar.JANUARY) {
            prevMonthTime.set(Calendar.MONTH, Calendar.DECEMBER);
            prevMonthTime.set(Calendar.YEAR, time.get(Calendar.YEAR) - 1);
            nextMonthTime.set(Calendar.MONTH, Calendar.FEBRUARY);
        } else if (time.get(Calendar.MONTH) == Calendar.DECEMBER) {
            prevMonthTime.set(Calendar.MONTH, Calendar.NOVEMBER);
            nextMonthTime.set(Calendar.MONTH, Calendar.JANUARY);
            nextMonthTime.set(Calendar.YEAR, time.get(Calendar.YEAR) + 1);
        } else {
            prevMonthTime.set(Calendar.MONTH, time.get(Calendar.MONTH) - 1);
            nextMonthTime.set(Calendar.MONTH, time.get(Calendar.MONTH) + 1);
        }

        prevMonthTime.getTime(); // needed to repopulate values
        nextMonthTime.getTime(); // needed to repopulate values

        eventsManager.getEventsByMonth(time);
        eventsManager.getEventsByMonth(nextMonthTime);
        eventsManager.getEventsByMonth(prevMonthTime);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        eventsManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
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
        Calendar time = Calendar.getInstance();
        time.set(newYear, newMonth, 1);
        time.getTime(); // needed to repopulate values
        return eventsManager.getEventsByMonth(time);
    }

    @Override
    public void onNewEvents() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCalendarView.notifyDatasetChanged();
            }
        });
    }

    @Override
    public void onDownloadStarted() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDownloadFinished() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onSelectedItemsChanged(List<Integer> positions) {
        eventsManager.setShowingCalendars(positions);
        mCalendarView.notifyDatasetChanged();
    }
}
