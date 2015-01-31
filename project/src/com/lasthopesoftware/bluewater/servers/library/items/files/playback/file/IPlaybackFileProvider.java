package com.lasthopesoftware.bluewater.servers.library.items.files.playback.file;

import java.util.List;

import com.lasthopesoftware.bluewater.data.service.objects.File;


public interface IPlaybackFileProvider {
	IPlaybackFile first();
	IPlaybackFile last();
	void add(File file);
	IPlaybackFile get(int filePos);
	void remove(int filePos);
	int indexOf(File file);
	int indexOf(int startingIndex, File file);
	int indexOf(IPlaybackFile file);
	int indexOf(int startingIndex, IPlaybackFile file);
	List<IPlaybackFile> getPlaybackFiles();
	List<File> getFiles();
	int size();
}