package com.lasthopesoftware.storage.specs;

import com.lasthopesoftware.storage.RecursiveFileAssertionTester;
import com.vedsoft.futures.callables.OneParameterCallable;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by david on 7/17/16.
 */
public class GivenAFileThatDoesNotExist {
	public static class AndItsParentDoesNotExist {
		public static class AndTheGrandParentDoesExist {
			@RunWith(MockitoJUnitRunner.class)
			public static class WhenTestingTheAssertion {
				private File grandParentFile;

				@Mock
				private OneParameterCallable<File, Boolean> mockCallable;
				private boolean fileAssertionResult;
				private final boolean expectedFileAssertionResult = true;

				@Before
				public void setUp() {
					grandParentFile = mock(File.class);
					when(grandParentFile.exists()).thenReturn(true);

					final File parentFile = mock(File.class);
					when(parentFile.exists()).thenReturn(false);
					when(parentFile.getParentFile()).thenReturn(grandParentFile);

					final File childFile = mock(File.class);
					when(childFile.exists()).thenReturn(false);
					when(childFile.getParentFile()).thenReturn(parentFile);

					when(mockCallable.call(any())).thenReturn(false);
					when(mockCallable.call(grandParentFile)).thenReturn(expectedFileAssertionResult);

					fileAssertionResult = RecursiveFileAssertionTester.recursivelyTestAssertion(childFile, mockCallable);
				}

				@Test
				public void ThenTheAssertionIsTestedOnTheGrandParentFile() {
					verify(mockCallable, times(1)).call(grandParentFile);
				}

				@Test
				public void ThenTheAssertionIsTheExpectedResult() {
					Assert.assertEquals(expectedFileAssertionResult, fileAssertionResult);
				}
			}
		}

		public static class AndTheGrandParentIsNull {
			@RunWith(MockitoJUnitRunner.class)
			public static class WhenTestingTheAssertion {
				private File parentFile;
				private File childFile;

				@Mock
				private OneParameterCallable<File, Boolean> mockCallable;
				private boolean fileAssertionResult;

				@Before
				public void setUp() {
					parentFile = mock(File.class);
					when(parentFile.exists()).thenReturn(false);
					when(parentFile.getParentFile()).thenReturn(null);

					childFile = mock(File.class);
					when(childFile.exists()).thenReturn(false);
					when(childFile.getParentFile()).thenReturn(parentFile);

					when(mockCallable.call(any())).thenReturn(true);

					fileAssertionResult = RecursiveFileAssertionTester.recursivelyTestAssertion(childFile, mockCallable);
				}

				@Test
				public void ThenTheAssertionIsNotTested() {
					verify(mockCallable, times(0)).call(any());
				}

				@Test
				public void ThenTheResultIsFalse() {
					Assert.assertFalse(fileAssertionResult);
				}
			}
		}
	}
}
