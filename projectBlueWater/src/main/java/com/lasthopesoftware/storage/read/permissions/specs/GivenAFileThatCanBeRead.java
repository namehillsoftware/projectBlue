package com.lasthopesoftware.storage.read.permissions.specs;

import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 7/17/16.
 */
public class GivenAFileThatCanBeRead {

	public static class WhenCheckingForPermissions {

		private boolean fileReadIsPossible;

		@Before
		public void before() {
			final FileReadPossibleArbitrator fileReadPossibleArbitrator = new FileReadPossibleArbitrator();
			final File file = mock(File.class);
			when(file.exists()).thenReturn(true);
			when(file.canRead()).thenReturn(true);

			fileReadIsPossible = fileReadPossibleArbitrator.isFileReadPossible(file);
		}

		@Test
		public void thenFileReadIsPossible() {
			Assert.assertTrue(this.fileReadIsPossible);
		}
	}
}
