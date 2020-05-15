package edu.brockport.voiceassistant;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;

import me.xdrop.fuzzywuzzy.FuzzySearch;
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

    private final Map<String, Date> CALENDAR = new HashMap<>();

    private final List<DateInfo> DATES = new ArrayList<>();
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
        for (int[] x = {0}; x[0] < dates.size(); x[0]++) {
            Iterable<Date> dateList = formatDate(dates.get(x[0]).text());

            dateList.forEach(date -> {
                String eventName = events.get(x[0]).text();

                if (CALENDAR.containsKey(eventName)) {
                    String duplicate = eventName + " Day 2";

                    for (int y = 2; CALENDAR.containsKey(duplicate.substring(0,duplicate.length()-1)+ y); y++){
                        duplicate = duplicate.substring(0, duplicate.length() - 1) + (y + 1);
                    }

                    eventName = duplicate;
                }

                CALENDAR.put(eventName, date);
            });
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
    private Iterable<Date> formatDate(String dateString) throws InputMismatchException {
        List<String> dateSplit = Arrays.asList(dateString.split(" "));
        SimpleDateFormat dateFormat;
        List<Date> dates = new ArrayList<>();

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
                    //only added at the start date for now, only one event of this form so not a huge deal
                    dateFormat = new SimpleDateFormat("MMMMM d, yyyy");
                    dates.add(dateFormat.parse(dateSplit.get(0) + " " + dateSplit.get(1) + ", " + dateSplit.get(4)));
                    break;
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
                    break;
                case 9:
                    // e.g. April 10, 2020, Friday, 9 AM – 5 PM
                    // Current method: chop off the second time and run it through like normal.
                    dateSplit = dateSplit.subList(0, 3);
                    dateFormat = new SimpleDateFormat("MMMMM d, yyyy");
                    dates.add(dateFormat.parse(String.join(" ", dateSplit)));
                    break;
                default:
                    LOGGER.error("Input {} not in expected format.", dateString);
                    throw new InputMismatchException("Input " + dateString + " not in expected format.");
            }
        } catch (ParseException e) {
            LOGGER.error(e.getLocalizedMessage());
        } catch (InputMismatchException e) {
            LOGGER.error("Input {} not in expected format.", dateString);
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
    public List<DateInfo> getEventDates(String eventName, Tense tense, boolean cleanEventNames) {
        // Remove all non-alphanumeric characters from the event name.
        String finalEventName = eventName.toLowerCase().replaceAll("[^a-z0-9]", "")
                .replace("graduation", "commencement ceremony");

        // Iterate through every key-value pair and compare the event name similarity to the event in the current loop
        // state.
        CALENDAR.forEach((currEventName, date) -> {
            // Remove all non-alphanumeric characters from the current event.
            String tempEvent = currEventName.toLowerCase().replaceAll("[^a-z0-9]", "");

            if (tense == Tense.PAST || !date.before(new Date())) {
                insertDate(new DateInfo(cleanEventNames ? getCleanEventName(currEventName) : currEventName,
                        date,
                        tempEvent.contains(finalEventName) ? 100 : FuzzySearch.partialRatio(finalEventName, tempEvent)));
            }
        });

        return DATES;
    }

    /**
     * Retrieves the event name for a given {@link java.util.Date}, considering only the date.
     *
     * @param eventDate The {@link java.util.Date} to consider.
     * @return The name of the event.
     *         null if no event is found.
     */
    public String getEventName(Date eventDate, boolean cleanEventName) {
        final ArrayList<String> events = new ArrayList<>();
        String ret = "";
        // Iterate through every key-value pair and compare the current date to eventDate. If they are the same date,
        // return it.
        CALENDAR.forEach((currEventName, date) -> {
            if (DateUtils.isSameDay(date, eventDate)) {
                events.add(currEventName);
            }
        });

        for (int i = 0; i < events.size(); i++) {
            if (i != 0) {
                if (i == events.size() - 1) {
                    if (events.size() > 2) {
                        ret += ",";
                    }

                    ret += " and ";
                } else {
                    ret += ", ";
                }
            }

            ret += events.get(i);
        }

        if(cleanEventName){
            ret = getCleanEventName(ret);
        }

        return (ret.equals("")) ? null : ret;
    }

    public DateInfo getDaysUntilEvent(String eventName, boolean cleanEventNames) {
        List<DateInfo> dates = getEventDates(eventName, Tense.NOTPAST, cleanEventNames);

        return dates.isEmpty() ? null : dates.get(0);
    }

    private void insertDate(DateInfo dateInfo) {
        DATES.sort(Comparator.comparing(o -> ((DateInfo) o).getSimilarity()).reversed());

        if (dateInfo.getSimilarity() >= DATE_SIMILARITY_THRESHOLD) {
            if (DATES.size() < MAX_DATES) {
                DATES.add(dateInfo);
            } else {
                int lastIndex = DATES.size() - 1;

                if (dateInfo.getSimilarity().compareTo(DATES.get(lastIndex).getSimilarity()) >= 0) {
                    DATES.set(lastIndex, dateInfo);
                }
            }
        }

        DATES.sort(Comparator.comparing(o -> ((DateInfo) o).getSimilarity()).reversed());
    }

    public List<DateInfo> getEventsInNextNDays(int numDays, boolean cleanEventNames) {
        Calendar calendar = Calendar.getInstance();

        Date today = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, numDays);
        Date cutoff = calendar.getTime();

        List <DateInfo> eventsInRange = new ArrayList<>();

        CALENDAR.forEach((eventName, date) -> {
            if(date.after(today) && date.before(cutoff)) {
                eventsInRange.add(new DateInfo(cleanEventNames ? getCleanEventName(eventName) : eventName, date, 0));
            }
        });

        eventsInRange.sort(Comparator.comparing(DateInfo::getDate));
        return eventsInRange;
    }

    private static String getCleanEventName(String eventName) {
        return eventName.replaceAll("Day \\d", "").replaceAll("[ ][(]\\d[)]", "").replaceAll("[(]\\d[)]", "");
    }
}
