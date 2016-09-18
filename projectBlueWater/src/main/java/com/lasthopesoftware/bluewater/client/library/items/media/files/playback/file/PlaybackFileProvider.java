package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlaybackFileProvider implements IPlaybackFileProvider {

	private final ArrayList<IFile> files;
	private final Context context;
	private final ConnectionProvider connectionProvider;
	
	private String playlistString = null;
	
	public PlaybackFileProvider(Context context, ConnectionProvider connectionProvider, List<IFile> files) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.files = files instanceof ArrayList<?> ? (ArrayList<IFile>)files : new ArrayList<>(files);
	}
	
	@Override
	public IPlaybackFile getNewPlaybackFile(int filePos) {
		return new PlaybackFileController(context, connectionProvider, get(filePos));
	}

	@Override
	public int indexOf(IFile file) {
		return indexOf(0, file);
	}
	
	@Override
	public int indexOf(int startingIndex, IFile file) {
		for (int i = startingIndex; i < files.size(); i++) {
			if (files.get(i).equals(file)) return i;
		}
		
		return -1;
	}

	@Override
	public List<IFile> getFiles() {
		return Collections.unmodifiableList(files);
	}

	@Override
	public int size() {
		return files.size();
	}

	@Override
	public IFile get(int filePos) {
		return files.get(filePos);
	}

	@Override
	public boolean add(IFile file) {
		final boolean isAdded = files.add(file);
		playlistString = null;
		return isAdded;
	}

	@Override
	public IFile remove(int filePos) {
		final IFile removedFile = files.remove(filePos);
		playlistString = null;
		
		return removedFile;
	}
	
	@Override
	public String toPlaylistString() {
		if (playlistString == null)
			playlistString = FileStringListUtilities.serializeFileStringList(files);

		return playlistString;
	}
}
