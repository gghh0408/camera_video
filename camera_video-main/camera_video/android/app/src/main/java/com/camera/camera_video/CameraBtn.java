package com.camera.camera_video;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.Nullable;

public class CameraBtn extends View {
    private float mMaxNum = 60; //最大值
    private int mInColor = 0; //内圈颜色
    private int mMiddleColor = 0; //内圈颜色

    private int mBigCircleSize; //外圈大小 单位sp
    private int mSmallCircleSize; //内圈大小 单位sp
    private int mPressSize;
    private int mStartAngle; //开始角度
    private int mDrawAngle; //需要绘制的角度
    private float mCurrentAngle = 0; //当前角度

    private int mWidth; //宽

    //画笔
    private Paint circlePaint;
    private Paint progressPaint;

    public CameraBtn(Context context) {
        this(context, null, 0);

    }

    public CameraBtn(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraBtn(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStartAngle = -90;
        mDrawAngle = 360;
        mInColor = Color.WHITE;
        mMiddleColor = Color.parseColor("#4877FA");
        mBigCircleSize = dp2px(64);
        mSmallCircleSize = dp2px(54);
        mPressSize = dp2px(32);
        mWidth = dp2px(64);
        circlePaint = new Paint();
        progressPaint = new Paint();
        setCirclePaint();
        setProgressPaint();
    }

    /**
     * 内圆弧画笔
     */
    private void setCirclePaint() {
        circlePaint.setColor(mInColor);
        circlePaint.setAntiAlias(true);
        circlePaint.setStrokeWidth(mSmallCircleSize); //大小
    }

    /**
     * 外圆弧画笔
     */
    private void setProgressPaint() {
        progressPaint.setColor(mMiddleColor);
        progressPaint.setAntiAlias(true);
        progressPaint.setStrokeWidth(10); //大小
        progressPaint.setStyle(Paint.Style.STROKE); //空心样式
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawInCircle(canvas);
        drawMiddleCircle(canvas);
        drawOutCircle(canvas);
    }

    //内圆弧
    private void drawInCircle(Canvas canvas) {
        int r = mSmallCircleSize / 2; //圆弧的一半
        if (mCurrentAngle > 0) {
            r = mPressSize / 2;
        }
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(mWidth / 2, mWidth / 2, r, circlePaint);
    }

    //進度圆弧
    private void drawMiddleCircle(Canvas canvas) {
        if (mCurrentAngle == 0) {
            return;
        }
        int r = mBigCircleSize / 2; //圆弧的一半
        RectF rectF = new RectF(0, 0, mWidth, mWidth);
        if (mCurrentAngle > mDrawAngle) {
            mCurrentAngle = mDrawAngle;
        }
        circlePaint.setStrokeWidth(10);
        circlePaint.setColor(Color.parseColor("#D8D8D8"));
        circlePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mWidth / 2, mWidth / 2, r, circlePaint);

        canvas.drawArc(rectF, mStartAngle, mCurrentAngle, false, progressPaint);
    }

    //外圆弧
    private void drawOutCircle(Canvas canvas) {
        if (mCurrentAngle > 0) {
            return;
        }
        int r = mBigCircleSize / 2; //圆弧的一半
        circlePaint.setStrokeWidth(2);
        circlePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mWidth / 2, mWidth / 2, r, circlePaint);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    public void setCurrentNum(float currentNum) {
        mCurrentAngle = currentNum * mDrawAngle / mMaxNum;
        invalidate();
    }
}
