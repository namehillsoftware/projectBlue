package com.lasthopesoftware.bluewater.shared.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by david on 5/17/15.
 */
public class ScaledWrapImageView extends ImageView {

    private boolean mIsLandscape;
    private final Bitmap mBitmap;

    public ScaledWrapImageView(Context context, Bitmap bitmap) {
        super(context);

        updateIsLandscape();
        mBitmap = bitmap;
    }

    public ScaledWrapImageView(Context context, Bitmap bitmap, AttributeSet attrs) {
        super(context, attrs);

        updateIsLandscape();
        mBitmap = bitmap;
    }

    public ScaledWrapImageView(Context context, Bitmap bitmap, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        updateIsLandscape();
        mBitmap = bitmap;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ScaledWrapImageView(Context context, Bitmap bitmap, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        updateIsLandscape();
        mBitmap = bitmap;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        updateIsLandscape(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    private void updateIsLandscape() {
        updateIsLandscape(getResources().getConfiguration());
    }

    private void updateIsLandscape(Configuration configuration) {
        mIsLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mIsLandscape) {
            final double scaleRatio = (double) width / (double)mBitmap.getWidth();
            height = (int) Math.floor((double) mBitmap.getHeight() * scaleRatio);
        } else {
            final double scaleRatio = (double) height / (double)mBitmap.getHeight();
            width = (int) Math.floor((double) mBitmap.getWidth() * scaleRatio);
        }

        setMeasuredDimension(width, height);
    }
}
