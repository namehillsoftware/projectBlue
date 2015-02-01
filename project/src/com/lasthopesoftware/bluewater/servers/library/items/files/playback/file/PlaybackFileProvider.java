package com.lasthopesoftware.bluewater.servers.library.items.files.playback.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;

import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.service.objects.IFile;


public class PlaybackFileProvider implements IPlaybackFileProvider {

	private final ArrayList<IFile> mFiles;
	private final Context mContext;
	
	private String mPlaylistString = null; 
	
	public PlaybackFileProvider(Context context, List<IFile> files) {
		mContext = context;
		
		mFiles = files instanceof ArrayList<?> ? (ArrayList<IFile>)files : new ArrayList<IFile>(files);
	}
	
	@Override
	public IPlaybackFile getPlaybackFile(int filePos) {
		return new PlaybackFile(mContext, get(filePos));
	}

	@Override
	public int indexOf(IFile file) {
		return indexOf(0, file);
	}
	
	@Override
	public int indexOf(int startingIndex, IFile file) {
		for (int i = startingIndex; i < mFiles.size(); i++) {
			if (mFiles.get(i).equals(file)) return i;
		}
		
		return -1;
	}

	@Override
	public List<IFile> getFiles() {
		return Collections.unmodifiableList(mFiles);
	}

	@Override
	public int size() {
		return mFiles.size();
	}

	@Override
	public IFile get(int filePos) {
		return mFiles.get(filePos);
	}

	@Override
	public boolean add(IFile file) {
		file.setPreviousFile(mFiles.get(mFiles.size() - 1));
		final boolean isAdded = mFiles.add(file);
		mPlaylistString = null;
		return isAdded;
	}

	@Override
	public IFile remove(int filePos) {
		final IFile removedFile = mFiles.remove(filePos);
		mPlaylistString = null;
		
		final IFile nextFile = removedFile.getNextFile();
		final IFile previousFile = removedFile.getPreviousFile();
		
		if (previousFile != null)
			previousFile.setNextFile(nextFile);
		
		if (nextFile != null)
			nextFile.setPreviousFile(previousFile);
		
		return removedFile;
	}
	
	@Override
	public String toPlaylistString() {
		if (mPlaylistString == null)
			mPlaylistString = Files.serializeFileStringList(mFiles);
		
		return mPlaylistString;
	}
}
