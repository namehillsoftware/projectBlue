package com.lasthopesoftware.bluewater.servers.library.items.files.playback.file;

import java.util.List;

import com.lasthopesoftware.bluewater.data.service.objects.IFile;


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