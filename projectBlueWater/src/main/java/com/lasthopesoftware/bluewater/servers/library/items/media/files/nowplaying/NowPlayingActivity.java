package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.list.NowPlayingFilesListActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.fluent.AsyncExceptionTask;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

public class NowPlayingActivity extends AppCompatActivity implements OnNowPlayingChangeListener {

	private static final org.slf4j.Logger mLogger = LoggerFactory.getLogger(NowPlayingActivity.class);

	private NowPlayingActivityProgressTrackerTask mTrackerTask;
	private NowPlayingActivityMessageHandler mHandler;
	private ImageButton mPlay;
	private ImageButton mPause;
	private RatingBar mSongRating;
	private RelativeLayout mContentView;
	private NowPlayingToggledVisibilityControls nowPlayingToggledVisibilityControls;
	private ImageButton isScreenKeptOnButton;

	private TimerTask mTimerTask;
	private ProgressBar mSongProgressBar;
	private ProgressBar mLoadingImg;
	private ImageView mNowPlayingImageView;
	private TextView mNowPlayingArtist;
	private TextView mNowPlayingTitle;

	private LocalBroadcastManager localBroadcastManager;

	private static FluentTask<Void, Void, Bitmap> getFileImageTask;
	private static ViewStructure mViewStructure;

	private static final String mFileNotFoundError = "The file %1s was not found!";

	private static boolean isScreenKeptOn;

	private final Runnable onConnectionLostListener = () -> WaitForConnectionDialog.show(NowPlayingActivity.this);

	private final BroadcastReceiver onPlaybackStartedReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			showNowPlayingControls();

			mPlay.setVisibility(View.INVISIBLE);
			mPause.setVisibility(View.VISIBLE);

			updateKeepScreenOnStatus();
		}
	};

	private final BroadcastReceiver onPlaybackStoppedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mTrackerTask != null) mTrackerTask.cancel(false);

			final int fileDuration = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileDuration,-1);
			if (fileDuration > -1) mSongProgressBar.setMax(fileDuration);

			final int filePosition = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.filePosition, -1);
			if (filePosition > -1) mSongProgressBar.setProgress(filePosition);

			mPlay.setVisibility(View.VISIBLE);
			mPause.setVisibility(View.INVISIBLE);

			disableKeepScreenOn();
		}
	};

	private static class ViewStructure {
		public final int fileKey;
		public Bitmap nowPlayingImage;
		public String nowPlayingArtist;
		public String nowPlayingTitle;
		public Float nowPlayingRating;
		
		public ViewStructure(final IFile file) {
			this.fileKey = file.getKey();
		}
		
		public void release() {
			if (nowPlayingImage != null)
				nowPlayingImage.recycle();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_now_playing);

		mContentView = (RelativeLayout)findViewById(R.id.viewNowPlayingRelativeLayout);

		mContentView.setOnClickListener(v -> showNowPlayingControls());

		mPlay = (ImageButton) findViewById(R.id.btnPlay);
		mPause = (ImageButton) findViewById(R.id.btnPause);
		mSongRating = (RatingBar) findViewById(R.id.rbSongRating);
		mSongProgressBar = (ProgressBar) findViewById(R.id.pbNowPlaying);
		mLoadingImg = (ProgressBar) findViewById(R.id.pbLoadingImg);
		mNowPlayingImageView = (ImageView) findViewById(R.id.imgNowPlaying);
		mNowPlayingArtist = (TextView) findViewById(R.id.tvSongArtist);
		mNowPlayingTitle = (TextView) findViewById(R.id.tvSongTitle);

		nowPlayingToggledVisibilityControls = new NowPlayingToggledVisibilityControls((LinearLayout) findViewById(R.id.llNpButtons), (LinearLayout) findViewById(R.id.menuControlsLinearLayout), mSongRating);
		nowPlayingToggledVisibilityControls.toggleVisibility(false);

		final IntentFilter playbackStoppedIntentFilter = new IntentFilter();
		playbackStoppedIntentFilter.addAction(PlaybackService.PlaylistEvents.onPlaylistPause);
		playbackStoppedIntentFilter.addAction(PlaybackService.PlaylistEvents.onPlaylistStop);

		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter);
		localBroadcastManager.registerReceiver(onPlaybackStartedReciever, new IntentFilter(PlaybackService.PlaylistEvents.onPlaylistStart));

		PlaybackService.addOnStreamingChangeListener(this);

		PollConnection.Instance.get(this).addOnConnectionLostListener(onConnectionLostListener);
		
		mPlay.setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.isVisible()) return;
			PlaybackService.play(v.getContext());
			mPlay.setVisibility(View.INVISIBLE);
			mPause.setVisibility(View.VISIBLE);
		});
		
		mPause.setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.isVisible()) return;
			PlaybackService.pause(v.getContext());
			mPlay.setVisibility(View.VISIBLE);
			mPause.setVisibility(View.INVISIBLE);
		});

		final ImageButton next = (ImageButton) findViewById(R.id.btnNext);
		next.setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.isVisible()) return;
			PlaybackService.next(v.getContext());
		});

		final ImageButton previous = (ImageButton) findViewById(R.id.btnPrevious);
		previous.setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.isVisible()) return;
			PlaybackService.previous(v.getContext());
		});

		final ImageButton shuffleButton = (ImageButton) findViewById(R.id.shuffleButton);
		setRepeatingIcon(shuffleButton);

		shuffleButton.setOnClickListener(v -> LibrarySession.GetActiveLibrary(v.getContext(), (owner, result) -> {
			if (result == null) return;
			final boolean isRepeating = !result.isRepeating();
			PlaybackService.setIsRepeating(v.getContext(), isRepeating);
			setRepeatingIcon(shuffleButton, isRepeating);
		}));

		final ImageButton viewNowPlayingListButton = (ImageButton) findViewById(R.id.viewNowPlayingListButton);
		viewNowPlayingListButton.setOnClickListener(v -> startActivity(new Intent(v.getContext(), NowPlayingFilesListActivity.class)));

		isScreenKeptOnButton = (ImageButton) findViewById(R.id.isScreenKeptOnButton);
		isScreenKeptOnButton.setOnClickListener(v -> {
			isScreenKeptOn = !isScreenKeptOn;
			updateKeepScreenOnStatus();
		});

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
			mSongProgressBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.custom_transparent_white), PorterDuff.Mode.SRC_IN);

		mHandler = new NowPlayingActivityMessageHandler(this);
	}
	
	@Override
	public void onStart() {
		super.onStart();

		updateKeepScreenOnStatus();

		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) initializeView();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) initializeView();

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void initializeView() {

		// Get initial view state from playlist controller if it is active
		if (PlaybackService.getPlaylistController() != null) {
			final IPlaybackFile filePlayer = PlaybackService.getPlaylistController().getCurrentPlaybackFile();

			setView(filePlayer);


			return;
		}

		mPlay.setVisibility(View.VISIBLE);
		mPause.setVisibility(View.INVISIBLE);

		// Otherwise set the view using the library persisted in the database
		LibrarySession.GetActiveLibrary(this, new TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library>() {

			@Override
			public void run(FluentTask<Integer, Void, Library> owner, final Library library) {
				final String savedTracksString = library.getSavedTracksString();
				if (savedTracksString == null || savedTracksString.isEmpty()) return;

				final AsyncTask<Void, Void, List<IFile>> getNowPlayingListTask = new AsyncTask<Void, Void, List<IFile>>() {

					@Override
					protected List<IFile> doInBackground(Void... params) {
						return FileStringListUtilities.parseFileStringList(SessionConnection.getSessionConnectionProvider(), savedTracksString);
					}

					@Override
					protected void onPostExecute(List<IFile> result) {
						setView(result.get(library.getNowPlayingId()), library.getNowPlayingProgress());
					}
				};

				getNowPlayingListTask.execute();
			}
		});
	}

	private void setRepeatingIcon(final ImageButton imageButton) {
		setRepeatingIcon(imageButton, false);
		LibrarySession.GetActiveLibrary(this, new TwoParameterRunnable<FluentTask<Integer, Void, Library>, Library>() {

			@Override
			public void run(FluentTask<Integer, Void, Library> owner, Library result) {
				if (result != null)
					setRepeatingIcon(imageButton, result.isRepeating());
			}

		});
	}
	
	private static void setRepeatingIcon(final ImageButton imageButton, boolean isRepeating) {
		imageButton.setImageDrawable(ViewUtils.getDrawable(imageButton.getContext(), isRepeating ? R.drawable.av_repeat_dark : R.drawable.av_no_repeat_dark));
	}

	private void updateKeepScreenOnStatus() {
		isScreenKeptOnButton.setImageDrawable(ViewUtils.getDrawable(this, isScreenKeptOn ? R.drawable.screen_on : R.drawable.screen_off));

		if (isScreenKeptOn)
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			disableKeepScreenOn();
	}

	private void disableKeepScreenOn() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	public RelativeLayout getContentView() {
		return mContentView;
	}
	
	public NowPlayingToggledVisibilityControls getNowPlayingToggledVisibilityControls() {
		return nowPlayingToggledVisibilityControls;
	}

	public ProgressBar getSongProgressBar() {
		return mSongProgressBar;
	}

	private void setView(final IPlaybackFile playbackFile) {
		setView(playbackFile.getFile(), playbackFile.getCurrentPosition());

		mPlay.setVisibility(playbackFile.isPlaying() ? View.INVISIBLE : View.VISIBLE);
		mPause.setVisibility(playbackFile.isPlaying() ? View.VISIBLE : View.INVISIBLE);

		if (mTrackerTask != null) mTrackerTask.cancel(false);
		mTrackerTask = NowPlayingActivityProgressTrackerTask.trackProgress(playbackFile, mHandler);
	}
	
	private void setView(final IFile file, final int initialFilePosition) {
		
		if (mViewStructure != null && mViewStructure.fileKey != file.getKey()) {
			mViewStructure.release();
			mViewStructure = null;
		}
		
		if (mViewStructure == null)
			mViewStructure = new ViewStructure(file);
		
		final ViewStructure viewStructure = mViewStructure;
		
		if (viewStructure.nowPlayingImage == null) {
			try {				
				// Cancel the getFileImageTask if it is already in progress
				if (getFileImageTask != null)
					getFileImageTask.cancel();
				
				mNowPlayingImageView.setVisibility(View.INVISIBLE);
				mLoadingImg.setVisibility(View.VISIBLE);
				
				getFileImageTask =
						ImageProvider
								.getImage(this, SessionConnection.getSessionConnectionProvider(), file)
								.onComplete(new TwoParameterRunnable<FluentTask<Void,Void,Bitmap>, Bitmap>() {

									@Override
									public void run(FluentTask<Void, Void, Bitmap> owner, Bitmap result) {
										if (viewStructure.nowPlayingImage != null)
											viewStructure.nowPlayingImage.recycle();
										viewStructure.nowPlayingImage = result;

										mNowPlayingImageView.setImageBitmap(result);

										displayImageBitmap();
									}
								});

				getFileImageTask.execute();
				
			} catch (Exception e) {
				mLogger.error(e.toString(), e);
			}
		} else {
			mNowPlayingImageView.setImageBitmap(viewStructure.nowPlayingImage);
			displayImageBitmap();
		}
		
		final AsyncTask<Void, Void, String> getArtistTask = new AsyncExceptionTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                if (viewStructure.nowPlayingArtist != null)
                    return viewStructure.nowPlayingArtist;

                try {
                    return file.getProperty(FilePropertiesProvider.ARTIST);
                } catch (FileNotFoundException e) {
                    handleFileNotFoundException(file, e);
                    return null;
                } catch (IOException e) {
                    setException(e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result, Exception exception) {
                if (handleIoException(file, initialFilePosition, exception)) return;

                mNowPlayingArtist.setText(result);
                viewStructure.nowPlayingArtist = result;
            }
        };
		getArtistTask.execute();

		final AsyncExceptionTask<Void, Void, String> getTitleTask = new AsyncExceptionTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                if (viewStructure.nowPlayingTitle != null)
                    return viewStructure.nowPlayingTitle;

                return file.getValue();
            }

            @Override
            protected void onPostExecute(String result, Exception exception) {
                if (handleIoException(file, initialFilePosition, exception)) return;

                mNowPlayingTitle.setText(result);
				mNowPlayingTitle.setSelected(true);
                viewStructure.nowPlayingTitle = result;
            }
        };
		getTitleTask.execute();

		final AsyncExceptionTask<Void, Void, Float> getRatingsTask = new AsyncExceptionTask<Void, Void, Float>() {

            @Override
            protected Float doInBackground(Void... params) {
                if (viewStructure.nowPlayingRating != null)
                    return viewStructure.nowPlayingRating;

                try {
                    if (file.getProperty(FilePropertiesProvider.RATING) != null && !file.getProperty(FilePropertiesProvider.RATING).isEmpty())
                        return Float.valueOf(file.getProperty(FilePropertiesProvider.RATING));
                } catch (FileNotFoundException e) {
                    handleFileNotFoundException(file, e);
                } catch (NumberFormatException | IOException e) {
                    setException(e);

                    return null;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Float result, Exception exception) {
                if (handleIoException(file, initialFilePosition, exception)) {
                    mSongRating.setRating(0f);
                    mSongRating.setOnRatingBarChangeListener(null);

                    return;
                }

                viewStructure.nowPlayingRating = result;

                mSongRating.setRating(result != null ? result : 0f);

                mSongRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {

                    @Override
                    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                        if (!fromUser || !nowPlayingToggledVisibilityControls.isVisible())
                            return;
                        file.setProperty(FilePropertiesProvider.RATING, String.valueOf(Math.round(rating)));

                        viewStructure.nowPlayingRating = rating;
                    }
                });
            }
        };
		getRatingsTask.execute();

		final AsyncExceptionTask<Void, Void, Integer> getNowPlayingDuration = new AsyncExceptionTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return file.getDuration();
                } catch (IOException e) {
                    setException(e);
                    return -1;
                }
            }

            @Override
            protected void onPostExecute(Integer result, Exception exception) {
                if (handleIoException(file, initialFilePosition, exception)) {
                    mSongProgressBar.setMax(100);
                    return;
                }

                if (result < 0) return;

				mSongProgressBar.setMax(result);
				mSongProgressBar.setProgress(initialFilePosition);
            }
        };

		getNowPlayingDuration.execute();
	}

	private void handleFileNotFoundException(IFile file, FileNotFoundException fe) {
		mLogger.error(String.format(mFileNotFoundError, file), fe);
	}
	
	private boolean handleIoException(IFile file, int position, Exception exception) {
		if (exception instanceof FileNotFoundException) {
			handleFileNotFoundException(file, (FileNotFoundException)exception);
			return false;
		}

		if (exception instanceof IOException) {
			resetViewOnReconnect(file, position);
			return true;
		}
		
		return false;
	}
	
	private void displayImageBitmap() {
		mNowPlayingImageView.setScaleType(ScaleType.CENTER_CROP);
		mLoadingImg.setVisibility(View.INVISIBLE);
		mNowPlayingImageView.setVisibility(View.VISIBLE);	
	}

	private void showNowPlayingControls() {
		nowPlayingToggledVisibilityControls.toggleVisibility(true);
		mContentView.invalidate();

		if (mTimerTask != null) mTimerTask.cancel();
		mTimerTask = new TimerTask() {
			boolean cancelled;

			@Override
			public void run() {
				if (cancelled) return;

				final Message msg = new Message();
				msg.what = NowPlayingActivityMessageHandler.HIDE_CONTROLS;
				mHandler.sendMessage(msg);
			}

			@Override
			public boolean cancel() {
				cancelled = true;
				return super.cancel();
			}
		};
		mHandler.postDelayed(mTimerTask, 5000);
	}
	
	private void resetViewOnReconnect(final IFile file, final int position) {
		PollConnection.Instance.get(this).addOnConnectionRegainedListener(new Runnable() {

			@Override
			public void run() {
				setView(file, position);
			}
		});
		WaitForConnectionDialog.show(this);
	}

	@Override
	public void onNowPlayingChange(PlaybackController controller, IPlaybackFile filePlayer) {
		setView(filePlayer);
	}

	@Override
	protected void onStop() {
		super.onStop();

		disableKeepScreenOn();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mTimerTask != null) mTimerTask.cancel();
		if (mTrackerTask != null) mTrackerTask.cancel(false);

		PlaybackService.removeOnStreamingChangeListener(this);

		localBroadcastManager.unregisterReceiver(onPlaybackStoppedReceiver);
		localBroadcastManager.unregisterReceiver(onPlaybackStartedReciever);

		PollConnection.Instance.get(this).removeOnConnectionLostListener(onConnectionLostListener);
	}
}
