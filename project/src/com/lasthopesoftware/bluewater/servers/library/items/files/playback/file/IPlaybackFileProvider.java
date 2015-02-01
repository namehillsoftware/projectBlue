package com.lasthopesoftware.bluewater.servers.library.items.files.playback.file;

import java.util.List;

import com.lasthopesoftware.bluewater.data.service.objects.File;


public interface IPlaybackFileProvider{
	boolean add(File file);
	File get(int filePos);
	File remove(int filePos);
	IPlaybackFile getPlaybackFile(int filePos);
	int indexOf(File file);
	int indexOf(int startingIndex, File file);
	List<File> getFiles();
	String toPlaylistString();
	int size();
}