package fr.bbodin.courbedepoids;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;

public class ChartRangeTest {
    private Calendar calendar(int year, int month, int day) {
        Calendar value = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        value.clear();
        value.set(year, month - 1, day, 15, 42, 19);
        return value;
    }

    @Test public void rangeIsInclusiveAndClearsTime() {
        ChartRange range = ChartRange.from(calendar(2026, 10, 9), 7, 0);

        assertEquals("2026-10-03", date(range.start));
        assertEquals("2026-10-09", date(range.end));
        assertEquals(0, range.start.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, range.end.get(Calendar.MINUTE));
        assertTrue(range.contains("2026-10-03"));
        assertTrue(range.contains("2026-10-09"));
        assertFalse(range.contains("2026-10-02"));
        assertFalse(range.contains("2026-10-10"));
    }

    @Test public void handlesMonthAndYearBoundaries() {
        ChartRange range = ChartRange.from(calendar(2026, 1, 2), 7, 0);

        assertEquals("2025-12-27", date(range.start));
        assertEquals("2026-01-02", date(range.end));
    }

    @Test public void clampsInvalidZoomAndScrollValues() {
        ChartRange tooSmall = ChartRange.from(calendar(2026, 10, 9), Integer.MIN_VALUE, Integer.MIN_VALUE);
        assertEquals(ChartRange.MIN_VISIBLE_DAYS, tooSmall.visibleDays);
        assertEquals(ChartRange.MIN_END_OFFSET_DAYS, tooSmall.endOffsetDays);

        ChartRange tooLarge = ChartRange.from(calendar(2026, 10, 9), Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(ChartRange.MAX_VISIBLE_DAYS, tooLarge.visibleDays);
        assertEquals(ChartRange.MAX_END_OFFSET_DAYS, tooLarge.endOffsetDays);
    }

    @Test public void rejectsMissingOrMalformedDates() {
        ChartRange range = ChartRange.from(calendar(2026, 10, 9), 7, 0);
        assertFalse(range.contains(null));
        assertFalse(range.contains(""));
        assertFalse(range.contains("2026-1-9"));
    }

    private String date(Calendar value) {
        return String.format(java.util.Locale.US, "%04d-%02d-%02d",
                value.get(Calendar.YEAR), value.get(Calendar.MONTH) + 1, value.get(Calendar.DAY_OF_MONTH));
    }
}
