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
    private Bitmap mBitmap;

    public ScaledWrapImageView(Context context) {
        super(context);

        updateIsLandscape();
    }

    public ScaledWrapImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        updateIsLandscape();
    }

    public ScaledWrapImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        updateIsLandscape();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        mBitmap = bm;
        super.setImageBitmap(bm);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ScaledWrapImageView(Context context, Bitmap bitmap, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        updateIsLandscape();
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
        if (mBitmap == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mIsLandscape) {
            final double scaleRatio = (double) width / (double)mBitmap.getWidth();
            height = (int) Math.round((double) mBitmap.getHeight() * scaleRatio);
        } else {
            final double scaleRatio = (double) height / (double)mBitmap.getHeight();
            width = (int) Math.round((double) mBitmap.getWidth() * scaleRatio);
        }

        setMeasuredDimension(width, height);
        setScaleType(ScaleType.FIT_XY);
    }
}
