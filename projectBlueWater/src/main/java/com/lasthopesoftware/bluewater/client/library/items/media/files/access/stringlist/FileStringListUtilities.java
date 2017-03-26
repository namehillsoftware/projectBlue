package com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.lazyj.Lazy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/26/15.
 */
public class FileStringListUtilities {

	private static final Lazy<ExecutorService> fileParsingExecutor = new Lazy<>(Executors::newCachedThreadPool);

	public static IPromise<List<ServiceFile>> promiseParsedFileStringList(@NonNull String fileList) {
		return new QueuedPromise<>(() -> parseFileStringList(fileList), fileParsingExecutor.getObject());
	}

	public static List<ServiceFile> parseFileStringList(@NonNull String fileList) {
		final String[] keys = fileList.split(";");

		if (keys.length < 2) return Collections.emptyList();

		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<ServiceFile> serviceFiles = new ArrayList<>(Integer.parseInt(keys[1]));

		for (int i = offset; i < keys.length; i++) {
			if (keys[i].equals("-1")) continue;

			serviceFiles.add(new ServiceFile(Integer.parseInt(keys[i])));
		}

		return serviceFiles;
	}

	public static IPromise<String> promiseSerializedFileStringList(List<ServiceFile> serviceFiles) {
		return new QueuedPromise<>(() -> serializeFileStringList(serviceFiles), fileParsingExecutor.getObject());
	}

	public static String serializeFileStringList(List<ServiceFile> serviceFiles) {
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
