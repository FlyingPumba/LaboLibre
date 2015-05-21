package ar.uba.dc.labolibre;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;

import com.alamkanak.weekview.WeekViewEvent;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class CalendarEvents implements EventFetchTask.EventFetchResponseListener {

    private NewEventsListener listener;
    private List<String> calendarNames;

    /**
     * A Calendar service object used to query or modify calendars via the
     * Calendar API. Note: Do not confuse this class with the
     * com.google.api.services.calendar.model.Calendar class.
     */
    com.google.api.services.calendar.Calendar mService;

    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private List<Integer> calendarColors;

    public CalendarEvents(@NonNull NewEventsListener listener, GoogleAccountCredential credential) {
        this.listener = listener;
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .build();
    }

    public void fetchEventsfromCalendars(List<String> calendarIDs, List<String> calendarNames, List<Integer> calendarColors,
                                         Calendar startTime, Calendar endTime) {
        this.calendarNames = calendarNames;
        this.calendarColors = calendarColors;
        EventFetchTask.EventRequest[] requests = new EventFetchTask.EventRequest[calendarIDs.size()];
        DateTime start = new DateTime(startTime.getTime());
        DateTime end = new DateTime(endTime.getTime());
        int i = 0;
        for(String cid : calendarIDs) {
            EventFetchTask.EventRequest r = new EventFetchTask.EventRequest(cid, start, end);
            requests[i] = r;
            i++;
        }

        EventFetchTask task = new EventFetchTask(mService, this);
        task.execute(requests);
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


    @Override
    public void onFetchFinished(List<Events> events) {
        List<WeekViewEvent> aux = new ArrayList<WeekViewEvent>();
        Random rnd = new Random(42);
        for (int i = 0; i < events.size(); i++) {
            Events es = events.get(i);
            String name = this.calendarNames.get(i);
            Integer color = this.calendarColors.get(i);
            for (Event e : es.getItems()) {
                Calendar start = Calendar.getInstance();
                start.setTimeInMillis(e.getStart().getDateTime().getValue());
                Calendar end = Calendar.getInstance();
                end.setTimeInMillis(e.getEnd().getDateTime().getValue());

                String eventName = name + " - " + e.getSummary();

                WeekViewEvent event = new WeekViewEvent(rnd.nextInt(10000), eventName, start, end);
                event.setColor(color);
                aux.add(event);
            }
        }
        listener.onNewEvents(aux);
    }

    public interface NewEventsListener {
        void onNewEvents(List<WeekViewEvent> events);
    }
}
