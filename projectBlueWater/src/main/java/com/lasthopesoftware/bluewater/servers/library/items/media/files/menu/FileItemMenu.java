package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

/**
 * Created by david on 4/12/15.
 */
public class FileItemMenu {

    private final Context mParentContext;
    private CharSequence mLoadingText;

    private ViewFlipper mViewFlipper;
    private RelativeLayout mTextLayout;
    private TextView mTextView;

    private SimpleTask<Void, Void, String> mGetFileValueTask;

    private View.OnAttachStateChangeListener onAttachStateChangeListener;

    private OnNowPlayingStartListener mNowPlayingStartListener;

    private IFile mFile;

    public FileItemMenu(Context parentContext) {
        mParentContext = parentContext;
    }

    public ViewFlipper getViewMenu(final IFile file) {
        return getViewMenu(file, null);
    }

    public ViewFlipper getViewMenu(final IFile file, OnNowPlayingStartListener onNowPlayingStartListener) {
        setOnNowPlayingStartListener(onNowPlayingStartListener);

        if (mFile == file && mViewFlipper != null) return mViewFlipper;

        mFile = file;

        if (mViewFlipper == null) {

            mViewFlipper = new ViewFlipper(mParentContext);

            mViewFlipper.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final LayoutInflater inflater = (LayoutInflater) mParentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTextLayout = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, mViewFlipper, false);
            mTextView = (TextView) mTextLayout.findViewById(R.id.tvStandard);
            mTextView.setMarqueeRepeatLimit(1);

            mViewFlipper.addView(mTextLayout);
        }

        if (mLoadingText == null)
            mLoadingText = mParentContext.getText(R.string.lbl_loading);

        mTextView.setText(mLoadingText);
        mTextView.setTypeface(null, Typeface.NORMAL);

        if (mGetFileValueTask != null) mGetFileValueTask.cancel(false);
        mGetFileValueTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, String>() {

            @Override
            public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
                return !owner.isCancelled() ? file.getValue() : null;
            }
        });
        mGetFileValueTask.addOnCompleteListener(new ISimpleTask.OnCompleteListener<Void, Void, String>() {

            @Override
            public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
                if (result != null)
                    mTextView.setText(result);
            }
        });
        mGetFileValueTask.execute();

        if (onAttachStateChangeListener != null) mTextLayout.removeOnAttachStateChangeListener(onAttachStateChangeListener);
        onAttachStateChangeListener = new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (mNowPlayingStartListener != null)
                    PlaybackService.removeOnStreamingStartListener(mNowPlayingStartListener);
            }

            @Override
            public void onViewAttachedToWindow(View v) {
                return;
            }
        };

        mTextLayout.addOnAttachStateChangeListener(onAttachStateChangeListener);
        return mViewFlipper;
    }

    private void setOnNowPlayingStartListener(OnNowPlayingStartListener onNowPlayingStartListener) {
        if (mNowPlayingStartListener == onNowPlayingStartListener) return;

        if (mNowPlayingStartListener != null) PlaybackService.removeOnStreamingStartListener(mNowPlayingStartListener);

        mNowPlayingStartListener = onNowPlayingStartListener;

        if (mNowPlayingStartListener != null)
            PlaybackService.addOnStreamingStartListener(mNowPlayingStartListener);
    }

    public TextView getTextView() {
        return mTextView;
    }
}
