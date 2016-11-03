package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.promises.IPromise;

import java.util.List;

/**
 * Created by david on 11/2/16.
 */
public class NowPlayingFileProvider implements INowPlayingFileProvider {

	private final Context context;

	public NowPlayingFileProvider(Context context) {
		this.context = context;
	}

	@Override
	public IPromise<IFile> getNowPlayingFile() {
		return
			LibrarySession
				.GetActiveLibrary(context)
				.then(library -> {
					final List<IFile> playlist = FileStringListUtilities.parseFileStringList(library.getSavedTracksString());
					return playlist.get(library.getNowPlayingId());
				});
	}
}
