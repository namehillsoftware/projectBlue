package com.lasthopesoftware.bluewater.servers.library;

import java.util.ArrayList;
import java.util.Locale;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the primary sections of the app.
 */
public class LibraryViewPagerAdapter extends  FragmentStatePagerAdapter {
	private ArrayList<IItem> mLibraryViews;
	private CategoryFragment[] mFragments;
	
	public LibraryViewPagerAdapter(FragmentManager fm) {
		super(fm);
		mLibraryViews = new ArrayList<IItem>();
		mFragments = new CategoryFragment[0];
	}
		
	public void setLibraryViews(ArrayList<IItem> libraryViews) {
		mLibraryViews = libraryViews;
		mFragments = new CategoryFragment[libraryViews.size()];
	}

	@Override
	public Fragment getItem(int i) {
		CategoryFragment returnFragment = mFragments[i];
		
		if (returnFragment == null) {
			returnFragment = new CategoryFragment();
			final Bundle args = new Bundle();
			args.putInt(CategoryFragment.ARG_CATEGORY_POSITION, i);
			returnFragment.setArguments(args);
			mFragments[i] = returnFragment;
		}
		
		return returnFragment;
	}

	@Override
	public int getCount() {
		return mFragments.length;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return !mLibraryViews.get(position).getValue().isEmpty() ? mLibraryViews.get(position).getValue().toUpperCase(Locale.ENGLISH) : "";
	}

	public ArrayList<IItem> getPages() {
		return mLibraryViews;
	}
}
