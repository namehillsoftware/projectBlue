package com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.util.ArrayList;
import java.util.Arrays;

public class FileListParameters implements IFileListParameterProvider {

	private static final CreateAndHold<FileListParameters> lazyFileListParameters = new Lazy<>(FileListParameters::new);

	public static FileListParameters getInstance() {
		return lazyFileListParameters.getObject();
	}

	private FileListParameters() {}

	@Override
	public String[] getFileListParameters(Item item) {
		return new String[] {
			"Browse/Files",
			"ID=" + item.getKey(),
			"Version=2"
		};
	}

	@Override
	public String[] getFileListParameters(Playlist playlist) {
		return new String[] { "Playlist/Files", "Playlist=" + playlist.getKey()};
	}

	public enum Options {
		None,
		Shuffled
	}

	public static class Helpers {
		public static String[] processParams(Options option, String... params) {
			final ArrayList<String> newParams = new ArrayList<>(Arrays.asList(params));
			newParams.add("Action=Serialize");
			if (option == Options.Shuffled)
				newParams.add("Shuffle=1");
			return newParams.toArray(new String[0]);
		}
	}
}
