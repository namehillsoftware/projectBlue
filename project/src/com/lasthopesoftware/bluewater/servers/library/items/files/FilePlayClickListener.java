package com.lasthopesoftware.bluewater.servers.library.items.files;

import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.PlaybackService;

public class FilePlayClickListener implements OnClickListener {
	private List<File> mFiles;
	private int mPosition;
	
	public FilePlayClickListener(int position, List<File> files) {
		mPosition = position;
		mFiles = files;
	}
	
	@Override
	public void onClick(View v) {
		PlaybackService.launchMusicService(v.getContext(), mPosition, Files.serializeFileStringList(mFiles));
	}
}
