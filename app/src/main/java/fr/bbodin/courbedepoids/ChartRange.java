package fr.bbodin.courbedepoids;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Date and bounds helpers for horizontally navigable charts. */
final class ChartRange {
    static final int MIN_VISIBLE_DAYS = 3;
    static final int MAX_VISIBLE_DAYS = 3650;
    static final int MIN_END_OFFSET_DAYS = -3650;
    static final int MAX_END_OFFSET_DAYS = 36500;
    static final int PREDICTION_DAYS = 14;

    final Calendar start;
    final Calendar end;
    final int visibleDays;
    final int endOffsetDays;

    private ChartRange(Calendar start, Calendar end, int visibleDays, int endOffsetDays) {
        this.start = start; this.end = end; this.visibleDays = visibleDays; this.endOffsetDays = endOffsetDays;
    }
    static int clampVisibleDays(int days) { return Math.max(MIN_VISIBLE_DAYS, Math.min(MAX_VISIBLE_DAYS, days)); }
    static int clampEndOffsetDays(int offsetDays) { return Math.max(MIN_END_OFFSET_DAYS, Math.min(MAX_END_OFFSET_DAYS, offsetDays)); }

    static ChartRange from(Calendar reference, int visibleDays, int endOffsetDays) {
        if (reference == null) throw new IllegalArgumentException("reference calendar must not be null");
        int days = clampVisibleDays(visibleDays), offset = clampEndOffsetDays(endOffsetDays);
        Calendar rangeEnd = (Calendar) reference.clone();
        clearTime(rangeEnd); rangeEnd.add(Calendar.DAY_OF_YEAR, -offset);
        Calendar rangeStart = (Calendar) rangeEnd.clone(); rangeStart.add(Calendar.DAY_OF_YEAR, -(days - 1));
        return new ChartRange(rangeStart, rangeEnd, days, offset);
    }

    static int[] allowedOffsets(Calendar today, String firstDate, String lastDate, int visibleDays) {
        if (today == null || firstDate == null || lastDate == null) return new int[]{MIN_END_OFFSET_DAYS, MAX_END_OFFSET_DAYS};
        int first = dayNumber(firstDate), last = dayNumber(lastDate), now = dayNumber(format(today));
        if (first == Integer.MIN_VALUE || last == Integer.MIN_VALUE || first > last)
            return new int[]{MIN_END_OFFSET_DAYS, MAX_END_OFFSET_DAYS};
        int minOffset = now - last - PREDICTION_DAYS;
        int maxOffset = now - first - clampVisibleDays(visibleDays) + 1;
        if (minOffset > maxOffset) minOffset = maxOffset;
        return new int[]{clampEndOffsetDays(minOffset), clampEndOffsetDays(maxOffset)};
    }

    static int clampOffsetToData(int offset, int visibleDays, int minOffset, int maxOffset) {
        int days = clampVisibleDays(visibleDays);
        int low = Math.min(minOffset, maxOffset), high = Math.max(minOffset, maxOffset);
        return Math.max(low, Math.min(high, clampEndOffsetDays(offset)));
    }

    boolean contains(String isoDate) {
        if (isoDate == null || isoDate.length() != 10) return false;
        return isoDate.compareTo(format(start)) >= 0 && isoDate.compareTo(format(end)) <= 0;
    }
    static int dayNumber(String isoDate) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            fmt.setLenient(false); fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = fmt.parse(isoDate);
            return date == null ? Integer.MIN_VALUE : (int)(date.getTime() / 86400000L);
        } catch (ParseException | RuntimeException e) { return Integer.MIN_VALUE; }
    }
    static String format(Calendar value) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(value.getTime());
    }
    private static void clearTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
    }
}
