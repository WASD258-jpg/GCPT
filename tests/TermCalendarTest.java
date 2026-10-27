import cn.gct.demo.TermCalendar;
import java.time.LocalDate;

public class TermCalendarTest {
    static int passed = 0;

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        passed++;
    }

    public static void main(String[] args) {
        LocalDate start = LocalDate.of(2026, 8, 31); // Monday of week 1
        LocalDate monday = LocalDate.of(2026, 10, 26); // Monday of week 9

        check(TermCalendar.weekOf(start, start) == 1, "week 1 on start date");
        check(TermCalendar.weekOf(start, LocalDate.of(2026, 9, 6)) == 1, "Sunday still week 1");
        check(TermCalendar.weekOf(start, LocalDate.of(2026, 9, 7)) == 2, "next Monday is week 2");
        check(TermCalendar.weekOf(start, monday) == 9, "2026-10-26 is week 9");
        check(TermCalendar.weekOf(start, LocalDate.of(2026, 8, 30)) == 0, "before term is week 0");
        check(TermCalendar.weekOf(null, monday) == 0, "missing start is week 0");

        check(TermCalendar.mondayOf(LocalDate.of(2026, 10, 28)).equals(monday), "normalize to Monday");
        check(TermCalendar.mondayOf(monday).equals(monday), "Monday stays Monday");
        check(TermCalendar.weekStart(start, 9).equals(monday), "week start date");
        check(TermCalendar.weekStart(start, 1).equals(start), "week 1 start date");
        check(TermCalendar.weekStart(start, 0).equals(start), "week index clamps to 1");

        check(TermCalendar.shortRange(start, 9).equals("10/26–11/1"), "short range");
        check(TermCalendar.fullRange(start, 9).equals("10月26日 – 11月1日"), "full range");
        check(TermCalendar.shortRange(start, 18).equals("12/28–1/3"), "range across year");
        check(TermCalendar.weekday(monday).equals("周一"), "weekday name");
        check(TermCalendar.weekday(LocalDate.of(2026, 10, 28)).equals("周三"), "wednesday name");
        check(TermCalendar.monthDay(monday).equals("10月26日"), "month day");
        check(TermCalendar.shortMonthDay(monday).equals("10/26"), "short month day");
        check(TermCalendar.fullDate(monday).equals("2026年10月26日"), "full date");
        check(TermCalendar.weekTitle(9).equals("第 9 周"), "week title");
        check(TermCalendar.compactWeek(9).equals("第9周"), "compact week");
        check(TermCalendar.iso(start).equals("2026-08-31"), "iso output");
        check(TermCalendar.parse("2026-08-31").equals(start), "iso parse");
        check(TermCalendar.parse("not-a-date") == null, "invalid parse is null");
        check(TermCalendar.parse("") == null, "empty parse is null");

        // A date picker may normalize any weekday to Monday; the week then stays consistent.
        check(TermCalendar.weekOf(start, TermCalendar.mondayOf(LocalDate.of(2026, 10, 30))) == 9,
                "normalized weekday keeps week");

        System.out.println("PASS: " + passed + " date/week assertions");
    }
}
