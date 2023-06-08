package com.lasthopesoftware.storage.read.permissions;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public interface IFileReadPossibleArbitrator {
	boolean isFileReadPossible(File file);
}
