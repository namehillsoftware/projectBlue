package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.client.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.list.NowPlayingFilesListActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class NowPlayingActivity extends AppCompatActivity {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NowPlayingActivity.class);

	public static void startNowPlayingActivity(final Context context) {
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(viewIntent);
	}


	private static final String fileNotFoundError = "The serviceFile %1s was not found!";

	private static boolean isScreenKeptOn;

	private static ViewStructure viewStructure;

	private static Bitmap nowPlayingBackgroundBitmap;

	private final CreateAndHold<Handler> messageHandler = new Lazy<>(() -> new Handler(getMainLooper()));

	private final LazyViewFinder<ImageButton> playButton = new LazyViewFinder<>(this, R.id.btnPlay);
	private final LazyViewFinder<ImageButton> pauseButton = new LazyViewFinder<>(this, R.id.btnPause);
	private final LazyViewFinder<RatingBar> songRating = new LazyViewFinder<>(this, R.id.rbSongRating);
	private final LazyViewFinder<RelativeLayout> contentView = new LazyViewFinder<>(this, R.id.viewNowPlayingRelativeLayout);
	private final LazyViewFinder<ProgressBar> songProgressBar = new LazyViewFinder<>(this, R.id.pbNowPlaying);
	private final LazyViewFinder<ImageView> nowPlayingImageViewFinder = new LazyViewFinder<>(this, R.id.imgNowPlaying);
	private final LazyViewFinder<TextView> nowPlayingArtist = new LazyViewFinder<>(this, R.id.tvSongArtist);
	private final LazyViewFinder<ImageButton> isScreenKeptOnButton = new LazyViewFinder<>(this, R.id.isScreenKeptOnButton);
	private final LazyViewFinder<TextView> nowPlayingTitle = new LazyViewFinder<>(this, R.id.tvSongTitle);
	private final LazyViewFinder<ImageView> nowPlayingImageLoading = new LazyViewFinder<>(this, R.id.imgNowPlayingLoading);
	private final LazyViewFinder<ProgressBar> loadingProgressBar = new LazyViewFinder<>(this, R.id.pbLoadingImg);
	private final CreateAndHold<NowPlayingToggledVisibilityControls> nowPlayingToggledVisibilityControls = new AbstractSynchronousLazy<NowPlayingToggledVisibilityControls>() {
		@Override
		protected NowPlayingToggledVisibilityControls create() {
			return new NowPlayingToggledVisibilityControls(new LazyViewFinder<>(NowPlayingActivity.this, R.id.llNpButtons), new LazyViewFinder<>(NowPlayingActivity.this, R.id.menuControlsLinearLayout), songRating);
		}
	};
	private final CreateAndHold<INowPlayingRepository> lazyNowPlayingRepository = new AbstractSynchronousLazy<INowPlayingRepository>() {
		@Override
		protected INowPlayingRepository create() {
			final LibraryRepository libraryRepository = new LibraryRepository(NowPlayingActivity.this);

			return
				new NowPlayingRepository(
					new SpecificLibraryProvider(
						new SelectedBrowserLibraryIdentifierProvider(NowPlayingActivity.this).getSelectedLibraryId(),
						libraryRepository),
					libraryRepository);
		}
	};
	private final Runnable onConnectionLostListener = () -> WaitForConnectionDialog.show(this);

	private final BroadcastReceiver onPlaybackChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1);
			if (playlistPosition < 0) return;

			showNowPlayingControls();
			updateKeepScreenOnStatus();

			setView(playlistPosition);
		}
	};

	private final BroadcastReceiver onPlaybackStartedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			togglePlayingButtons(true);

			updateKeepScreenOnStatus();
		}
	};

	private final BroadcastReceiver onPlaybackStoppedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			togglePlayingButtons(false);
			disableKeepScreenOn();
		}
	};

	private final BroadcastReceiver onTrackPositionChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final long fileDuration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration,-1);
			if (fileDuration > -1) setTrackDuration(fileDuration);

			final long filePosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
			if (filePosition > -1) setTrackProgress(filePosition);
		}
	};

	private final CreateAndHold<Promise<ImageProvider>> lazyImageProvider = new AbstractSynchronousLazy<Promise<ImageProvider>>() {
		@Override
		protected Promise<ImageProvider> create() {
			return SessionConnection.getInstance(NowPlayingActivity.this).promiseSessionConnection()
				.then(connectionProvider -> {
					final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();

					return new ImageProvider(
						NowPlayingActivity.this,
						connectionProvider,
						new AndroidDiskCacheDirectoryProvider(NowPlayingActivity.this),
						new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache)));
				});
		}
	};

	private TimerTask timerTask;

	private LocalBroadcastManager localBroadcastManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_now_playing);

		contentView.findView().setOnClickListener(v -> showNowPlayingControls());

		nowPlayingToggledVisibilityControls.getObject().toggleVisibility(false);

		final IntentFilter playbackStoppedIntentFilter = new IntentFilter();
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistPause);
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistStop);

		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter);
		localBroadcastManager.registerReceiver(onPlaybackStartedReceiver, new IntentFilter(PlaylistEvents.onPlaylistStart));
		localBroadcastManager.registerReceiver(onPlaybackChangedReceiver, new IntentFilter(PlaylistEvents.onPlaylistChange));
		localBroadcastManager.registerReceiver(onTrackPositionChanged, new IntentFilter(TrackPositionBroadcaster.trackPositionUpdate));

		PollConnection.Instance.get(this).addOnConnectionLostListener(onConnectionLostListener);

		setNowPlayingBackgroundBitmap();

		playButton.findView().setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
			PlaybackService.play(v.getContext());
			playButton.findView().setVisibility(View.INVISIBLE);
			pauseButton.findView().setVisibility(View.VISIBLE);
		});
		
		pauseButton.findView().setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
			PlaybackService.pause(v.getContext());
			playButton.findView().setVisibility(View.VISIBLE);
			pauseButton.findView().setVisibility(View.INVISIBLE);
		});

		final ImageButton next = findViewById(R.id.btnNext);
		if (next != null) {
			next.setOnClickListener(v -> {
				if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
				PlaybackService.next(v.getContext());
			});
		}

		final ImageButton previous = findViewById(R.id.btnPrevious);
		if (previous != null) {
			previous.setOnClickListener(v -> {
				if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
				PlaybackService.previous(v.getContext());
			});
		}

		final ImageButton shuffleButton = findViewById(R.id.repeatButton);
		setRepeatingIcon(shuffleButton);

		if (shuffleButton != null) {
			shuffleButton.setOnClickListener(v ->
				lazyNowPlayingRepository.getObject()
					.getNowPlaying()
					.eventually(LoopedInPromise.response(result -> {
						final boolean isRepeating = !result.isRepeating;
						if (isRepeating)
							PlaybackService.setRepeating(v.getContext());
						else
							PlaybackService.setCompleting(v.getContext());

						setRepeatingIcon(shuffleButton, isRepeating);

						return result;
					}, messageHandler.getObject())));
		}

		final ImageButton viewNowPlayingListButton = findViewById(R.id.viewNowPlayingListButton);
		if (viewNowPlayingListButton != null)
			viewNowPlayingListButton.setOnClickListener(v -> startActivity(new Intent(v.getContext(), NowPlayingFilesListActivity.class)));

		isScreenKeptOnButton.findView().setOnClickListener(v -> {
			isScreenKeptOn = !isScreenKeptOn;
			updateKeepScreenOnStatus();
		});

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
			songProgressBar.findView().getProgressDrawable().setColorFilter(getResources().getColor(R.color.custom_transparent_white), PorterDuff.Mode.SRC_IN);
	}
	
	@Override
	public void onStart() {
		super.onStart();

		updateKeepScreenOnStatus();

		InstantiateSessionConnectionActivity.restoreSessionConnection(this)
			.eventually(LoopedInPromise.response(perform(restore -> {
				if (!restore) initializeView();
			}), messageHandler.getObject()));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) initializeView();

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setNowPlayingBackgroundBitmap() {
		if (nowPlayingBackgroundBitmap != null) {
			final ImageView nowPlayingImageLoadingView = nowPlayingImageLoading.findView();
			nowPlayingImageLoadingView.setImageBitmap(nowPlayingBackgroundBitmap);
			nowPlayingImageLoadingView.setScaleType(ScaleType.CENTER_CROP);
			return;
		}

		new DefaultImageProvider(this).promiseFileBitmap()
			.eventually(bitmap -> new LoopedInPromise<>(() -> {
				nowPlayingBackgroundBitmap = bitmap;

				setNowPlayingBackgroundBitmap();

				return null;
			}, messageHandler.getObject()));
	}

	private void initializeView() {
		playButton.findView().setVisibility(View.VISIBLE);
		pauseButton.findView().setVisibility(View.INVISIBLE);

		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.eventually(np -> SessionConnection.getInstance(NowPlayingActivity.this)
				.promiseSessionConnection()
				.eventually(LoopedInPromise.response(connectionProvider -> {
					final ServiceFile serviceFile = np.playlist.get(np.playlistPosition);
					final long filePosition = connectionProvider != null && viewStructure != null && viewStructure.urlKeyHolder.equals(new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey()))
						? viewStructure.filePosition
						: np.filePosition;

					setView(serviceFile, filePosition);
					return null;
				}, messageHandler.getObject())))
			.excuse(perform(error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error)));

		bindService(new Intent(this, PlaybackService.class), new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				togglePlayingButtons(((PlaybackService)(((GenericBinder<?>)service).getService())).isPlaying());
				unbindService(this);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
		}, BIND_AUTO_CREATE);
	}

	private void setRepeatingIcon(final ImageButton imageButton) {
		setRepeatingIcon(imageButton, false);
		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.eventually(LoopedInPromise.response(result -> {
				if (result != null)
					setRepeatingIcon(imageButton, result.isRepeating);

				return null;
			}, messageHandler.getObject()));
	}
	
	private static void setRepeatingIcon(final ImageButton imageButton, boolean isRepeating) {
		imageButton.setImageDrawable(ViewUtils.getDrawable(imageButton.getContext(), isRepeating ? R.drawable.av_repeat_dark : R.drawable.av_no_repeat_dark));
	}

	private void updateKeepScreenOnStatus() {
		isScreenKeptOnButton.findView().setImageDrawable(ViewUtils.getDrawable(this, isScreenKeptOn ? R.drawable.screen_on : R.drawable.screen_off));

		if (isScreenKeptOn)
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			disableKeepScreenOn();
	}

	private void disableKeepScreenOn() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void togglePlayingButtons(boolean isPlaying) {
		playButton.findView().setVisibility(ViewUtils.getVisibility(!isPlaying));
		pauseButton.findView().setVisibility(ViewUtils.getVisibility(isPlaying));
	}

	private void setView(final int playlistPosition) {
		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.eventually(np -> SessionConnection.getInstance(this)
				.promiseSessionConnection()
				.eventually(LoopedInPromise.response(connectionProvider -> {
					if (playlistPosition >= np.playlist.size()) return null;

					final ServiceFile serviceFile = np.playlist.get(playlistPosition);

					final long filePosition =
						viewStructure != null && viewStructure.urlKeyHolder.equals(new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey()))
							? viewStructure.filePosition
							: 0;

					setView(serviceFile, filePosition);

					return null;
				}, messageHandler.getObject())))
			.excuse(perform(e -> logger.error("An error occurred while getting the Now Playing data", e)));
	}
	
	private void setView(final ServiceFile serviceFile, final long initialFilePosition) {
		SessionConnection.getInstance(this).promiseSessionConnection()
			.eventually(LoopedInPromise.response(perform(connectionProvider -> {
				final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey());

				if (viewStructure != null && !viewStructure.urlKeyHolder.equals(urlKeyHolder)) {
					viewStructure.release();
					viewStructure = null;
				}

				if (viewStructure == null)
					viewStructure = new ViewStructure(urlKeyHolder, serviceFile);

				final ViewStructure localViewStructure = viewStructure;

				final ImageView nowPlayingImage = nowPlayingImageViewFinder.findView();

				loadingProgressBar.findView().setVisibility(View.VISIBLE);
				nowPlayingImage.setVisibility(View.INVISIBLE);

				setNowPlayingImage(localViewStructure, serviceFile);

				if (localViewStructure.fileProperties != null) {
					setFileProperties(serviceFile, initialFilePosition, localViewStructure.fileProperties);
					return;
				}

				disableViewWithMessage(R.string.lbl_loading);

				final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance());
				filePropertiesProvider
					.promiseFileProperties(serviceFile)
					.eventually(LoopedInPromise.response(fileProperties -> {
						localViewStructure.fileProperties = fileProperties;
						setFileProperties(serviceFile, initialFilePosition, fileProperties);
						return null;
					}, messageHandler.getObject()))
					.excuse(e -> LoopedInPromise.<Throwable, Boolean>response(exception -> handleIoException(serviceFile, initialFilePosition, exception), messageHandler.getObject()).promiseResponse(e));
			}), messageHandler.getObject()));
	}

	private void setNowPlayingImage(ViewStructure viewStructure, ServiceFile serviceFile) {
		final ImageView nowPlayingImage = nowPlayingImageViewFinder.findView();

		loadingProgressBar.findView().setVisibility(View.VISIBLE);
		nowPlayingImage.setVisibility(View.INVISIBLE);

		if (viewStructure.promisedNowPlayingImage == null) {
			viewStructure.promisedNowPlayingImage =
				lazyImageProvider.getObject().eventually(provider -> provider.promiseFileBitmap(serviceFile));
		}

		viewStructure.promisedNowPlayingImage
			.eventually(bitmap -> new LoopedInPromise<>(() -> setNowPlayingImage(bitmap), messageHandler.getObject()))
			.excuse(perform(e -> {
				if (e instanceof CancellationException) {
					logger.info("Bitmap retrieval cancelled", e);
					return;
				}

				logger.error("There was an error retrieving the image for serviceFile " + serviceFile, e);
			}));
	}

	private Void setNowPlayingImage(Bitmap bitmap) {
		nowPlayingImageViewFinder.findView().setImageBitmap(bitmap);

		loadingProgressBar.findView().setVisibility(View.INVISIBLE);
		if (bitmap != null)
			displayImageBitmap();

		return null;
	}

	private void setFileProperties(final ServiceFile serviceFile, final long initialFilePosition, Map<String, String> fileProperties) {
		final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
		nowPlayingArtist.findView().setText(artist);

		final String title = fileProperties.get(FilePropertiesProvider.NAME);
		nowPlayingTitle.findView().setText(title);
		nowPlayingTitle.findView().setSelected(true);

		Float fileRating = null;
		final String stringRating = fileProperties.get(FilePropertiesProvider.RATING);
		try {
			if (stringRating != null && !stringRating.isEmpty())
				fileRating = Float.valueOf(stringRating);
		} catch (NumberFormatException e) {
			logger.info("Failed to parse rating", e);
		}

		setFileRating(serviceFile, fileRating);

		final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

		setTrackDuration(duration > 0 ? duration : 100);
		setTrackProgress(initialFilePosition);
	}

	private void setFileRating(ServiceFile serviceFile, Float rating) {
		final RatingBar songRatingBar = songRating.findView();
		songRatingBar.setRating(rating != null ? rating : 0f);

		songRatingBar.setOnRatingBarChangeListener((ratingBar, newRating, fromUser) -> {
			if (!fromUser || !nowPlayingToggledVisibilityControls.getObject().isVisible())
				return;

			final String stringRating = String.valueOf(Math.round(newRating));
			SessionConnection.getInstance(this).promiseSessionConnection()
				.then(perform(c -> FilePropertiesStorage.storeFileProperty(c, FilePropertyCache.getInstance(), serviceFile, FilePropertiesProvider.RATING, stringRating, false)));
			viewStructure.fileProperties.put(FilePropertiesProvider.RATING, stringRating);
		});

		songRatingBar.setEnabled(true);
	}

	private void setTrackDuration(long duration) {
		songProgressBar.findView().setMax((int) duration);

		if (viewStructure != null)
			viewStructure.fileDuration = duration;
	}

	private void setTrackProgress(long progress) {
		songProgressBar.findView().setProgress((int)progress);

		if (viewStructure != null)
			viewStructure.filePosition = progress;
	}

	private boolean handleFileNotFoundException(ServiceFile serviceFile, FileNotFoundException fe) {
		logger.error(String.format(fileNotFoundError, serviceFile), fe);
		disableViewWithMessage(R.string.file_not_found);
		return true;
	}
	
	private boolean handleIoException(ServiceFile serviceFile, long position, Throwable exception) {
		if (exception instanceof FileNotFoundException)
			return handleFileNotFoundException(serviceFile, (FileNotFoundException)exception);

		if (exception instanceof IOException) {
			resetViewOnReconnect(serviceFile, position);
			return true;
		}
		
		return false;
	}
	
	private void displayImageBitmap() {
		final ImageView nowPlayingImage = nowPlayingImageViewFinder.findView();
		nowPlayingImage.setScaleType(ScaleType.CENTER_CROP);
		nowPlayingImage.setVisibility(View.VISIBLE);
	}

	private void showNowPlayingControls() {
		nowPlayingToggledVisibilityControls.getObject().toggleVisibility(true);
		contentView.findView().invalidate();

		if (timerTask != null) timerTask.cancel();
		timerTask = new TimerTask() {
			boolean cancelled;

			@Override
			public void run() {
				if (!cancelled)
					nowPlayingToggledVisibilityControls.getObject().toggleVisibility(false);
			}

			@Override
			public boolean cancel() {
				cancelled = true;
				return super.cancel();
			}
		};

		messageHandler.getObject().postDelayed(timerTask, 5000);
	}
	
	private void resetViewOnReconnect(final ServiceFile serviceFile, final long position) {
		PollConnection.Instance.get(this).addOnConnectionRegainedListener(() -> {
			if (viewStructure == null || !serviceFile.equals(viewStructure.serviceFile)) return;

			if (viewStructure.promisedNowPlayingImage != null) {
				viewStructure.promisedNowPlayingImage.cancel();
				viewStructure.promisedNowPlayingImage = null;
			}

			setView(serviceFile, position);
		});
		WaitForConnectionDialog.show(this);
	}

	private void disableViewWithMessage(@StringRes int messageId) {
		nowPlayingTitle.findView().setText(messageId);
		nowPlayingArtist.findView().setText("");
		songRating.findView().setRating(0);
		songRating.findView().setEnabled(false);
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

		localBroadcastManager.unregisterReceiver(onPlaybackStoppedReceiver);
		localBroadcastManager.unregisterReceiver(onPlaybackStartedReceiver);
		localBroadcastManager.unregisterReceiver(onPlaybackChangedReceiver);
		localBroadcastManager.unregisterReceiver(onTrackPositionChanged);

		PollConnection.Instance.get(this).removeOnConnectionLostListener(onConnectionLostListener);
	}

	private static class ViewStructure {
		final UrlKeyHolder<Integer> urlKeyHolder;
		final ServiceFile serviceFile;
		Map<String, String> fileProperties;
		Promise<Bitmap> promisedNowPlayingImage;
		long filePosition;
		long fileDuration;

		ViewStructure(UrlKeyHolder<Integer> urlKeyHolder, ServiceFile serviceFile) {
			this.urlKeyHolder = urlKeyHolder;
			this.serviceFile = serviceFile;
		}

		void release() {
			if (promisedNowPlayingImage == null) return;

			promisedNowPlayingImage
				.then(bitmap -> {
					if (bitmap != null)
						bitmap.recycle();

					return null;
				});

			promisedNowPlayingImage.cancel();
		}
	}
}
