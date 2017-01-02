package com.codefromlab.profilestackview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by Akhilesh on 02-01-2017.
 */

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StackView extends FrameLayout implements View.OnTouchListener {

    public static final String TAG = StackView.class.getSimpleName();

    private static final float SECTION_COUNT = 6.0f;
    private static final int INVALID_POINTER_ID = -1;
    private static final long DURATION_FAST = 100;
    private static final long DURATION_SLOW = 200;

    private CircleImageView mImageView;

    private TextView mLeftTextView;
    private TextView mRightTextView;
    private TextView mTopTextView;
    private TextView mBottomTextView;

    private boolean touchEventStarted;
    private float mItemX;
    private float mItemY;
    private int mItemHeight;
    private int mItemWidth;
    private float mLeftBoundary;
    private float mRightBoundary;
    private float mTopBoundary;
    private float mBottomBoundary;
    private int mItemHalfHeight;
    private int mItemHalfWidth;
    private int mParentHeight;
    private int mParentWidth;
    private StackItemSwipeListener mStackItemSwipeListener;
    private int mActivePointerId = INVALID_POINTER_ID;
    private float mDownTouchX;
    private float mDownTouchY;
    private float mPosX;
    private float mPosY;
    private boolean isAnimationRunning;

    public interface StackItemSwipeListener {

        void onSwipeLeft();

        void onSwipeRight();

        void onSwipeTop();

        void onSwipeBottom();

    }

    private enum ExitStatus {
        LEFT, RIGHT, TOP, BOTTOM, RESET
    }

    public StackView(Context context) {
        super(context);
        init();
    }

    public StackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.stack_view_layout, this);
        mImageView = (CircleImageView) view.findViewById(R.id.image);
        mLeftTextView = (TextView) view.findViewById(R.id.tv_left);
        mRightTextView = (TextView) view.findViewById(R.id.tv_right);
        mTopTextView = (TextView) view.findViewById(R.id.tv_top);
        mBottomTextView = (TextView) view.findViewById(R.id.tv_bottom);

        mLeftTextView.setAlpha(0.0f);
        mRightTextView.setAlpha(0.0f);
        mTopTextView.setAlpha(0.0f);
        mBottomTextView.setAlpha(0.0f);

        mImageView.setOnTouchListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mImageView.setOnTouchListener(null);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        startTrackingTouch();

        switch (MotionEventCompat.getActionMasked(event)) {

            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = event.getPointerId(0);

                mDownTouchX = event.getX(mActivePointerId);
                mDownTouchY = event.getY(mActivePointerId);

                mPosX = view.getX();
                mPosY = view.getY();

                view.getParent().requestDisallowInterceptTouchEvent(true);
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:{
                // Find the index of the active pointer and fetch its position
                final int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                final float xMove = event.getX(pointerIndexMove);
                final float yMove = event.getY(pointerIndexMove);

                //from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved
                final float dx = xMove - mDownTouchX;
                final float dy = yMove - mDownTouchY;


                // Move the frame
                mPosX += dx;
                mPosY += dy;

                mImageView.setX(mPosX);
                mImageView.setY(mPosY);
                resetStackItem();
                updateAlphaOfBadges(mPosX, mPosY);
                break;
            }

            case MotionEvent.ACTION_UP:{
                mActivePointerId = INVALID_POINTER_ID;

                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:{
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;
            }
        }


        return true;
    }

    private void resetStackItem() {
        if (hasCrossedLeftBoundary()) {
            animateAndExit(ExitStatus.LEFT,-mParentWidth,0, DURATION_FAST);
        } else if (hasCrossedRightBoundary()) {
            animateAndExit(ExitStatus.RIGHT,mParentWidth,0, DURATION_FAST);
        } else if (hasCrossedTopBoundary()) {
            animateAndExit(ExitStatus.TOP,0,-mParentHeight, DURATION_FAST);
        } else if (hasCrossedBottomBoundary()) {
            animateAndExit(ExitStatus.BOTTOM, 0, mParentHeight, DURATION_FAST);
        }else{
            animateAndExit(ExitStatus.RESET,mItemX,mItemY,DURATION_SLOW);
        }
    }

    private void animateAndExit(final ExitStatus exitStatus,float exitX,float exitY,long duration) {
        isAnimationRunning = true;

        Animator.AnimatorListener listener=new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                stopTrackingTouch();
                isAnimationRunning = false;
                switch (exitStatus) {
                    case LEFT:
                        if (mStackItemSwipeListener != null) {
                            mStackItemSwipeListener.onSwipeLeft();
                        }
                        break;
                    case RIGHT:
                        if (mStackItemSwipeListener != null) {
                            mStackItemSwipeListener.onSwipeRight();
                        }
                        break;
                    case TOP:
                        if (mStackItemSwipeListener != null) {
                            mStackItemSwipeListener.onSwipeTop();
                        }
                        break;
                    case BOTTOM:
                        if (mStackItemSwipeListener != null) {
                            mStackItemSwipeListener.onSwipeTop();
                        }
                        break;
                    case RESET: {

                        break;
                    }
                }
            }
        };

        if (exitStatus != ExitStatus.RESET) {
            mImageView.animate()
                    .setDuration(duration)
                    .setInterpolator(new AccelerateInterpolator())
                    .x(exitX)
                    .y(exitY)
                    .setListener(listener);
        }else{
            mImageView.animate()
                    .setDuration(duration)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .x(exitX)
                    .y(exitY)
                    .setListener(listener);
        }


    }

    private void animateAndReset() {

    }

    private void startTrackingTouch() {
        if (!touchEventStarted) {
            touchEventStarted = true;

            mItemX = mImageView.getX();
            mItemY = mImageView.getY();
            mItemHeight = mImageView.getHeight();
            mItemWidth = mImageView.getWidth();
            mItemHalfHeight = mItemHeight / 2;
            mItemHalfWidth = mItemWidth / 2;
            mParentHeight = getHeight();
            mParentWidth = getWidth();
            mLeftBoundary = mParentWidth * (1 / SECTION_COUNT);
            mRightBoundary = mParentWidth * ((SECTION_COUNT - 1) / SECTION_COUNT);
            mTopBoundary = mParentHeight * (1 / SECTION_COUNT);
            mBottomBoundary = mParentHeight * (SECTION_COUNT - 1 / SECTION_COUNT);
        }

    }

    private void stopTrackingTouch() {
        if (touchEventStarted) {
            touchEventStarted = false;
            mImageView.setX(mItemX);
            mImageView.setY(mItemY);

            mLeftBoundary = 0.0f;
            mRightBoundary = 0.0f;
            mTopBoundary = 0.0f;
            mBottomBoundary = 0.0f;

            mPosX = 0;
            mPosY = 0;

            mDownTouchY = 0;
            mDownTouchX = 0;

            mLeftTextView.setAlpha(0.0f);
            mRightTextView.setAlpha(0.0f);
            mTopTextView.setAlpha(0.0f);
            mBottomTextView.setAlpha(0.0f);
        }

    }

    private void updateAlphaOfBadges(float posX,float posY) {
        float alphaH = posX / (mParentWidth * 0.50f);
        float alphaV = posX / (mParentHeight * 0.50f);

        mLeftTextView.setAlpha(alphaH);
        mRightTextView.setAlpha(alphaH);
        mTopTextView.setAlpha(alphaV);
        mBottomTextView.setAlpha(alphaV);

    }



    private boolean hasCrossedLeftBoundary() {
        return (mImageView.getX() + mItemHalfWidth) + (mItemWidth / 2) < mLeftBoundary;
    }

    private boolean hasCrossedRightBoundary() {
        return (mImageView.getX() + mItemHalfWidth) + (mItemWidth / 2) > mRightBoundary;
    }

    private boolean hasCrossedTopBoundary() {
        return (mImageView.getY() + mItemHalfHeight) + (mItemHeight / 2) < mTopBoundary;
    }

    private boolean hasCrossedBottomBoundary() {
        return (mImageView.getY() + mItemHalfHeight) + (mItemHeight / 2) > mBottomBoundary;

    }

    public StackItemSwipeListener getStackItemSwipeListener() {
        return mStackItemSwipeListener;
    }

    public void setStackItemSwipeListener(StackItemSwipeListener mStackItemSwipeListener) {
        this.mStackItemSwipeListener = mStackItemSwipeListener;
    }
}
