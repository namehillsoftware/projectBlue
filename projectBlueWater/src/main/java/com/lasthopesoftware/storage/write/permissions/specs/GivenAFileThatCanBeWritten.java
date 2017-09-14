package com.lasthopesoftware.storage.write.permissions.specs;

import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 7/17/16.
 */
public class GivenAFileThatCanBeWritten {

	public static class WhenCheckingForPermissions {

		private boolean fileWriteIsPossible;

		@Before
		public void before() {
			final FileWritePossibleArbitrator fileWritePossibleArbitrator = FileWritePossibleArbitrator.getInstance();
			final File file = mock(File.class);
			when(file.exists()).thenReturn(true);
			when(file.canWrite()).thenReturn(true);

			fileWriteIsPossible = fileWritePossibleArbitrator.isFileWritePossible(file);
		}

		@Test
		public void thenFileWriteIsPossible() {
			Assert.assertTrue(this.fileWriteIsPossible);
		}
	}
}
