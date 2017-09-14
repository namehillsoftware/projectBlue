package com.lasthopesoftware.bluewater.client.library.items.media.files.io;

import com.namehillsoftware.lazyj.Lazy;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FileStreamWriter implements IFileStreamWriter {

	private static final Lazy<FileStreamWriter> instance = new Lazy<>(FileStreamWriter::new);

	private FileStreamWriter() {}

	@Override
	public void writeStreamToFile(InputStream inputStream, File file) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			IOUtils.copy(inputStream, fos);
			fos.flush();
		}
	}

	public static FileStreamWriter getInstance() {
		return instance.getObject();
	}
}
