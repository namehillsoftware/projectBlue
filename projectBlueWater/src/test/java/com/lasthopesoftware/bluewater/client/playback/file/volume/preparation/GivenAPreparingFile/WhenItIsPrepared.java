package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.GivenAPreparingFile;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.FakeFilePreparer;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparer;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenItIsPrepared {

	private static final EmptyPlaybackHandler emptyPlaybackHandler = new EmptyPlaybackHandler(0);
	private static PreparedPlayableFile returnedFile;

	@BeforeClass
	public static void establish() throws ExecutionException, InterruptedException {
		final FakeFilePreparer fakeFilePreparer = new FakeFilePreparer(emptyPlaybackHandler, emptyPlaybackHandler);
		final MaxFileVolumePreparer maxFileVolumePreparer = new MaxFileVolumePreparer(fakeFilePreparer, (sf) -> new Promise<>(.89f));
		returnedFile = new FuturePromise<>(maxFileVolumePreparer.promisePreparedPlaybackFile(
			new ServiceFile(5),
			0)).get();
	}

	@Test
	public void thenTheFileIsReturned() {
		assertThat(returnedFile.getPlaybackHandler()).isEqualTo(emptyPlaybackHandler);
	}

	@Test
	public void thenTheVolumeIsManagedByTheMaxFileVolumeManager() {
		assertThat(returnedFile.getPlayableFileVolumeManager().getVolume()).isEqualTo(.89f);
	}
}
