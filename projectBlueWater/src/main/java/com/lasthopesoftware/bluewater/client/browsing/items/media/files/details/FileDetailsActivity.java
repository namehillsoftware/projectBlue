package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ImageDiskFileCacheFactory;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FormattedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.ImageCacheKeyLookup;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.MemoryCachedImageAccess;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.StaticLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ScaledWrapImageView;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public class FileDetailsActivity extends AppCompatActivity {

	private static final Logger logger = LoggerFactory.getLogger(FileDetailsActivity.class);
	public static final String FILE_KEY = "com.lasthopesoftware.bluewater.activities.ViewFiles.FILE_KEY";
    private static final int trackNameMarqueeDelay = 1500;

	private Bitmap mFileImage;

	private static final Set<String> PROPERTIES_TO_SKIP = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		KnownFileProperties.AUDIO_ANALYSIS_INFO,
		KnownFileProperties.GET_COVER_ART_INFO,
		KnownFileProperties.IMAGE_FILE,
		KnownFileProperties.KEY,
		KnownFileProperties.STACK_FILES,
		KnownFileProperties.STACK_TOP,
		KnownFileProperties.STACK_VIEW)));

	private static final AbstractSynchronousLazy<RelativeLayout.LayoutParams> imgFileThumbnailLayoutParams =
			new AbstractSynchronousLazy<RelativeLayout.LayoutParams>() {
				@Override
				protected RelativeLayout.LayoutParams create() {
					final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

					return layoutParams;
				}
			};

	private int fileKey = -1;

    private final LazyViewFinder<ListView> lvFileDetails = new LazyViewFinder<>(this, R.id.lvFileDetails);
    private final LazyViewFinder<ProgressBar> pbLoadingFileDetails = new LazyViewFinder<>(this, R.id.pbLoadingFileDetails);
    private final AbstractSynchronousLazy<ScaledWrapImageView> imgFileThumbnailBuilder = new AbstractSynchronousLazy<ScaledWrapImageView>() {
	    @Override
	    protected final ScaledWrapImageView create() {
		    final RelativeLayout rlFileThumbnailContainer = findViewById(R.id.rlFileThumbnailContainer);
		    if (rlFileThumbnailContainer == null) return null;

		    final ScaledWrapImageView imgFileThumbnail = new ScaledWrapImageView(FileDetailsActivity.this);
		    imgFileThumbnail.setBackgroundResource(R.drawable.drop_shadow);

		    imgFileThumbnail.setLayoutParams(imgFileThumbnailLayoutParams.getObject());

		    rlFileThumbnailContainer.addView(imgFileThumbnail);

		    return imgFileThumbnail;
	    }
    };

    private LazyViewFinder<ProgressBar> pbLoadingFileThumbnail = new LazyViewFinder<>(this, R.id.pbLoadingFileThumbnail);
    //        final RatingBar rbFileRating = (RatingBar) findViewById(R.id.rbFileRating);
    private LazyViewFinder<TextView> fileNameTextViewFinder = new LazyViewFinder<>(this, R.id.tvFileName);
    private LazyViewFinder<TextView> artistTextViewFinder = new LazyViewFinder<>(this, R.id.tvArtist);
	private final Lazy<DefaultImageProvider> defaultImageProvider = new Lazy<>(() -> new DefaultImageProvider(this));

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_view_file_details);

        fileKey = getIntent().getIntExtra(FILE_KEY, -1);

        setView(fileKey);
		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.viewFileDetailsRelativeLayout));
	}

	private void setView(final int fileKey) {
		if (fileKey < 0) {
        	finish();
        	return;
        }

        lvFileDetails.findView().setVisibility(View.INVISIBLE);
        pbLoadingFileDetails.findView().setVisibility(View.VISIBLE);

        imgFileThumbnailBuilder.getObject().setVisibility(View.INVISIBLE);
        pbLoadingFileThumbnail.findView().setVisibility(View.VISIBLE);

        fileNameTextViewFinder.findView().setText(getText(R.string.lbl_loading));
		artistTextViewFinder.findView().setText(getText(R.string.lbl_loading));

		SessionConnection.getInstance(this).promiseSessionConnection()
			.then(c -> new FormattedSessionFilePropertiesProvider(c, FilePropertyCache.getInstance(), ParsingScheduler.instance()))
			.eventually(f -> f.promiseFileProperties(new ServiceFile(fileKey)))
			.eventually(LoopedInPromise.response(new VoidResponse<>(fileProperties -> {
				setFileNameFromProperties(fileProperties);

				final String artist = fileProperties.get(KnownFileProperties.ARTIST);
				if (artist != null)
					this.artistTextViewFinder.findView().setText(artist);

				final List<Entry<String, String>> filePropertyList =
					Stream.of(fileProperties.entrySet())
						.filter(e -> !PROPERTIES_TO_SKIP.contains(e.getKey()))
						.sortBy(Entry::getKey)
						.collect(Collectors.toList());

				lvFileDetails.findView().setAdapter(new FileDetailsAdapter(FileDetailsActivity.this, R.id.linFileDetailsRow, filePropertyList));
				pbLoadingFileDetails.findView().setVisibility(View.INVISIBLE);
				lvFileDetails.findView().setVisibility(View.VISIBLE);
			}), this))
			.excuse(new HandleViewIoException(this, () -> setView(fileKey)))
			.excuse(forward())
			.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(this), this))
			.then(new VoidResponse<>(v -> finish()));

//        final SimpleTask<Void, Void, Float> getRatingsTask = new SimpleTask<Void, Void, Float>(new OnExecuteListener<Void, Void, Float>() {
//
//			@Override
//			public Float onExecute(ISimpleTask<Void, Void, Float> owner, Void... params) throws Exception {
//
//				if (filePropertiesProvider.getProperty(SessionFilePropertiesProvider.RATING) != null && !filePropertiesProvider.getProperty(SessionFilePropertiesProvider.RATING).isEmpty())
//					return Float.valueOf(filePropertiesProvider.getProperty(SessionFilePropertiesProvider.RATING));
//
//				return (float) 0;
//			}
//		});
//
//		getRatingsTask.onComplete(new OnCompleteListener<Void, Void, Float>() {
//
//			@Override
//			public void onComplete(ISimpleTask<Void, Void, Float> owner, Float result) {
//				rbFileRating.setRating(result);
//				rbFileRating.invalidate();
//
//				rbFileRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
//
//					@Override
//					public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
//						if (!fromUser) return;
//						filePropertiesProvider.setProperty(SessionFilePropertiesProvider.RATING, String.valueOf(Math.round(rating)));
//					}
//				});
//			}
//		});
//		getRatingsTask.onError(new HandleViewIoException(this, onConnectionRegainedListener));
//		getRatingsTask.execute();

		SessionConnection.getInstance(this).promiseSessionConnection()
			.eventually(connectionProvider -> {
				final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
				final CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider =
					new CachedSessionFilePropertiesProvider(connectionProvider, filePropertyCache,
						new SessionFilePropertiesProvider(connectionProvider, filePropertyCache));

				final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider =
					new SelectedBrowserLibraryIdentifierProvider(FileDetailsActivity.this);

				return new ImageProvider(
					new StaticLibraryIdentifierProvider(selectedLibraryIdentifierProvider),
					new MemoryCachedImageAccess(
						new ImageCacheKeyLookup(new CachedSessionFilePropertiesProvider(connectionProvider, filePropertyCache,
							new SessionFilePropertiesProvider(connectionProvider, filePropertyCache))),
						ImageDiskFileCacheFactory.getInstance(FileDetailsActivity.this),
						LibraryConnectionProvider.Instance.get(FileDetailsActivity.this)))
					.promiseFileBitmap(new ServiceFile(fileKey));
			})
			.eventually(bitmap ->
				bitmap != null
					? new Promise<>(bitmap)
					: defaultImageProvider.getObject().promiseFileBitmap())
			.eventually(LoopedInPromise.response(new VoidResponse<>(result -> {
				mFileImage = result;

				imgFileThumbnailBuilder.getObject().setImageBitmap(result);

				pbLoadingFileThumbnail.findView().setVisibility(View.INVISIBLE);
				imgFileThumbnailBuilder.getObject().setVisibility(View.VISIBLE);
			}), this));
	}

	private void setFileNameFromProperties(Map<String, String> fileProperties) {
		final String fileName = fileProperties.get(KnownFileProperties.NAME);
		if (fileName == null) return;

		final TextView fileNameTextView = fileNameTextViewFinder.findView();
		fileNameTextView.setText(fileName);

		fileNameTextView.postDelayed(() -> fileNameTextView.setSelected(true), trackNameMarqueeDelay);

		final SpannableString spannableString = new SpannableString(String.format(getString(R.string.lbl_details), fileName));
		spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, fileName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		setTitle(spannableString);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		// Update the intent
		setIntent(intent);
		fileKey = intent.getIntExtra(FILE_KEY, -1);
		setView(fileKey);
	}

	@Override
	public void onStart() {
		super.onStart();

		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			this.finish();
			return true;
		}
	    return super.onOptionsItemSelected(item);
	}
}
