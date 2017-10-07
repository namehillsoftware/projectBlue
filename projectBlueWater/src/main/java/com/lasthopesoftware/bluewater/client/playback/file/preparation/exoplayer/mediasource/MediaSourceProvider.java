package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;

public final class MediaSourceProvider {
	private Context context;
	@NonNull
	private final DataSourceFactoryProvider dataSourceFactoryProvider;

	public MediaSourceProvider(@NonNull Context context, @NonNull DataSourceFactoryProvider dataSourceFactoryProvider) {
		this.context = context;
		this.dataSourceFactoryProvider = dataSourceFactoryProvider;
	}

	public MediaSource getMediaSource(@NonNull Uri uri) {
		ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
		Handler mainHandler = new Handler(context.getMainLooper());
		return new ExtractorMediaSource(uri, dataSourceFactoryProvider.getFactory(uri), extractorsFactory, mainHandler, null); // Listener defined
	}
}
