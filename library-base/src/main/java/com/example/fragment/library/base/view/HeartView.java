package com.example.fragment.library.base.view;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnticipateOvershootInterpolator;

public class HeartView extends View {

    private int width;
    private int height;
    private Context context;
    private int heartColor;

    private boolean isStopped;
    private ValueAnimator animatorLeftHeart;
    private ValueAnimator animatorRightHeart;
    private final static String POSITION_X = "positionX";
    private final static String POSITION_Y = "positionY";

    private float leftHeartX;
    private float leftHeartY;
    private float rightHeartX;
    private float rightHeartY;

// #MARK - Constructors

    public HeartView(Context context) {
        super(context);
        init(context, Color.parseColor("#FF7800"));
    }

    public HeartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, Color.parseColor("#FF7800"));
    }

    private void init(Context context, int heartColor) {
        this.context = context;
        this.heartColor = heartColor;
        this.isStopped = true;
    }

// #MARK - User's Methods

    public void start() {
        this.isStopped = false;

        leftHeartX = width / 4 + width / 8;
        leftHeartY = height / 4 + height / 8;

        PropertyValuesHolder widthPropertyHolder = PropertyValuesHolder.ofFloat(POSITION_X, width / 4 + width / 8, width / 2 + width / 8);
        PropertyValuesHolder heightPropertyHolder = PropertyValuesHolder.ofFloat(POSITION_Y, height / 4 + height / 8, height / 2 + height / 8);
        animatorLeftHeart = ValueAnimator.ofPropertyValuesHolder(widthPropertyHolder, heightPropertyHolder);
        animatorLeftHeart.setDuration(1000);
        animatorLeftHeart.setStartDelay(500);
        animatorLeftHeart.setInterpolator(new AnticipateOvershootInterpolator());
        animatorLeftHeart.addUpdateListener(leftHeartAnimationUpdateListener);
        animatorLeftHeart.setRepeatMode(ValueAnimator.REVERSE);
        animatorLeftHeart.setRepeatCount(ValueAnimator.INFINITE);

        widthPropertyHolder = PropertyValuesHolder.ofFloat(POSITION_X, width / 2 + width / 8, width / 4 + width / 8);
        heightPropertyHolder = PropertyValuesHolder.ofFloat(POSITION_Y, height / 4 + height / 8, height / 2 + height / 8);
        animatorRightHeart = ValueAnimator.ofPropertyValuesHolder(widthPropertyHolder, heightPropertyHolder);
        animatorRightHeart.setDuration(1000);
        animatorRightHeart.setInterpolator(new AnticipateOvershootInterpolator());
        animatorRightHeart.addUpdateListener(rightHeartAnimationUpdateListener);
        animatorRightHeart.setRepeatCount(ValueAnimator.INFINITE);
        animatorRightHeart.setRepeatMode(ValueAnimator.REVERSE);

        animatorRightHeart.start();
        animatorLeftHeart.start();

        invalidate();
    }

    public void stop() {
        this.isStopped = true;
        animatorLeftHeart.cancel();
        animatorRightHeart.cancel();
        invalidate();
    }

    public boolean isStopped() {
        return this.isStopped;
    }

    public void setHeartColor(int color) {
        this.heartColor = color;
        invalidate();
    }

// #MARK - Utility Methods

    private int measureWidth(int widthMeasureSpec) {
        int result = 0;

        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);
        int screenWidth = size.x;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
//            result = screenWidth;
            result = 200;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        this.width = result;
        return result;
    }

    private int measureHeight(int heightMeasureSpec) {
        int result = 0;

        int specMode = MeasureSpec.getMode(heightMeasureSpec);
        int specSize = MeasureSpec.getSize(heightMeasureSpec);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenHeight = size.y;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
//            result = screenHeight;
            result = 200;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        this.height = result;
        return result;
    }

    private float measureCircleRadius(int width, int height) {
        float radius = (float) Math.sqrt(Math.pow(width / 2, 2) + Math.pow(height / 2, 2)) / 4;
        return radius + 2;
    }

// #MARK - Listeners Methods

    ValueAnimator.AnimatorUpdateListener leftHeartAnimationUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (!isStopped) {
                leftHeartX = (Float) animation.getAnimatedValue(POSITION_X);
                leftHeartY = (Float) animation.getAnimatedValue(POSITION_Y);
                invalidate();
            }
        }
    };

    ValueAnimator.AnimatorUpdateListener rightHeartAnimationUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (!isStopped) {
                rightHeartX = (Float) animation.getAnimatedValue(POSITION_X);
                rightHeartY = (Float) animation.getAnimatedValue(POSITION_Y);
                invalidate();
            }
        }
    };

// #MARK - Override Methods

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        if (this.width != this.height) {
            return;
        }
        if (!this.isStopped) {
            drawRhombus(canvas);
            drawCircle(canvas, rightHeartX, rightHeartY);
            drawCircle(canvas, leftHeartX, leftHeartY);
        }
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

// #MARK - Drawing Methods

    private void drawRhombus(Canvas canvas) {
        Paint rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(this.heartColor);
        RectF rect = new RectF();
        float sizeOffset = (float) (width * 0.145);
        float xOffset = (float) (width * 0.075);
        float yOffset = (float) (height * 0.075);
        rect.set(width / 4 + xOffset, height / 4 + sizeOffset - yOffset, width - width / 4 - sizeOffset + xOffset, height - height / 4 - yOffset);
        canvas.rotate(-45f, rect.centerX(), rect.centerY());
        canvas.drawRect(rect, rectPaint);
        canvas.rotate(45f, rect.centerX(), rect.centerY());

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(this.heartColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(3);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(width / 2, height / 4);
        path.lineTo(width / 4, height / 2);
        path.moveTo(width / 4, height / 2);
        path.lineTo(width / 2, height - height / 4);
        path.moveTo(width / 2, height - height / 4);
        path.lineTo(width - width / 4, height / 2);
        path.moveTo(width - width / 4, height / 2);
        path.lineTo(width / 2, height / 4);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawCircle(Canvas canvas, float x, float y) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(this.heartColor);
        float circleRadius = measureCircleRadius(this.width, this.height);
        canvas.drawCircle(x, y, circleRadius, paint);
    }

}
