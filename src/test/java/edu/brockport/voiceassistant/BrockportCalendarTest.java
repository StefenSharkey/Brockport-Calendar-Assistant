package edu.brockport.voiceassistant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class BrockportCalendarTest {

    private static String pastEvent;
    private static String notPastEvent;
    private static String fakeEvent;

    private static Date pastDate;
    private static Date notPastDate;
    private static Date fakeDate;

    @BeforeAll
    static void setup() {
        pastEvent = "Bport Homecoming & Family Weekend";
        notPastEvent = "Independence Day, College Closed";
        fakeEvent = "This is a fake event that should not work for any use case because it is fake.";

        pastDate = getDate("2020-03-11T12:00:00-05:00");
        notPastDate = getDate("2020-07-04T12:00:00-05:00");
        fakeDate = getDate("2020-07-05T12:00:00-05:00");
    }

    private static Date getDate(String dateString) {
        return Date.from(OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
    }

    @Test
    @DisplayName("1. Gets date of a past event with a past tense.")
    public void getDate_PastEvent_PastTense_ShouldReturnDate() throws IOException {
        assertFalse(new BrockportCalendar().getEventDates(pastEvent, Tense.PAST).isEmpty());
    }

    @Test
    @DisplayName("2. Fails to get date of a past event with a not past tense.")
    public void getDate_PastEvent_NotPastTense_ShouldNotReturnDate() throws IOException {
        assertTrue(new BrockportCalendar().getEventDates(pastEvent, Tense.NOTPAST).isEmpty());
    }

    @Test
    @DisplayName("3. Gets date of a not past event with a not past tense.")
    public void getDate_NotPastEvent_NotPastTense_ShouldReturnDate() throws IOException {
        assertFalse(new BrockportCalendar().getEventDates(notPastEvent, Tense.NOTPAST).isEmpty());
    }

    @Test
    @DisplayName("4. Fails to get date of a not past event with a past tense.")
    public void getDate_NotPastEvent_PastTense_ShouldReturnDate() throws IOException {
        assertTrue(new BrockportCalendar().getEventDates(notPastEvent, Tense.PAST).isEmpty());
    }

    @Test
    @DisplayName("5. Fails to get date of a fake event with a past tense.")
    public void getDate_FakeEvent_PastTense_ShouldNotReturnDate() throws IOException {
        assertTrue(new BrockportCalendar().getEventDates(fakeEvent, Tense.PAST).isEmpty());
    }

    @Test
    @DisplayName("6. Fails to get date of a fake event with a not past tense.")
    public void getDate_FakeEvent_NotPastTense_ShouldNotReturnDate() throws IOException {
        assertTrue(new BrockportCalendar().getEventDates(fakeEvent, Tense.NOTPAST).isEmpty());
    }

    @Test
    @DisplayName("7. Gets name of a past date.")
    public void getEvent_PastDate_ShouldReturnEvent() throws IOException {
        assertNotNull(new BrockportCalendar().getEventName(pastDate));
    }

    @Test
    @DisplayName("8. Gets name of a not past date.")
    public void getEvent_NotPastDate_ShouldReturnEvent() throws IOException {
        assertNotNull(new BrockportCalendar().getEventName(notPastDate));
    }

    @Test
    @DisplayName("9. Fails to get name of a fake date.")
    public void getEvent_FakeDate_ShouldNotReturnEvent() throws IOException {
        assertNull(new BrockportCalendar().getEventName(fakeDate));
    }

    @Test
    @DisplayName("10. Gets days until not past event.")
    public void getDaysUntilEvent_NotPastEvent_ShouldReturnDays() throws IOException {
        assertNotNull(new BrockportCalendar().getDaysUntilEvent(notPastEvent));
    }

    @Test
    @DisplayName("11. Fails to get days until past event.")
    public void getDaysUntilEvent_PastEvent_ShouldNotReturnDays() throws IOException {
        assertNotNull(new BrockportCalendar().getDaysUntilEvent(pastEvent));
    }
}
