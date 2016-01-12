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

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NowPlayingActivity.class);

	private NowPlayingActivityProgressTrackerTask nowPlayingActivityProgressTrackerTask;
	private NowPlayingActivityMessageHandler nowPlayingActivityMessageHandler;
	private ImageButton playButton;
	private ImageButton pauseButton;
	private RatingBar songRating;
	private RelativeLayout contentView;
	private NowPlayingToggledVisibilityControls nowPlayingToggledVisibilityControls;
	private ImageButton isScreenKeptOnButton;

	private TimerTask timerTask;
	private ProgressBar songProgressBar;
	private ProgressBar loadingImg;
	private ImageView nowPlayingImageView;
	private TextView nowPlayingArtist;
	private TextView nowPlayingTitle;

	private LocalBroadcastManager localBroadcastManager;

	private static FluentTask<Void, Void, Bitmap> getFileImageTask;
	private static ViewStructure viewStructure;

	private static final String fileNotFoundError = "The file %1s was not found!";

	private static boolean isScreenKeptOn;

	private final Runnable onConnectionLostListener = () -> WaitForConnectionDialog.show(NowPlayingActivity.this);

	private final BroadcastReceiver onPlaybackStartedReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			showNowPlayingControls();

			playButton.setVisibility(View.INVISIBLE);
			pauseButton.setVisibility(View.VISIBLE);

			updateKeepScreenOnStatus();
		}
	};

	private final BroadcastReceiver onPlaybackStoppedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (nowPlayingActivityProgressTrackerTask != null) nowPlayingActivityProgressTrackerTask.cancel(false);

			final int fileDuration = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileDuration,-1);
			if (fileDuration > -1) songProgressBar.setMax(fileDuration);

			final int filePosition = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.filePosition, -1);
			if (filePosition > -1) songProgressBar.setProgress(filePosition);

			playButton.setVisibility(View.VISIBLE);
			pauseButton.setVisibility(View.INVISIBLE);

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

		contentView = (RelativeLayout)findViewById(R.id.viewNowPlayingRelativeLayout);

		contentView.setOnClickListener(v -> showNowPlayingControls());

		playButton = (ImageButton) findViewById(R.id.btnPlay);
		pauseButton = (ImageButton) findViewById(R.id.btnPause);
		songRating = (RatingBar) findViewById(R.id.rbSongRating);
		songProgressBar = (ProgressBar) findViewById(R.id.pbNowPlaying);
		loadingImg = (ProgressBar) findViewById(R.id.pbLoadingImg);
		nowPlayingImageView = (ImageView) findViewById(R.id.imgNowPlaying);
		nowPlayingArtist = (TextView) findViewById(R.id.tvSongArtist);
		nowPlayingTitle = (TextView) findViewById(R.id.tvSongTitle);

		nowPlayingToggledVisibilityControls = new NowPlayingToggledVisibilityControls((LinearLayout) findViewById(R.id.llNpButtons), (LinearLayout) findViewById(R.id.menuControlsLinearLayout), songRating);
		nowPlayingToggledVisibilityControls.toggleVisibility(false);

		final IntentFilter playbackStoppedIntentFilter = new IntentFilter();
		playbackStoppedIntentFilter.addAction(PlaybackService.PlaylistEvents.onPlaylistPause);
		playbackStoppedIntentFilter.addAction(PlaybackService.PlaylistEvents.onPlaylistStop);

		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter);
		localBroadcastManager.registerReceiver(onPlaybackStartedReciever, new IntentFilter(PlaybackService.PlaylistEvents.onPlaylistStart));

		PlaybackService.addOnStreamingChangeListener(this);

		PollConnection.Instance.get(this).addOnConnectionLostListener(onConnectionLostListener);
		
		playButton.setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.isVisible()) return;
			PlaybackService.play(v.getContext());
			playButton.setVisibility(View.INVISIBLE);
			pauseButton.setVisibility(View.VISIBLE);
		});
		
		pauseButton.setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.isVisible()) return;
			PlaybackService.pause(v.getContext());
			playButton.setVisibility(View.VISIBLE);
			pauseButton.setVisibility(View.INVISIBLE);
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
			songProgressBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.custom_transparent_white), PorterDuff.Mode.SRC_IN);

		nowPlayingActivityMessageHandler = new NowPlayingActivityMessageHandler(this);
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
		final IPlaybackFile filePlayer = PlaybackService.getCurrentPlaybackFile();
		if (filePlayer != null) {
			setView(filePlayer);
			return;
		}

		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.INVISIBLE);

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
		return contentView;
	}
	
	public NowPlayingToggledVisibilityControls getNowPlayingToggledVisibilityControls() {
		return nowPlayingToggledVisibilityControls;
	}

	public ProgressBar getSongProgressBar() {
		return songProgressBar;
	}

	private void setView(final IPlaybackFile playbackFile) {
		setView(playbackFile.getFile(), playbackFile.getCurrentPosition());

		playButton.setVisibility(playbackFile.isPlaying() ? View.INVISIBLE : View.VISIBLE);
		pauseButton.setVisibility(playbackFile.isPlaying() ? View.VISIBLE : View.INVISIBLE);

		if (nowPlayingActivityProgressTrackerTask != null) nowPlayingActivityProgressTrackerTask.cancel(false);
		nowPlayingActivityProgressTrackerTask = NowPlayingActivityProgressTrackerTask.trackProgress(playbackFile, nowPlayingActivityMessageHandler);
	}
	
	private void setView(final IFile file, final int initialFilePosition) {
		
		if (viewStructure != null && viewStructure.fileKey != file.getKey()) {
			viewStructure.release();
			viewStructure = null;
		}
		
		if (viewStructure == null)
			viewStructure = new ViewStructure(file);
		
		final ViewStructure viewStructure = NowPlayingActivity.viewStructure;
		
		if (viewStructure.nowPlayingImage == null) {
			try {				
				// Cancel the getFileImageTask if it is already in progress
				if (getFileImageTask != null)
					getFileImageTask.cancel();
				
				nowPlayingImageView.setVisibility(View.INVISIBLE);
				loadingImg.setVisibility(View.VISIBLE);
				
				getFileImageTask =
						ImageProvider
								.getImage(this, SessionConnection.getSessionConnectionProvider(), file)
								.onComplete(new TwoParameterRunnable<FluentTask<Void,Void,Bitmap>, Bitmap>() {

									@Override
									public void run(FluentTask<Void, Void, Bitmap> owner, Bitmap result) {
										if (viewStructure.nowPlayingImage != null)
											viewStructure.nowPlayingImage.recycle();
										viewStructure.nowPlayingImage = result;

										nowPlayingImageView.setImageBitmap(result);

										displayImageBitmap();
									}
								});

				getFileImageTask.execute();
				
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		} else {
			nowPlayingImageView.setImageBitmap(viewStructure.nowPlayingImage);
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

                nowPlayingArtist.setText(result);
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

                nowPlayingTitle.setText(result);
				nowPlayingTitle.setSelected(true);
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
                    songRating.setRating(0f);
                    songRating.setOnRatingBarChangeListener(null);

                    return;
                }

                viewStructure.nowPlayingRating = result;

                songRating.setRating(result != null ? result : 0f);

                songRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {

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
                    songProgressBar.setMax(100);
                    return;
                }

                if (result < 0) return;

				songProgressBar.setMax(result);
				songProgressBar.setProgress(initialFilePosition);
            }
        };

		getNowPlayingDuration.execute();
	}

	private void handleFileNotFoundException(IFile file, FileNotFoundException fe) {
		logger.error(String.format(fileNotFoundError, file), fe);
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
		nowPlayingImageView.setScaleType(ScaleType.CENTER_CROP);
		loadingImg.setVisibility(View.INVISIBLE);
		nowPlayingImageView.setVisibility(View.VISIBLE);
	}

	private void showNowPlayingControls() {
		nowPlayingToggledVisibilityControls.toggleVisibility(true);
		contentView.invalidate();

		if (timerTask != null) timerTask.cancel();
		timerTask = new TimerTask() {
			boolean cancelled;

			@Override
			public void run() {
				if (cancelled) return;

				final Message msg = new Message();
				msg.what = NowPlayingActivityMessageHandler.HIDE_CONTROLS;
				nowPlayingActivityMessageHandler.sendMessage(msg);
			}

			@Override
			public boolean cancel() {
				cancelled = true;
				return super.cancel();
			}
		};
		nowPlayingActivityMessageHandler.postDelayed(timerTask, 5000);
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

		if (timerTask != null) timerTask.cancel();
		if (nowPlayingActivityProgressTrackerTask != null) nowPlayingActivityProgressTrackerTask.cancel(false);

		PlaybackService.removeOnStreamingChangeListener(this);

		localBroadcastManager.unregisterReceiver(onPlaybackStoppedReceiver);
		localBroadcastManager.unregisterReceiver(onPlaybackStartedReciever);

		PollConnection.Instance.get(this).removeOnConnectionLostListener(onConnectionLostListener);
	}
}
