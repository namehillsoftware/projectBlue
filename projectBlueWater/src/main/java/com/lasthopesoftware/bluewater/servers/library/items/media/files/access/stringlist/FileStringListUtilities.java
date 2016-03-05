package com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 11/26/15.
 */
public class FileStringListUtilities {

	public static ArrayList<IFile> parseFileStringList(String fileList) {
		final String[] keys = fileList.split(";");

		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<IFile> files = new ArrayList<>(Integer.parseInt(keys[1]));

		for (int i = offset; i < keys.length; i++) {
			if (keys[i].equals("-1")) continue;

			files.add(new File(Integer.parseInt(keys[i])));
		}

		return files;
	}

	public static String serializeFileStringList(List<IFile> files) {
		final int fileSize = files.size();
		// Take a guess that most keys will not be greater than 8 characters and add some more
		// for the first characters
		final StringBuilder sb = new StringBuilder(fileSize * 9 + 8);
		sb.append("2;").append(fileSize).append(";-1;");

		for (IFile file : files)
			sb.append(file.getKey()).append(";");

		return sb.toString();
	}
}
