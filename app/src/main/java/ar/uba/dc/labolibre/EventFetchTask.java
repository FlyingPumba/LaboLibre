package ar.uba.dc.labolibre;

import android.os.AsyncTask;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Event;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An asynchronous task that handles the Calendar API event list retrieval.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */
public class EventFetchTask extends AsyncTask<Void, Void, Void> {
    private UpcomingEventsActivity mActivity;

    /**
     * Constructor.
     * @param activity UpcomingEventsActivity that spawned this task.
     */
    EventFetchTask(UpcomingEventsActivity activity) {
        this.mActivity = activity;
    }

    /**
     * Background task to call Calendar API to fetch event list.
     * @param params no parameters needed for this task.
     */
    @Override
    protected Void doInBackground(Void... params) {
        try {
            mActivity.clearEvents();
            mActivity.updateEventList(fetchEventsFromCalendar());

        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
            mActivity.showGooglePlayServicesAvailabilityErrorDialog(
                    availabilityException.getConnectionStatusCode());
        } catch (UserRecoverableAuthIOException userRecoverableException) {
            mActivity.startActivityForResult(
                    userRecoverableException.getIntent(),
                    UpcomingEventsActivity.REQUEST_AUTHORIZATION);
        } catch (IOException e) {
            mActivity.updateStatus("The following error occurred: " +
                    e.getMessage());
        }
        return null;
    }

    /**
     * Fetch a list of the next 10 events from the primary calendar.
     * @return List of Strings describing returned events.
     * @throws IOException
     */
    private List<String> fetchEventsFromCalendar() throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        List<String> eventStrings = new ArrayList<String>();

        Events events = getEventsFromCalendar(now, mActivity.getResources().getString(R.string.labo1_cid));
        eventStrings.addAll(parseEvents(mActivity.getResources().getString(R.string.labo1_name), events.getItems()));
        events = getEventsFromCalendar(now, mActivity.getResources().getString(R.string.labo2_cid));
        eventStrings.addAll(parseEvents(mActivity.getResources().getString(R.string.labo2_name), events.getItems()));
        events = getEventsFromCalendar(now, mActivity.getResources().getString(R.string.labo3_cid));
        eventStrings.addAll(parseEvents(mActivity.getResources().getString(R.string.labo3_name), events.getItems()));
        events = getEventsFromCalendar(now, mActivity.getResources().getString(R.string.labo4_cid));
        eventStrings.addAll(parseEvents(mActivity.getResources().getString(R.string.labo4_name), events.getItems()));
        events = getEventsFromCalendar(now, mActivity.getResources().getString(R.string.labo5_cid));
        eventStrings.addAll(parseEvents(mActivity.getResources().getString(R.string.labo5_name), events.getItems()));
        events = getEventsFromCalendar(now, mActivity.getResources().getString(R.string.labo6_cid));
        eventStrings.addAll(parseEvents(mActivity.getResources().getString(R.string.labo6_name), events.getItems()));
        events = getEventsFromCalendar(now, mActivity.getResources().getString(R.string.laboTuring_cid));
        eventStrings.addAll(parseEvents(mActivity.getResources().getString(R.string.laboTuring_name), events.getItems()));


        return eventStrings;
    }

    private Events getEventsFromCalendar(DateTime date, String cid) throws IOException  {
        // List the next 20 events from the calendar.
        Events events = mActivity.mService.events().list(cid)
                .setMaxResults(20)
                .setTimeMin(date)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        return events;
    }

    private List<String> parseEvents(String calendarName, List<Event> events) {
        List<String> eventStrings = new ArrayList<String>();
        for (Event event : events) {
            DateTime start = event.getStart().getDateTime();
            if (start == null) {
                // All-day events don't have start times, so just use
                // the start date.
                start = event.getStart().getDate();
            }
            eventStrings.add(
                    String.format("%s (%s): %s", calendarName, start.toString(), event.getSummary()));
        }
        return eventStrings;
    }
}