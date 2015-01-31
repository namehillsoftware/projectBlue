package com.lasthopesoftware.bluewater.servers.library.items.files.playback.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;


public class PlaybackFileProvider implements IPlaybackFileProvider {

	private final ArrayList<File> mFiles;
	private final Context mContext;
	
	private String mPlaylistString = null; 
	
	public PlaybackFileProvider(Context context, List<File> files) {
		mContext = context;
		
		mFiles = files instanceof ArrayList<?> ? (ArrayList<File>)files : new ArrayList<File>(files);
	}
	
	@Override
	public IPlaybackFile getPlaybackFile(int filePos) {
		return new PlaybackFile(mContext, get(filePos));
	}

	@Override
	public IPlaybackFile firstPlaybackFile() {
		return getPlaybackFile(0);
	}

	@Override
	public IPlaybackFile lastPlaybackFile() {
		return getPlaybackFile(mFiles.size() - 1);
	}

	@Override
	public int indexOf(File file) {
		return indexOf(0, file);
	}
	
	@Override
	public int indexOf(int startingIndex, File file) {
		for (int i = startingIndex; i < mFiles.size(); i++) {
			if (mFiles.get(i).equals(file)) return i;
		}
		
		return -1;
	}

	@Override
	public List<File> getFiles() {
		return Collections.unmodifiableList(mFiles);
	}

	@Override
	public int size() {
		return mFiles.size();
	}

	@Override
	public File get(int filePos) {
		return mFiles.get(filePos);
	}

	@Override
	public boolean add(File file) {
		file.setPreviousFile(mFiles.get(mFiles.size() - 1));
		final boolean isAdded = mFiles.add(file);
		mPlaylistString = null;
		return isAdded;
	}

	@Override
	public File remove(int filePos) {
		final File removedFile = mFiles.remove(filePos);
		mPlaylistString = null;
		
		final File nextFile = removedFile.getNextFile();
		final File previousFile = removedFile.getPreviousFile();
		
		if (previousFile != null)
			previousFile.setNextFile(nextFile);
		
		if (nextFile != null)
			nextFile.setPreviousFile(previousFile);
		
		return removedFile;
	}
	
	@Override
	public String toString() {
		if (mPlaylistString == null)
			mPlaylistString = Files.serializeFileStringList(mFiles);
		
		return mPlaylistString;
	}
}
