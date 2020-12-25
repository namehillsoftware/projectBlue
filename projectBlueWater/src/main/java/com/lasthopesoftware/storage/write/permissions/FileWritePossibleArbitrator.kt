package com.lasthopesoftware.storage.write.permissions;

import com.lasthopesoftware.storage.RecursiveFileAssertionTester;

import java.io.File;

public final class FileWritePossibleArbitrator implements IFileWritePossibleArbitrator {

	@Override
	public boolean isFileWritePossible(File file) {
		return RecursiveFileAssertionTester.recursivelyTestAssertion(file, File::canWrite);
	}
}
