package com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.access.ImageAccess;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.service.objects.IFile;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.ServerListActivity;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionLostListener;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.list.NowPlayingFilesListActivity;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.threading.AsyncExceptionTask;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class NowPlayingActivity extends Activity implements 
	OnNowPlayingChangeListener, 
	OnNowPlayingPauseListener,
	OnNowPlayingStopListener,
	OnNowPlayingStartListener,
	OnConnectionLostListener
{
	private NowPlayingActivityProgressTrackerTask mTrackerTask;
	private NowPlayingActivityMessageHandler mHandler;
	private ImageButton mPlay;
	private ImageButton mPause;
	private ImageButton mNext;
	private ImageButton mPrevious;
	private RatingBar mSongRating;
	private FrameLayout mContentView;
	private RelativeLayout mControlNowPlaying, mViewCoverArt;
	private Timer mHideTimer;
	private TimerTask mTimerTask;
	
	private ProgressBar mSongProgressBar;
	private ProgressBar mLoadingImg;
	private ImageView mNowPlayingImageView;
	private TextView mNowPlayingArtist;
	private TextView mNowPlayingTitle;
	private static ImageAccess getFileImageTask;
	
	private static ViewStructure mViewStructure;
		
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
		
		mContentView = new FrameLayout(this);
		setContentView(mContentView);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mViewCoverArt = (RelativeLayout) inflater.inflate(R.layout.activity_view_cover_art, mContentView, false);
		mControlNowPlaying = (RelativeLayout) inflater.inflate(R.layout.activity_control_now_playing, mContentView, false);
		mControlNowPlaying.setVisibility(View.INVISIBLE);
		mContentView.addView(mViewCoverArt);
		mContentView.addView(mControlNowPlaying);
		
		mHideTimer = new Timer("Fade Timer");
		
		
		mContentView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showNowPlayingControls();
			}
		});

		mPlay = (ImageButton) findViewById(R.id.btnPlay);
		mPause = (ImageButton) findViewById(R.id.btnPause);
		mNext = (ImageButton) findViewById(R.id.btnNext);
		mPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		mSongRating = (RatingBar) findViewById(R.id.rbSongRating);
		mSongProgressBar = (ProgressBar) findViewById(R.id.pbNowPlaying);
		mLoadingImg = (ProgressBar) findViewById(R.id.pbLoadingImg);
		mNowPlayingImageView = (ImageView) findViewById(R.id.imgNowPlaying);
		mNowPlayingArtist = (TextView) findViewById(R.id.tvSongArtist);
		mNowPlayingTitle = (TextView) findViewById(R.id.tvSongTitle);
		
		PlaybackService.addOnStreamingChangeListener(this);
		PlaybackService.addOnStreamingPauseListener(this);
		PlaybackService.addOnStreamingStopListener(this);
		PlaybackService.addOnStreamingStartListener(this);
		PollConnection.Instance.get(this).addOnConnectionLostListener(this);
		
		mPlay.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				PlaybackService.play(v.getContext());
				mPlay.setVisibility(View.INVISIBLE);
				mPause.setVisibility(View.VISIBLE);
			}
		});
		
		mPause.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				PlaybackService.pause(v.getContext());
				mPlay.setVisibility(View.VISIBLE);
				mPause.setVisibility(View.INVISIBLE);
			}
		});

		mNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				PlaybackService.next(v.getContext());
			}
		});
		
		mPrevious.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				PlaybackService.previous(v.getContext());
			}
		});
		
		mHandler = new NowPlayingActivityMessageHandler(this);
		
		// Get initial view state from playlist controller if it is active
		if (PlaybackService.getPlaylistController() != null) {
			final IPlaybackFile filePlayer = PlaybackService.getPlaylistController().getCurrentFilePlayer();
			
			setView(filePlayer.getFile());
			mPlay.setVisibility(filePlayer.isPlaying() ?  View.INVISIBLE : View.VISIBLE);
			mPause.setVisibility(filePlayer.isPlaying() ? View.VISIBLE : View.INVISIBLE);
			
			return;
		}
		
		mPlay.setVisibility(View.VISIBLE);
		mPause.setVisibility(View.INVISIBLE);
		
		// Otherwise set the view using the library persisted in the database
		LibrarySession.GetLibrary(this, new OnCompleteListener<Integer, Void, Library>() {
			
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
				final String savedTracksString = library.getSavedTracksString();
				if (savedTracksString == null || savedTracksString.isEmpty()) return;
				
				final AsyncTask<Void, Void, List<IFile>> getNowPlayingListTask = new AsyncTask<Void, Void, List<IFile>>() {

					@Override
					protected List<IFile> doInBackground(Void... params) {
						return Files.deserializeFileStringList(savedTracksString);
					}
					
					@Override
					protected void onPostExecute(List<IFile> result) {
						setView(result.get(library.getNowPlayingId()));
						mSongProgressBar.setProgress(library.getNowPlayingProgress());
					}
				};
				
				getNowPlayingListTask.execute();
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_now_playing, menu);
		setRepeatingIcon(menu.findItem(R.id.menu_repeat_playlist));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_connection_settings:
				startActivity(new Intent(this, ServerListActivity.class));
				return true;
			case R.id.menu_repeat_playlist:
				final Context _context = this;
				LibrarySession.GetLibrary(this, new OnCompleteListener<Integer, Void, Library>() {

					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
						if (result == null) return;
						final boolean isRepeating = !result.isRepeating();
						PlaybackService.setIsRepeating(_context, isRepeating);
						setRepeatingIcon(item, isRepeating);
					}
				});
				return true;
			case R.id.menu_view_now_playing_files:
				startActivity(new Intent(this, NowPlayingFilesListActivity.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void setRepeatingIcon(final MenuItem item) {
		item.setIcon(R.drawable.av_no_repeat_dark);
		LibrarySession.GetLibrary(this, new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result != null)
					setRepeatingIcon(item, result.isRepeating());
			}
			
		});
	}
	
	private static void setRepeatingIcon(final MenuItem item, boolean isRepeating) {
		item.setIcon(isRepeating ? R.drawable.av_repeat_dark : R.drawable.av_no_repeat_dark);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mHideTimer != null) {
			mHideTimer.cancel();
			mHideTimer.purge();
		}
		
		if (mTrackerTask != null) mTrackerTask.cancel(false);
		
		PlaybackService.removeOnStreamingStartListener(this);
		PlaybackService.removeOnStreamingChangeListener(this);
		PlaybackService.removeOnStreamingPauseListener(this);
		PollConnection.Instance.get(this).removeOnConnectionLostListener(this);
	}
	
	public FrameLayout getContentView() {
		return mContentView;
	}
	
	public RelativeLayout getControlNowPlaying() {
		return mControlNowPlaying;
	}
	
	public RelativeLayout getViewCoverArt() {
		return mViewCoverArt;
	}
	
	public ProgressBar getSongProgressBar() {
		return mSongProgressBar;
	}
	
	private void setView(final IFile file) {
		
		try {
			
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
					
					getFileImageTask = ImageAccess.getImage(this, file, new OnCompleteListener<Void, Void, Bitmap>() {
						
						@Override
						public void onComplete(ISimpleTask<Void, Void, Bitmap> owner, Bitmap result) {
							if (viewStructure.nowPlayingImage != null)
								viewStructure.nowPlayingImage.recycle();
							viewStructure.nowPlayingImage = result;
							
							mNowPlayingImageView.setImageBitmap(result);
							
							displayImageBitmap();
						}
					});
					
				} catch (Exception e) {
					LoggerFactory.getLogger(getClass()).error(e.toString(), e);
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
						return file.getProperty(FileProperties.ARTIST);
					} catch (IOException e) {
						setException(e);
						return null;
					}
				}
			
				@Override
				protected void onPostExecute(String result, Exception exception) {
					if (handleIoException(file, exception)) return;
					
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
					if (handleIoException(file, exception)) return;
					
					mNowPlayingTitle.setText(result);
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
						if (file.getProperty(FileProperties.RATING) != null && !file.getProperty(FileProperties.RATING).isEmpty())
							return Float.valueOf(file.getProperty(FileProperties.RATING));
					} catch (NumberFormatException | IOException e) {
						setException(e);
						
						return null;
					}
					
					return null;
				}
				
				@Override
				protected void onPostExecute(Float result, Exception exception) {
					if (handleIoException(file, exception)) {
						mSongRating.setRating(0f);
						mSongRating.setOnRatingBarChangeListener(null);
						
						return;
					}
					
					viewStructure.nowPlayingRating = result;
					
					mSongRating.setRating(result != null ? result.floatValue() : 0f);
					
					mSongRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
						
						@Override
						public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
							if (!fromUser || !mControlNowPlaying.isShown()) return;
							file.setProperty(FileProperties.RATING, String.valueOf(Math.round(rating)));
							
							viewStructure.nowPlayingRating = Float.valueOf(rating);
						}
					});
				}
			};
			getRatingsTask.execute();
			
			mSongProgressBar.setMax(file.getDuration());			
		} catch (IOException ioE) {
			resetViewOnReconnect(file);
		}
	}
	
	private boolean handleIoException(IFile file, Exception exception) {
		if (exception != null && exception instanceof IOException) {
			resetViewOnReconnect(file);
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
		final PlaybackController playlistController = PlaybackService.getPlaylistController();
		showNowPlayingControls(playlistController != null ? playlistController.getCurrentFilePlayer() : null);
	}
	
	private void showNowPlayingControls(final IPlaybackFile filePlayer) {
		if (mTrackerTask != null) mTrackerTask.cancel(false);
		if (filePlayer != null) mTrackerTask = NowPlayingActivityProgressTrackerTask.trackProgress(filePlayer, mHandler);
		
		mControlNowPlaying.setVisibility(View.VISIBLE);
		mContentView.invalidate();
		if (mTimerTask != null) mTimerTask.cancel();
		mHideTimer.purge();
		mTimerTask = new TimerTask() {
			
			@Override
			public void run() {
				final Message msg = new Message();
				msg.what = NowPlayingActivityMessageHandler.HIDE_CONTROLS;
				mHandler.sendMessage(msg);
				if (mTrackerTask != null) mTrackerTask.cancel(false);
			}
		};
		mHideTimer.schedule(mTimerTask, 5000);
	}
	
	private void resetViewOnReconnect(final IFile file) {
		PollConnection.Instance.get(this).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
			
			@Override
			public void onConnectionRegained() {
				setView(file);
			}
		});
		WaitForConnectionDialog.show(this);
	}

	@Override
	public void onNowPlayingChange(PlaybackController controller, IPlaybackFile filePlayer) {		
		setView(filePlayer.getFile());
		mSongProgressBar.setProgress(filePlayer.getCurrentPosition());
	}
	
	@Override
	public void onNowPlayingStart(PlaybackController controller, IPlaybackFile filePlayer) {		
		showNowPlayingControls(filePlayer);
		
		mPlay.setVisibility(View.INVISIBLE);
		mPause.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onNowPlayingPause(PlaybackController controller, IPlaybackFile filePlayer) {
		handleNowPlayingStopping(controller, filePlayer);
	}

	@Override
	public void onNowPlayingStop(PlaybackController controller, IPlaybackFile filePlayer) {
		handleNowPlayingStopping(controller, filePlayer);
	}
	
	private void handleNowPlayingStopping(PlaybackController controller, IPlaybackFile filePlayer) {
		if (mTrackerTask != null) mTrackerTask.cancel(false);
		
		int duration = 100;
		try {
			duration = filePlayer.getDuration();
		} catch (IOException e) {
			LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
		}
		
		mSongProgressBar.setMax(duration);
		mSongProgressBar.setProgress(filePlayer.getCurrentPosition());
		
		mPlay.setVisibility(View.VISIBLE);
		mPause.setVisibility(View.INVISIBLE);
		
		mControlNowPlaying.invalidate();
	}

	@Override
	public void onConnectionLost() {
		WaitForConnectionDialog.show(this);
	}
}
