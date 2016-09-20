package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnPlaybackCompleteListener;

/**
 * Created by david on 9/19/16.
 */
public class PlaybackController implements IPlaybackController, MediaPlayer.OnCompletionListener {

    private final MediaPlayer mediaPlayer;
    private OnPlaybackCompleteListener onPlaybackCompleteListener;

    public PlaybackController(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        this.mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void pause() {
        this.mediaPlayer.pause();
    }

    @Override
    public void seekTo(int pos) {
        this.mediaPlayer.seekTo(pos);
    }

    @Override
    public int getCurrentPosition() {
        return this.mediaPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return this.mediaPlayer.getDuration();
    }

    @Override
    public void start() {
        this.mediaPlayer.start();
    }

    @Override
    public void stop() {
        this.mediaPlayer.stop();
    }

    public void setOnPlaybackCompleteListener(OnPlaybackCompleteListener listener) {
        this.onPlaybackCompleteListener = listener;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (onPlaybackCompleteListener != null)
            onPlaybackCompleteListener.onFileComplete();
    }
}
