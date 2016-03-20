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
import android.support.annotation.StringRes;
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
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.servers.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.fluent.FluentTask;

import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class NowPlayingActivity extends AppCompatActivity implements OnNowPlayingChangeListener {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NowPlayingActivity.class);

	private static FluentTask<Void, Void, Bitmap> getFileImageTask;

	public static void startNowPlayingActivity(final Context context) {
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(viewIntent);
	}

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
		public final UrlKeyHolder<Integer> urlKeyHolder;
		public Map<String, String> fileProperties;
		public Bitmap nowPlayingImage;
		
		public ViewStructure(UrlKeyHolder<Integer> urlKeyHolder) {
			this.urlKeyHolder = urlKeyHolder;
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

		shuffleButton.setOnClickListener(v -> LibrarySession.GetActiveLibrary(v.getContext(), result -> {
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
		LibrarySession.GetActiveLibrary(this, library -> {
			final String savedTracksString = library.getSavedTracksString();
			if (savedTracksString == null || savedTracksString.isEmpty()) return;

			final AsyncTask<Void, Void, List<IFile>> getNowPlayingListTask = new AsyncTask<Void, Void, List<IFile>>() {

				@Override
				protected List<IFile> doInBackground(Void... params) {
					return FileStringListUtilities.parseFileStringList(savedTracksString);
				}

				@Override
				protected void onPostExecute(List<IFile> result) {
					setView(result.get(library.getNowPlayingId()), library.getNowPlayingProgress());
				}
			};

			getNowPlayingListTask.execute();
		});
	}

	private void setRepeatingIcon(final ImageButton imageButton) {
		setRepeatingIcon(imageButton, false);
		LibrarySession.GetActiveLibrary(this, result -> {
			if (result != null)
				setRepeatingIcon(imageButton, result.isRepeating());
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
		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(SessionConnection.getSessionConnectionProvider().getUrlProvider().getBaseUrl(), file.getKey());

		if (viewStructure != null && !viewStructure.urlKeyHolder.equals(urlKeyHolder)) {
			viewStructure.release();
			viewStructure = null;
		}
		
		if (viewStructure == null)
			viewStructure = new ViewStructure(urlKeyHolder);
		
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
								.getImage(this, SessionConnection.getSessionConnectionProvider(), file.getKey())
								.onComplete((owner, result) -> {
									if (viewStructure.nowPlayingImage != null)
										viewStructure.nowPlayingImage.recycle();
									viewStructure.nowPlayingImage = result;

									nowPlayingImageView.setImageBitmap(result);

									displayImageBitmap();
								});

				getFileImageTask.execute();
				
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		} else {
			nowPlayingImageView.setImageBitmap(viewStructure.nowPlayingImage);
			displayImageBitmap();
		}

		if (viewStructure.fileProperties != null) {
			setFileProperties(file, initialFilePosition, viewStructure.fileProperties);
			return;
		}

		disableViewWithMessage(R.string.lbl_loading);

		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), file.getKey());
		filePropertiesProvider
				.onComplete(fileProperties -> {
					viewStructure.fileProperties = fileProperties;
					setFileProperties(file, initialFilePosition, fileProperties);
				})
				.onError(exception -> handleIoException(file, initialFilePosition, exception))
				.execute();
	}

	private void setFileProperties(final IFile file, final int initialFilePosition, Map<String, String> fileProperties) {
		final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
		nowPlayingArtist.setText(artist);

		final String title = fileProperties.get(FilePropertiesProvider.NAME);
		nowPlayingTitle.setText(title);
		nowPlayingTitle.setSelected(true);

		Float fileRating = null;
		final String stringRating = fileProperties.get(FilePropertiesProvider.RATING);
		try {
			if (stringRating != null && !stringRating.isEmpty())
				fileRating = Float.valueOf(stringRating);
		} catch (NumberFormatException e) {
			logger.info("Failed to parse rating", e);
		}

		setFileRating(file, fileRating);

		final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

		songProgressBar.setMax(duration > 0 ? duration : 100);
		songProgressBar.setProgress(initialFilePosition);
	}

	private void setFileRating(IFile file, Float rating) {
		songRating.setRating(rating != null ? rating : 0f);

		songRating.setOnRatingBarChangeListener((ratingBar, newRating, fromUser) -> {
			if (!fromUser || !nowPlayingToggledVisibilityControls.isVisible())
				return;

			final String stringRating = String.valueOf(Math.round(newRating));
			FilePropertiesStorage.storeFileProperty(SessionConnection.getSessionConnectionProvider(), file.getKey(), FilePropertiesProvider.RATING, stringRating);
			viewStructure.fileProperties.put(FilePropertiesProvider.RATING, stringRating);
		});

		songRating.setEnabled(true);
	}

	private boolean handleFileNotFoundException(IFile file, FileNotFoundException fe) {
		logger.error(String.format(fileNotFoundError, file), fe);
		disableViewWithMessage(R.string.file_not_found);
		return true;
	}
	
	private boolean handleIoException(IFile file, int position, Exception exception) {
		if (exception instanceof FileNotFoundException)
			return handleFileNotFoundException(file, (FileNotFoundException)exception);

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
		PollConnection.Instance.get(this).addOnConnectionRegainedListener(() -> setView(file, position));
		WaitForConnectionDialog.show(this);
	}

	private void disableViewWithMessage(@StringRes int messageId) {
		nowPlayingTitle.setText(messageId);
		nowPlayingArtist.setText("");
		songRating.setRating(0);
		songRating.setEnabled(false);
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
