package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;

public class FilePlayClickListener implements OnClickListener {
	private List<IFile> mFiles;
	private int mPosition;
	
	public FilePlayClickListener(int position, List<IFile> files) {
		mPosition = position;
		mFiles = files;
	}
	
	@Override
	public void onClick(View v) {
		PlaybackService.launchMusicService(v.getContext(), mPosition, Files.serializeFileStringList(mFiles));
	}
}
