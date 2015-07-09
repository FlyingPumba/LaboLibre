package ar.uba.dc.labolibre;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class CalendarEventsManager implements GoogleCalendarAuthorizator.AuthorizationListener, CalendarEventsFetcher.NewEventsListener {

    private List<String> monthEventsBeingFetched = new ArrayList<>();
    private List<String> cids;
    private List<String> cnames;
    private List<Integer> ccolors;

    AppCompatActivity activity;
    CalendarEventsFetcher.NewEventsListener listener;

    GoogleCalendarAuthorizator authorizator;
    boolean authorizating = false;
    CalendarEventsFetcher fetcher;
    boolean fetching = false;

    Calendar timePending = null;

    public CalendarEventsManager(AppCompatActivity activity, CalendarEventsFetcher.NewEventsListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.authorizator = new GoogleCalendarAuthorizator(activity, this);
        this.fetcher = new CalendarEventsFetcher(this, authorizator.getCredential());

        // prepare calendars info
        initCalendarsInfo();
    }

    public List<WeekViewEvent> getEventsByMonth(Calendar time) {
        // check if they are in the DB
        WeekViewEvent[] fromDB = getFromDB(time);
        if (fromDB == null || fromDB.length == 0) {
            // if not, try to fetch the from internet
            getFromInternet(time);
            return new ArrayList<WeekViewEvent>();
        } else {
            return Arrays.asList(fromDB);
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
                timePending = time;
                authorizator.requestAuthorization();
            }
        } else {
            if (isDeviceOnline()) {
                // check if we are not already fetching that month
                if (!monthEventsBeingFetched.contains(calendarTime2ColumnName(time))) {
                    // store current month as being fetched
                    monthEventsBeingFetched.add(calendarTime2ColumnName(time));
                    fetcher.fetchEventsfromCalendars(cids, cnames, ccolors, time, endTime);
                }
            } else {
                // yield: no connection
            }
        }
    }

    private void initCalendarsInfo() {
        // prepare calendar ids
        cids = new ArrayList<String>();
        cids.add(activity.getResources().getString(R.string.labo1_cid));
        cids.add(activity.getResources().getString(R.string.labo2_cid));
        cids.add(activity.getResources().getString(R.string.labo3_cid));
        cids.add(activity.getResources().getString(R.string.labo4_cid));
        cids.add(activity.getResources().getString(R.string.labo5_cid));
        cids.add(activity.getResources().getString(R.string.labo6_cid));
        cids.add(activity.getResources().getString(R.string.laboTuring_cid));

        // prepare calendar names
        cnames = new ArrayList<String>();
        cnames.add(activity.getResources().getString(R.string.labo1_name));
        cnames.add(activity.getResources().getString(R.string.labo2_name));
        cnames.add(activity.getResources().getString(R.string.labo3_name));
        cnames.add(activity.getResources().getString(R.string.labo4_name));
        cnames.add(activity.getResources().getString(R.string.labo5_name));
        cnames.add(activity.getResources().getString(R.string.labo6_name));
        cnames.add(activity.getResources().getString(R.string.laboTuring_name));

        // prepare calendar colors
        ccolors = new ArrayList<Integer>();
        ccolors.add(activity.getResources().getColor(R.color.md_light_blue_400));
        ccolors.add(activity.getResources().getColor(R.color.md_red_400));
        ccolors.add(activity.getResources().getColor(R.color.md_light_green_400));
        ccolors.add(activity.getResources().getColor(R.color.md_amber_400));
        ccolors.add(activity.getResources().getColor(R.color.md_purple_400));
        ccolors.add(activity.getResources().getColor(R.color.md_pink_400));
        ccolors.add(activity.getResources().getColor(R.color.md_indigo_400));
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
        if (timePending != null) {
            List<WeekViewEvent> events = getEventsByMonth(timePending);
            if (!events.isEmpty()) {
                listener.onNewEvents(events);
            }
        }
    }

    @Override
    public void onNewEvents(List<WeekViewEvent> events) {
        if (events.size() == 0) {
            return;
        }

        // once fetched, store them in the DB
        try {
            //create or open an existing databse using the default name
            DB snappydb = DBFactory.open(activity);

            // all this events belong to one month
            WeekViewEvent e = events.get(0);
            int m = e.getStartTime().get(Calendar.MONTH);
            int y = e.getStartTime().get(Calendar.YEAR);
            snappydb.put(monthAndYear2ColumnName(m, y), events.toArray());

            snappydb.close();
            // remove entry from beingFetched
            if(monthEventsBeingFetched.contains(monthAndYear2ColumnName(m, y))) {
                monthEventsBeingFetched.remove(monthAndYear2ColumnName(m, y));
            }
            listener.onNewEvents(events);
        } catch (SnappydbException e) {
            Log.d(this.getClass().getName(), e.toString());
        }
    }

    private String calendarTime2ColumnName(Calendar time) {
        return monthAndYear2ColumnName(time.get(Calendar.MONTH), time.get(Calendar.YEAR));
    }

    private String monthAndYear2ColumnName(int m, int y) {
        return "" + m + y;
    }
}
