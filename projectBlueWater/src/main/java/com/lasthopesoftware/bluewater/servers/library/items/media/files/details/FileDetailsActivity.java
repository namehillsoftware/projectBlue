package com.lasthopesoftware.bluewater.servers.library.items.media.files.details;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.image.ImageAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FormattedFilePropertiesProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FileDetailsActivity extends Activity {

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
	
	private int mFileKey = -1;

    private FileDetailsActivity _this = this;
    private ListView lvFileDetails;
    private ProgressBar pbLoadingFileDetails;
    private ImageView imgFileThumbnail;
    private ProgressBar pbLoadingFileThumbnail;
    //        final RatingBar rbFileRating = (RatingBar) findViewById(R.id.rbFileRating);
    private TextView tvFileName;
    private TextView tvArtist;

	private final OnConnectionRegainedListener mOnConnectionRegainedListener = new OnConnectionRegainedListener() {
		
		@Override
		public void onConnectionRegained() {
			setView(mFileKey);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        if (!getResources().getBoolean(R.bool.is_landscape))
            setTheme(R.style.AppThemeNoActionBarShadowTheme);

		super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_view_file_details);

        mFileKey = getIntent().getIntExtra(FILE_KEY, -1);

        lvFileDetails = (ListView) findViewById(R.id.lvFileDetails);
        pbLoadingFileDetails = (ProgressBar) findViewById(R.id.pbLoadingFileDetails);
        imgFileThumbnail = (ImageView) findViewById(R.id.imgFileThumbnail);
        pbLoadingFileThumbnail = (ProgressBar) findViewById(R.id.pbLoadingFileThumbnail);
        tvFileName = (TextView) findViewById(R.id.tvFileName);
        tvArtist = (TextView) findViewById(R.id.tvArtist);

        setView(mFileKey);
	}

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @SuppressWarnings("unchecked")
	private void setView(final int fileKey) {
		if (fileKey < 0) {
        	finish();
        	return;
        }

        lvFileDetails.setVisibility(View.INVISIBLE);
        pbLoadingFileDetails.setVisibility(View.VISIBLE);
        
        imgFileThumbnail.setVisibility(View.INVISIBLE);
        pbLoadingFileThumbnail.setVisibility(View.VISIBLE);
        
        final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(fileKey);
        
        tvFileName.setText(getText(R.string.lbl_loading));
        final SimpleTask<Void, Void, String> getFileNameTask = new SimpleTask<>(new OnExecuteListener<Void, Void, String>() {
			
			@Override
			public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
				return filePropertiesProvider.getProperty(FilePropertiesProvider.NAME);
			}
		});
        getFileNameTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
				if (result == null) return;
				tvFileName.setText(result);

                tvFileName.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvFileName.setSelected(true);
                    }
                }, trackNameMarqueeDelay);

                final SpannableString spannableString = new SpannableString(String.format(getString(R.string.lbl_details), result));
                spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                setTitle(spannableString);
			}
		});
        getFileNameTask.addOnErrorListener(new HandleViewIoException(this, mOnConnectionRegainedListener));
        getFileNameTask.execute();
        
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
//		getRatingsTask.addOnCompleteListener(new OnCompleteListener<Void, Void, Float>() {
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
//		getRatingsTask.addOnErrorListener(new HandleViewIoException(this, mOnConnectionRegainedListener));
//		getRatingsTask.execute();
        
        final SimpleTask<Void, Void, List<Entry<String, String>>> getFilePropertiesTask = new SimpleTask<>(new OnExecuteListener<Void, Void, List<Entry<String, String>>>() {
			
			@Override
			public List<Entry<String, String>> onExecute(ISimpleTask<Void, Void, List<Entry<String, String>>> owner, Void... params) throws Exception {
				final FormattedFilePropertiesProvider formattedFileProperties = new FormattedFilePropertiesProvider(mFileKey);
				final Map<String, String> fileProperties = formattedFileProperties.getRefreshedProperties();
				final ArrayList<Entry<String, String>> results = new ArrayList<>(fileProperties.size());
				
				for (Entry<String, String> entry : fileProperties.entrySet()) {
					if (PROPERTIES_TO_SKIP.contains(entry.getKey())) continue;
					results.add(entry);
				}
				
				return results;
			}
		});
        
        getFilePropertiesTask.addOnCompleteListener(new OnCompleteListener<Void, Void, List<Entry<String, String>>>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Entry<String, String>>> owner, List<Entry<String, String>> result) {

				lvFileDetails.setAdapter(new FileDetailsAdapter(_this, R.id.linFileDetailsRow, result));
				pbLoadingFileDetails.setVisibility(View.INVISIBLE);
				lvFileDetails.setVisibility(View.VISIBLE);
			}
		});
        getFilePropertiesTask.addOnErrorListener(new HandleViewIoException(this, mOnConnectionRegainedListener));
        getFilePropertiesTask.execute();
                
        ImageAccess.getImage(this, fileKey, new OnCompleteListener<Void, Void, Bitmap>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, Bitmap> owner, Bitmap result) {
				if (mFileImage != null) mFileImage.recycle();
				mFileImage = result;
				
				imgFileThumbnail.setImageBitmap(mFileImage);

				pbLoadingFileThumbnail.setVisibility(View.INVISIBLE);
				imgFileThumbnail.setVisibility(View.VISIBLE);
			}
		});

        tvArtist.setText(getText(R.string.lbl_loading));
        final SimpleTask<Void, Void, String> getFileArtistTask = new SimpleTask<>(new OnExecuteListener<Void, Void, String>() {

            @Override
            public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
                return filePropertiesProvider.getProperty(FilePropertiesProvider.ARTIST);
            }
        });
        getFileArtistTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {

            @Override
            public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
                if (result == null || tvArtist == null) return;
                tvArtist.setText(result);
            }
        });
        getFileArtistTask.addOnErrorListener(new HandleViewIoException(this, mOnConnectionRegainedListener));
        getFileArtistTask.execute();
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
		if (mFileImage != null) mFileImage.recycle();
		
		super.onDestroy();
	}
}
