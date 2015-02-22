package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlaybackFileProvider implements IPlaybackFileProvider {

	private final ArrayList<IFile> mFiles;
	private final Context mContext;
	
	private String mPlaylistString = null; 
	
	public PlaybackFileProvider(Context context, List<IFile> files) {
		mContext = context;
		
		mFiles = files instanceof ArrayList<?> ? (ArrayList<IFile>)files : new ArrayList<IFile>(files);
	}
	
	@Override
	public IPlaybackFile getNewPlaybackFile(int filePos) {
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
		final boolean isAdded = mFiles.add(file);
		mPlaylistString = null;
		return isAdded;
	}

	@Override
	public IFile remove(int filePos) {
		final IFile removedFile = mFiles.remove(filePos);
		mPlaylistString = null;
		
		return removedFile;
	}
	
	@Override
	public String toPlaylistString() {
		if (mPlaylistString == null)
			mPlaylistString = Files.serializeFileStringList(mFiles);
		
		return mPlaylistString;
	}
}
