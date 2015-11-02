package com.lasthopesoftware.bluewater.servers.library;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewChangedListener;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A {@link LibraryViewPagerAdapter} that returns a fragment corresponding to
 * one of the primary sections of the app.
 */
public class LibraryViewPagerAdapter extends  FragmentStatePagerAdapter {
	private ArrayList<IItem> mLibraryViews = new ArrayList<>();
    private OnViewChangedListener onViewChangedListener;
	private Runnable onAnyMenuShown;
	private Runnable onAllMenusHidden;

	public LibraryViewPagerAdapter(FragmentManager fm) {
		super(fm);
	}
		
	public void setLibraryViews(ArrayList<IItem> libraryViews) {
		mLibraryViews = libraryViews;
	}

	@Override
	public Fragment getItem(int i) {
        // The position correlates to the ID returned by the server at the high-level Library views
        final LibraryViewFragment libraryViewFragment = LibraryViewFragment.getPreparedFragment(i);
        if (onViewChangedListener != null) libraryViewFragment.setOnViewChangedListener(onViewChangedListener);
		if (onAllMenusHidden != null) libraryViewFragment.setOnAllMenusHidden(onAllMenusHidden);
		if (onAnyMenuShown != null) libraryViewFragment.setOnAnyMenuShown(onAnyMenuShown);

		return libraryViewFragment;
	}

	@Override
	public int getCount() {
		return mLibraryViews.size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return !mLibraryViews.get(position).getValue().isEmpty() ? mLibraryViews.get(position).getValue().toUpperCase(Locale.ENGLISH) : "";
	}


    public void setOnViewChangedListener(OnViewChangedListener onViewChangedListener) {
        this.onViewChangedListener = onViewChangedListener;
    }

	public void setOnAnyMenuShown(Runnable onAnyMenuShown) {
		this.onAnyMenuShown = onAnyMenuShown;
	}

	public void setOnAllMenusHidden(Runnable onAllMenusHidden) {
		this.onAllMenusHidden = onAllMenusHidden;
	}
}
