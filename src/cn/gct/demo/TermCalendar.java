package cn.gct.demo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** Pure date/week arithmetic; also exercised on the desktop JVM by tests. */
public final class TermCalendar {
    public static final String[] DAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private TermCalendar() {}

    public static LocalDate parse(String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(iso.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String iso(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    /** Normalizes any date to the Monday of its week. */
    public static LocalDate mondayOf(LocalDate date) {
        if (date == null) return null;
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 1-based teaching week, or 0 when the date is before week 1. */
    public static int weekOf(LocalDate termStart, LocalDate date) {
        if (termStart == null || date == null) return 0;
        long days = ChronoUnit.DAYS.between(termStart, date);
        if (days < 0) return 0;
        return (int) (days / 7) + 1;
    }

    public static LocalDate weekStart(LocalDate termStart, int week) {
        if (termStart == null) return null;
        return termStart.plusWeeks(Math.max(1, week) - 1L);
    }

    public static String weekday(LocalDate date) {
        if (date == null) return "";
        return DAYS[date.getDayOfWeek().getValue() - 1];
    }

    /** e.g. 10月26日 */
    public static String monthDay(LocalDate date) {
        if (date == null) return "";
        return date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
    }

    /** e.g. 2026年10月26日 */
    public static String fullDate(LocalDate date) {
        if (date == null) return "";
        return date.getYear() + "年" + monthDay(date);
    }

    /** e.g. 10/26 */
    public static String shortMonthDay(LocalDate date) {
        if (date == null) return "";
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    /** e.g. 10/26–11/1 */
    public static String shortRange(LocalDate termStart, int week) {
        LocalDate start = weekStart(termStart, week);
        if (start == null) return "";
        return shortMonthDay(start) + "–" + shortMonthDay(start.plusDays(6));
    }

    /** e.g. 10月26日 – 11月1日 */
    public static String fullRange(LocalDate termStart, int week) {
        LocalDate start = weekStart(termStart, week);
        if (start == null) return "";
        return monthDay(start) + " – " + monthDay(start.plusDays(6));
    }

    /** e.g. 第 9 周 */
    public static String weekTitle(int week) {
        return "第 " + Math.max(1, week) + " 周";
    }

    public static String compactWeek(int week) {
        return "第" + Math.max(1, week) + "周";
    }
}
