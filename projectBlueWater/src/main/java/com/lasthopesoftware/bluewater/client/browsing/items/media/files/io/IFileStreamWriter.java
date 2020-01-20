package com.lasthopesoftware.bluewater.client.browsing.items.media.files.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface IFileStreamWriter {
	void writeStreamToFile(InputStream inputStream, File file) throws IOException;
}
