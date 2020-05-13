/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.brockport.voiceassistant;

import com.google.actions.api.ActionRequest;
import com.google.actions.api.ActionResponse;
import com.google.actions.api.DialogflowApp;
import com.google.actions.api.ForIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BrockportCalendarApp extends DialogflowApp {

    @ForIntent("getdate")
    public ActionResponse getdate(ActionRequest request) throws IOException {
        String eventName = (String) request.getParameter("event");
        Tense tense = Tense.valueOf(((String) request.getParameter("tense")).toUpperCase());
        List<DateInfo> dates = new BrockportCalendar().getEventDates(eventName, tense);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM d, yyyy");

        final String[] response = {"You asked about " + eventName};

        if (tense == Tense.PAST) {
            response[0] += ", including past events";
        }

        response[0] += ".\n";

        if (dates.isEmpty()) {
            response[0] += "There are no events occurring with that name.";
        } else {
            if (dates.size() > 1) {
                response[0] += "I found " + dates.size() + " possible dates with this event.\n";
            }

            dates.forEach(dateInfo ->
                    response[0] += "I found " + dateInfo.getCleanEventName() + " occuring on " + dateFormat.format(dateInfo.getDate()) + ".\n"
            );
        }

        return getResponseBuilder(request).add(response[0]).build();
    }

    @ForIntent("getevent")
    public ActionResponse getevent(ActionRequest request) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        Date date = Date.from(OffsetDateTime.parse((String) request.getParameter("date"), formatter).toInstant());
        Tense tense = Tense.valueOf(((String) request.getParameter("tense")).toUpperCase());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM d, yyyy");

        String eventName = new BrockportCalendar().getEventName(date);

        String response = "You asked about " + dateFormat.format(date);

        if (tense == Tense.PAST) {
            response += ", including past events";
        }

        response += ".\n";

        if (eventName == null) {
            response += "There were no events found.";
        } else {
            response += "The event is " + eventName + ".";
        }

        return getResponseBuilder(request).add(response).build();
    }

    @ForIntent("getdaysuntilevent")
    public ActionResponse getdaysuntilevent(ActionRequest request) throws IOException {
        String eventName = (String) request.getParameter("event");
        DateInfo dateInfo = new BrockportCalendar().getDaysUntilEvent(eventName);

        String response = "You asked about how many days there are until " + eventName + ".\n";

        if (dateInfo == null) {
            response += "There were no events found.";
        } else {
            int days = (int) LocalDate.now().until(dateInfo
                    .getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), ChronoUnit.DAYS);
            response += "There are " + days + " days until " + dateInfo.getCleanEventName() + ".";
        }

        return getResponseBuilder(request).add(response).build();
    }

    @ForIntent("getfutureevents")
    public ActionResponse getfutureevents(ActionRequest request) throws IOException {
        int numDays = ((Number) request.getParameter("numdays")).intValue();
        String response;

        if(numDays <= 50 && numDays > 0) {
            List<DateInfo> events = new BrockportCalendar().getEventsInNextNDays(numDays);

            response = "You asked about upcoming events in the next " + numDays + " days.\n";

            if (events.size() == 0) {
                response += "There were no events found.";
            } else {
                if (events.size() == 1) {
                    response += "There was one event found in the next " + numDays + " days:\n";
                } else {
                    response += "There were " + events.size() + " events found in the next " + numDays + " days:\n";
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM d, yyyy");

                for (DateInfo event : events) {
                    response += event.getCleanEventName() + " on " + dateFormat.format(event.getDate()) + "\n";
                }
            }
        } else {
            response = "Number of days must be between 1 and 50.";
        }

        return getResponseBuilder(request).add(response).build();
    }

}
