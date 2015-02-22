package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import java.util.List;


public interface IPlaybackFileProvider{
	boolean add(IFile file);
	IFile get(int filePos);
	IFile remove(int filePos);
	IPlaybackFile getNewPlaybackFile(int filePos);
	int indexOf(IFile file);
	int indexOf(int startingIndex, IFile file);
	List<IFile> getFiles();
	String toPlaylistString();
	int size();
}