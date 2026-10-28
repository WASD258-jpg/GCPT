package cn.gcpt.app;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Course details in a rounded card dialog; the raw school text stays available for verification. */
public final class CourseDetailDialog {
    private CourseDetailDialog() {}

    public static void show(Context c, CourseParser.Course course, boolean conflict) {
        final Dialog dialog = new Dialog(c);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        int screenW = c.getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(screenW - Ui.dp(c, 40), Ui.dp(c, 560));

        ScrollView root = new ScrollView(c);
        root.setBackground(Ui.round(Ui.CARD, 8, c));
        root.setClipToOutline(true);
        root.setFillViewport(true);
        root.setVerticalScrollBarEnabled(false);

        LinearLayout content = Ui.column(c);
        root.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View accent = new View(c);
        accent.setBackgroundColor(WeekGrid.accentColor(course.name));
        content.addView(accent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 4)));

        LinearLayout header = Ui.column(c);
        header.setBackgroundColor(Color.TRANSPARENT);
        header.setPadding(Ui.dp(c, 20), Ui.dp(c, 18), Ui.dp(c, 20), Ui.dp(c, 12));
        content.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleRow = Ui.row(c);
        header.addView(titleRow);

        LinearLayout titleBlock = Ui.column(c);
        titleRow.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout meta = Ui.row(c);
        String timeRange = PeriodTimes.range(course.start, course.end);
        boolean examClock = course.timeText != null && !course.timeText.isEmpty();
        String whenText = examClock
                ? "考试 " + course.timeText
                : "第 " + course.start + "–" + course.end + " 节"
                        + (timeRange.isEmpty() ? "" : " · " + timeRange);
        TextView metaText = Ui.medium(c, TermCalendar.DAYS[Math.max(0, Math.min(6, course.day - 1))]
                + " · " + whenText, 11.5f,
                WeekGrid.accentColor(course.name));
        meta.addView(metaText);
        if (course.adjusted) {
            TextView adj = Ui.medium(c, "调课", 9.5f, Color.WHITE);
            adj.setPadding(Ui.dp(c, 7), Ui.dp(c, 2), Ui.dp(c, 7), Ui.dp(c, 2));
            adj.setBackground(Ui.round(WeekGrid.accentColor(course.name), 4, c));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Ui.dp(c, 8), 0, 0, 0);
            meta.addView(adj, lp);
        }
        if (conflict) {
            TextView clash = Ui.medium(c, "时段重叠", 9.5f, Ui.DANGER);
            clash.setPadding(Ui.dp(c, 7), Ui.dp(c, 2), Ui.dp(c, 7), Ui.dp(c, 2));
            clash.setBackground(Ui.round(0xFFFFE9E7, 4, c));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Ui.dp(c, 8), 0, 0, 0);
            meta.addView(clash, lp);
        }
        titleBlock.addView(meta);

        String displayName = course.name.replace("【考试】", "")
                .replace("【调】", "").replace("[调]", "").trim();
        if (displayName.isEmpty()) displayName = course.name;
        TextView title = Ui.bold(c, displayName, 19, Ui.TEXT);
        title.setLineSpacing(Ui.dp(c, 2), 1f);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = Ui.dp(c, 7);
        titleBlock.addView(title, titleLp);

        TextView weeks = Ui.text(c, "教学周 " + course.weeksText, 12, 0xFF405A70);
        LinearLayout.LayoutParams weeksLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weeksLp.topMargin = Ui.dp(c, 6);
        titleBlock.addView(weeks, weeksLp);

        TextView close = Ui.medium(c, "×", 22, 0xFF49637A);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.ripple(0x00000000, 18, 0x2214395C, c));
        close.setClickable(true);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(Ui.dp(c, 38), Ui.dp(c, 38));
        closeLp.setMargins(Ui.dp(c, 8), 0, 0, 0);
        titleRow.addView(close, closeLp);

        LinearLayout body = Ui.column(c);
        body.setPadding(Ui.dp(c, 22), Ui.dp(c, 8), Ui.dp(c, 22), Ui.dp(c, 8));
        content.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        infoRow(c, body, "地点", course.room == null || course.room.isEmpty() ? "地点待确认" : course.room);
        if (examClock) {
            infoRow(c, body, "考试时间", course.timeText);
        } else if (!timeRange.isEmpty()) {
            infoRow(c, body, "时间", timeRange);
        }
        if (course.seat != null && !course.seat.isEmpty()) infoRow(c, body, "座位", course.seat);
        if (course.teacher != null && !course.teacher.isEmpty()) infoRow(c, body, "教师", course.teacher);
        if (course.group != null && !course.group.isEmpty()) infoRow(c, body, "班级", course.group);
        if (course.hours != null && !course.hours.isEmpty()) infoRow(c, body, "学时", course.hours);

        TextView rawTitle = Ui.medium(c, "学校原文", 11, Ui.TEXT_FAINT);
        LinearLayout.LayoutParams rawTitleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rawTitleLp.topMargin = Ui.dp(c, 14);
        rawTitleLp.bottomMargin = Ui.dp(c, 6);
        body.addView(rawTitle, rawTitleLp);

        LinearLayout rawBox = Ui.column(c);
        rawBox.setBackground(Ui.round(0xFFF6F8FB, 12, c));
        rawBox.setPadding(Ui.dp(c, 13), Ui.dp(c, 11), Ui.dp(c, 13), Ui.dp(c, 11));
        body.addView(rawBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        String raw = course.raw == null ? "" : course.raw.replaceAll("\\n{3,}", "\n\n").trim();
        TextView rawText = Ui.text(c, raw, 11.5f, 0xFF4B6073);
        rawText.setLineSpacing(Ui.dp(c, 2), 1f);
        rawBox.addView(rawText);

        View divider = Ui.divider(c);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dividerLp.topMargin = Ui.dp(c, 10);
        content.addView(divider, dividerLp);

        LinearLayout footer = Ui.row(c);
        footer.setPadding(Ui.dp(c, 18), Ui.dp(c, 10), Ui.dp(c, 18), Ui.dp(c, 14));
        content.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView copy = Ui.action(c, "复制原文", false);
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("课程原文", course.raw == null ? "" : course.raw));
            Toast.makeText(c, "学校原文已复制", Toast.LENGTH_SHORT).show();
        });
        footer.addView(copy);

        TextView done = Ui.action(c, "关闭", true);
        done.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams doneLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        doneLp.setMargins(Ui.dp(c, 10), 0, 0, 0);
        footer.addView(done, doneLp);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.48f);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.CENTER);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w == null) return;
            int maxH = (int) (c.getResources().getDisplayMetrics().heightPixels * 0.88);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            w.setLayout(width, Math.min(root.getMeasuredHeight(), maxH));
        });
        dialog.show();
        root.setAlpha(0f);
        root.setScaleX(0.96f);
        root.setScaleY(0.96f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300)
                .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f)).start();
    }

    private static void infoRow(Context c, LinearLayout parent, String label, String value) {
        LinearLayout row = Ui.row(c);
        row.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView l = Ui.text(c, label, 11.5f, Ui.TEXT_FAINT);
        row.addView(l, new LinearLayout.LayoutParams(Ui.dp(c, 46), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView v = Ui.medium(c, value, 13, Ui.TEXT);
        v.setLineSpacing(Ui.dp(c, 1), 1f);
        row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    }
}
