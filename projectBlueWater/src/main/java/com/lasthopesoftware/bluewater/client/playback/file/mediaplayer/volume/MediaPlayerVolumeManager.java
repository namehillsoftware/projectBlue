package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.volume;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;


public class MediaPlayerVolumeManager implements ManagePlayableFileVolume {

	private final MediaPlayer mediaPlayer;
	private float volume;

	public MediaPlayerVolumeManager(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;
		mediaPlayer.setVolume(volume, volume);
	}

	@Override
	public float getVolume() {
		return volume;
	}
}
