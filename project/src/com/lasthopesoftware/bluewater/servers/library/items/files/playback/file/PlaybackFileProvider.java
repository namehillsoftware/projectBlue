package com.lasthopesoftware.bluewater.servers.library.items.files.playback.file;

import java.util.List;

import android.content.Context;

import com.lasthopesoftware.bluewater.data.service.objects.File;


public class PlaybackFileProvider implements IPlaybackFileProvider {

	public PlaybackFileProvider(Context context, List<File> files) {
		
	}
	
	@Override
	public IPlaybackFile get(int filePos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPlaybackFile first() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPlaybackFile last() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(File file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(int filePos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int indexOf(File file) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int indexOf(int startingIndex, File file) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int indexOf(IPlaybackFile file) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int indexOf(int startingIndex, IPlaybackFile file) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<IPlaybackFile> getPlaybackFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<File> getFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
