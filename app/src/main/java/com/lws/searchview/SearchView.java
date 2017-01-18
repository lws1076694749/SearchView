package com.lws.searchview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by wenshan.lu on 2017/1/18.
 */

public class SearchView extends View {

    private static final String TAG = "SearchView";

    public enum State {
        NONE, STARTING, SEARCHING, ENDING
    }

    private Paint mPaint;

    private int mViewWidth, mViewHeight;

    // 当前的状态
    private State mCurrentState = State.NONE;

    // 放大镜与外部圆环
    private Path mSearchPath, mCirclePath;

    // 测量Path 并截取部分的工具
    private PathMeasure mMeasure;

    // 默认的动效周期 2s
    private int defaultDuration = 2000;

    // 控制各个过程的动画
    private ValueAnimator mStartingAnimator;
    private ValueAnimator mSearchingAnimator;
    private ValueAnimator mEndingAnimator;

    // 动画数值(用于控制动画状态,因为同一时间内只允许有一种状态出现,具体数值处理取决于当前状态)
    private float mAnimatorValue = 0;

    // 动效过程监听器
    private ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private Animator.AnimatorListener mAnimatorListener;

    // 用于控制动画状态转换
    private Handler mAnimatorHandler;

    // 判断是否已经搜索结束
    private boolean isOver = false;

    // 记录搜索动画执行次数
    private int count = 0;
    // 搜索动画执行的最大次数
    private int maxCount = 5;

    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
        initPath();
        initListener();
        initHandler();
        initAnimator();

        // 进入开始动画
        mCurrentState = State.STARTING;
        this.isOver = false;
        mStartingAnimator.start();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(15);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);
    }

    private void initPath() {
        mSearchPath = new Path();
        mCirclePath = new Path();
        mMeasure = new PathMeasure();

        // 注意,不要到360度,否则内部会自动优化,测量不能取到需要的数值
        // 放大镜圆环
        RectF oval1 = new RectF(-50, -50, 50, 50);
        mSearchPath.addArc(oval1, 45, 359.9f);

        // 外部圆环
        RectF oval2 = new RectF(-100, -100, 100, 100);
        mCirclePath.addArc(oval2, 45, 359.9f);

        float[] pos = new float[2];

        // 放大镜把手的位置
        mMeasure.setPath(mCirclePath, false);
        mMeasure.getPosTan(0, pos, null);

        // 放大镜把手
        mSearchPath.lineTo(pos[0], pos[1]);

        Log.i("TAG", "pos = " + pos[0] + " : " + pos[1]);
    }

    private void initListener() {
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimatorValue = (float) animation.getAnimatedValue();
//                mAnimatorValue = animation.getAnimatedFraction();
                invalidate();
            }
        };

        mAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // 每当一个动画结束时发送消息
                mAnimatorHandler.sendEmptyMessage(0);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };
    }

    private void initAnimator() {
        mStartingAnimator = ValueAnimator.ofFloat(0, 1).setDuration(defaultDuration);
        mSearchingAnimator = ValueAnimator.ofFloat(0, 1).setDuration(defaultDuration);
        mEndingAnimator = ValueAnimator.ofFloat(1, 0).setDuration(defaultDuration);

        mStartingAnimator.addUpdateListener(mUpdateListener);
        mSearchingAnimator.addUpdateListener(mUpdateListener);
        mEndingAnimator.addUpdateListener(mUpdateListener);

        mStartingAnimator.addListener(mAnimatorListener);
        mSearchingAnimator.addListener(mAnimatorListener);
        mEndingAnimator.addListener(mAnimatorListener);
    }

    private void initHandler() {
        mAnimatorHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (mCurrentState) {
                    case STARTING:
//                        Log.i(TAG, "handleMessage: Starting  isOver = " + isOver);
                        // 开始动画结束
                        mStartingAnimator.cancel();
                        if (!isOver) {
                            // 搜索动画开始
                            mSearchingAnimator.start();
                            mCurrentState = State.SEARCHING;
                            count++;
                        } else {
                            // 结束动画开始
                            mEndingAnimator.start();
                            mCurrentState = State.ENDING;
                        }
                        break;
                    case SEARCHING:
//                        Log.i(TAG, "handleMessage: Searching  isOver = " + isOver);
                        if (!isOver && count < maxCount) {
                            // 搜索未结束，继续执行搜索动画
                            mSearchingAnimator.start();
                            count++;
                        } else {
                            // 如果搜索已经结束 则进入结束动画
                            mCurrentState = State.ENDING;
                            mEndingAnimator.start();
                            isOver = true;
                            count = 0;
                        }
                        break;
                    case ENDING:
//                        Log.i(TAG, "handleMessage: Ending  isOver = " + isOver);
                        mCurrentState = State.NONE;
                        break;
                }
            }
        };
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setColor(Color.WHITE);
        canvas.translate(mViewWidth / 2, mViewHeight / 2);
        canvas.drawColor(Color.parseColor("#0082D7"));

        switch (mCurrentState) {
            case NONE:
                canvas.drawPath(mSearchPath, mPaint);
                break;
            case STARTING:
                mMeasure.setPath(mSearchPath, false);
                Path dst = new Path();
                // 终点不变，另一个端点从起点慢慢向终点移动,mAnimatorValue:0 -> 1
                mMeasure.getSegment(mMeasure.getLength() * mAnimatorValue, mMeasure.getLength(), dst, true);
                canvas.drawPath(dst, mPaint);
                break;
            case SEARCHING:
                mMeasure.setPath(mCirclePath, false);
                Path dst2 = new Path();
                // 终点按动画轨迹运动
                float stop = mMeasure.getLength() * mAnimatorValue;
                // 前半部分，起点与终点之间的距离 =（mMeasure.getLength() - 200）* mAnimatorValue
                // 后半部分，起点与终点之间的距离 =（mMeasure.getLength() + 200）* mAnimatorValue - 200
                // 200 这个常量也可以根据 mMeasure.getLength() 的值按比例获取
                float start = (float) (stop - ((0.5 - Math.abs(mAnimatorValue - 0.5)) * 200f));
                mMeasure.getSegment(start, stop, dst2, true);
                canvas.drawPath(dst2, mPaint);
                break;
            case ENDING:
                mMeasure.setPath(mSearchPath, false);
                Path dst3 = new Path();
                // 终点不变，另一点端点从终点慢慢向起点延伸,mAnimatorValue:1 -> 0
                mMeasure.getSegment(mMeasure.getLength() * mAnimatorValue, mMeasure.getLength(), dst3, true);
                canvas.drawPath(dst3, mPaint);
                break;
        }
    }

    public void start() {
        if (mStartingAnimator.isRunning() || mSearchingAnimator.isRunning() || mEndingAnimator.isRunning()) {
            return;
        }
        this.mCurrentState = State.STARTING;
        this.isOver = false;
        mStartingAnimator.start();
        postInvalidate();
    }

    // 控制动画尽快执行结束搜索动画
    public void stop() {
        this.isOver = true;
    }

    public void setMaxCount(int count) {
        this.maxCount = count;
    }
}
