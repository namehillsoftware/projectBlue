package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewFileDetails;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener.OnSwipeRightListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class FileListAdapter extends ArrayAdapter<File> {
	
	private List<File> mFiles;
	
	public FileListAdapter(Context context, int resource, List<File> files) {
		super(context, resource, files);
		
		mFiles = files;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final ViewFlipper parentView = new ViewFlipper(parent.getContext());
		parentView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		final  OnSwipeListener onSwipeListener = new OnSwipeListener(parentView.getContext());
		onSwipeListener.setOnSwipeRightListener(new OnSwipeRightListener() {
			
			@Override
			public boolean onSwipeRight(View view) {
				parentView.showPrevious();
				return true;
			}
		});
		parentView.setOnTouchListener(onSwipeListener);
		        
		final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final RelativeLayout rl = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, null);
		final TextView textView = (TextView) rl.findViewById(R.id.tvStandard);
		final File file = getItem(position);
		
        textView.setMarqueeRepeatLimit(1);
        textView.setText(rl.getContext().getText(R.string.lbl_loading));
        GetFileValueTask.getFileValue(position, file, (ListView)parent, textView);
        
        final OnNowPlayingStartListener checkIfIsPlayingFileListener = new OnNowPlayingStartListener() {
			
			@Override
			public void onNowPlayingStart(PlaylistController controller, FilePlayer filePlayer) {
				textView.setTypeface(null, filePlayer.getFile().getKey() == file.getKey() ? Typeface.BOLD : Typeface.NORMAL);
			}
		};
		
		rl.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
			
			@Override
			public void onViewDetachedFromWindow(View v) {
				StreamingMusicService.removeOnStreamingStartListener(checkIfIsPlayingFileListener);
			}
			
			@Override
			public void onViewAttachedToWindow(View v) {
				if (StreamingMusicService.getPlaylistController() == null)
					StreamingMusicService.resumeSavedPlaylist(v.getContext());
				
				final PlaylistController playlistController = StreamingMusicService.getPlaylistController();
		        if (playlistController != null && playlistController.getCurrentFilePlayer() != null && playlistController.getCurrentFilePlayer().getFile().getKey() == file.getKey())
		        	textView.setTypeface(null, Typeface.BOLD);
		        
				StreamingMusicService.addOnStreamingStartListener(checkIfIsPlayingFileListener);
			}
		});
        
		parentView.addView(rl);
		
		final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_file_item_menu, null);
        fileMenu.setOnTouchListener(onSwipeListener);
        
        final ImageButton addButton = (ImageButton)fileMenu.findViewById(R.id.btnAddToPlaylist);
        addButton.setOnClickListener(new AddClickHandler(file));
        addButton.setOnTouchListener(onSwipeListener);
        
        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
        playButton.setOnClickListener(new PlayClickHandler(position, mFiles));
        playButton.setOnTouchListener(onSwipeListener);
        
        final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);
        viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickHandler(file));
        viewFileDetailsButton.setOnTouchListener(onSwipeListener);
		
		parentView.addView(fileMenu);
        
		return parentView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private List<File> mFiles;
		private int mPosition;
		
		public PlayClickHandler(int position, List<File> files) {
			mPosition = position;
			mFiles = files;
		}
		
		@Override
		public void onClick(View v) {
			StreamingMusicService.streamMusic(v.getContext(), mPosition, Files.serializeFileStringList(mFiles));
		}
	}
	
	private static class AddClickHandler implements OnClickListener {
		private File mFile;
		
		public AddClickHandler(File file) {
			mFile = file;
		}
		
		@Override
		public void onClick(View v) {
			final Context _context = v.getContext();
			if (StreamingMusicService.getPlaylistController() == null) 
				StreamingMusicService.resumeSavedPlaylist(_context);
			
			StreamingMusicService.getPlaylistController().addFile(mFile);
			
			JrSession.GetLibrary(_context, new OnCompleteListener<Integer, Void, Library>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
					if (result == null) return;
					String newFileString = result.getSavedTracksString();
					if (!newFileString.endsWith(";")) newFileString += ";";
					newFileString += mFile.getKey() + ";";
					result.setSavedTracksString(newFileString);
					
					Toast.makeText(_context, _context.getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();;
				}
				
			});
			
		}
	}
	
	private static class ViewFileDetailsClickHandler implements OnClickListener {
		private File mFile;
		
		public ViewFileDetailsClickHandler(File file) {
			mFile = file;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), ViewFileDetails.class);
    		intent.putExtra(ViewFileDetails.FILE_KEY, mFile.getKey());
    		v.getContext().startActivity(intent);
		}
	}
	
	private static class GetFileValueTask extends AsyncTask<String, Void, String> {
		private int mPosition;
		private ListView mParentListView;
		private TextView mTextView;
		private File mFile;
		
		public static GetFileValueTask getFileValue(int position, File file, ListView parentListView, TextView textView) {
			return (GetFileValueTask) (new GetFileValueTask(position, file, parentListView, textView)).execute();
		}
		
		private GetFileValueTask(int position, File file, ListView parentListView, TextView textView) {
			mPosition = position;
			mParentListView = parentListView;
			mFile = file;
			mTextView = textView;
		}

		@Override
		protected String doInBackground(String... params) {
			if ((mPosition < mParentListView.getFirstVisiblePosition() - 10) || (mPosition > mParentListView.getLastVisiblePosition() + 10)) return null;
			return mFile.getValue();
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result == null) return;
			
			mTextView.setText(result);
		}
	}
}