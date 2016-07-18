package com.lasthopesoftware.storage.write.permissions;

import com.lasthopesoftware.storage.RecursiveFileAssertionTester;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public class FileWritePossibleArbitrator implements IFileWritePossibleArbitrator {

	@Override
	public boolean isFileWritePossible(File file) {
		return RecursiveFileAssertionTester.recursivelyTestAssertion(file, File::canWrite);
	}
}
