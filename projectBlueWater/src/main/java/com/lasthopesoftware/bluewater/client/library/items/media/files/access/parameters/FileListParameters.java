package com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters;

import java.util.ArrayList;
import java.util.Arrays;

public class FileListParameters {
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
			return newParams.toArray(new String[newParams.size()]);
		}
	}
}
