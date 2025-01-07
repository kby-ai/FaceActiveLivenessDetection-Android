package com.kbyai.faceattribute;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class FaceRectView extends View {

    public enum DispState {
        NO_FACE,
        FACE_DETECTED,
        ROUND_NORMAL,
        ROUND_ZOOM_IN,
        ROUND_ZOOM_OUT
    };

    private Paint paint;
    private static final int DEFAULT_FACE_RECT_THICKNESS = 6;

    private Paint scrimPaint;
    private Paint noFaceScrimPaint;
    private Paint outSideScrimPaint;
    private Paint eraserPaint;
    private Paint boxPaint;
    private int mShader = 0;
    private DispState mMode = DispState.NO_FACE;

    private boolean hasFace = false;

    @ColorInt
    private int boxGradientStartColor;
    @ColorInt
    private int boxGradientEndColor;

    Context mContext;

    public FaceRectView(Context context) {
        this(context, null);

        mContext = context;
        init();
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        paint = new Paint();

        init();
    }

    public void setHasFace(boolean hasFace) {
        this.hasFace = hasFace;
    }

    public void setMode(DispState mode) {
        mMode = mode;
        postInvalidate();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        scrimPaint = new Paint();
        noFaceScrimPaint = new Paint();
        outSideScrimPaint = new Paint();
        // Sets up a gradient background color at vertical.

        eraserPaint = new Paint();
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(1);
        boxPaint.setColor(Color.WHITE);

        boxGradientStartColor = mContext.getColor(R.color.bounding_box_gradient_start);
        boxGradientEndColor = mContext.getColor(R.color.md_theme_light_onPrimary);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(getWidth() > 0 && mShader == 0) {
            mShader = 1;
            scrimPaint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            mContext.getColor(R.color.object_confirmed_bg_gradient_start),
                            mContext.getColor(R.color.object_confirmed_bg_gradient_end),
                            Shader.TileMode.CLAMP));

            noFaceScrimPaint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            mContext.getColor(R.color.bg_gradient_noface_start),
                            mContext.getColor(R.color.bg_gradient_noface_end),
                            Shader.TileMode.CLAMP));
        }

        RectF rect = new RectF();
        if(mMode == DispState.ROUND_NORMAL) {
            int margin = getWidth() / 6;
            int rectHeight = (getWidth() - 2 * margin) * 4 / 3;
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);

            rect = new RectF(margin,  (getHeight() - rectHeight) / 2, getWidth() - margin, (getHeight() - rectHeight) / 2 + rectHeight);
            canvas.drawRoundRect(rect, rect.width() / 2, rect.height() / 2, eraserPaint);
        } else if(mMode == DispState.ROUND_ZOOM_OUT) {     //zoom in
            int margin = getWidth() / 4;
            int rectHeight = (getWidth() - 2 * margin) * 4 / 3;
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);

            rect = new RectF(margin,  (getHeight() - rectHeight) / 2, getWidth() - margin, (getHeight() - rectHeight) / 2 + rectHeight);
            canvas.drawRoundRect(rect, rect.width() / 2, rect.height() / 2, eraserPaint);
        } else if(mMode == DispState.ROUND_ZOOM_IN) {     //zoom out
            int margin = getWidth() / 15;
            int rectHeight = getWidth() * 7 / 5;
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);

            rect = new RectF(margin,  (getHeight() - rectHeight) / 2, getWidth() - margin, (getHeight() - rectHeight) / 2 + rectHeight);
            canvas.drawRoundRect(rect, rect.width() / 2, rect.height() / 2, eraserPaint);
        } else if(mMode == DispState.FACE_DETECTED || mMode == DispState.NO_FACE) {
            int margin = getWidth() / 6;
            int rectHeight = (getWidth() - 2 * margin) * 4 / 3;
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), noFaceScrimPaint);

            rect = new RectF(margin,  (getHeight() - rectHeight) / 2, getWidth() - margin, (getHeight() - rectHeight) / 2 + rectHeight);

            if(hasFace) {
                outSideScrimPaint.setStyle(Paint.Style.STROKE);
                outSideScrimPaint.setStrokeWidth(30);
                outSideScrimPaint.setColor(Color.GREEN);
                outSideScrimPaint.setAntiAlias(true);

                canvas.drawRoundRect(rect, 50, 50, outSideScrimPaint);
            }
            canvas.drawRoundRect(rect, 50, 50, eraserPaint);
        }

        // Draws the bounding box with a gradient border color at vertical.
        if(mMode != DispState.NO_FACE) {
            boxPaint.setShader(
                    new LinearGradient(
                            rect.left,
                            rect.top,
                            rect.left,
                            rect.bottom,
                            boxGradientStartColor,
                            boxGradientEndColor,
                            Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, rect.width() / 2, rect.height() / 2, boxPaint);
        }
    }

}