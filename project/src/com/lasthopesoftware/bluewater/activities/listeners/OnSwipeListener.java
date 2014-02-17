package com.lasthopesoftware.bluewater.activities.listeners;

import com.j256.ormlite.logger.LoggerFactory;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnSwipeListener implements OnTouchListener {

	private GestureDetector mGestureDetector;
	private Context mContext;
	private View mActiveView;
	
	private OnSwipeUpListener mOnSwipeUpListener;
	private OnSwipeDownListener mOnSwipeDownListener;
	private OnSwipeLeftListener mOnSwipeLeftListener;
	private OnSwipeRightListener mOnSwipeRightListener;
	
	public OnSwipeListener(Context context) {
		mContext = context;
		mGestureDetector = new GestureDetector(mContext, new GestureListener());
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		mActiveView = view;
		return mGestureDetector.onTouchEvent(event);
	}
	
	public void setOnSwipeUpListener(OnSwipeUpListener listener) {
		mOnSwipeUpListener = listener;
	}
	
	public void setOnSwipeDownListener(OnSwipeDownListener listener) {
		mOnSwipeDownListener = listener;
	}
	
	public void setOnSwipeLeftListener(OnSwipeLeftListener listener) {
		mOnSwipeLeftListener = listener;
	}
	
	public void setOnSwipeRightListener(OnSwipeRightListener listener) {
		mOnSwipeRightListener = listener;
	}
	
	private final class GestureListener extends SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            if (mOnSwipeRightListener == null) return false;
                            return mOnSwipeRightListener.onSwipeRight(mActiveView);
                        }
                        
                        if (mOnSwipeLeftListener == null) return false;
                        return mOnSwipeLeftListener.onSwipeLeft(mActiveView);
                    }
                }
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                    	if (mOnSwipeDownListener == null) return false;
                        return mOnSwipeDownListener.onSwipeDown(mActiveView);
                    }
                    if (mOnSwipeUpListener == null) return false;
                    return mOnSwipeUpListener.onSwipeUp(mActiveView);
                }
            } catch (Exception exception) {
                LoggerFactory.getLogger(GestureListener.class).error(exception.getMessage(), exception);
            }
            
            return false;
        }
    }
	
	public interface OnSwipeUpListener {
		boolean onSwipeUp(View view);
	}
	
	public interface OnSwipeDownListener {
		boolean onSwipeDown(View view);
	}
	
	public interface OnSwipeLeftListener {
		boolean onSwipeLeft(View view);
	}
	
	public interface OnSwipeRightListener {
		boolean onSwipeRight(View view);
	}
}
