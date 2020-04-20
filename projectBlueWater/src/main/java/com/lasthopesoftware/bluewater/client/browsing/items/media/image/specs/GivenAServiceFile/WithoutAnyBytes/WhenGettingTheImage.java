package com.lasthopesoftware.bluewater.client.browsing.items.media.image.specs.GivenAServiceFile.WithoutAnyBytes;

import android.graphics.Bitmap;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingTheImage extends AndroidContext {

	private static Bitmap bitmap;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final ImageProvider imageProvider = new ImageProvider(() -> new LibraryId(2), (l, s) -> new Promise<>(new byte[0]));

		bitmap = new FuturePromise<>(imageProvider.promiseFileBitmap(new ServiceFile(34))).get();
	}

	@Test
	public void thenTheBitmapIsNull() {
		assertThat(bitmap).isNull();
	}
}
