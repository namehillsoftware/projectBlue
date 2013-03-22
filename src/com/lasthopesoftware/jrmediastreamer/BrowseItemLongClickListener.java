package com.lasthopesoftware.jrmediastreamer;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class BrowseItemLongClickListener implements OnItemLongClickListener {

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ViewFlipper parentView = (ViewFlipper)view;
		parentView.showNext();
		return true;
	}
	
	

}
