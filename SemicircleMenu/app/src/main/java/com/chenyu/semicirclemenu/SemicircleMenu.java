package com.chenyu.semicirclemenu;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Administrator on 2016/7/29.
 */
public class SemicircleMenu extends ViewGroup {
    /**
     * 当每秒移动角度达到该值时，认为是快速移动
     */
    private static final int FLINGABLE_VALUE = 300;

    /**
     * 如果移动角度达到该值，则屏蔽点击
     */
    private static final int NOCLICK_VALUE = 3;

    /**
     * 当每秒移动角度达到该值时，认为是快速移动
     */
    private int mFlingableValue = FLINGABLE_VALUE;

    /**
     * 判断是否正在自动滚动
     */
    private boolean isFling;

    /**
     * 自动滚动的Runnable
     */
    private AutoFlingRunnable mFlingRunnable;


    //菜单项的文本
    private String[] mItemTexts;

    //菜单项的图标
    private int[] mItemIcons;

    //菜单项的数目
    private int mMenuItemCount;

    private int mRadius;

    //容器的内边距
    private static final float PADDING_LAYOUT = 1/20F;
    private float mPadding;

    //容器内child item的默认尺寸
    private static final float DEFAULT_CHILD_DIMENSION = 1/4f;

    /**
     * 布局时的开始角度
     */
    private double mStartAngle = 90;

    /**
     * 每个Item之间相距的角度
     */
    private float mAngleDelay;

    //上一次触摸的x坐标
    private float mLastX;

    //上一次触摸的y坐标
    private float mLastY;

    /**
     * 检测按下到抬起时旋转的角度
     */
    private float mTmpAngle;
    /**
     * 检测按下到抬起时使用的时间
     */
    private long mDownTime;

    //手指触摸的 flag
    private boolean mTouchFlag = false;

    //位置矫正的 flag
    private boolean mCorrectPositionFlag = false;

    //自动滚动所转过的角度
    private float mAutoFlingAngle = 0;

    private OnMenuItemClickListener mOnMenuItemClickListener;

    /**
     *  点击事件接口
     */
    public interface OnMenuItemClickListener
    {
        void itemClick(View view,int pos);
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener mOnMenuItemClickListener)
    {
        this.mOnMenuItemClickListener = mOnMenuItemClickListener;
    }


    private OnCentralItemCallback mOnCentralItemCallback;

    /**
     *  滚动结束的回调
     */
    public interface OnCentralItemCallback
    {
        void centralItemOperate(int pos);
    }

    public void setOnCentralItemCallback(OnCentralItemCallback mOnCentralItemCallback)
    {
        this.mOnCentralItemCallback = mOnCentralItemCallback;
    }

    public SemicircleMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int resWidth = 0;
        int resHeight = 0;

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if(widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY)
        {
            resWidth = getSuggestedMinimumWidth();
            resWidth = resWidth == 0 ? getDefaultWidth() : resWidth;

            resHeight = getSuggestedMinimumHeight();
            resHeight = resHeight == 0 ? getDefaultWidth() : resHeight;
        }else {
            resWidth = resHeight = Math.min(width,height);
        }

        //我们只需要半圆区域，因此把高度限制为一半
        setMeasuredDimension(resWidth,resHeight /2);

        mRadius = Math.max(getMeasuredWidth(),getMeasuredHeight());

        final  int count = getChildCount();
        int childSize = (int) (mRadius * DEFAULT_CHILD_DIMENSION);
        int childMode = MeasureSpec.EXACTLY;

        for(int i = 0; i < count;i++)
        {
            final  View child = getChildAt(i);
            if(child.getVisibility() == GONE)
            {
                continue;
            }

            int makeMeasureSpec = MeasureSpec.makeMeasureSpec(childSize,childMode);
            child.measure(makeMeasureSpec,makeMeasureSpec);

        }

        mPadding = PADDING_LAYOUT * mRadius;
    }

    private int getDefaultWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return Math.min(outMetrics.widthPixels, outMetrics.heightPixels);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        int layoutRadius = mRadius;

        final  int childCount = getChildCount();

        int left,top;
        int cWidth = (int) (layoutRadius * DEFAULT_CHILD_DIMENSION);

        float tmp = layoutRadius / 2f - cWidth / 2 - mPadding;

        for(int i =0;i<childCount;i++)
        {
            final View child = getChildAt(i);
            if(child.getId() == R.id.id_circle_menu_item_center)
                continue;
            if(child.getVisibility() ==GONE)
                continue;

            mStartAngle %= 360;

            left = (int) (layoutRadius/2 + Math.round(tmp*Math.cos(Math.toRadians(mStartAngle))-1/2f *cWidth));
            top = (int) (Math.round(tmp*Math.sin(Math.toRadians(mStartAngle))-1/2f * cWidth));

            child.layout(left,top,left+cWidth,top+cWidth);
            mStartAngle -= mAngleDelay;
        }

        //布局结束的时候，如果不在滚动同时也不在被触摸的时候，触发滚动结束回调
        if(!isFling && !mTouchFlag )
        {
            mOnCentralItemCallback.centralItemOperate((Integer) findChildViewUnder(layoutRadius/2,tmp).getTag());
            Log.d("cylog", "View Tag :" + findChildViewUnder(layoutRadius / 2, tmp).getTag());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        Log.d("cylog","draw my self");
        super.onDraw(canvas);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        float x = ev.getX();
        float y = ev.getY();


        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                mDownTime = System.currentTimeMillis();
                mTmpAngle = 0;
                mTouchFlag = true;

                //如果按下的时候，正在自动滚动状态，那么取消滚动，并且进行位置矫正
                if(isFling)
                {
                    removeCallbacks(mFlingRunnable);
                    isFling = false;
                    mCorrectPositionFlag = true;
                    post(mFlingRunnable = new AutoFlingRunnable(getCorrectAngle(mAutoFlingAngle % mAngleDelay)));
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float start = getAngle(mLastX,mLastY);
                float end = getAngle(x,y);
                if(getQuadrant(x,y) == 4)
                {
                    mStartAngle += end - start;
                    mTmpAngle += end - start;
                }else{
                    mStartAngle += start -end;
                    mTmpAngle += start -end;
                }
                requestLayout();
                mLastX = x;
                mLastY = y;
                break;

            case MotionEvent.ACTION_UP:
                mTouchFlag = false;
                float anglePerSecond = mTmpAngle * 1000 / (System.currentTimeMillis() - mDownTime);

                //如果角速度超过规定的值，那么认为是快速滚动，开启快速滚动任务
                //否则，直接进行位置矫正
                if(Math.abs(anglePerSecond) >= mFlingableValue && !isFling)
                {
                    mAutoFlingAngle = mTmpAngle;
                    post(mFlingRunnable = new AutoFlingRunnable(anglePerSecond));
                    return true;
                }else if(Math.abs(anglePerSecond) < mFlingableValue)
                {
                    float mDeltaAngle = mTmpAngle % mAngleDelay ;
                    if(mDeltaAngle != 0)
                    {
                        post(mFlingRunnable = new AutoFlingRunnable(getCorrectAngle(mDeltaAngle)));
                        return true;
                    }
                }

                // 如果当前旋转角度超过NOCLICK_VALUE屏蔽点击
                if (Math.abs(mTmpAngle) > NOCLICK_VALUE)
                {
                    return true;
                }

                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 获取当前触摸点相对于圆心的角度
     * @param mTouchX 触摸点x坐标
     * @param mTouchY 触摸点y坐标
     * @return
     */
    private float getAngle(float mTouchX, float mTouchY)
    {
        double x = mTouchX - (mRadius / 2d);
        double y = mTouchY;
        return (float) (Math.asin(y/Math.hypot(x,y))*180/Math.PI);
    }

    private int getQuadrant(float x,float y)
    {
        int tmpX = (int) (x-mRadius/2);
        if(tmpX >= 0)
            return 4;
        else
            return 3;
    }

    /**
     * 获取位置矫正所需的角度
     * @param angle 对mAngleDelay求余后的角度
     * @return
     */
    private float getCorrectAngle(float angle)
    {
        if(angle > 0 && angle <= mAngleDelay/2)
        {
            mCorrectPositionFlag = true;
            return -angle;
        }else if(angle >mAngleDelay/2)
        {
            mCorrectPositionFlag = true;
            return (mAngleDelay -angle);
        }else if(angle < 0 && Math.abs(angle) <= mAngleDelay/2)
        {
            mCorrectPositionFlag = true;
            return -angle;
        }else if(angle < 0 && Math.abs(angle) > mAngleDelay/2){
            mCorrectPositionFlag = true;
            return -(mAngleDelay -Math.abs(angle));
        }
        return 0;
    }

    private class AutoFlingRunnable implements Runnable
    {

        private float angelPerSecond;

        public AutoFlingRunnable(float velocity)
        {
            this.angelPerSecond = velocity;
        }

        public void run()
        {
            if(mCorrectPositionFlag)
            {
                float angle = angelPerSecond;
                Log.d("cylog"," Here is angle :" + angle);
                mStartAngle += angle;
                requestLayout();
                mCorrectPositionFlag = false;
            }else {
                // 如果小于20,则停止，同时进行位置矫正
                if ((int) Math.abs(angelPerSecond) < 20) {
                    isFling = false;
                    mCorrectPositionFlag = true;
                    this.angelPerSecond = getCorrectAngle(mAutoFlingAngle % mAngleDelay);
                    postDelayed(this,30);
                    Log.d("cylog"," mAutoFlingAngle = "+mAutoFlingAngle);
                    return;
                }
                isFling = true;
                // 不断改变mStartAngle，让其滚动，/30为了避免滚动太快
                mStartAngle += (angelPerSecond / 30);
                mAutoFlingAngle += (angelPerSecond / 30);
                // 逐渐减小这个值
                angelPerSecond /= 1.0666F;
                postDelayed(this, 30);
                // 重新布局
                requestLayout();
            }
        }
    }


    /**
     * 获取某个坐标上的子View
     * @param x
     * @param y
     * @return View
     */
    private View findChildViewUnder(float x,float y)
    {
        final int count = getChildCount();
        for(int i = count - 1; i >= 0; i--)
        {
            final View child = getChildAt(i);
            if(x >= child.getLeft() && x <= child.getRight() && y>= child.getTop() && y <= child.getBottom())
                return child;
        }
        return null;
    }

    /**
     *  设置菜单的文本信息
     *
     */
    public void setMenuItemIconsAndTexts(int[] resIds,String[] texts)
    {
        mItemIcons = resIds;
        mItemTexts = texts;

        if(resIds == null && texts == null)
        {
            throw new IllegalArgumentException("菜单文本和图片必须设置其一");
        }

        //初始化mMenuItemCount
        mMenuItemCount = resIds == null ? texts.length : resIds.length;

        if(resIds != null && texts != null)
        {
            mMenuItemCount = Math.min(resIds.length,texts.length);
        }
        mAngleDelay = 360 / mMenuItemCount;
        addMenuItems();
    }

    private void addMenuItems() {
        LayoutInflater mInflater = LayoutInflater.from(getContext());

        /**
         *  初始化item view
         */
        for(int i = 0; i < mMenuItemCount; i++)
        {
            final int j = i;
            View view = mInflater.inflate(R.layout.circle_menu_item,this,false);
            view.setTag(i);
            ImageView iv = (ImageView) view.findViewById(R.id.id_circle_menu_item_image);
            TextView tv = (TextView) view.findViewById(R.id.id_circle_menu_item_text);

            if(iv != null && mItemIcons != null)
            {
                iv.setVisibility(View.VISIBLE);
                iv.setImageResource(mItemIcons[i]);

                //点击事件的处理
                iv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mOnMenuItemClickListener != null)
                        {
                            mOnMenuItemClickListener.itemClick(v,j);
                        }
                    }
                });
            }

            if(tv != null)
            {
                tv.setVisibility(View.VISIBLE);
                tv.setText(mItemTexts[i]);

                //点击事件的处理
                tv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mOnMenuItemClickListener != null)
                        {
                            mOnMenuItemClickListener.itemClick(v,j);
                        }
                    }
                });
            }

            addView(view);
        }
    }
}
