package com.lasthopesoftware.bluewater.client.stored.service.specs.GivenARequestToSync;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;
import com.lasthopesoftware.specs.AndroidContext;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class WhenStartingTheService extends AndroidContext {

	private final Context spiedContext = spy(ApplicationProvider.getApplicationContext());

	@Override
	public void before() {
		StoredSyncService.doSync(spiedContext);
	}

	@Test
	public void thenStartForegroundIsCalled() {
		verify(spiedContext, times(1)).startService(argThat(a -> StoredSyncService.class.getName().equals(a.getComponent().getClassName())));
	}
}
