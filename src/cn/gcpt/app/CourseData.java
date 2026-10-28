package cn.gcpt.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cached, structured timetable plus the raw text kept for verification. */
public final class CourseData {
    public String term = "";
    public String rawText = "";
    public String lastSync = "";
    public final List<CourseParser.Course> courses = new ArrayList<CourseParser.Course>();
    public final List<String> pending = new ArrayList<String>();

    public boolean isEmpty() {
        return courses.isEmpty();
    }

    public boolean hasPending() {
        return !pending.isEmpty();
    }

    public void clear() {
        term = "";
        rawText = "";
        lastSync = "";
        courses.clear();
        pending.clear();
    }

    /** Replaces the in-memory data. Throws only for a malformed cache envelope. */
    public void load(JSONObject obj) throws JSONException {
        clear();
        term = obj.optString("term", "待确认");
        JSONArray cells = obj.optJSONArray("cells");
        Set<String> seen = new HashSet<String>();
        if (cells != null) {
            for (int i = 0; i < cells.length(); i++) {
                JSONObject cell = cells.getJSONObject(i);
                int day = cell.optInt("day");
                String raw = cell.optString("text");
                String key = day + "|" + cell.optString("status") + "|" + raw;
                if (!seen.add(key)) continue;
                CourseParser.Result parsed = CourseParser.parseCell(day, raw, cell.optString("status"));
                courses.addAll(parsed.courses);
                pending.addAll(parsed.pending);
            }
        }
        JSONArray other = obj.optJSONArray("pending");
        if (other != null) {
            for (int i = 0; i < other.length(); i++) {
                pending.addAll(CourseParser.pendingItems(other.optString(i)));
            }
        }
    }

    public List<CourseParser.Course> coursesOn(int day, int week) {
        List<CourseParser.Course> result = new ArrayList<CourseParser.Course>();
        if (day < 1 || day > 7 || week < 1) return result;
        for (CourseParser.Course c : courses) {
            if (c.day == day && c.weeks.contains(week)) result.add(c);
        }
        result.sort((a, b) -> a.start != b.start ? a.start - b.start : a.end - b.end);
        return result;
    }

    public int countOn(int day, int week) {
        return coursesOn(day, week).size();
    }

    public int countInWeek(int week) {
        int n = 0;
        for (CourseParser.Course c : courses) if (c.weeks.contains(week)) n++;
        return n;
    }

    /** Number of distinct overlapping pairs in one week; each pair counted once. */
    public int conflictPairs(int week) {
        int pairs = 0;
        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                if (CourseParser.overlaps(courses.get(i), courses.get(j), week)) pairs++;
            }
        }
        return pairs;
    }

    public boolean overlaps(CourseParser.Course course, int week) {
        for (CourseParser.Course other : courses) {
            if (other != course && CourseParser.overlaps(course, other, week)) return true;
        }
        return false;
    }

    /** Stable semantic fingerprint used by the daily auto-sync to detect real changes. */
    public String signature() {
        List<CourseParser.Course> sorted = new ArrayList<CourseParser.Course>(courses);
        sorted.sort((a, b) -> {
            if (a.day != b.day) return a.day - b.day;
            if (a.start != b.start) return a.start - b.start;
            if (a.end != b.end) return a.end - b.end;
            int c = a.name.compareTo(b.name);
            if (c != 0) return c;
            return a.weeks.toString().compareTo(b.weeks.toString());
        });
        StringBuilder b = new StringBuilder();
        for (CourseParser.Course c : sorted) {
            // Exam entries are rebuilt from examsJson on every render; keeping them out of the
            // fingerprint stops the daily auto-sync from reporting phantom changes.
            if (c.name != null && c.name.startsWith("【考试】")) continue;
            b.append(c.day).append('|').append(c.start).append('|').append(c.end).append('|')
                    .append(c.name).append('|').append(c.weeks).append('|')
                    .append(c.room).append('|').append(c.teacher).append('|')
                    .append(c.group).append('|').append(c.hours).append('\n');
        }
        b.append("#pending#");
        List<String> pendingSorted = new ArrayList<String>(pending);
        Collections.sort(pendingSorted);
        for (String item : pendingSorted) b.append('|').append(item);
        b.append("#term#").append(term);
        return b.toString();
    }
}
