package ar.uba.dc.labolibre;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.alamkanak.weekview.WeekViewEvent;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class CalendarEventsManager implements GoogleCalendarAuthorizator.AuthorizationListener, CalendarEventsFetcher.EventsFetcherListener {

    private List<String> monthEventsBeingFetched = new ArrayList<>();
    private List<String> cids;
    private List<String> cnames;
    private List<Integer> ccolors;
    private List<Integer> showing;

    AppCompatActivity activity;
    EventsManagerListener listener;

    GoogleCalendarAuthorizator authorizator;
    boolean authorizating = false;
    CalendarEventsFetcher fetcher;

    List<Calendar> timePending;

    public CalendarEventsManager(AppCompatActivity activity, EventsManagerListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.authorizator = new GoogleCalendarAuthorizator(activity, this);
        this.fetcher = new CalendarEventsFetcher(this, authorizator.getCredential());

        timePending = new ArrayList<Calendar>();

        // prepare calendars info
        initCalendarsInfo();
    }

    public List<WeekViewEvent> getEventsByMonth(Calendar time) {
        // check if they are in the DB
        WeekViewEvent[] fromDB = getFromDB(time);
        if (fromDB == null) {
            // if not, try to fetch the from internet
            getFromInternet(time);
            return new ArrayList<WeekViewEvent>();
        } else {
            return filterShowingCalendars(Arrays.asList(fromDB));
        }
    }

    private WeekViewEvent[] getFromDB(Calendar time) {
        try {
            //create or open an existing databse using the default name
            DB snappydb = DBFactory.open(activity);
            WeekViewEvent [] events = snappydb.getObjectArray(calendarTime2ColumnName(time), WeekViewEvent.class);
            snappydb.close();
            return events;
        } catch (SnappydbException e) {
            Log.d(this.getClass().getName(), e.toString());
            return null;
        }
    }

    private void getFromInternet(Calendar time) {
        if (monthEventsBeingFetched.contains(calendarTime2ColumnName(time))) {
            return;
        }

        Calendar endTime = Calendar.getInstance();
        endTime.setTime(time.getTime());

        // compute endTime (one month)
        if (time.get(Calendar.MONTH) == Calendar.DECEMBER) {
                endTime.set(Calendar.MONTH, Calendar.JANUARY);
                endTime.set(Calendar.YEAR, time.get(Calendar.YEAR) + 1);
            } else {
                endTime.set(Calendar.MONTH, time.get(Calendar.MONTH) + 1);
            }
        endTime.getTime(); // needed to repopulate values

        if (!authorizator.hasValidCredential()) {
            if (!authorizating) {
                authorizating = true;
                timePending.add(time);
                authorizator.requestAuthorization();
            }
        } else {
            if (isDeviceOnline()) {
                // store current month as being fetched
                monthEventsBeingFetched.add(calendarTime2ColumnName(time));
                fetcher.fetchEventsfromCalendars(cids, cnames, ccolors, time, endTime);
                listener.onDownloadStarted();
            } else {
                // yield: no connection
                Toast.makeText(activity, "No connection available", Toast.LENGTH_LONG).show();
            }
        }
    }

    private List<WeekViewEvent> filterShowingCalendars(List<WeekViewEvent> weekViewEvents) {
        List<WeekViewEvent> events = new ArrayList<WeekViewEvent>();
        for (WeekViewEvent e :weekViewEvents) {
            if (showing.contains(getCalendarIndex(e))) {
                events.add(e);
            }
        }
        return events;
    }

    private Integer getCalendarIndex(WeekViewEvent e) {
        return ccolors.indexOf(e.getColor());
    }

    private void initCalendarsInfo() {
        // prepare calendar ids
        String[] aux_ids = activity.getResources().getStringArray(R.array.calendar_ids);
        cids = new ArrayList<String>();
        showing = new ArrayList<Integer>();
        for (int i = 0; i < aux_ids.length; i++) {
            cids.add(aux_ids[i]);
            showing.add(i);
        }

        // prepare calendar names
        String[] aux_names = activity.getResources().getStringArray(R.array.calendar_names);
        cnames = new ArrayList<String>();
        for (int i = 0; i < aux_names.length; i++) {
            cnames.add(aux_names[i]);
        }

        // prepare calendar colors
        int[] aux_colors = activity.getResources().getIntArray(R.array.calendar_colors);
        ccolors = new ArrayList<Integer>();
        for (int i = 0; i < aux_colors.length; i++) {
            ccolors.add(aux_colors[i]);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        authorizator.onAuthorizationResult(requestCode, resultCode, data);
    }


    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    public void onValidAuthorizationObtained() {
        authorizating = false;
        if (!timePending.isEmpty()) {
            for(Calendar time : timePending) {
                List<WeekViewEvent> events = getEventsByMonth(time);
                if (!events.isEmpty()) {
                    listener.onNewEvents();
                }
            }
            timePending.clear();
        }
    }

    @Override
    public void onEventsFetchFinished(Calendar time, List<WeekViewEvent> events) {
        String key = calendarTime2ColumnName(time);
        // once fetched, store them in the DB
        try {
            //create or open an existing databse using the default name
            DB snappydb = DBFactory.open(activity);

            // all this events belong to one month
            snappydb.put(key, events.toArray());

            snappydb.close();

            listener.onNewEvents();
        } catch (SnappydbException e) {
            Log.d(this.getClass().getName(), e.toString());
        }
        // remove entry from beingFetched
        if(monthEventsBeingFetched.contains(key)) {
            monthEventsBeingFetched.remove(key);
        }
        if (monthEventsBeingFetched.isEmpty()) {
            listener.onDownloadFinished();
        }
    }

    private String calendarTime2ColumnName(Calendar time) {
        return monthAndYear2ColumnName(time.get(Calendar.MONTH), time.get(Calendar.YEAR));
    }

    private String monthAndYear2ColumnName(int m, int y) {
        return "" + m + y;
    }

    public void setShowingCalendars(List<Integer> selectedItems) {
        showing = selectedItems;
    }

    public interface EventsManagerListener {
        void onNewEvents();
        void onDownloadStarted();
        void onDownloadFinished();
    }
}
