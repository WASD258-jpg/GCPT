package cn.gcpt.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.VelocityTracker;
import android.widget.OverScroller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seven-day timetable that scrolls in both directions with a frozen day header and period column.
 * All overlapping courses stay visible; nothing is silently dropped.
 */
public final class WeekGrid extends View {
    public interface Listener {
        void onCourseClick(CourseParser.Course course);
    }

    private static final int[] FILL = {
            0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
            0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF
    };
    private static final int[] ACCENT = {
            0xFF2F6B57, 0xFF7A5C45, 0xFF5C6470, 0xFF8A5E4A,
            0xFF4F6E68, 0xFF8A5F6B, 0xFF7A6A3A, 0xFF5D647A
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<CourseParser.Course> weekCourses = new ArrayList<CourseParser.Course>();
    private final List<RectF> hitRects = new ArrayList<RectF>();
    private final List<CourseParser.Course> hitCourses = new ArrayList<CourseParser.Course>();
    private final OverScroller scroller;
    private VelocityTracker tracker;
    private final int touchSlop;
    private final float density, scaledDensity;
    private final float headerH, leftW, minColW;
    private float rowH;
    private final int week, todayDay, slots;
    private final LocalDate termStart;
    private final boolean showTeacher;
    private final Listener listener;
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);

    public static int fillColor(String courseName) {
        return FILL[(courseName.hashCode() & 0x7fffffff) % FILL.length];
    }

    public static int accentColor(String courseName) {
        return ACCENT[(courseName.hashCode() & 0x7fffffff) % ACCENT.length];
    }

    private float colW;
    private int contentW, contentH;
    private int scrollX, scrollY;
    private float downX, downY, lastX, lastY;
    private boolean dragging;

    public WeekGrid(Context context, CourseData data, int week, LocalDate termStart,
                    boolean showTeacher, int todayDay, Listener listener) {
        super(context);
        this.week = week;
        this.termStart = termStart;
        this.showTeacher = showTeacher;
        this.todayDay = todayDay;
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        headerH = dp(47);
        leftW = dp(46);
        rowH = dp(58);
        minColW = dp(82);
        int max = 10;
        for (CourseParser.Course c : data.courses) {
            if (c.weeks.contains(week)) {
                weekCourses.add(c);
                if (c.end > max) max = c.end;
            }
        }
        weekCourses.sort((a, b) -> a.start != b.start ? a.start - b.start : a.end - b.end);
        slots = Math.min(16, Math.max(PeriodTimes.count(), max));
        scroller = new OverScroller(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setFocusable(true);
        setClickable(true);
        setContentDescription(TermCalendar.weekTitle(week) + "课表，本周" + weekCourses.size()
                + "条排课；可上下左右滚动，点击课程查看详情");
    }

    private float dp(float v) {
        return v * density;
    }

    private float sp(float v) {
        return v * scaledDensity;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (h == 0) h = (int) (headerH + slots * rowH);
        setMeasuredDimension(w, h);
        updateGeometry(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGeometry(w, h);
    }

    private void updateGeometry(int width, int height) {
        float available = Math.max(0, height - headerH);
        float base = dp(58);
        // On tall portrait screens, let a short week breathe instead of leaving a blank card.
        rowH = available > 0 && slots * base < available
                ? Math.min(dp(92), available / slots)
                : base;
        colW = Math.max(minColW, (width - leftW) / 7f);
        contentW = (int) Math.ceil(leftW + 7 * colW);
        contentH = (int) Math.ceil(slots * rowH);
        scrollX = clamp(scrollX, 0, maxScrollX());
        scrollY = clamp(scrollY, 0, maxScrollY());
    }

    private int maxScrollX() {
        return Math.max(0, contentW - getWidth());
    }

    private int maxScrollY() {
        return Math.max(0, contentH - Math.max(0, getHeight() - (int) headerH));
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        hitRects.clear();
        hitCourses.clear();

        drawBody(canvas);
        drawPeriodColumn(canvas);
        drawDayHeader(canvas);
        drawCorner(canvas);
        drawEdges(canvas);
        if (weekCourses.isEmpty()) drawEmptyState(canvas);
        else drawEndNote(canvas);
        drawScrollbars(canvas);
    }

    private void drawEndNote(Canvas canvas) {
        if (headerH + contentH >= getHeight() - dp(4)) return;
        int save = canvas.save();
        canvas.clipRect(leftW, headerH, getWidth(), getHeight());
        paint.setTypeface(regular);
        paint.setTextSize(sp(10.5f));
        paint.setColor(0xFF9AAAB9);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("以下无排课", leftW + (getWidth() - leftW) / 2f,
                headerH + contentH + dp(32) - scrollY, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.restoreToCount(save);
    }

    private void drawBody(Canvas canvas) {
        int save = canvas.save();
        canvas.clipRect(leftW, headerH, getWidth(), getHeight());
        canvas.translate(leftW - scrollX, headerH - scrollY);

        if (todayDay >= 1 && todayDay <= 7) {
            paint.setColor(0xFFF3F6F2);
            canvas.drawRect((todayDay - 1) * colW, 0, todayDay * colW, contentH, paint);
        }

        paint.setColor(Ui.LINE_SOFT);
        paint.setStrokeWidth(dp(1));
        for (int s = 0; s <= slots; s++) {
            float y = s * rowH;
            canvas.drawLine(0, y, 7 * colW, y, paint);
        }
        for (int d = 0; d <= 7; d++) {
            float x = d * colW;
            canvas.drawLine(x, 0, x, contentH, paint);
        }

        for (CourseParser.Course c : weekCourses) drawCourse(canvas, c);
        canvas.restoreToCount(save);
    }

    private void drawCourse(Canvas canvas, CourseParser.Course c) {
        int colorIndex = (c.name.hashCode() & 0x7fffffff) % FILL.length;
        float x = (c.day - 1) * colW + dp(3);
        float y = (c.start - 1) * rowH + dp(3);
        float w = colW - dp(6);
        float h = (c.end - c.start + 1) * rowH - dp(6);
        if (w < dp(10) || h < dp(10)) return;

        RectF block = new RectF(x, y, x + w, y + h);
        paint.setColor(FILL[colorIndex]);
        canvas.drawRoundRect(block, dp(2), dp(2), paint);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1));
        stroke.setColor(Ui.LINE);
        canvas.drawRoundRect(block, dp(2), dp(2), stroke);

        // Accent rail on the left edge.
        RectF rail = new RectF(x + dp(3), y + dp(9), x + dp(6), y + h - dp(9));
        paint.setColor(ACCENT[colorIndex]);
        canvas.drawRoundRect(rail, dp(1), dp(1), paint);

        boolean conflict = false;
        for (CourseParser.Course other : weekCourses) {
            if (other != c && CourseParser.overlaps(c, other, week)) {
                conflict = true;
                break;
            }
        }
        if (conflict) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(2));
            stroke.setColor(Ui.DANGER);
            canvas.drawRoundRect(block, dp(2), dp(2), stroke);
        }

        float textX = x + dp(11);
        float maxW = w - dp(16);
        float bottom = y + h - dp(5);
        float lineH = dp(16);
        float next = y + dp(15);

        paint.setTypeface(medium);
        paint.setColor(Ui.TEXT);
        paint.setTextSize(sp(13));
        int nameLines = drawWrapped(canvas, c.name, textX, next, maxW, lineH,
                h >= rowH * 3.2f ? 3 : h >= rowH * 2.2f ? 2 : 1);
        next += nameLines * lineH;

        paint.setTypeface(regular);
        paint.setTextSize(sp(10.5f));
        if (next + dp(2) <= bottom) {
            paint.setColor(Ui.TEXT_SOFT);
            drawSingleLine(canvas, c.room, textX, next, maxW);
            next += dp(14);
        }
        if (showTeacher && c.teacher != null && !c.teacher.isEmpty() && next + dp(2) <= bottom) {
            paint.setTextSize(sp(10));
            paint.setColor(Ui.TEXT_FAINT);
            drawSingleLine(canvas, c.teacher, textX, next, maxW);
        }

        if (h >= dp(96)) {
            paint.setTypeface(medium);
            paint.setTextSize(sp(9.5f));
            paint.setColor(ACCENT[colorIndex]);
            String footer = PeriodTimes.range(c.start, c.end);
            drawSingleLine(canvas, footer, textX, y + h - dp(8), maxW);
            if (h >= dp(130)) {
                paint.setTypeface(regular);
                paint.setColor(Ui.TEXT_FAINT);
                drawSingleLine(canvas, c.weeksText, textX, y + h - dp(8) - dp(13), maxW);
            }
        }

        hitRects.add(block);
        hitCourses.add(c);
    }

    private int drawWrapped(Canvas canvas, String text, float x, float y, float maxW,
                            float lineH, int maxLines) {
        if (text == null || text.isEmpty() || maxLines <= 0) return 0;
        int lines = 0;
        String rest = text;
        while (rest.length() > 0 && lines < maxLines) {
            int n = paint.breakText(rest, true, maxW, null);
            n = safeBreak(rest, n);
            if (n <= 0) break;
            String part = rest.substring(0, n);
            rest = rest.substring(n);
            if (rest.length() > 0 && lines == maxLines - 1) {
                String candidate = part + "…";
                int k = paint.breakText(candidate, true, maxW, null);
                k = safeBreak(candidate, k);
                part = candidate.substring(0, Math.max(0, k));
                rest = "";
            }
            canvas.drawText(part, x, y + lines * lineH, paint);
            lines++;
        }
        return lines;
    }

    /** Avoid cutting an ASCII token such as BIM or A6-N514 in half. */
    private int safeBreak(String text, int n) {
        if (n <= 0 || n >= text.length()) return n;
        if (isAsciiToken(text.charAt(n - 1)) && isAsciiToken(text.charAt(n))) {
            int start = n - 1;
            while (start > 0 && isAsciiToken(text.charAt(start - 1))) start--;
            if (start > 0) return start;
        }
        return n;
    }

    private boolean isAsciiToken(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9') || c == '-';
    }

    private void drawSingleLine(Canvas canvas, String text, float x, float baseline, float maxW) {
        if (text == null || text.isEmpty()) return;
        if (paint.measureText(text) <= maxW) {
            canvas.drawText(text, x, baseline, paint);
            return;
        }
        String candidate = text + "…";
        int k = paint.breakText(candidate, true, maxW, null);
        k = safeBreak(candidate, k);
        if (k > 0) canvas.drawText(candidate.substring(0, k), x, baseline, paint);
    }

    private void drawDayHeader(Canvas canvas) {
        int save = canvas.save();
        canvas.clipRect(leftW, 0, getWidth(), headerH);
        canvas.drawRect(leftW, 0, getWidth(), headerH, fill(0xFFFFFFFF));
        canvas.translate(leftW - scrollX, 0);
        for (int day = 1; day <= 7; day++) {
            boolean today = day == todayDay;
            float x = (day - 1) * colW;
            if (today) {
                paint.setColor(Ui.TEAL_SOFT);
                RectF pill = new RectF(x + dp(5), dp(5), x + colW - dp(5), headerH - dp(5));
                canvas.drawRoundRect(pill, dp(2), dp(2), paint);
            }
            String label = TermCalendar.DAYS[day - 1];
            paint.setTypeface(medium);
            paint.setTextSize(sp(12.5f));
            paint.setColor(today ? Ui.TEAL : Ui.TEXT);
            paint.setTextAlign(Paint.Align.CENTER);
            float cx = x + colW / 2;
            canvas.drawText(label, cx, today ? dp(22) : dp(21), paint);
            if (termStart != null) {
                LocalDate date = termStart.plusDays((week - 1) * 7L + day - 1);
                paint.setTypeface(regular);
                paint.setTextSize(sp(9));
                paint.setColor(today ? Ui.TEAL : Ui.TEXT_FAINT);
                canvas.drawText(TermCalendar.shortMonthDay(date), cx, dp(37), paint);
            }
            paint.setTextAlign(Paint.Align.LEFT);
        }
        canvas.restoreToCount(save);
    }

    private void drawPeriodColumn(Canvas canvas) {
        int save = canvas.save();
        canvas.clipRect(0, headerH, leftW, getHeight());
        canvas.drawRect(0, headerH, leftW, getHeight(), fill(0xFFFFFFFF));
        canvas.translate(0, headerH - scrollY);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int s = 1; s <= slots; s++) {
            paint.setTypeface(medium);
            paint.setTextSize(sp(12));
            paint.setColor(Ui.TEXT_SOFT);
            float baseline = (s - 1) * rowH + rowH / 2f + sp(4);
            canvas.drawText(String.valueOf(s), leftW / 2f, baseline, paint);
            if (rowH >= dp(52)) {
                paint.setTypeface(regular);
                paint.setTextSize(sp(7.5f));
                paint.setColor(Ui.TEXT_FAINT);
                canvas.drawText(PeriodTimes.start(s), leftW / 2f, baseline + sp(10), paint);
            }
        }
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.restoreToCount(save);
    }

    private void drawCorner(Canvas canvas) {
        canvas.drawRect(0, 0, leftW, headerH, fill(0xFFFFFFFF));
        paint.setTypeface(regular);
        paint.setTextSize(sp(8.5f));
        paint.setColor(Ui.TEXT_FAINT);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("周 / 节", leftW / 2f, headerH / 2f + sp(3), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawEdges(Canvas canvas) {
        paint.setColor(Ui.LINE);
        paint.setStrokeWidth(1);
        canvas.drawLine(0, headerH, getWidth(), headerH, paint);
        canvas.drawLine(leftW, 0, leftW, getHeight(), paint);
    }

    private void drawEmptyState(Canvas canvas) {
        float cx = leftW + Math.max(0, getWidth() - leftW) / 2f;
        float cy = headerH + Math.max(0, getHeight() - (int) headerH) / 2f;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(medium);
        paint.setTextSize(sp(16));
        paint.setColor(0xFF51687D);
        canvas.drawText("本周没有排课", cx, cy - dp(6), paint);
        paint.setTypeface(regular);
        paint.setTextSize(sp(11.5f));
        paint.setColor(0xFF92A2B1);
        canvas.drawText("可以切换周次，或连接教务更新课表", cx, cy + dp(18), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawScrollbars(Canvas canvas) {
        int maxX = maxScrollX();
        int maxY = maxScrollY();
        paint.setColor(0x33425A70);
        if (maxX > 0) {
            float track = getWidth() - leftW;
            float thumb = Math.max(dp(36), track * track / contentW);
            float x = leftW + (track - thumb) * scrollX / (float) maxX;
            RectF r = new RectF(x, getHeight() - dp(4), x + thumb, getHeight() - dp(2));
            canvas.drawRoundRect(r, dp(1), dp(1), paint);
        }
        if (maxY > 0) {
            float track = getHeight() - headerH;
            float thumb = Math.max(dp(36), track * track / contentH);
            float y = headerH + (track - thumb) * scrollY / (float) maxY;
            RectF r = new RectF(getWidth() - dp(4), y, getWidth() - dp(2), y + thumb);
            canvas.drawRoundRect(r, dp(1), dp(1), paint);
        }
    }

    private Paint fill(int color) {
        paint.setColor(color);
        return paint;
    }

    private void scrollBy(float dx, float dy) {
        scrollX = clamp((int) (scrollX + dx), 0, maxScrollX());
        scrollY = clamp((int) (scrollY + dy), 0, maxScrollY());
        invalidate();
    }

    private void tap(float x, float y) {
        if (x < leftW || y < headerH) return;
        float cx = x - leftW + scrollX;
        float cy = y - headerH + scrollY;
        for (int i = hitRects.size() - 1; i >= 0; i--) {
            if (hitRects.get(i).contains(cx, cy)) {
                performClick();
                if (listener != null) listener.onCourseClick(hitCourses.get(i));
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (tracker == null) tracker = VelocityTracker.obtain();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) scroller.abortAnimation();
                tracker.clear();
                tracker.addMovement(e);
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                tracker.addMovement(e);
                if (!dragging) {
                    float total = Math.abs(e.getX() - downX) + Math.abs(e.getY() - downY);
                    if (total > touchSlop) dragging = true;
                }
                if (dragging) {
                    scrollBy(lastX - e.getX(), lastY - e.getY());
                    lastX = e.getX();
                    lastY = e.getY();
                }
                return true;
            case MotionEvent.ACTION_UP:
                tracker.addMovement(e);
                if (dragging) {
                    tracker.computeCurrentVelocity(1000);
                    float vx = tracker.getXVelocity();
                    float vy = tracker.getYVelocity();
                    if (Math.abs(vx) > dp(60) || Math.abs(vy) > dp(60)) {
                        scroller.fling(scrollX, scrollY, (int) -vx, (int) -vy,
                                0, maxScrollX(), 0, maxScrollY());
                        invalidate();
                    }
                } else {
                    tap(e.getX(), e.getY());
                }
                recycleTracker();
                return true;
            case MotionEvent.ACTION_CANCEL:
                recycleTracker();
                return true;
            default:
                return true;
        }
    }

    private void recycleTracker() {
        if (tracker != null) {
            tracker.recycle();
            tracker = null;
        }
        dragging = false;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollX = clamp(scroller.getCurrX(), 0, maxScrollX());
            scrollY = clamp(scroller.getCurrY(), 0, maxScrollY());
            invalidate();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
