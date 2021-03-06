package com.arcusapp.labolibre.util;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An asynchronous task that handles the Calendar API event list retrieval.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */
public class EventFetchTask extends AsyncTask<EventFetchTask.EventRequest, Void, List<Events>> {

    static public String CALENDAR_ID = "calendar_id";

    private EventFetchResponseListener listener;
    private Calendar calendarService;
    private DateTime timeRequest;

    public EventFetchTask(Calendar service, @NonNull EventFetchResponseListener listener) {
        this.calendarService = service;
        this.listener = listener;
    }

    @Override
    protected List<Events> doInBackground(EventRequest... params) {
        timeRequest = params[0].startTime;

        List<Events> events = new ArrayList<Events>();
        for (EventRequest r : params) {
            try {
                events.add(getEventsFromCalendar(r.calendarID, r.startTime, r.endTime));
            } catch (IOException e) {
                // do nothing
            }
        }

        return events;
    }

    @Override
    protected void onPostExecute(List<Events> events) {
        listener.onFetchFinished(timeRequest, events);
        super.onPostExecute(events);
    }

    private Events getEventsFromCalendar(String cid, DateTime datemin, DateTime datemax) throws IOException  {
        // List the next 20 events from the calendar.
        Events events = calendarService.events().list(cid)
                .setMaxResults(100)
                .setTimeMin(datemin)
                .setTimeMax(datemax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        events.set(CALENDAR_ID, cid);
        return events;
    }

    public static class EventRequest {
        String calendarID = "";
        DateTime startTime;
        DateTime endTime;

        public EventRequest(String calendarID, DateTime startTime, DateTime endTime) {
            this.calendarID = calendarID;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public interface EventFetchResponseListener {
        void onFetchFinished(DateTime timeRequest, List<Events> events);
    }
}