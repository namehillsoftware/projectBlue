package com.lasthopesoftware.bluewater.activities.adapters.filelist.listeners;

import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;

public class PlayClickListener implements OnClickListener {
	private List<File> mFiles;
	private int mPosition;
	
	public PlayClickListener(int position, List<File> files) {
		mPosition = position;
		mFiles = files;
	}
	
	@Override
	public void onClick(View v) {
		StreamingMusicService.launchMusicService(v.getContext(), mPosition, Files.serializeFileStringList(mFiles));
	}
}
