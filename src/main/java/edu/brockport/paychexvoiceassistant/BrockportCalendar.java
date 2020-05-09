package edu.brockport.paychexvoiceassistant;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrockportCalendar {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrockportCalendar.class);
    private static final String WEBSITE = "https://www.brockport.edu/academics/calendar/";

    private static final double DATE_SIMILARITY_THRESHOLD = 0.20;

    private final HashMap<String, Date> CALENDAR = new HashMap<>();

    private List<DateInfo> dates = new ArrayList<>();
    private static final int MAX_DATES = 3;

    /**
     * Initializes a connection with the Brockport calendar website, and retrieves and stores all dates and events.
     *
     * @throws IOException If the website connection cannot be succesfully established.
     */
    public BrockportCalendar() throws IOException {
        Document doc = Jsoup.connect(WEBSITE).get();
        Elements events = doc.getElementsByClass("ev");
        Elements dates = doc.getElementsByClass("date");

        // Since some dates have multiple events, create multiple key-value pairs with the same date, appending "Day X"
        // to the name, with X being the Xth occurrence of that event.
        for(int x = 0; x < dates.size(); x++) {
            ArrayList<Date> dateList = formatDate(dates.get(x).text());

            for (Date date : dateList) {
                String event = events.get(x).text();

                if (CALENDAR.containsKey(event)) {
                    String duplicate = event + " Day 2";

                    for (int y = 2; CALENDAR.containsKey(duplicate.substring(0,duplicate.length()-1)+ y); y++){
                        duplicate = duplicate.substring(0, duplicate.length()-1) + (y+1);
                    }

                    event = duplicate;
                }

                CALENDAR.put(event, date);
            }
        }
    }

    /**
     * Formats a string of all date and time variants that the Brockport calendar uses into a format parsable by
     * {@link java.util.Date}. If a date range is detected, multiple strings will be formatted.
     *
     * The following are examples of formats that are understood:
     * July 4, 2020
     * August 23, 2019, Friday
     * September 26 – 28, 2019
     * October 14 & 15, 2019, Monday & Tuesday
     * August 26, 2019, Monday, 8 AM
     * April 10, 2020, Friday, 9 AM – 5 PM
     *
     * @param dateString A date and time string in one of the expected input formats.
     * @return A list of parsed dates.
     * @throws InputMismatchException If the given date and time string is not in a recognized
     */
    private ArrayList<Date> formatDate(String dateString) throws InputMismatchException {
        List<String> dateSplit = Arrays.asList(dateString.split(" "));
        SimpleDateFormat dateFormat;
        ArrayList<Date> dates = new ArrayList<>();

        try {
            switch (dateSplit.size()) {
                case 3:
                    // e.g. July 4, 2020
                    dateFormat = new SimpleDateFormat("MMMMM d, yyyy");
                    dates.add(dateFormat.parse(dateString));
                    break;
                case 4:
                    // e.g. August 23, 2019, Friday
                    dateFormat = new SimpleDateFormat("MMMMM d, yyyy, EEEEE");
                    dates.add(dateFormat.parse(dateString));
                    break;
                case 5:
                    // e.g. September 26 – 28, 2019
                case 8:
                    // e.g. October 14 & 15, 2019, Monday & Tuesday
                    dateFormat = new SimpleDateFormat("MMMMM d yyyy");
                    Date startDate = dateFormat.parse(String.format("%s %s %s", dateSplit.get(0),
                                                                                dateSplit.get(1),
                                                                                dateSplit.get(4)));

                    dateFormat = new SimpleDateFormat("MMMMM d, yyyy");
                    Date endDate = dateFormat.parse(String.format("%s %s %s", dateSplit.get(0),
                                                                              dateSplit.get(3),
                                                                              dateSplit.get(4)));

                    while (!startDate.after(endDate)) {
                        dates.add(startDate);
                        startDate = DateUtils.addDays(startDate, 1);
                    }

                    break;
                case 6:
                    // e.g. August 26, 2019, Monday, 8 AM
                    dateFormat = new SimpleDateFormat("MMMMM d, yyyy, EEEEE, hh a");
                    dates.add(dateFormat.parse(dateString));
                case 9:
                    // e.g. April 10, 2020, Friday, 9 AM – 5 PM
                    // Current method: chop off the second time and run it through like normal.
                    dateSplit = dateSplit.subList(0, 6);
                    dateString = String.join(" ", dateSplit);
                    break;
                default:
                    LOGGER.error("Input " + dateString + " not in expected format.");
                    throw new InputMismatchException("Input " + dateString + " not in expected format.");
            }
        } catch (ParseException e) {
            LOGGER.error(e.getLocalizedMessage());
        } catch (InputMismatchException e) {
            LOGGER.error("Input " + dateString + "not in expected format.");
            throw new InputMismatchException("Input " + dateString + " not in expected format.");
        }

        return dates;
    }

    /**
     * Retrieves up to {@code MAX_DATES} number of {@link java.util.Date}s, wrapped in a sorted
     * {@link java.util.ArrayList<java.util.Date>}, for an event, since multiple events with the same name can occur and
     * event name matching may not be ideal.
     *
     * @param eventName The event name.
     * @param tense If {@code Tense.PAST}, considers any past events. If {@code Tense.NOTPAST}, only future events are
     *             considered.
     * @return The {@link java.util.ArrayList<java.util.Date>} for an event.
     */
    public List<DateInfo> getEventDates(String eventName, Tense tense) {
        // Remove all non-alphanumeric characters from the event name.
        eventName = eventName.toLowerCase().replaceAll("[^a-z0-9]", "");

        // Iterate through every key-value pair and compare the event name similarity to the event in the current loop
        // state.
        for (Map.Entry<String, Date> entry : CALENDAR.entrySet()) {
            // Remove all non-alphanumeric characters from the current event.
            String event = entry.getKey();
            String tempEvent = event.toLowerCase().replaceAll("[^a-z0-9]", "");
            Date tempDate = entry.getValue();

            if (tense == Tense.PAST || !tempDate.before(new Date())) {
                insertDate(new DateInfo(event,
                        tempDate,
                        tempEvent.contains(eventName) ? 100 : FuzzySearch.partialRatio(eventName, tempEvent)));
            }
        }

        return dates;
    }

    /**
     * Retrieves the event name for a given {@link java.util.Date}, considering only the date.
     *
     * @param eventDate The {@link java.util.Date} to consider.
     * @return The name of the event.
     *         null if no event is found.
     */
    public String getEventName(Date eventDate) {
        // Iterate through every key-value pair and compare the current date to eventDate. If they are the same date,
        // return it.
        for (Map.Entry<String, Date> entry : CALENDAR.entrySet()) {
            if (DateUtils.isSameDay(entry.getValue(), eventDate)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public DateInfo getDaysUntilEvent(String event) {
        List<DateInfo> dates = getEventDates(event, Tense.NOTPAST);

        if (dates.isEmpty()) {
            return null;
        } else {
            return dates.get(0);
        }
    }

    private void insertDate(DateInfo dateInfo) {
        dates.sort(Comparator.comparing(o -> ((DateInfo) o).getSimilarity()).reversed());

        if (dateInfo.getSimilarity() >= DATE_SIMILARITY_THRESHOLD) {
            if (dates.size() < MAX_DATES) {
                dates.add(dateInfo);
            } else {
                int lastIndex = dates.size() - 1;

                if (dateInfo.getSimilarity().compareTo(dates.get(lastIndex).getSimilarity()) >= 0) {
                    dates.set(lastIndex, dateInfo);
                }
            }
        }

        dates.sort(Comparator.comparing(o -> ((DateInfo) o).getSimilarity()).reversed());
    }

    public List<DateInfo> getEventsInNextNDays(int numDays) {
        final int DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
        final Date today = new Date();

        Date cutoff = new Date();
        for(int i=0; i<numDays; i++){
            cutoff.setTime(cutoff.getTime() + DAY_IN_MILLISECONDS);
        }

        List <DateInfo> eventsInRange = new ArrayList();
        for(Map.Entry<String, Date> entry : CALENDAR.entrySet()){
            if(entry.getValue().after(today) && entry.getValue().before(cutoff)){
                eventsInRange.add(new DateInfo(entry.getKey(), entry.getValue(), 0));
            }
        }

        eventsInRange.sort(Comparator.comparing(o -> ((DateInfo) o).getDate()));
        return eventsInRange;
    }
}
