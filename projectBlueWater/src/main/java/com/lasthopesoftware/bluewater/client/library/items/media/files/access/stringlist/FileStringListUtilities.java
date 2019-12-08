package com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class FileStringListUtilities {

	public static Promise<Collection<ServiceFile>> promiseParsedFileStringList(@NonNull String fileList) {
		return new QueuedPromise<>(() -> parseFileStringList(fileList), ParsingScheduler.instance().getScheduler());
	}

	@NonNull
	private static Collection<ServiceFile> parseFileStringList(@NonNull String fileList) {
		final String[] keys = fileList.split(";");

		if (keys.length < 2) return Collections.emptySet();

		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<ServiceFile> serviceFiles = new ArrayList<>(Integer.parseInt(keys[1]));

		for (int i = offset; i < keys.length; i++) {
			if (keys[i].equals("-1")) continue;

			serviceFiles.add(new ServiceFile(Integer.parseInt(keys[i])));
		}

		return serviceFiles;
	}

	public static Promise<String> promiseSerializedFileStringList(Collection<ServiceFile> serviceFiles) {
		return new QueuedPromise<>(() -> serializeFileStringList(serviceFiles), ParsingScheduler.instance().getScheduler());
	}

	private static String serializeFileStringList(Collection<ServiceFile> serviceFiles) {
		final int fileSize = serviceFiles.size();
		// Take a guess that most keys will not be greater than 8 characters and add some more
		// for the first characters
		final StringBuilder sb = new StringBuilder(fileSize * 9 + 8);
		sb.append("2;").append(fileSize).append(";-1;");

		for (ServiceFile serviceFile : serviceFiles)
			sb.append(serviceFile.getKey()).append(";");

		return sb.toString();
	}
}
