package com.lasthopesoftware.bluewater;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import androidx.databinding.DataBinderMapper;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.ViewDataBinding;
import com.lasthopesoftware.bluewater.databinding.ActivityControlNowPlayingBindingImpl;
import com.lasthopesoftware.bluewater.databinding.ActivityViewCoverArtBindingImpl;
import com.lasthopesoftware.bluewater.databinding.ActivityViewNowPlayingBindingImpl;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataBinderMapperImpl extends DataBinderMapper {
  private static final int LAYOUT_ACTIVITYCONTROLNOWPLAYING = 1;

  private static final int LAYOUT_ACTIVITYVIEWCOVERART = 2;

  private static final int LAYOUT_ACTIVITYVIEWNOWPLAYING = 3;

  private static final SparseIntArray INTERNAL_LAYOUT_ID_LOOKUP = new SparseIntArray(3);

  static {
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.lasthopesoftware.bluewater.R.layout.control_now_playing_top_sheet, LAYOUT_ACTIVITYCONTROLNOWPLAYING);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.lasthopesoftware.bluewater.R.layout.activity_view_cover_art, LAYOUT_ACTIVITYVIEWCOVERART);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.lasthopesoftware.bluewater.R.layout.activity_view_now_playing, LAYOUT_ACTIVITYVIEWNOWPLAYING);
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = view.getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
        case  LAYOUT_ACTIVITYCONTROLNOWPLAYING: {
          if ("layout/activity_control_now_playing_0".equals(tag)) {
            return new ActivityControlNowPlayingBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_control_now_playing is invalid. Received: " + tag);
        }
        case  LAYOUT_ACTIVITYVIEWCOVERART: {
          if ("layout/activity_view_cover_art_0".equals(tag)) {
            return new ActivityViewCoverArtBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_view_cover_art is invalid. Received: " + tag);
        }
        case  LAYOUT_ACTIVITYVIEWNOWPLAYING: {
          if ("layout/activity_view_now_playing_0".equals(tag)) {
            return new ActivityViewNowPlayingBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_view_now_playing is invalid. Received: " + tag);
        }
      }
    }
    return null;
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
    if(views == null || views.length == 0) {
      return null;
    }
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = views[0].getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
      }
    }
    return null;
  }

  @Override
  public int getLayoutId(String tag) {
    if (tag == null) {
      return 0;
    }
    Integer tmpVal = InnerLayoutIdLookup.sKeys.get(tag);
    return tmpVal == null ? 0 : tmpVal;
  }

  @Override
  public String convertBrIdToString(int localId) {
    String tmpVal = InnerBrLookup.sKeys.get(localId);
    return tmpVal;
  }

  @Override
  public List<DataBinderMapper> collectDependencies() {
    ArrayList<DataBinderMapper> result = new ArrayList<DataBinderMapper>(1);
    result.add(new androidx.databinding.library.baseAdapters.DataBinderMapperImpl());
    return result;
  }

  private static class InnerBrLookup {
    static final SparseArray<String> sKeys = new SparseArray<String>(3);

    static {
      sKeys.put(0, "_all");
      sKeys.put(1, "coverArtVm");
      sKeys.put(2, "vm");
    }
  }

  private static class InnerLayoutIdLookup {
    static final HashMap<String, Integer> sKeys = new HashMap<String, Integer>(3);

    static {
      sKeys.put("layout/activity_control_now_playing_0", com.lasthopesoftware.bluewater.R.layout.control_now_playing_top_sheet);
      sKeys.put("layout/activity_view_cover_art_0", com.lasthopesoftware.bluewater.R.layout.activity_view_cover_art);
      sKeys.put("layout/activity_view_now_playing_0", com.lasthopesoftware.bluewater.R.layout.activity_view_now_playing);
    }
  }
}
