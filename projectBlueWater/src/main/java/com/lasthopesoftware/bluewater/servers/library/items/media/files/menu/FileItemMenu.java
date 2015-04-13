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
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.shared.listener.OnSwipeListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

/**
 * Created by david on 4/12/15.
 */
public class FileItemMenu {

    public FileItemMenu(final CharSequence loadingText, final ViewFlipper viewFlipper, final RelativeLayout textLayout, final TextView textView, final View menuView) {
        this.loadingText = loadingText;
        this.viewFlipper = viewFlipper;
        this.textLayout = textLayout;
        this.textView = textView;
        this.menuView = menuView;
    }

    public final CharSequence loadingText;
    public final ViewFlipper viewFlipper;
    public final RelativeLayout textLayout;
    public final TextView textView;

    public View menuView;
    public SimpleTask<Void, Void, String> getFileValueTask;
    public OnNowPlayingStartListener checkIfIsPlayingFileListener;
    public View.OnAttachStateChangeListener onAttachStateChangeListener;

    public FileItemMenu getFileItemMenu(final IFile file, View convertView, View menuView, final ViewGroup parent) {
        if (convertView == null) {

            final ViewFlipper viewFlipper = new ViewFlipper(parent.getContext());

            viewFlipper.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final OnSwipeListener onSwipeListener = new OnSwipeListener(viewFlipper.getContext());
            onSwipeListener.setOnSwipeRightListener(new OnSwipeListener.OnSwipeRightListener() {

                @Override
                public boolean onSwipeRight(View view) {
                    viewFlipper.showPrevious();
                    return true;
                }
            });
            viewFlipper.setOnTouchListener(onSwipeListener);

            final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final RelativeLayout rl = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, viewFlipper, false);
            final TextView textView = (TextView) rl.findViewById(R.id.tvStandard);
            textView.setMarqueeRepeatLimit(1);

            viewFlipper.addView(rl);

            viewFlipper.addView(menuView);
            viewFlipper.setTag(new FileItemMenu(parent.getContext().getText(R.string.lbl_loading), viewFlipper, rl, textView, menuView));

            convertView = viewFlipper;
        }

        if (((ViewFlipper)convertView).getDisplayedChild() != 0) ((ViewFlipper)convertView).showPrevious();

        final FileItemMenu viewHolder = (FileItemMenu) convertView.getTag();

        viewHolder.textView.setText(viewHolder.loadingText);

        viewHolder.textView.setTypeface(null, Typeface.NORMAL);

        final PlaybackController playlistController = PlaybackService.getPlaylistController();
        if (playlistController != null && playlistController.getCurrentPlaybackFile() != null)
            viewHolder.textView.setTypeface(null, getIsFilePlaying(position, file, playlistController.getPlaylist(), playlistController.getCurrentPlaybackFile().getFile()) ? Typeface.BOLD : Typeface.NORMAL);

        if (viewHolder.getFileValueTask != null) viewHolder.getFileValueTask.cancel(false);
        viewHolder.getFileValueTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, String>() {

            @Override
            public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
                return !owner.isCancelled() ? file.getValue() : null;
            }
        });
        viewHolder.getFileValueTask.addOnCompleteListener(new ISimpleTask.OnCompleteListener<Void, Void, String>() {

            @Override
            public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
                if (result == null) return;

                viewHolder.textView.setText(result);
                onTextViewPopulated(position, file, viewHolder.textView);
            }
        });
        viewHolder.getFileValueTask.execute();

        if (viewHolder.checkIfIsPlayingFileListener != null) PlaybackService.removeOnStreamingStartListener(viewHolder.checkIfIsPlayingFileListener);
        viewHolder.checkIfIsPlayingFileListener = new OnNowPlayingStartListener() {

            @Override
            public void onNowPlayingStart(PlaybackController controller, IPlaybackFile filePlayer) {
                viewHolder.textView.setTypeface(null, getIsFilePlaying(position, file, controller.getPlaylist(), filePlayer.getFile()) ? Typeface.BOLD : Typeface.NORMAL);
            }
        };

        PlaybackService.addOnStreamingStartListener(viewHolder.checkIfIsPlayingFileListener);

        if (viewHolder.onAttachStateChangeListener != null) viewHolder.textLayout.removeOnAttachStateChangeListener(viewHolder.onAttachStateChangeListener);
        viewHolder.onAttachStateChangeListener = new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (viewHolder.checkIfIsPlayingFileListener != null)
                    PlaybackService.removeOnStreamingStartListener(viewHolder.checkIfIsPlayingFileListener);
            }

            @Override
            public void onViewAttachedToWindow(View v) {
                return;
            }
        };

        viewHolder.textLayout.addOnAttachStateChangeListener(viewHolder.onAttachStateChangeListener);

        ((ViewFlipper)convertView).removeView(viewHolder.menuView);
        viewHolder.menuView = menuView;
        ((ViewFlipper)convertView).addView(viewHolder.menuView);

        return viewHolder;
    }
}
