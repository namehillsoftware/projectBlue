package com.lasthopesoftware.bluewater.shared.android.view;

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

        setScaleType(ScaleType.FIT_XY);

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mIsLandscape) {
            final int newHeight = scaleInteger(mBitmap.getHeight(), (double) width / (double)mBitmap.getWidth());

            if (newHeight > height) {
                width = scaleInteger(width, (double) height / (double) newHeight);
            } else {
                height = newHeight;
            }
        } else {
            final int newWidth = scaleInteger(mBitmap.getWidth(), (double) height / (double)mBitmap.getHeight());

            if (newWidth > width) {
                height = scaleInteger(height, (double) width / (double) newWidth );
            } else {
                width = newWidth;
            }
        }

        setMeasuredDimension(width, height);
    }

    private static int scaleInteger(int srcInt, double scaleRatio) {
        return (int) Math.round((double) srcInt * scaleRatio);
    }
}
