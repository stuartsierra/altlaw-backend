package org.altlaw.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.glowacki.CalendarParser;
import org.glowacki.CalendarParserException;
import java.text.ParseException;
import java.util.Locale;


public class DateUtils {

    public static final int THIS_YEAR =
        Calendar.getInstance().get(Calendar.YEAR);

    public static final TimeZone GMT =
        TimeZone.getTimeZone("GMT");

    public static final SimpleDateFormat ISO8601_GMT;
    static {
        ISO8601_GMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO8601_GMT.setTimeZone(GMT);
    }

    public static final SimpleDateFormat RFC2822;
    static {
        RFC2822 = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z",
                                       Locale.US);
    }

    public static final SimpleDateFormat UNIXDATE =
        new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    /** Converts a Calendar object to an ISO-8601 date/time string.
     * Always converts to GMT.  Adapted from example at
     * http://www.dynamicobjects.com/d2r/archives/003057.html */
    public static String calendarToISO8601(Calendar cal) {
        Date date = cal.getTime();
        return ISO8601_GMT.format(date); // guaranteed GMT
    }

    /** Converts a Calendar object to an RFC 2822 date/time string. */
    public static String calendarToRFC2822(Calendar cal) {
        Date date = cal.getTime();
        return RFC2822.format(date);
    }

    /** Converts a Date object to an ISO-8601 date/time string.
     * Always converts to GMT. */
    public static String dateToISO8601(Date date) {
        return ISO8601_GMT.format(date); // guaranteed GMT
    }

    /** Converts a Date object to an RFC 2822 date/time string. */
    public static String dateToRFC2822(Date date) {
        return RFC2822.format(date);
    }

    public static Date parseDateISO8601(String date) throws ParseException {
        return ISO8601_GMT.parse(date);
    }

    public static Date parseDateRFC2822(String date) throws ParseException {
        return RFC2822.parse(date);
    }

    public static Date parseUnixDate(String date) throws ParseException {
        return UNIXDATE.parse(date);
    }

    /** Returns a timestamp string for the current time in ISO-8601
     * format.  Always in GMT. */
    public static String timestamp() {
        return calendarToISO8601(Calendar.getInstance(GMT));
    }

    public static String http_timestamp() {
        return calendarToRFC2822(Calendar.getInstance(GMT));
    }

    public static Calendar parsePastDate(String string) {
        try {
            Calendar cal = CalendarParser.parse(string, CalendarParser.MM_DD_YY);
            while (cal.get(Calendar.YEAR) > THIS_YEAR) {
                cal.roll(Calendar.YEAR, -100);
            }
            return cal;
        } catch (CalendarParserException e) {
            return null;
        }
    }

    public static String sqlDateString(Calendar cal) {
        if (cal == null) { return null; }
        return new java.sql.Date(cal.getTimeInMillis()).toString();
    }

    public static String sqlDateString(Date d) {
        if (d == null) { return null; }
        return new java.sql.Date(d.getTime()).toString();
    }
}