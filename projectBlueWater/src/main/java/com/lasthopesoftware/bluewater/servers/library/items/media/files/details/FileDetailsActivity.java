package com.lasthopesoftware.bluewater.servers.library.items.media.files.details;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FormattedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.shared.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ScaledWrapImageView;
import com.vedsoft.lazyj.AbstractLazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FileDetailsActivity extends AppCompatActivity {

	private static final Logger logger = LoggerFactory.getLogger(FileDetailsActivity.class);
	public static final String FILE_KEY = "com.lasthopesoftware.bluewater.activities.ViewFiles.FILE_KEY";
    private static final int trackNameMarqueeDelay = 1500;

	private Bitmap mFileImage;

	private static final Set<String> PROPERTIES_TO_SKIP = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
															new String[] {
																FilePropertiesProvider.AUDIO_ANALYSIS_INFO,
																FilePropertiesProvider.GET_COVER_ART_INFO,
																FilePropertiesProvider.IMAGE_FILE,
																FilePropertiesProvider.KEY,
																FilePropertiesProvider.STACK_FILES,
																FilePropertiesProvider.STACK_TOP,
																FilePropertiesProvider.STACK_VIEW })));

	private static final AbstractLazy<RelativeLayout.LayoutParams> imgFileThumbnailLayoutParams =
			new AbstractLazy<RelativeLayout.LayoutParams>() {
				@Override
				protected RelativeLayout.LayoutParams initialize() throws Exception {
					final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

					return layoutParams;
				}
			};

	private int fileKey = -1;

    private final LazyViewFinder<ListView> lvFileDetails = new LazyViewFinder<>(this, R.id.lvFileDetails);
    private final LazyViewFinder<ProgressBar> pbLoadingFileDetails = new LazyViewFinder<>(this, R.id.pbLoadingFileDetails);
    private final AbstractLazy<ScaledWrapImageView> imgFileThumbnailBuilder = new AbstractLazy<ScaledWrapImageView>() {
	    @Override
	    protected final ScaledWrapImageView initialize() throws Exception {
		    final RelativeLayout rlFileThumbnailContainer = (RelativeLayout) findViewById(R.id.rlFileThumbnailContainer);
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

	private boolean isDestroyed;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_view_file_details);

        fileKey = getIntent().getIntExtra(FILE_KEY, -1);

        setView(fileKey);
		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.viewFileDetailsRelativeLayout));
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

        final FormattedFilePropertiesProvider formattedFilePropertiesProvider = new FormattedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), fileKey);

        fileNameTextViewFinder.findView().setText(getText(R.string.lbl_loading));
		artistTextViewFinder.findView().setText(getText(R.string.lbl_loading));

		formattedFilePropertiesProvider
				.onComplete(fileProperties -> {
					setFileNameFromProperties(fileProperties);

					final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
					if (artist != null)
						this.artistTextViewFinder.findView().setText(artist);

					final ArrayList<Entry<String, String>> filePropertyList = new ArrayList<>(fileProperties.size());

					for (Entry<String, String> entry : fileProperties.entrySet()) {
						if (!PROPERTIES_TO_SKIP.contains(entry.getKey()))
							filePropertyList.add(entry);
					}

					Collections.sort(filePropertyList, (lhs, rhs) -> lhs.getKey().compareTo(rhs.getKey()));

					lvFileDetails.findView().setAdapter(new FileDetailsAdapter(FileDetailsActivity.this, R.id.linFileDetailsRow, filePropertyList));
					pbLoadingFileDetails.findView().setVisibility(View.INVISIBLE);
					lvFileDetails.findView().setVisibility(View.VISIBLE);
				})
				.onError(new HandleViewIoException<>(this, () -> setView(fileKey)))
				.execute();

//        final SimpleTask<Void, Void, Float> getRatingsTask = new SimpleTask<Void, Void, Float>(new OnExecuteListener<Void, Void, Float>() {
//
//			@Override
//			public Float onExecute(ISimpleTask<Void, Void, Float> owner, Void... params) throws Exception {
//
//				if (filePropertiesProvider.getProperty(FilePropertiesProvider.RATING) != null && !filePropertiesProvider.getProperty(FilePropertiesProvider.RATING).isEmpty())
//					return Float.valueOf(filePropertiesProvider.getProperty(FilePropertiesProvider.RATING));
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
//						filePropertiesProvider.setProperty(FilePropertiesProvider.RATING, String.valueOf(Math.round(rating)));
//					}
//				});
//			}
//		});
//		getRatingsTask.onError(new HandleViewIoException(this, onConnectionRegainedListener));
//		getRatingsTask.execute();

        ImageProvider
		        .getImage(this, SessionConnection.getSessionConnectionProvider(), fileKey)
		        .onComplete((owner, result) -> {
			        if (mFileImage != null) mFileImage.recycle();

			        if (isDestroyed) {
				        if (result != null) result.recycle();
				        return;
			        }

			        mFileImage = result;

			        imgFileThumbnailBuilder.getObject().setImageBitmap(result);

			        pbLoadingFileThumbnail.findView().setVisibility(View.INVISIBLE);
			        imgFileThumbnailBuilder.getObject().setVisibility(View.VISIBLE);
		        })
		        .execute();
	}

	private void setFileNameFromProperties(Map<String, String> fileProperties) {
		final String fileName = fileProperties.get(FilePropertiesProvider.NAME);
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
	    switch (item.getItemId()) {
	    case android.R.id.home:
	        this.finish();
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		isDestroyed = true;
		if (mFileImage != null) mFileImage.recycle();

		super.onDestroy();
	}
}
