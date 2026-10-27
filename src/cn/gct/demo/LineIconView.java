package cn.gct.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Tiny hand-drawn line icons; no icon font or vector asset dependency. */
public final class LineIconView extends View {
    public static final int HOME = 1;
    public static final int CALENDAR = 2;
    public static final int CUP = 3;
    public static final int GEAR = 4;
    public static final int DOC = 5;
    public static final int PERSON = 6;
    public static final int BUILDING = 7;
    public static final int CROSS = 8;
    public static final int BOOK = 9;
    public static final int QR = 10;
    public static final int DROP = 11;
    public static final int SEARCH = 12;
    public static final int INFO = 13;
    public static final int MENU = 14;
    public static final int ARROW = 15;
    public static final int CHEVRON = 16;
    public static final int CLOCK_HOURS = 17;
    public static final int WALLET = 18;
    public static final int CHEVRON_RIGHT = 19;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int kind;
    private int color;

    public LineIconView(Context c, int kind, int color) {
        super(c);
        this.kind = kind;
        this.color = color;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(1.8f * getResources().getDisplayMetrics().density);
    }

    public void setKind(int kind) {
        this.kind = kind;
        invalidate();
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int fallback = Math.round(24 * getResources().getDisplayMetrics().density);
        int w = resolveSize(fallback, widthMeasureSpec);
        int h = resolveSize(fallback, heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth(), getHeight()) / 24f;
        canvas.save();
        canvas.translate((getWidth() - 24 * scale) / 2f, (getHeight() - 24 * scale) / 2f);
        canvas.scale(scale, scale);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.8f / scale);
        path.reset();
        switch (kind) {
            case HOME:
                path.moveTo(4, 10);
                path.lineTo(12, 3);
                path.lineTo(20, 10);
                path.moveTo(6, 9.5f);
                path.lineTo(6, 20);
                path.lineTo(18, 20);
                path.lineTo(18, 9.5f);
                path.moveTo(10, 20);
                path.lineTo(10, 15);
                path.lineTo(14, 15);
                path.lineTo(14, 20);
                canvas.drawPath(path, paint);
                break;
            case CALENDAR:
                RectF cal = new RectF(4, 5, 20, 21);
                canvas.drawRoundRect(cal, 4, 4, paint);
                canvas.drawLine(4, 9, 20, 9, paint);
                canvas.drawLine(8, 3, 8, 7, paint);
                canvas.drawLine(16, 3, 16, 7, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(8, 13, 1.1f, paint);
                canvas.drawCircle(12, 13, 1.1f, paint);
                canvas.drawCircle(16, 13, 1.1f, paint);
                canvas.drawCircle(8, 17, 1.1f, paint);
                canvas.drawCircle(12, 17, 1.1f, paint);
                break;
            case CUP:
                path.moveTo(6, 8);
                path.lineTo(18, 8);
                path.lineTo(16.5f, 20);
                path.lineTo(7.5f, 20);
                path.close();
                canvas.drawPath(path, paint);
                RectF handle = new RectF(17, 10, 22, 16);
                canvas.drawArc(handle, -80, 180, false, paint);
                canvas.drawLine(9, 5, 9, 3, paint);
                canvas.drawLine(12.5f, 5, 12.5f, 3, paint);
                canvas.drawLine(16, 5, 16, 3, paint);
                break;
            case GEAR:
                canvas.drawCircle(12, 12, 7.5f, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(12, 12, 2.2f, paint);
                break;
            case DOC:
                path.moveTo(6, 3);
                path.lineTo(14, 3);
                path.lineTo(18, 7);
                path.lineTo(18, 21);
                path.lineTo(6, 21);
                path.close();
                canvas.drawPath(path, paint);
                canvas.drawLine(14, 3, 14, 7, paint);
                canvas.drawLine(14, 7, 18, 7, paint);
                canvas.drawLine(9, 11, 15, 11, paint);
                canvas.drawLine(9, 14, 15, 14, paint);
                canvas.drawLine(9, 17, 13, 17, paint);
                break;
            case PERSON:
                canvas.drawCircle(12, 8, 3.5f, paint);
                path.moveTo(5, 21);
                path.cubicTo(5, 15, 8, 13, 12, 13);
                path.cubicTo(16, 13, 19, 15, 19, 21);
                canvas.drawPath(path, paint);
                break;
            case BUILDING:
                canvas.drawRoundRect(new RectF(5, 4, 19, 21), 2, 2, paint);
                canvas.drawLine(10, 21, 10, 15, paint);
                canvas.drawLine(10, 15, 14, 15, paint);
                canvas.drawLine(14, 15, 14, 21, paint);
                for (int y : new int[]{8, 12}) {
                    for (int x : new int[]{8, 12, 16}) {
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(x, y, 1, paint);
                        paint.setStyle(Paint.Style.STROKE);
                    }
                }
                break;
            case CROSS:
                canvas.drawCircle(12, 12, 8, paint);
                canvas.drawLine(12, 7, 12, 17, paint);
                canvas.drawLine(7, 12, 17, 12, paint);
                break;
            case BOOK:
                path.moveTo(4, 6);
                path.cubicTo(8, 4, 11, 4, 12, 6);
                path.cubicTo(13, 4, 16, 4, 20, 6);
                path.lineTo(20, 19);
                path.cubicTo(16, 17, 13, 17, 12, 19);
                path.cubicTo(11, 17, 8, 17, 4, 19);
                path.close();
                canvas.drawPath(path, paint);
                canvas.drawLine(12, 6, 12, 19, paint);
                break;
            case QR:
                canvas.drawRect(4, 4, 9, 9, paint);
                canvas.drawRect(15, 4, 20, 9, paint);
                canvas.drawRect(4, 15, 9, 20, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(6.5f, 6.5f, 8.5f, 8.5f, paint);
                canvas.drawRect(17.5f, 6.5f, 19.5f, 8.5f, paint);
                canvas.drawRect(6.5f, 17.5f, 8.5f, 19.5f, paint);
                canvas.drawCircle(13, 12, 1, paint);
                canvas.drawCircle(17, 17, 1, paint);
                canvas.drawCircle(13, 17, 1, paint);
                break;
            case DROP:
                path.moveTo(12, 3);
                path.cubicTo(12, 3, 6, 10, 6, 14);
                path.cubicTo(6, 18, 9, 21, 12, 21);
                path.cubicTo(15, 21, 18, 18, 18, 14);
                path.cubicTo(18, 10, 12, 3, 12, 3);
                path.close();
                canvas.drawPath(path, paint);
                break;
            case SEARCH:
                canvas.drawCircle(11, 11, 6, paint);
                canvas.drawLine(15.5f, 15.5f, 21, 21, paint);
                break;
            case INFO:
                canvas.drawCircle(12, 12, 9, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(12, 7.5f, 1.2f, paint);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(12, 11, 12, 17, paint);
                break;
            case MENU:
                canvas.drawLine(5, 7, 19, 7, paint);
                canvas.drawLine(5, 12, 19, 12, paint);
                canvas.drawLine(5, 17, 19, 17, paint);
                break;
            case ARROW:
                canvas.drawLine(5, 12, 19, 12, paint);
                canvas.drawLine(14, 7, 19, 12, paint);
                canvas.drawLine(14, 17, 19, 12, paint);
                break;
            case CHEVRON:
                canvas.drawLine(14, 6, 8, 12, paint);
                canvas.drawLine(8, 12, 14, 18, paint);
                break;
            case CHEVRON_RIGHT:
                canvas.drawLine(10, 6, 16, 12, paint);
                canvas.drawLine(16, 12, 10, 18, paint);
                break;
            case CLOCK_HOURS:
                canvas.drawCircle(12, 12, 8.5f, paint);
                canvas.drawLine(12, 7, 12, 12, paint);
                canvas.drawLine(12, 12, 16, 14, paint);
                break;
            case WALLET:
                canvas.drawRoundRect(new RectF(3, 6, 21, 20), 3, 3, paint);
                canvas.drawLine(3, 10, 21, 10, paint);
                canvas.drawRoundRect(new RectF(15, 12.5f, 21.5f, 16.5f), 1.5f, 1.5f, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(18, 14.5f, 0.9f, paint);
                break;
            default:
                break;
        }
        canvas.restore();
    }
}
