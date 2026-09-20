package arman.papoyan.phchecker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CaptureOverlayView extends View {

    public static final float HOLE_WIDTH_RATIO  = 0.2083f;
    public static final float HOLE_HEIGHT_RATIO = 0.0781f;

    private static final float CORNER_RADIUS_DP = 14f;

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final RectF holeRect = new RectF();

    private final float cornerRadiusPx;

    public CaptureOverlayView(Context context) {
        this(context, null);
    }

    public CaptureOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        dimPaint.setColor(0xA6000000);
        dimPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(0xFFFFB74D);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(3f));
        borderPaint.setStrokeCap(Paint.Cap.ROUND);

        cornerRadiusPx = dp(CORNER_RADIUS_DP);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float holeW = w * HOLE_WIDTH_RATIO;
        float holeH = h * HOLE_HEIGHT_RATIO;
        float left = (w - holeW) / 2f;
        float top = (h - holeH) / 2f;
        holeRect.set(left, top, left + holeW, top + holeH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        clipPath.reset();
        clipPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        clipPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        clipPath.addRoundRect(holeRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);
        canvas.drawPath(clipPath, dimPaint);

        canvas.drawRoundRect(holeRect, cornerRadiusPx, cornerRadiusPx, borderPaint);
    }
}