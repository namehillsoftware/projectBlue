package com.lasthopesoftware.bluewater.servers.library.items.media.files.list;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewFlipListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewFlippedListener;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.IDataTask;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

public class SearchFilesActivity extends FragmentActivity {

	private ProgressBar pbLoading;
	private ListView fileListView;

    private ViewFlipper mFlippedView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        fileListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        
        handleIntent(getIntent());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
	
	@SuppressWarnings("unchecked")
	private void handleIntent(Intent intent) {
		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) return;
        
		final String query = intent.getStringExtra(SearchManager.QUERY);
        if (query == null || query.isEmpty()) return;

        setTitle(String.format(getString(R.string.title_activity_search_results), query));
        
		final Files filesContainer = new Files("Files/Search", "Query=[Media Type]=[Audio] " + query);
        final SearchFilesActivity _this = this;
        filesContainer.setOnFilesCompleteListener(new IDataTask.OnCompleteListener<List<IFile>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<IFile>> owner, List<IFile> result) {
				if (result == null) return;
				
				final FileListAdapter fileListAdapter = new FileListAdapter(_this, R.id.tvStandard, result);
                final LongClickViewFlipListener longClickViewFlipListener = new LongClickViewFlipListener();
                longClickViewFlipListener.setOnViewFlipped(new OnViewFlippedListener() {
                    @Override
                    public void onViewFlipped(ViewFlipper viewFlipper) {
                        mFlippedView = viewFlipper;
                    }
                });
                fileListView.setOnItemLongClickListener(longClickViewFlipListener);
		    	fileListView.setAdapter(fileListAdapter);
		    	
		    	
			}
		});
        
        filesContainer.setOnFilesErrorListener(new HandleViewIoException(_this, new OnConnectionRegainedListener() {
			
				@Override
				public void onConnectionRegained() {
					filesContainer.getFilesAsync();
				}
			})
        );
                
        fileListView.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.INVISIBLE);
        
        filesContainer.getFilesAsync();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}

    @Override
    public void onBackPressed() {
        if (LongClickViewFlipListener.tryFlipToPreviousView(mFlippedView)) return;

        super.onBackPressed();
    }
}
