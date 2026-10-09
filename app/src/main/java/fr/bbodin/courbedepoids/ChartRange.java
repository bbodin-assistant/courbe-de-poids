package fr.bbodin.courbedepoids;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Pure date-range logic shared by home refresh and chart gestures. */
final class ChartRange {
    static final int MIN_VISIBLE_DAYS = 3;
    static final int MAX_VISIBLE_DAYS = 3650;
    static final int MIN_END_OFFSET_DAYS = -3650;
    static final int MAX_END_OFFSET_DAYS = 36500;

    final Calendar start;
    final Calendar end;
    final int visibleDays;
    final int endOffsetDays;

    private ChartRange(Calendar start, Calendar end, int visibleDays, int endOffsetDays) {
        this.start = start;
        this.end = end;
        this.visibleDays = visibleDays;
        this.endOffsetDays = endOffsetDays;
    }

    static int clampVisibleDays(int days) {
        return Math.max(MIN_VISIBLE_DAYS, Math.min(MAX_VISIBLE_DAYS, days));
    }

    static int clampEndOffsetDays(int offsetDays) {
        return Math.max(MIN_END_OFFSET_DAYS, Math.min(MAX_END_OFFSET_DAYS, offsetDays));
    }

    static ChartRange from(Calendar reference, int visibleDays, int endOffsetDays) {
        if (reference == null) throw new IllegalArgumentException("reference calendar must not be null");
        int days = clampVisibleDays(visibleDays);
        int offset = clampEndOffsetDays(endOffsetDays);
        Calendar rangeEnd = (Calendar) reference.clone();
        rangeEnd.set(Calendar.HOUR_OF_DAY, 0);
        rangeEnd.set(Calendar.MINUTE, 0);
        rangeEnd.set(Calendar.SECOND, 0);
        rangeEnd.set(Calendar.MILLISECOND, 0);
        rangeEnd.add(Calendar.DAY_OF_YEAR, -offset);
        Calendar rangeStart = (Calendar) rangeEnd.clone();
        rangeStart.add(Calendar.DAY_OF_YEAR, -(days - 1));
        return new ChartRange(rangeStart, rangeEnd, days, offset);
    }

    boolean contains(String isoDate) {
        if (isoDate == null || isoDate.length() != 10) return false;
        String first = format(start);
        String last = format(end);
        return isoDate.compareTo(first) >= 0 && isoDate.compareTo(last) <= 0;
    }

    private static String format(Calendar value) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(value.getTime());
    }
}
