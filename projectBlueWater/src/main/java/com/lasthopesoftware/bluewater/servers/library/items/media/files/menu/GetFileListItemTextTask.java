package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.os.AsyncTask;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;

import java.util.concurrent.ExecutionException;

/**
 * Created by david on 4/14/15.
 */
public class GetFileListItemTextTask extends AsyncTask<Void, Void, String> {

    private final IFile file;
    private final TextView textView;

    public GetFileListItemTextTask(IFile file, TextView textView) {
        this.file = file;
        this.textView = textView;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        textView.setText(R.string.lbl_loading);
    }

    @Override
    protected String doInBackground(Void... params) {
        if (isCancelled()) return null;

        final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), file.getKey());
        try {
            return !isCancelled() ? filePropertiesProvider.get().get(FilePropertiesProvider.NAME) : null;
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        if (s != null) textView.setText(s);
    }
}
