package com.lasthopesoftware.bluewater.databinding;
import com.lasthopesoftware.bluewater.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityViewNowPlayingBindingImpl extends ActivityViewNowPlayingBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = new androidx.databinding.ViewDataBinding.IncludedLayouts(4);
        sIncludes.setIncludes(0,
            new String[] {"activity_view_cover_art"},
            new int[] {2},
            new int[] {com.lasthopesoftware.bluewater.R.layout.activity_view_cover_art});
        sIncludes.setIncludes(1,
            new String[] {"activity_control_now_playing"},
            new int[] {3},
            new int[] {com.lasthopesoftware.bluewater.R.layout.control_now_playing_top_sheet});
        sViewsWithIds = null;
    }
    // views
    @NonNull
    private final android.widget.RelativeLayout mboundView1;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityViewNowPlayingBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 4, sIncludes, sViewsWithIds));
    }
    private ActivityViewNowPlayingBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 2
            , (com.lasthopesoftware.bluewater.databinding.ActivityControlNowPlayingBinding) bindings[3]
            , (com.lasthopesoftware.bluewater.databinding.ActivityViewCoverArtBinding) bindings[2]
            , (android.widget.RelativeLayout) bindings[0]
            );
        setContainedBinding(this.control);
        setContainedBinding(this.coverArt);
        this.mboundView1 = (android.widget.RelativeLayout) bindings[1];
        this.mboundView1.setTag(null);
        this.viewNowPlayingRelativeLayout.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x10L;
        }
        coverArt.invalidateAll();
        control.invalidateAll();
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        if (coverArt.hasPendingBindings()) {
            return true;
        }
        if (control.hasPendingBindings()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.vm == variableId) {
            setVm((com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingViewModel) variable);
        }
        else if (BR.coverArtVm == variableId) {
            setCoverArtVm((com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setVm(@Nullable com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingViewModel Vm) {
        this.mVm = Vm;
        synchronized(this) {
            mDirtyFlags |= 0x4L;
        }
        notifyPropertyChanged(BR.vm);
        super.requestRebind();
    }
    public void setCoverArtVm(@Nullable com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel CoverArtVm) {
        this.mCoverArtVm = CoverArtVm;
        synchronized(this) {
            mDirtyFlags |= 0x8L;
        }
        notifyPropertyChanged(BR.coverArtVm);
        super.requestRebind();
    }

    @Override
    public void setLifecycleOwner(@Nullable androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        super.setLifecycleOwner(lifecycleOwner);
        coverArt.setLifecycleOwner(lifecycleOwner);
        control.setLifecycleOwner(lifecycleOwner);
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeCoverArt((com.lasthopesoftware.bluewater.databinding.ActivityViewCoverArtBinding) object, fieldId);
            case 1 :
                return onChangeControl((com.lasthopesoftware.bluewater.databinding.ActivityControlNowPlayingBinding) object, fieldId);
        }
        return false;
    }
    private boolean onChangeCoverArt(com.lasthopesoftware.bluewater.databinding.ActivityViewCoverArtBinding CoverArt, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeControl(com.lasthopesoftware.bluewater.databinding.ActivityControlNowPlayingBinding Control, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x2L;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingViewModel vm = mVm;
        com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingCoverArtViewModel coverArtVm = mCoverArtVm;

        if ((dirtyFlags & 0x14L) != 0) {
        }
        if ((dirtyFlags & 0x18L) != 0) {
        }
        // batch finished
        if ((dirtyFlags & 0x14L) != 0) {
            // api target 1

            this.control.setVm(vm);
        }
        if ((dirtyFlags & 0x18L) != 0) {
            // api target 1

            this.coverArt.setVm(coverArtVm);
        }
        executeBindingsOn(coverArt);
        executeBindingsOn(control);
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): coverArt
        flag 1 (0x2L): control
        flag 2 (0x3L): vm
        flag 3 (0x4L): coverArtVm
        flag 4 (0x5L): null
    flag mapping end*/
    //end
}
