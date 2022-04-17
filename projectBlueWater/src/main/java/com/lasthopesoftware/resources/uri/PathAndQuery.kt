package com.lasthopesoftware.resources.uri;

import android.net.Uri;

public class PathAndQuery {
	public static String forUri(Uri uri) {
		return uri.getPath() + "?" + uri.getQuery();
	}
}
