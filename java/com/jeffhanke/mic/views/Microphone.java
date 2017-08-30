package com.jeffhanke.mic.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import com.jeffhanke.mic.R;

/**
 * Created by jeffreyhanke on 8/10/17.
 */

public class Microphone extends View{

    private static final int TOP_SIZE = 180;
    private static final int BASE_SIZE = 120;

    private Rect mRect;
    private RectF mOval;
    private RectF mBaseRect;
    private Paint mTop;
    private Paint mBase;
    private Paint mBorder;
    private Paint mButton;
    private Path mTrap;
    private Bitmap mMesh;
    private BitmapShader mShade;
    private int mRectSize;

    public Microphone(Context context) {
        super(context);
        init(null);
    }

    public Microphone(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Microphone(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Microphone(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet set) {
        mRect = new Rect();
        mOval = new RectF();
        mBaseRect = new RectF();
        mTop = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBase = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButton = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrap = new Path();

        mMesh = BitmapFactory.decodeResource(getResources(), R.drawable.mesh);
        mShade = new BitmapShader(mMesh, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mTop.setShader(mShade);

        if (set == null)
            return;

        TypedArray ta = getContext().obtainStyledAttributes(set, R.styleable.Microphone);

        mRectSize = ta.getDimensionPixelSize(R.styleable.Microphone_rect_size, TOP_SIZE);

        mTrap.setFillType(Path.FillType.EVEN_ODD);

        mBase.setColor(Color.parseColor("#545454"));
        mBorder.setStyle(Paint.Style.STROKE);
        mBorder.setStrokeWidth(5);
        mBorder.setColor((Color.parseColor("#afafaf")));
        mButton.setColor((Color.parseColor("#cbccd8")));

        ta.recycle();
    }

    public void setBaseColor(String col) {
        mBase.setColor(Color.parseColor(col));
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mRect.left = (canvas.getWidth()/2) - mRectSize/2;
        mRect.top = (canvas.getHeight()/2) - mRectSize - 50;
        mRect.right = mRect.left + mRectSize;
        mRect.bottom = mRect.top + mRectSize;

        float cx1, cy1, cx2, cy2;
        float radius1 = mRectSize/2;
        float radius2 = 70;

        cx1 = canvas.getWidth()/2;
        cy1 = mRect.top;
        cx2 = cx1;
        cy2 = canvas.getHeight()/2 + 200;

        mOval.left = mRect.left;
        mOval.top = mRect.bottom - 50;
        mOval.right = mRect.right;
        mOval.bottom = mOval.top + 100;

        mTrap.reset();
        mTrap.moveTo(mRect.left, mRect.bottom);
        mTrap.lineTo(mRect.left + 20, mRect.bottom + 500);
        mTrap.lineTo(mRect.right - 20, mRect.bottom + 500);
        mTrap.lineTo(mRect.right, mRect.bottom);
        mTrap.lineTo(mRect.left, mRect.bottom);
        mTrap.close();

        mBaseRect.left = (canvas.getWidth()/2) - BASE_SIZE/2;
        mBaseRect.top = mRect.bottom + 450;
        mBaseRect.right = mBaseRect.left + BASE_SIZE;
        mBaseRect.bottom = mBaseRect.top + BASE_SIZE + 150;
        int cornerRad = 25;

        canvas.drawRoundRect(mBaseRect, cornerRad, cornerRad, mBase);
        canvas.drawRoundRect(mBaseRect, cornerRad, cornerRad, mBorder);
        canvas.drawPath(mTrap, mBase);
        canvas.drawPath(mTrap, mBorder);
        canvas.drawCircle(cx2, cy2, radius2, mButton);
        canvas.drawOval(mOval, mBorder);
        canvas.drawCircle(cx1, cy1, radius1, mBorder);
        canvas.drawRect(mRect, mBorder);
        canvas.drawRect(mRect, mTop);
        canvas.drawCircle(cx1, cy1, radius1, mTop);
        canvas.drawOval(mOval, mTop);
    }
}