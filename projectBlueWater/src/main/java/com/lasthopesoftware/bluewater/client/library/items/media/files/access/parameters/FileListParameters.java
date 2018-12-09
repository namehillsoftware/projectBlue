package com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters;

import com.lasthopesoftware.bluewater.client.library.items.IItem;

import java.util.ArrayList;
import java.util.Arrays;

public class FileListParameters implements IFileListParameterProvider {

	@Override
	public String[] getFileListParameters(IItem item) {
		return new String[] {"Browse/Files", "ID=" + String.valueOf(item.getKey()), "Version=2"};
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
