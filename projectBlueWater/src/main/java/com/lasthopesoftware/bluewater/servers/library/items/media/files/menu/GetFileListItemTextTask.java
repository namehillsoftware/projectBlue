package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.os.AsyncTask;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

/**
 * Created by david on 4/14/15.
 */
public class GetFileListItemTextTask extends AsyncTask<Void, Void, String> {

    private final IFile mFile;
    private final TextView mTextView;

    public GetFileListItemTextTask(IFile file, TextView textView) {
        mFile = file;
        mTextView = textView;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mTextView.setText(R.string.lbl_loading);
    }

    @Override
    protected String doInBackground(Void... params) {
        return !isCancelled() ? mFile.getValue() : null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        if (s != null) mTextView.setText(s);
    }
}
