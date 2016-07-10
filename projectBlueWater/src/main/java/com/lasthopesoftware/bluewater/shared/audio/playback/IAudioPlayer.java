package com.lasthopesoftware.bluewater.shared.audio.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFilePreparedListener;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 5/8/16.
 */
public interface IAudioPlayer {
	void initialize();
	boolean isCreated();
	boolean isPrepared();
	void prepare();
	void release();
	int getCurrentPosition();
	boolean isBuffered();
	boolean isPlaying();
	void pause();
	void seekTo(int pos);
	void start();
	void stop();
	float getVolume();
	void setVolume();

	/* Listener methods */
	void setOnFileCompleteListener(OneParameterRunnable<IPlaybackFile> listener);
	void setOnFilePreparedListener(OnFilePreparedListener listener);
	void setOnFileErrorListener(OnFileErrorListener listener);
	void setOnFileBufferedListener(OnFileBufferedListener listener);
}
