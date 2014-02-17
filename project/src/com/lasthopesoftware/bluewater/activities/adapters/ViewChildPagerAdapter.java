package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.ArrayList;
import java.util.Locale;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.lasthopesoftware.bluewater.activities.fragments.CategoryFragment;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the primary sections of the app.
 */
public class ViewChildPagerAdapter extends  FragmentStatePagerAdapter {
	private ArrayList<IJrItem<?>> mLibraryViews;
	private ArrayList<CategoryFragment> fragments;
	
	public ViewChildPagerAdapter(FragmentManager fm) {
		super(fm);
		mLibraryViews = new ArrayList<IJrItem<?>>();
	}
		
	public void setLibraryViews(ArrayList<IJrItem<?>> libraryViews) {
		mLibraryViews = libraryViews;
		fragments = new ArrayList<CategoryFragment>(libraryViews.size());
	}

	@Override
	public Fragment getItem(int i) {
		if (fragments.size() <= i) {
			CategoryFragment fragment = new CategoryFragment();
			Bundle args = new Bundle();
			args.putInt(CategoryFragment.ARG_CATEGORY_POSITION, i);
			fragment.setArguments(args);
			fragments.add(fragment);
		}
		
		return fragments.get(i);
	}

	@Override
	public int getCount() {
		return getPages().size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return !mLibraryViews.get(position).getValue().isEmpty() ? mLibraryViews.get(position).getValue().toUpperCase(Locale.ENGLISH) : "";
	}

	public ArrayList<IJrItem<?>> getPages() {
		return mLibraryViews;
	}
}
