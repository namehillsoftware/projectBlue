package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
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
import com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers.HandleViewNowPlayingMessages;
import com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers.ProgressTrackerTask;
import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.access.ImageAccess;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionLostListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.AsyncExceptionTask;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ViewNowPlaying extends Activity implements 
	OnNowPlayingChangeListener, 
	OnNowPlayingPauseListener,
	OnNowPlayingStopListener,
	OnNowPlayingStartListener,
	OnConnectionLostListener
{
	private ProgressTrackerTask mTrackerTask;
	private HandleViewNowPlayingMessages mHandler;
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
	
	private ViewStructure mViewStructure;
		
	private static class ViewStructure {
		public final int fileKey;
		public Bitmap nowPlayingImage;
		public String nowPlayingArtist;
		public String nowPlayingTitle;
		public Float nowPlayingRating;
		
		public ViewStructure(final File file) {
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
		
		StreamingMusicService.addOnStreamingChangeListener(this);
		StreamingMusicService.addOnStreamingPauseListener(this);
		StreamingMusicService.addOnStreamingStopListener(this);
		StreamingMusicService.addOnStreamingStartListener(this);
		PollConnection.Instance.get(this).addOnConnectionLostListener(this);
		
		mPlay.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.play(v.getContext());
				mPlay.setVisibility(View.INVISIBLE);
				mPause.setVisibility(View.VISIBLE);
			}
		});
		
		mPause.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.pause(v.getContext());
				mPlay.setVisibility(View.VISIBLE);
				mPause.setVisibility(View.INVISIBLE);
			}
		});

		mNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.next(v.getContext());
			}
		});
		
		mPrevious.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.previous(v.getContext());
			}
		});
		
		mHandler = new HandleViewNowPlayingMessages(this);
		
		if (StreamingMusicService.getPlaylistController() == null)
			StreamingMusicService.resumeSavedPlaylist(this);
		
		// Get initial view state from playlist controller if it is active
		if (StreamingMusicService.getPlaylistController() != null) {
			final FilePlayer filePlayer = StreamingMusicService.getPlaylistController().getCurrentFilePlayer();
			
			setView(filePlayer.getFile());
			mPlay.setVisibility(filePlayer.isPlaying() ?  View.INVISIBLE : View.VISIBLE);
			mPause.setVisibility(filePlayer.isPlaying() ? View.VISIBLE : View.INVISIBLE);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnection.restoreSessionConnection(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_now_playing, menu);
		setRepeatingIcon(menu.findItem(R.id.menu_repeat_playlist));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_connection_settings:
				startActivity(new Intent(this, SelectServer.class));
				return true;
			case R.id.menu_repeat_playlist:
				final Context _context = this;
				final MenuItem _item = item;
				LibrarySession.GetLibrary(this, new OnCompleteListener<Integer, Void, Library>() {

					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
						if (result == null) return;
						StreamingMusicService.setIsRepeating(_context, !result.isRepeating());
						setRepeatingIcon(_item);
					}
				});
				return true;
			case R.id.menu_view_now_playing_files:
				startActivity(new Intent(this, ViewNowPlayingFiles.class));
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
					item.setIcon(result.isRepeating() ? R.drawable.av_repeat_dark : R.drawable.av_no_repeat_dark);
			}
			
		});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mViewStructure != null) mViewStructure.release();
		
		if (mHideTimer != null) {
			mHideTimer.cancel();
			mHideTimer.purge();
		}
		
		if (mTrackerTask != null) mTrackerTask.cancel(false);
		
		StreamingMusicService.removeOnStreamingStartListener(this);
		StreamingMusicService.removeOnStreamingChangeListener(this);
		StreamingMusicService.removeOnStreamingPauseListener(this);
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
	
	private void setView(final File file) {
		
		try {
			
			if (mViewStructure != null && mViewStructure.fileKey != file.getKey()) {
				mViewStructure.release();
				mViewStructure = null;
			}
			
			if (mViewStructure == null)
				mViewStructure = new ViewStructure(file);
			

			if (mViewStructure.nowPlayingImage == null) {
				try {				
					// Cancel the getFileImageTask if it is already in progress
					if (getFileImageTask != null)
						getFileImageTask.cancel();
					
					mNowPlayingImageView.setVisibility(View.INVISIBLE);
					mLoadingImg.setVisibility(View.VISIBLE);
					
					getFileImageTask = ImageAccess.getImage(this, file, new OnCompleteListener<Void, Void, Bitmap>() {
						
						@Override
						public void onComplete(ISimpleTask<Void, Void, Bitmap> owner, Bitmap result) {
							if (mViewStructure.nowPlayingImage != null)
								mViewStructure.nowPlayingImage.recycle();
							mViewStructure.nowPlayingImage = result;
							
							mNowPlayingImageView.setImageBitmap(result);
							
							displayImageBitmap();
						}
					});
					
				} catch (Exception e) {
					LoggerFactory.getLogger(getClass()).error(e.toString(), e);
				}
			} else {
				mNowPlayingImageView.setImageBitmap(mViewStructure.nowPlayingImage);
				displayImageBitmap();
			}
			
			final AsyncTask<Void, Void, String> getArtistTask = new AsyncExceptionTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					if (mViewStructure.nowPlayingArtist != null)
						return mViewStructure.nowPlayingArtist;
					
					try {
						return file.getProperty("Artist");
					} catch (IOException e) {
						setException(e);
						return null;
					}
				}
			
				@Override
				protected void onPostExecute(String result, Exception exception) {
					if (hasError() && exception instanceof IOException) {
						resetViewOnReconnect(file);
						return;
					}
					
					mNowPlayingArtist.setText(result);
					mViewStructure.nowPlayingArtist = result;
				}
			};
			getArtistTask.execute();
			
			final AsyncExceptionTask<Void, Void, String> getTitleTask = new AsyncExceptionTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					if (mViewStructure.nowPlayingTitle != null)
						return mViewStructure.nowPlayingTitle;
					
					return file.getValue();
				}
				
				@Override
				protected void onPostExecute(String result, Exception exception) {
					if (hasError() && exception instanceof IOException) {
						resetViewOnReconnect(file);
						return;
					}
					
					mNowPlayingTitle.setText(result);
					mViewStructure.nowPlayingTitle = result;
				}
			};
			getTitleTask.execute();
			
			final AsyncExceptionTask<Void, Void, Float> getRatingsTask = new AsyncExceptionTask<Void, Void, Float>() {

				@Override
				protected Float doInBackground(Void... params) {
					if (mViewStructure.nowPlayingRating != null)
						return mViewStructure.nowPlayingRating;
					
					try {
						if (file.getProperty(FileProperties.RATING) != null && !file.getProperty(FileProperties.RATING).isEmpty())
							return Float.valueOf(file.getProperty(FileProperties.RATING));
					} catch (NumberFormatException | IOException e) {
						setException(e);
						
						return Float.valueOf(0f);
					}
					
					return Float.valueOf(0f);
				}
				
				@Override
				protected void onPostExecute(Float result, Exception exception) {
					if (hasError() && exception instanceof IOException) {
						resetViewOnReconnect(file);
						return;
					}
					
					mViewStructure.nowPlayingRating = Float.valueOf(result);
					
					mSongRating.setRating(mViewStructure.nowPlayingRating);
					mSongRating.invalidate();
					
					mSongRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
						
						@Override
						public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
							if (!fromUser || !mControlNowPlaying.isShown()) return;
							file.setProperty(FileProperties.RATING, String.valueOf(Math.round(rating)));
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
	
	private final <TParams, TProgress, TResult> void addOnErrorListener(final ISimpleTask<TParams, TProgress, TResult> simpleTask, final File file) {
		simpleTask.addOnErrorListener(new OnErrorListener<TParams, TProgress, TResult>() {

			@Override
			public boolean onError(ISimpleTask<TParams, TProgress, TResult> owner, boolean isHandled, Exception innerException) {
				if (isHandled || !(innerException instanceof IOException)) return false;
				
				resetViewOnReconnect(file);
				return true;
			}
		});
	}
		
	private void displayImageBitmap() {
		mNowPlayingImageView.setScaleType(ScaleType.CENTER_CROP);
		mLoadingImg.setVisibility(View.INVISIBLE);
		mNowPlayingImageView.setVisibility(View.VISIBLE);	
	}
	
	private void showNowPlayingControls() {
		final PlaylistController playlistController = StreamingMusicService.getPlaylistController();
		if (playlistController != null)
			showNowPlayingControls(playlistController.getCurrentFilePlayer());
	}
	
	private void showNowPlayingControls(final FilePlayer filePlayer) {
		if (mTrackerTask != null) mTrackerTask.cancel(false);
		mTrackerTask = ProgressTrackerTask.trackProgress(filePlayer, mHandler);
		
		mControlNowPlaying.setVisibility(View.VISIBLE);
		mContentView.invalidate();
		if (mTimerTask != null) mTimerTask.cancel();
		mHideTimer.purge();
		mTimerTask = new TimerTask() {
			
			@Override
			public void run() {
				final Message msg = new Message();
				msg.what = HandleViewNowPlayingMessages.HIDE_CONTROLS;
				mHandler.sendMessage(msg);
				if (mTrackerTask != null) mTrackerTask.cancel(false);
			}
		};
		mHideTimer.schedule(mTimerTask, 5000);
	}
	
	private void resetViewOnReconnect(final File file) {
		PollConnection.Instance.get(this).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
			
			@Override
			public void onConnectionRegained() {
				setView(file);
			}
		});
		WaitForConnectionDialog.show(this);
	}

	@Override
	public void onNowPlayingChange(PlaylistController controller, FilePlayer filePlayer) {		
		setView(filePlayer.getFile());
		mSongProgressBar.setProgress(filePlayer.getCurrentPosition());
	}
	
	@Override
	public void onNowPlayingStart(PlaylistController controller, FilePlayer filePlayer) {		
		showNowPlayingControls(filePlayer);
		
		mPlay.setVisibility(View.INVISIBLE);
		mPause.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onNowPlayingPause(PlaylistController controller, FilePlayer filePlayer) {
		handleNowPlayingStopping(controller, filePlayer);
	}

	@Override
	public void onNowPlayingStop(PlaylistController controller, FilePlayer filePlayer) {
		handleNowPlayingStopping(controller, filePlayer);
	}
	
	private void handleNowPlayingStopping(PlaylistController controller, FilePlayer filePlayer) {
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
