package com.lasthopesoftware.resources.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class ActiveNetworkFinder implements LookupActiveNetwork {

	private final CreateAndHold<ConnectivityManager> lazyConnectivityManager;

	public ActiveNetworkFinder(Context context) {
		lazyConnectivityManager = new Lazy<>(() -> (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
	}

	@Override
	public NetworkInfo getActiveNetworkInfo() {
		return lazyConnectivityManager.getObject().getActiveNetworkInfo();
	}
}
