package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import android.app.Activity;
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
import com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers.ProgressTrackerThread;
import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.data.service.access.ImageTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ViewNowPlaying extends Activity implements 
	OnNowPlayingChangeListener, 
	OnNowPlayingStopListener,
	OnNowPlayingStartListener,
	ISimpleTask.OnStartListener<String, Void, Void>
{
	private Thread mTrackerThread;
	private HandleViewNowPlayingMessages mHandler;
	private ImageButton mPlay;
	private ImageButton mPause;
	private ImageButton mNext;
	private ImageButton mPrevious;
	private RatingBar mSongRating;
	private static FrameLayout mContentView;
	private static RelativeLayout mControlNowPlaying, mViewCoverArt;
	private Timer mHideTimer;
	private TimerTask mTimerTask;
	
	private ProgressBar mSongProgressBar;
	private ProgressBar mLoadingImg;
	private ImageView mNowPlayingImg;
	private TextView mNowPlayingArtist;
	private TextView mNowPlayingTitle;
	private static ImageTask getFileImageTask;
	
	private FilePlayer mFilePlayer = null;
	
	private Library mLibrary;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mLibrary = JrSession.GetLibrary(this);
		
		mContentView = new FrameLayout(this);
		setContentView(mContentView);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mViewCoverArt = (RelativeLayout) inflater.inflate(R.layout.activity_view_cover_art, null);
		mControlNowPlaying = (RelativeLayout) inflater.inflate(R.layout.activity_control_now_playing, null);
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
		mNowPlayingImg = (ImageView) findViewById(R.id.imgNowPlaying);
		mNowPlayingArtist = (TextView) findViewById(R.id.tvSongArtist);
		mNowPlayingTitle = (TextView) findViewById(R.id.tvSongTitle);
		
		StreamingMusicService.addOnStreamingChangeListener(this);
		StreamingMusicService.addOnStreamingStopListener(this);
		StreamingMusicService.addOnStreamingStartListener(this);
		PollConnectionTask.Instance.get(this).addOnStartListener(this);
		
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
			mFilePlayer = StreamingMusicService.getPlaylistController().getCurrentFilePlayer();
					
			if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();
	
			mTrackerThread = new Thread(new ProgressTrackerThread(mFilePlayer, mHandler));
			mTrackerThread.setPriority(Thread.MIN_PRIORITY);
			mTrackerThread.setName("Song Progress Tracker Thread");
			mTrackerThread.start();
			
			setView(mFilePlayer.getFile());
			mPlay.setVisibility(mFilePlayer.isPlaying() ?  View.INVISIBLE : View.VISIBLE);
			mPause.setVisibility(mFilePlayer.isPlaying() ? View.VISIBLE : View.INVISIBLE);
			return;
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
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
				StreamingMusicService.setIsRepeating(this, !mLibrary.isRepeating());
				setRepeatingIcon(item);
				return true;
			case R.id.menu_view_now_playing_files:
				startActivity(new Intent(this, ViewNowPlayingFiles.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void setRepeatingIcon(MenuItem item) {
		item.setIcon(mLibrary.isRepeating() ? R.drawable.av_repeat_dark : R.drawable.av_no_repeat_dark);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mHideTimer != null) {
			mHideTimer.cancel();
			mHideTimer.purge();
		}
		
		if (mTrackerThread != null) mTrackerThread.interrupt();
		StreamingMusicService.removeOnStreamingStartListener(this);
		StreamingMusicService.removeOnStreamingChangeListener(this);
		StreamingMusicService.removeOnStreamingStopListener(this);
		PollConnectionTask.Instance.get(this).removeOnStartListener(this);
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
	
	@SuppressWarnings("unchecked")
	private void setView(File file) {
		final File _file = file;
				
		try {
			@SuppressWarnings("rawtypes")
			final OnErrorListener onSimpleIoExceptionErrors = new OnErrorListener() {
				
				@Override
				public boolean onError(ISimpleTask owner, Exception innerException) {
					return !(innerException instanceof IOException);
				}
			};
			
			final SimpleTask<Void, Void, String> getArtistTask = new SimpleTask<Void, Void, String>();
			getArtistTask.setOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
				
				@Override
				public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
					return _file.getProperty("Artist");
				}
			});
			getArtistTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(_file);
						return;
					}
					
					mNowPlayingArtist.setText(result);
				}
			});
			getArtistTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getArtistTask.execute();
			
			final SimpleTask<Void, Void, String> getTitleTask = new SimpleTask<Void, Void, String>();
			getTitleTask.setOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
				
				@Override
				public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
					return _file.getValue();
				}
			});
			getTitleTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(_file);
						return;
					}
					
					mNowPlayingTitle.setText(result);
				}
			});
			getTitleTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getTitleTask.execute();
			
			final SimpleTask<Void, Void, String> getAlbumTask = new SimpleTask<Void, Void, String>();
			getAlbumTask.setOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
				
				@Override
				public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
					if (_file.getProperty("Album") != null)
						return _file.getProperty("Artist") + ":" + _file.getProperty("Album");
					
					return null;
				}
			});
			getAlbumTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(_file);
						return;
					}
					
					try {			
						// Cancel the getFileImageTask if it is already in progress
						if (getFileImageTask != null && (getFileImageTask.getStatus() == AsyncTask.Status.PENDING || getFileImageTask.getStatus() == AsyncTask.Status.RUNNING)) {
							getFileImageTask.cancel(true);
						}
						
						mNowPlayingImg.setVisibility(View.INVISIBLE);
						mLoadingImg.setVisibility(View.VISIBLE);
						getFileImageTask = new ImageTask(result == null ? String.valueOf(_file.getKey()) : result, _file.getKey());
						getFileImageTask.addOnCompleteListener(new OnCompleteListener<Void, Void, Bitmap>() {
							
							@Override
							public void onComplete(ISimpleTask<Void, Void, Bitmap> owner, Bitmap result) {
								mNowPlayingImg.setImageBitmap(result);
								mNowPlayingImg.setScaleType(ScaleType.CENTER_CROP);
								mLoadingImg.setVisibility(View.INVISIBLE);
								mNowPlayingImg.setVisibility(View.VISIBLE);
							}
						});
						
						getFileImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					} catch (Exception e) {
						LoggerFactory.getLogger(ViewNowPlaying.class).error(e.toString(), e);
					}
				}
			});
			getAlbumTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getAlbumTask.execute();
			
			final SimpleTask<Void, Void, Float> getRatingsTask = new SimpleTask<Void, Void, Float>();
			getRatingsTask.setOnExecuteListener(new OnExecuteListener<Void, Void, Float>() {
				
				@Override
				public Float onExecute(ISimpleTask<Void, Void, Float> owner, Void... params) throws Exception {
					
					if (_file.getProperty("Rating") != null && !_file.getProperty("Rating").isEmpty())
						return Float.valueOf(_file.getProperty("Rating"));
					
					return (float) 0;
				}
			});
			getRatingsTask.addOnCompleteListener(new OnCompleteListener<Void, Void, Float>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, Float> owner, Float result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(_file);
						return;
					}
					
					mSongRating.setRating(result);
					mSongRating.invalidate();
					
					mSongRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
						
						@Override
						public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
							if (!fromUser || !mControlNowPlaying.isShown()) return;
							_file.setProperty("Rating", String.valueOf(Math.round(rating)));
						}
					});
				}
			});
			getRatingsTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getRatingsTask.execute();
			
			
			mSongProgressBar.setMax(_file.getDuration());
			
		} catch (IOException ioE) {
			resetViewOnReconnect(_file);
		}
	}
	
	private void showNowPlayingControls() {
		mControlNowPlaying.setVisibility(View.VISIBLE);
		mContentView.invalidate();
		if (mTimerTask != null) mTimerTask.cancel();
		mHideTimer.purge();
		mTimerTask = new TimerTask() {
			
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = HandleViewNowPlayingMessages.HIDE_CONTROLS;
				mHandler.sendMessage(msg);
			}
		};
		mHideTimer.schedule(mTimerTask, 5000);
	}
	
	private boolean containsIoException(List<Exception> exceptions) {
		for (Exception exception : exceptions)
			if (exception instanceof IOException) return true;
		
		return false;
	}
	
	private void resetViewOnReconnect(File file) {
		final File _file = file;
		PollConnectionTask.Instance.get(this).addOnCompleteListener(new OnCompleteListener<String, Void, Void>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, Void> owner, Void result) {
				setView(_file);
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
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();

		mTrackerThread = new Thread(new ProgressTrackerThread(filePlayer, mHandler));
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
		
		showNowPlayingControls();
		
		mPlay.setVisibility(View.INVISIBLE);
		mPause.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onNowPlayingStop(PlaylistController controller, FilePlayer file) {
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();
		
		try {
			mSongProgressBar.setMax(file.getDuration());
			mSongProgressBar.setProgress(file.getCurrentPosition());
		} catch (IOException e) {
			LoggerFactory.getLogger(ViewNowPlaying.class).error(e.getMessage(), e);
		}
		
		mPlay.setVisibility(View.VISIBLE);
		mPause.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onStart(ISimpleTask<String, Void, Void> owner) {
		WaitForConnectionDialog.show(this);
	}
}
