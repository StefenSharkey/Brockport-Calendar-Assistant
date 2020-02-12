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

package edu.brockport.paychexvoiceassistant;

import com.google.actions.api.ActionRequest;
import com.google.actions.api.ActionResponse;
import com.google.actions.api.DialogflowApp;
import com.google.actions.api.ForIntent;
import com.google.actions.api.response.ResponseBuilder;
import com.google.protobuf.Any;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PaychexBrockportCalendarApp extends DialogflowApp {

    public static void main(String[] args) {
        try {
            BrockportCalendar cal = new BrockportCalendar();
            Calendar calendar = Calendar.getInstance();
            calendar.set(2019, Calendar.OCTOBER, 15);

            Date eventDate = cal.getEventDate("mid-term", false);
            String eventName = cal.getEventName(calendar.getTime());

            System.out.println(eventDate);
            System.out.println(eventName);
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage());
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PaychexBrockportCalendarApp.class);

    // Note: Do not store any state as an instance variable.
    // It is ok to have final variables where the variable is assigned a value in
    // the constructor but remains unchanged. This is required to ensure thread-
    // safety as the entry point (ActionServlet/ActionsAWSHandler) instances may
    // be reused by the server.

    @ForIntent("getdate")
    public ActionResponse getdate(ActionRequest request) {
        ActionResponse actionResponse = null;

//        try {
            LOGGER.info("get_date start.");

//            String event = (String) request.getParameter("event");
//            Tense tense = Tense.valueOf((String) request.getParameter("tense"));
//
//            BrockportCalendar calendar = new BrockportCalendar();
//
//            String response = "You asked about " + event + " with tense " + tense + ".\n";
//            Date date = calendar.getEventDate(event, tense.equals(Tense.PAST));
//            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM d, yyyy");
//
//            response += "That occurs on " + dateFormat.format(date) + ".";
            String response = "Test";

            ResponseBuilder responseBuilder = getResponseBuilder(request).add(response).endConversation();
            actionResponse = responseBuilder.build();
            LOGGER.info(actionResponse.toString());
            LOGGER.info("get_date end.");
//        } catch (IOException e) {
//            LOGGER.error(e.getLocalizedMessage());
//        }
        return actionResponse;
    }
}
