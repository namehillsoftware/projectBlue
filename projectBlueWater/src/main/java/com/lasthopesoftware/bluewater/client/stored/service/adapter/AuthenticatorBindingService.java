package com.lasthopesoftware.bluewater.client.stored.service.adapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class AuthenticatorBindingService extends Service {

	private final CreateAndHold<DummyAuthenticator> lazyAuthenticator = new Lazy<>(() -> new DummyAuthenticator(this));

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return lazyAuthenticator.getObject().getIBinder();
	}
}
