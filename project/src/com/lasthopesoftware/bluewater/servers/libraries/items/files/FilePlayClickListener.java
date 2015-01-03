package com.lasthopesoftware.bluewater.servers.libraries.items.files;

import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.servers.libraries.items.files.nowplaying.service.NowPlayingService;

public class FilePlayClickListener implements OnClickListener {
	private List<File> mFiles;
	private int mPosition;
	
	public FilePlayClickListener(int position, List<File> files) {
		mPosition = position;
		mFiles = files;
	}
	
	@Override
	public void onClick(View v) {
		NowPlayingService.launchMusicService(v.getContext(), mPosition, Files.serializeFileStringList(mFiles));
	}
}
