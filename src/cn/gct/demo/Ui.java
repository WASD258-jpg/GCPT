package cn.gct.demo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Platform-only visual system for the morning-mist glass direction (demo C). */
public final class Ui {
    public static final int BG_TOP = 0xFFF3F0E7;
    public static final int BG_MID = 0xFFE7EDE8;
    public static final int BG_BOTTOM = 0xFFDCE8ED;
    public static final int BG = BG_TOP;
    public static final int TEXT = 0xFF233B38;
    public static final int TEXT_SOFT = 0xFF566C66;
    public static final int TEXT_FAINT = 0xFF7D9088;
    public static final int ACCENT = 0xFF276757;
    public static final int ACCENT_SOFT = 0xFFE1EFE9;
    public static final int GLASS = 0xB8FFFFFF;
    public static final int GLASS_STRONG = 0xD9FFFFFF;
    public static final int CARD = 0xFFFFFFFF;
    public static final int CARD_BORDER = 0xD9FFFFFF;
    public static final int LINE = 0x1F233B38;
    public static final int LINE_SOFT = 0x12233B38;
    public static final int DANGER = 0xFFB5483E;
    public static final int WARN_BG = 0xFFF7EEDC;
    public static final int WARN_TEXT = 0xFF855A20;
    public static final int INFO_BG = 0xFFE8F0EB;

    // Backwards-compatible aliases used by older screens.
    public static final int NAVY = TEXT;
    public static final int NAVY_DEEP = BG_TOP;
    public static final int NAVY_SOFT = ACCENT;
    public static final int TEAL = ACCENT;
    public static final int TEAL_SOFT = ACCENT_SOFT;

    private Ui() {}

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static float dpf(Context c, float value) {
        return value * c.getResources().getDisplayMetrics().density;
    }

    public static boolean wide(Context c) {
        return c.getResources().getConfiguration().screenWidthDp
                > c.getResources().getConfiguration().screenHeightDp;
    }

    public static GradientDrawable background(Context c) {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{BG_TOP, BG_MID, BG_BOTTOM});
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dpf(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundStroke(int color, float radiusDp, int strokeColor, float strokeDp, Context c) {
        GradientDrawable d = round(color, radiusDp, c);
        d.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return d;
    }

    public static GradientDrawable gradient(int startColor, int endColor, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor});
        d.setCornerRadius(dpf(c, radiusDp));
        return d;
    }

    public static Drawable ripple(int bgColor, float radiusDp, int rippleColor, Context c) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor),
                round(bgColor, radiusDp, c), null);
    }

    public static Drawable rippleDrawable(Drawable content, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    public static TextView text(Context c, CharSequence value, float sizeSp, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView medium(Context c, CharSequence value, float sizeSp, int color) {
        TextView t = text(c, value, sizeSp, color);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static TextView bold(Context c, CharSequence value, float sizeSp, int color) {
        TextView t = text(c, value, sizeSp, color);
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return t;
    }

    /** Editorial serif used by the new hero lines and the brand. */
    public static TextView heading(Context c, CharSequence value, float sizeSp, int color) {
        TextView t = text(c, value, sizeSp, color);
        t.setTypeface(Typeface.create("serif", Typeface.BOLD));
        t.setLineSpacing(dpf(c, 4), 1f);
        return t;
    }

    public static TextView serif(Context c, CharSequence value, float sizeSp, int color) {
        TextView t = text(c, value, sizeSp, color);
        t.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        return t;
    }

    public static void letterSpacing(TextView t, float value) {
        t.setLetterSpacing(value);
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        return glassCard(c, 24, 18, 16);
    }

    public static LinearLayout glassCard(Context c, float radiusDp, float padH, float padV) {
        LinearLayout l = column(c);
        GradientDrawable bg = roundStroke(GLASS, radiusDp, CARD_BORDER, 1, c);
        l.setBackground(bg);
        l.setElevation(dpf(c, 6));
        int ph = dp(c, padH);
        l.setPadding(ph, dp(c, padV), ph, dp(c, padV));
        return l;
    }

    public static LinearLayout tintedCard(Context c, int color, int radiusDp) {
        LinearLayout l = column(c);
        l.setBackground(round(color, radiusDp, c));
        int p = dp(c, 14);
        l.setPadding(p, dp(c, 12), p, dp(c, 12));
        return l;
    }

    /** Primary action: deep green pill; secondary: translucent white pill. */
    public static TextView action(Context c, String label, boolean primary) {
        TextView t = medium(c, label, 12.5f, primary ? Color.WHITE : TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        t.setMinHeight(0);
        t.setMinWidth(0);
        t.setBackground(rippleDrawable(
                primary ? round(ACCENT, 16, c)
                        : roundStroke(0xE6FFFFFF, 16, CARD_BORDER, 1, c),
                0x1A276757));
        t.setClickable(true);
        t.setFocusable(true);
        t.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(c, 44)));
        return t;
    }

    public static TextView actionWide(Context c, String label, boolean primary) {
        TextView t = action(c, label, primary);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, dp(c, 44), 1));
        return t;
    }

    public static TextView ghost(Context c, String label, float sizeSp) {
        TextView t = medium(c, label, sizeSp, ACCENT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 6), dp(c, 6), dp(c, 6), dp(c, 6));
        t.setBackground(ripple(0x00000000, 12, 0x14276757, c));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static TextView iconAction(Context c, String glyph) {
        TextView t = medium(c, glyph, 20, TEXT_SOFT);
        t.setGravity(Gravity.CENTER);
        t.setBackground(ripple(0x40FFFFFF, 18, 0x14276757, c));
        t.setClickable(true);
        t.setFocusable(true);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 36), dp(c, 36)));
        return t;
    }

    public static TextView chip(Context c, String label, boolean selected) {
        TextView t = medium(c, label, 11.5f, selected ? ACCENT : TEXT_SOFT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        t.setBackground(selected
                ? round(ACCENT_SOFT, 12, c)
                : roundStroke(0x66FFFFFF, 12, CARD_BORDER, 1, c));
        return t;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    public static View gap(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    public static View flexibleGap(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        return v;
    }

    public static LinearLayout.LayoutParams margin(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }

    public static void setMargins(View v, Context c, float left, float top, float right, float bottom) {
        ViewGroupParams.ensure(v);
        android.view.ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) raw).setMargins(dp(c, left), dp(c, top), dp(c, right), dp(c, bottom));
            v.setLayoutParams(raw);
        }
    }

    public static void weight(View v, float weight) {
        ViewGroupParams.ensure(v);
        android.view.ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) raw).weight = weight;
            ((LinearLayout.LayoutParams) raw).width = 0;
            v.setLayoutParams(raw);
        }
    }

    public static void size(View v, Context c, float widthDp, float heightDp) {
        ViewGroupParams.ensure(v);
        android.view.ViewGroup.LayoutParams raw = v.getLayoutParams();
        raw.width = widthDp < 0 ? -1 : dp(c, widthDp);
        raw.height = heightDp < 0 ? -2 : dp(c, heightDp);
        v.setLayoutParams(raw);
    }

    public static void pad(View v, Context c, float l, float t, float r, float b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    private static final class ViewGroupParams {
        static void ensure(View v) {
            if (v.getLayoutParams() == null) {
                v.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }
    }
}
