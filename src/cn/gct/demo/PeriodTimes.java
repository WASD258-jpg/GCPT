package cn.gct.demo;

/** Accurate 11-period timetable for the current school timetable. */
public final class PeriodTimes {
    private static final String[][] TIMES = {
            {"08:30", "09:10"},
            {"09:15", "09:55"},
            {"10:15", "10:55"},
            {"11:00", "11:40"},
            {"14:00", "14:40"},
            {"14:45", "15:25"},
            {"15:40", "16:20"},
            {"16:25", "17:05"},
            {"18:30", "19:30"},
            {"19:40", "20:40"},
            {"21:00", "21:40"}
    };

    private PeriodTimes() {
    }

    public static int count() {
        return TIMES.length;
    }

    public static String start(int period) {
        if (period < 1 || period > TIMES.length) return "";
        return TIMES[period - 1][0];
    }

    public static String end(int period) {
        if (period < 1 || period > TIMES.length) return "";
        return TIMES[period - 1][1];
    }

    public static String range(int start, int end) {
        if (start < 1 || end < 1 || start > TIMES.length || end > TIMES.length) return "";
        return TIMES[start - 1][0] + "–" + TIMES[end - 1][1];
    }
}
