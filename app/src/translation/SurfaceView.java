/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import com.android.internal.view.BaseIWindow;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.CompatibilityInfo.Translator;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides a dedicated drawing surface embedded inside of a view hierarchy.
 * You can control the format of this surface and, if you like, its size; the
 * SurfaceView takes care of placing the surface at the correct location on the
 * screen
 * 提供一个专门的绘图画面嵌入到view里面
 * 你可以控制surface的格式和尺寸，surfaceView可以将surface放在屏幕上正确的位置
 *
 * <p>The surface is Z ordered so that it is behind the window holding its
 * SurfaceView; the SurfaceView punches a hole in its window to allow its
 * surface to be displayed. The view hierarchy will take care of correctly
 * compositing with the Surface any siblings of the SurfaceView that would
 * normally appear on top of it. This can be used to place overlays such as
 * buttons on top of the Surface, though note however that it can have an
 * impact on performance since a full alpha-blended composite will be performed
 * each time the Surface changes.
 * surface是有z值的，所以它处在SurfaceView所在窗口的后面
 * SurfaceView在窗口上开一个洞，这样surface才可以展示出来
 * view视图层会处理好SurfaceView里的surface，所以surface正常是显示在视图层的顶层
 * 这个可以被用来放置遮盖层，例如button的遮盖层
 * 注意:每当Surface改变的时候，带有透明度的组合会显示，所以会影响到外观
 *
 * <p> The transparent region that makes the surface visible is based on the
 * layout positions in the view hierarchy. If the post-layout transform
 * properties are used to draw a sibling view on top of the SurfaceView, the
 * view may not be properly composited with the surface.
 * 透明区域使得surface可见，这是基于布局的位置的，
 * 如果后布局转换属性用来绘制view，在SurfaceView的上面，这个view可能不能合适的和surface组合在一起
 *
 * <p>Access to the underlying surface is provided via the SurfaceHolder interface,
 * which can be retrieved by calling {@link #getHolder}.
 * 提供了接触到底层surface的方法：通过surfaceHolder接口，可以这个对象同过调用#getHolder()
 *
 * <p>The Surface will be created for you while the SurfaceView's window is
 * visible; you should implement {@link SurfaceHolder.Callback#surfaceCreated}
 * and {@link SurfaceHolder.Callback#surfaceDestroyed} to discover when the
 * Surface is created and destroyed as the window is shown and hidden.
 * 当SurfaceView所在窗口可见的时候，surface会被创建，你应该实现SurfaceHolder.Callback#surfaceCreated()
 * 方法，和SurfaceHolder.Callback#surfaceDestroyed()方法，来判定Surface随着窗口显示和隐藏而创建和销毁
 *
 * <p>One of the purposes of this class is to provide a surface in which a
 * secondary thread can render into the screen. If you are going to use it
 * this way, you need to be aware of some threading semantics:
 * 这个类的目的之一是提供一个surface，这样可以在另外的线程来在屏幕上渲染图形，如果你想这样使用，
 * 你要注意下面这些线程相关的东西
 *
 * <ul>
 * <li> All SurfaceView and
 * {@link SurfaceHolder.Callback SurfaceHolder.Callback} methods will be called
 * from the thread running the SurfaceView's window (typically the main thread
 * of the application).
 * 所有SurfaceView以及SurfaceHolder.Callback方法会在窗口所在的线程中调用（一般是应用的主线程）
 *
 * They thus need to correctly synchronize with any state that is also touched by the drawing thread.
 * <li> You must ensure that the drawing thread only touches the underlying
 * Surface while it is valid -- between
 * {@link SurfaceHolder.Callback#surfaceCreated SurfaceHolder.Callback.surfaceCreated()}
 * and
 * {@link SurfaceHolder.Callback#surfaceDestroyed SurfaceHolder.Callback.surfaceDestroyed()}.
 * </ul>
 * 所以需要正确的同步所有和绘制线程相关的状态
 * 当surface是有效个时候，一定要确认只有绘制线程能够操作surface
 *
 */
public class SurfaceView extends View {
    static private final String TAG = "SurfaceView";
    static private final boolean DEBUG = false;

    final ArrayList<SurfaceHolder.Callback> mCallbacks
            = new ArrayList<SurfaceHolder.Callback>();

    final int[] mLocation = new int[2];

    final ReentrantLock mSurfaceLock = new ReentrantLock();
    final Surface mSurface = new Surface();       // Current surface in use
    final Surface mNewSurface = new Surface();    // New surface we are switching to
    boolean mDrawingStopped = true;

    final WindowManager.LayoutParams mLayout
            = new WindowManager.LayoutParams();
    IWindowSession mSession;
    MyWindow mWindow;
    final Rect mVisibleInsets = new Rect();
    final Rect mWinFrame = new Rect();
    final Rect mOverscanInsets = new Rect();
    final Rect mContentInsets = new Rect();
    final Rect mStableInsets = new Rect();
    final Rect mOutsets = new Rect();
    final Configuration mConfiguration = new Configuration();

    static final int KEEP_SCREEN_ON_MSG = 1;
    static final int GET_NEW_SURFACE_MSG = 2;
    static final int UPDATE_WINDOW_MSG = 3;

    int mWindowType = WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA;

    boolean mIsCreating = false;

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KEEP_SCREEN_ON_MSG: {
                    setKeepScreenOn(msg.arg1 != 0);
                } break;
                case GET_NEW_SURFACE_MSG: {
                    handleGetNewSurface();
                } break;
                case UPDATE_WINDOW_MSG: {
                    updateWindow(false, false);
                } break;
            }
        }
    };

    final ViewTreeObserver.OnScrollChangedListener mScrollChangedListener
            = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            updateWindow(false, false);
        }
    };

    boolean mRequestedVisible = false;
    boolean mWindowVisibility = false;
    boolean mViewVisibility = false;
    int mRequestedWidth = -1;
    int mRequestedHeight = -1;
    /* Set SurfaceView's format to 565 by default to maintain backward
     * compatibility with applications assuming this format.
     * 设置SurfaceView的颜色格式默认为565,来保持向后的兼容性
     */
    int mRequestedFormat = PixelFormat.RGB_565;

    boolean mHaveFrame = false;
    boolean mSurfaceCreated = false;
    long mLastLockTime = 0;

    boolean mVisible = false;
    int mLeft = -1;
    int mTop = -1;
    int mWidth = -1;
    int mHeight = -1;
    int mFormat = -1;
    final Rect mSurfaceFrame = new Rect();
    int mLastSurfaceWidth = -1, mLastSurfaceHeight = -1;
    boolean mUpdateWindowNeeded;
    boolean mReportDrawNeeded;
    private Translator mTranslator;

    private final ViewTreeObserver.OnPreDrawListener mDrawListener =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // reposition ourselves where the surface is
                    mHaveFrame = getWidth() > 0 && getHeight() > 0;
                    updateWindow(false, false);
                    return true;
                }
            };
    private boolean mGlobalListenersAdded;

    public SurfaceView(Context context) {
        super(context);
        init();
    }

    public SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setWillNotDraw(true);
    }

    /**
     * Return the SurfaceHolder providing access and control over this
     * SurfaceView's underlying surface.
     * 返回SurfaceHolder，来接触和控制这个surfaceView的surface
     *
     * @return SurfaceHolder The holder of the surface.
     */
    public SurfaceHolder getHolder() {
        return mSurfaceHolder;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mParent.requestTransparentRegion(this);
        mSession = getWindowSession();
        mLayout.token = getWindowToken();
        mLayout.setTitle("SurfaceView");
        mViewVisibility = getVisibility() == VISIBLE;

        if (!mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnScrollChangedListener(mScrollChangedListener);
            observer.addOnPreDrawListener(mDrawListener);
            mGlobalListenersAdded = true;
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mWindowVisibility = visibility == VISIBLE;
        mRequestedVisible = mWindowVisibility && mViewVisibility;
        updateWindow(false, false);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mViewVisibility = visibility == VISIBLE;
        boolean newRequestedVisible = mWindowVisibility && mViewVisibility;
        if (newRequestedVisible != mRequestedVisible) {
            // our base class (View) invalidates the layout only when
            // we go from/to the GONE state. However, SurfaceView needs
            // to request a re-layout when the visibility changes at all.
            // This is needed because the transparent region is computed
            // as part of the layout phase, and it changes (obviously) when
            // the visibility changes.
            // 我们的view只有当状态（View.GONE）改变时，才会重绘，然而SurfaceView需要去重新布局
            // 当所有的可见状态改变时，这是因为透明区域组合到了布局层次，当可见状态改变时，他会可见的改变
            requestLayout();
        }
        mRequestedVisible = newRequestedVisible;
        updateWindow(false, false);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.removeOnScrollChangedListener(mScrollChangedListener);
            observer.removeOnPreDrawListener(mDrawListener);
            mGlobalListenersAdded = false;
        }

        mRequestedVisible = false;
        updateWindow(false, false);
        mHaveFrame = false;
        if (mWindow != null) {
            try {
                mSession.remove(mWindow);
            } catch (RemoteException ex) {
                // Not much we can do here...
            }
            mWindow = null;
        }
        mSession = null;
        mLayout.token = null;

        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = mRequestedWidth >= 0
                    ? resolveSizeAndState(mRequestedWidth, widthMeasureSpec, 0)
                    : getDefaultSize(0, widthMeasureSpec);
        int height = mRequestedHeight >= 0
                     ? resolveSizeAndState(mRequestedHeight, heightMeasureSpec, 0)
                     : getDefaultSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    /** @hide */
    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        boolean result = super.setFrame(left, top, right, bottom);
        updateWindow(false, false);
        return result;
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        if (mWindowType == WindowManager.LayoutParams.TYPE_APPLICATION_PANEL) {
            return super.gatherTransparentRegion(region);
        }

        boolean opaque = true;
        if ((mPrivateFlags & PFLAG_SKIP_DRAW) == 0) {
            // this view draws, remove it from the transparent region
//            这个view可见了，把它从透明区域移除
            opaque = super.gatherTransparentRegion(region);
        } else if (region != null) {
            int w = getWidth();
            int h = getHeight();
            if (w>0 && h>0) {
                getLocationInWindow(mLocation);
                // otherwise, punch a hole in the whole hierarchy
                //另外，在整个view层级上开个洞
                int l = mLocation[0];
                int t = mLocation[1];
                region.op(l, t, l+w, t+h, Region.Op.UNION);
            }
        }
        if (PixelFormat.formatHasAlpha(mRequestedFormat)) {
            opaque = false;
        }
        return opaque;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mWindowType != WindowManager.LayoutParams.TYPE_APPLICATION_PANEL) {
            // draw() is not called when SKIP_DRAW is set
//            当skip_draw设置了之后，这个方法不会被调用
            if ((mPrivateFlags & PFLAG_SKIP_DRAW) == 0) {
                // punch a whole in the view-hierarchy below us
                //开一个洞在view_hierarchy
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            }
        }
        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mWindowType != WindowManager.LayoutParams.TYPE_APPLICATION_PANEL) {
            // if SKIP_DRAW is cleared, draw() has already punched a hole
            // 如果skip_draw设置被清除，draw()方法已经开了洞
            if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
                // punch a whole in the view-hierarchy below us
                // 开一个洞在view_hierarchy
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            }
        }
        super.dispatchDraw(canvas);
    }

    /**
     * Control whether the surface view's surface is placed on top of another
     * regular surface view in the window (but still behind the window itself).
     * This is typically used to place overlays on top of an underlying media
     * surface view.
     * 控制这个SurfaceView的surface是否被放置在其他规则surfaceView上面，（但是依然在窗口下面）
     * 这个一般用来添加遮盖层在其他的底层多媒体surfaceview
     *
     * <p>Note that this must be set before the surface view's containing
     * window is attached to the window manager.
     * 注意：这个方法必须在surfaceView所在窗口关联到windowManager之前
     *
     * <p>Calling this overrides any previous call to {@link #setZOrderOnTop}.
     * 调用这个方法会覆盖所有其他的先前的调用setZOrderOnTop（）的设置
     */
    public void setZOrderMediaOverlay(boolean isMediaOverlay) {
        mWindowType = isMediaOverlay
                      ? WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY
                      : WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA;
    }

    /**
     * Control whether the surface view's surface is placed on top of its
     * window.  Normally it is placed behind the window, to allow it to
     * (for the most part) appear to composite with the views in the
     * hierarchy.  By setting this, you cause it to be placed above the
     * window.  This means that none of the contents of the window this
     * SurfaceView is in will be visible on top of its surface.
     * 控制这个SurfaceView的surface是否放置在窗口之前。正常情况下，它放置在窗口之后
     * 为了使它（大部分情况下）显示出来并且和其他的view配合起来显示。设置了这个方法，你
     * 可以把surface放在窗口之上，这意味着这个surfaceView所在window中的所有内容会变得可见在
     * surface上
     *
     * <p>Note that this must be set before the surface view's containing
     * window is attached to the window manager.
     * 注意：这个方法必须在此SurfaceView所在的窗口关联上window manager之前
     *
     * <p>Calling this overrides any previous call to {@link #setZOrderMediaOverlay}.
     * 设置这个会覆盖所有之前setZOrderMediaOverlay（）设置的属性
     */
    public void setZOrderOnTop(boolean onTop) {
        if (onTop) {
            mWindowType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            // ensures the surface is placed below the IME
//            确保这个surface已经放置在IME之下
            mLayout.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        } else {
            mWindowType = WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA;
            mLayout.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        }
    }

    /**
     * Control whether the surface view's content should be treated as secure,
     * preventing it from appearing in screenshots or from being viewed on
     * non-secure displays.
     * 控制这个SurfaceView的内容是否要安全的对待，防止他出现在截图之上，或者在其他非安全显示上
     *
     * <p>Note that this must be set before the surface view's containing
     * window is attached to the window manager.
     * 注意：这个方法必须在此SurfaceView所在的窗口关联上window manager之前
     *
     * <p>See {@link android.view.Display#FLAG_SECURE} for details.
     * 要得知更多细节，看这个参数
     *
     * @param isSecure True if the surface view is secure.
     *                 True代表这个SurfaceView是安全的，
     */
    public void setSecure(boolean isSecure) {
        if (isSecure) {
            mLayout.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        } else {
            mLayout.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
        }
    }

    /**
     * Hack to allow special layering of windows.  The type is one of the
     * types in WindowManager.LayoutParams.  This is a hack so:
     * 设置来允许window特殊的层次,这个type是WindowManager.LayoutParams的type,
     * @hide
     */
    public void setWindowType(int type) {
        mWindowType = type;
    }

    /** @hide */
    protected void updateWindow(boolean force, boolean redrawNeeded) {
        if (!mHaveFrame) {
            return;
        }
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null) {
            mTranslator = viewRoot.mTranslator;
        }

        if (mTranslator != null) {
            mSurface.setCompatibilityTranslator(mTranslator);
        }

        int myWidth = mRequestedWidth;
        if (myWidth <= 0) myWidth = getWidth();
        int myHeight = mRequestedHeight;
        if (myHeight <= 0) myHeight = getHeight();

        getLocationInWindow(mLocation);
        final boolean creating = mWindow == null;
        final boolean formatChanged = mFormat != mRequestedFormat;
        final boolean sizeChanged = mWidth != myWidth || mHeight != myHeight;
        final boolean visibleChanged = mVisible != mRequestedVisible;

        if (force || creating || formatChanged || sizeChanged || visibleChanged
                || mLeft != mLocation[0] || mTop != mLocation[1]
                || mUpdateWindowNeeded || mReportDrawNeeded || redrawNeeded) {

            if (DEBUG) Log.i(TAG, "Changes: creating=" + creating
                    + " format=" + formatChanged + " size=" + sizeChanged
                    + " visible=" + visibleChanged
                    + " left=" + (mLeft != mLocation[0])
                    + " top=" + (mTop != mLocation[1]));

            try {
                final boolean visible = mVisible = mRequestedVisible;
                mLeft = mLocation[0];
                mTop = mLocation[1];
                mWidth = myWidth;
                mHeight = myHeight;
                mFormat = mRequestedFormat;

                // Scaling/Translate window's layout here because mLayout is not used elsewhere.

                // Places the window relative
                mLayout.x = mLeft;
                mLayout.y = mTop;
                mLayout.width = getWidth();
                mLayout.height = getHeight();
                if (mTranslator != null) {
                    mTranslator.translateLayoutParamsInAppWindowToScreen(mLayout);
                }

                mLayout.format = mRequestedFormat;
                mLayout.flags |=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_SCALED
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                ;
                if (!getContext().getResources().getCompatibilityInfo().supportsScreen()) {
                    mLayout.privateFlags |=
                            WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                }
                mLayout.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_NO_MOVE_ANIMATION;

                if (mWindow == null) {
                    Display display = getDisplay();
                    mWindow = new MyWindow(this);
                    mLayout.type = mWindowType;
                    mLayout.gravity = Gravity.START|Gravity.TOP;
                    mSession.addToDisplayWithoutInputChannel(mWindow, mWindow.mSeq, mLayout,
                                                             mVisible ? VISIBLE : GONE, display.getDisplayId(), mContentInsets,
                                                             mStableInsets);
                }

                boolean realSizeChanged;
                boolean reportDrawNeeded;

                int relayoutResult;

                mSurfaceLock.lock();
                try {
                    mUpdateWindowNeeded = false;
                    reportDrawNeeded = mReportDrawNeeded;
                    mReportDrawNeeded = false;
                    mDrawingStopped = !visible;

                    if (DEBUG) Log.i(TAG, "Cur surface: " + mSurface);

                    relayoutResult = mSession.relayout(
                            mWindow, mWindow.mSeq, mLayout, mWidth, mHeight,
                            visible ? VISIBLE : GONE,
                            WindowManagerGlobal.RELAYOUT_DEFER_SURFACE_DESTROY,
                            mWinFrame, mOverscanInsets, mContentInsets,
                            mVisibleInsets, mStableInsets, mOutsets, mConfiguration,
                            mNewSurface);
                    if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_FIRST_TIME) != 0) {
                        reportDrawNeeded = true;
                    }

                    if (DEBUG) Log.i(TAG, "New surface: " + mNewSurface
                            + ", vis=" + visible + ", frame=" + mWinFrame);

                    mSurfaceFrame.left = 0;
                    mSurfaceFrame.top = 0;
                    if (mTranslator == null) {
                        mSurfaceFrame.right = mWinFrame.width();
                        mSurfaceFrame.bottom = mWinFrame.height();
                    } else {
                        float appInvertedScale = mTranslator.applicationInvertedScale;
                        mSurfaceFrame.right = (int) (mWinFrame.width() * appInvertedScale + 0.5f);
                        mSurfaceFrame.bottom = (int) (mWinFrame.height() * appInvertedScale + 0.5f);
                    }

                    final int surfaceWidth = mSurfaceFrame.right;
                    final int surfaceHeight = mSurfaceFrame.bottom;
                    realSizeChanged = mLastSurfaceWidth != surfaceWidth
                            || mLastSurfaceHeight != surfaceHeight;
                    mLastSurfaceWidth = surfaceWidth;
                    mLastSurfaceHeight = surfaceHeight;
                } finally {
                    mSurfaceLock.unlock();
                }

                try {
                    redrawNeeded |= creating | reportDrawNeeded;

                    SurfaceHolder.Callback callbacks[] = null;

                    final boolean surfaceChanged = (relayoutResult
                            & WindowManagerGlobal.RELAYOUT_RES_SURFACE_CHANGED) != 0;
                    if (mSurfaceCreated && (surfaceChanged || (!visible && visibleChanged))) {
                        mSurfaceCreated = false;
                        if (mSurface.isValid()) {
                            if (DEBUG) Log.i(TAG, "visibleChanged -- surfaceDestroyed");
                            callbacks = getSurfaceCallbacks();
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceDestroyed(mSurfaceHolder);
                            }
                        }
                    }

                    mSurface.transferFrom(mNewSurface);

                    if (visible && mSurface.isValid()) {
                        if (!mSurfaceCreated && (surfaceChanged || visibleChanged)) {
                            mSurfaceCreated = true;
                            mIsCreating = true;
                            if (DEBUG) Log.i(TAG, "visibleChanged -- surfaceCreated");
                            if (callbacks == null) {
                                callbacks = getSurfaceCallbacks();
                            }
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceCreated(mSurfaceHolder);
                            }
                        }
                        if (creating || formatChanged || sizeChanged
                                || visibleChanged || realSizeChanged) {
                            if (DEBUG) Log.i(TAG, "surfaceChanged -- format=" + mFormat
                                    + " w=" + myWidth + " h=" + myHeight);
                            if (callbacks == null) {
                                callbacks = getSurfaceCallbacks();
                            }
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceChanged(mSurfaceHolder, mFormat, myWidth, myHeight);
                            }
                        }
                        if (redrawNeeded) {
                            if (DEBUG) Log.i(TAG, "surfaceRedrawNeeded");
                            if (callbacks == null) {
                                callbacks = getSurfaceCallbacks();
                            }
                            for (SurfaceHolder.Callback c : callbacks) {
                                if (c instanceof SurfaceHolder.Callback2) {
                                    ((SurfaceHolder.Callback2)c).surfaceRedrawNeeded(
                                            mSurfaceHolder);
                                }
                            }
                        }
                    }
                } finally {
                    mIsCreating = false;
                    if (redrawNeeded) {
                        if (DEBUG) Log.i(TAG, "finishedDrawing");
                        mSession.finishDrawing(mWindow);
                    }
                    mSession.performDeferredDestroy(mWindow);
                }
            } catch (RemoteException ex) {
            }
            if (DEBUG) Log.v(
                    TAG, "Layout: x=" + mLayout.x + " y=" + mLayout.y +
                            " w=" + mLayout.width + " h=" + mLayout.height +
                            ", frame=" + mSurfaceFrame);
        }
    }

    private SurfaceHolder.Callback[] getSurfaceCallbacks() {
        SurfaceHolder.Callback callbacks[];
        synchronized (mCallbacks) {
            callbacks = new SurfaceHolder.Callback[mCallbacks.size()];
            mCallbacks.toArray(callbacks);
        }
        return callbacks;
    }

    void handleGetNewSurface() {
        updateWindow(false, false);
    }

    /**
     * Check to see if the surface has fixed size dimensions or if the surface's
     * dimensions are dimensions are dependent on its current layout.
     * 查询这个surface是否具有固定的尺寸，或者这个surface的尺寸是决定于现在的布局
     *
     * @return true if the surface has dimensions that are fixed in size
     * @hide
     */
    public boolean isFixedSize() {
        return (mRequestedWidth != -1 || mRequestedHeight != -1);
    }

    private static class MyWindow extends BaseIWindow {
        private final WeakReference<SurfaceView> mSurfaceView;

        public MyWindow(SurfaceView surfaceView) {
            mSurfaceView = new WeakReference<SurfaceView>(surfaceView);
        }

        @Override
        public void resized(Rect frame, Rect overscanInsets, Rect contentInsets,
                            Rect visibleInsets, Rect stableInsets, Rect outsets, boolean reportDraw,
                            Configuration newConfig) {
            SurfaceView surfaceView = mSurfaceView.get();
            if (surfaceView != null) {
                if (DEBUG) Log.v(
                        "SurfaceView", surfaceView + " got resized: w=" + frame.width()
                                + " h=" + frame.height() + ", cur w=" + mCurWidth + " h=" + mCurHeight);
                surfaceView.mSurfaceLock.lock();
                try {
                    if (reportDraw) {
                        surfaceView.mUpdateWindowNeeded = true;
                        surfaceView.mReportDrawNeeded = true;
                        surfaceView.mHandler.sendEmptyMessage(UPDATE_WINDOW_MSG);
                    } else if (surfaceView.mWinFrame.width() != frame.width()
                            || surfaceView.mWinFrame.height() != frame.height()) {
                        surfaceView.mUpdateWindowNeeded = true;
                        surfaceView.mHandler.sendEmptyMessage(UPDATE_WINDOW_MSG);
                    }
                } finally {
                    surfaceView.mSurfaceLock.unlock();
                }
            }
        }

        @Override
        public void dispatchAppVisibility(boolean visible) {
            // The point of SurfaceView is to let the app control the surface.
//            SurfaceView上的点使得app来控制surface
        }

        @Override
        public void dispatchGetNewSurface() {
            SurfaceView surfaceView = mSurfaceView.get();
            if (surfaceView != null) {
                Message msg = surfaceView.mHandler.obtainMessage(GET_NEW_SURFACE_MSG);
                surfaceView.mHandler.sendMessage(msg);
            }
        }

        @Override
        public void windowFocusChanged(boolean hasFocus, boolean touchEnabled) {
            Log.w("SurfaceView", "Unexpected focus in surface: focus=" + hasFocus + ", touchEnabled=" + touchEnabled);
        }

        @Override
        public void executeCommand(String command, String parameters, ParcelFileDescriptor out) {
        }

        int mCurWidth = -1;
        int mCurHeight = -1;
    }

    private final SurfaceHolder mSurfaceHolder = new SurfaceHolder() {

        private static final String LOG_TAG = "SurfaceHolder";

        @Override
        public boolean isCreating() {
            return mIsCreating;
        }

        @Override
        public void addCallback(Callback callback) {
            synchronized (mCallbacks) {
                // This is a linear search, but in practice we'll
//                这是一个线性查找，练习的时候，
                // have only a couple callbacks, so it doesn't matter.
                // 仅有一对回调，这没什么关系
                if (mCallbacks.contains(callback) == false) {
                    mCallbacks.add(callback);
                }
            }
        }

        @Override
        public void removeCallback(Callback callback) {
            synchronized (mCallbacks) {
                mCallbacks.remove(callback);
            }
        }

        @Override
        public void setFixedSize(int width, int height) {
            if (mRequestedWidth != width || mRequestedHeight != height) {
                mRequestedWidth = width;
                mRequestedHeight = height;
                requestLayout();
            }
        }

        @Override
        public void setSizeFromLayout() {
            if (mRequestedWidth != -1 || mRequestedHeight != -1) {
                mRequestedWidth = mRequestedHeight = -1;
                requestLayout();
            }
        }

        @Override
        public void setFormat(int format) {

            // for backward compatibility reason, OPAQUE always
            // means 565 for SurfaceView
            //为了向下兼容，不透明意味着用的是565的格式
            if (format == PixelFormat.OPAQUE)
                format = PixelFormat.RGB_565;

            mRequestedFormat = format;
            if (mWindow != null) {
                updateWindow(false, false);
            }
        }

        /**
         * @deprecated setType is now ignored.
         * 已经无效了，设置了也会被忽略
         */
        @Override
        @Deprecated
        public void setType(int type) { }

        @Override
        public void setKeepScreenOn(boolean screenOn) {
            Message msg = mHandler.obtainMessage(KEEP_SCREEN_ON_MSG);
            msg.arg1 = screenOn ? 1 : 0;
            mHandler.sendMessage(msg);
        }

        /**
         * Gets a {@link Canvas} for drawing into the SurfaceView's Surface
         * 提供一个画布，这样就可以往surface上绘制
         *
         * After drawing into the provided {@link Canvas}, the caller must
         * invoke {@link #unlockCanvasAndPost} to post the new contents to the surface.
         * 绘制到画布上之后，调用者必须执行unlockCanvasAndPost将画的内容显示到surface上
         *
         * The caller must redraw the entire surface.
         * 这个方法必须重绘整个surface
         * @return A canvas for drawing into the surface.
         * 返回一个画布，用来向surface上绘制
         */
        @Override
        public Canvas lockCanvas() {
            return internalLockCanvas(null);
        }

        /**
         * Gets a {@link Canvas} for drawing into the SurfaceView's Surface
         * 回去一个画布，用来向SurfaceView上的Surface绘制东西
         *
         * After drawing into the provided {@link Canvas}, the caller must
         * invoke {@link #unlockCanvasAndPost} to post the new contents to the surface.
         * 执行完绘制之后，调用者需要执行unlockCanvasAndPost来将内容发送到surface上
         *
         * @param inOutDirty A rectangle that represents the dirty region that the caller wants
         * to redraw.  This function may choose to expand the dirty rectangle if for example
         * the surface has been resized or if the previous contents of the surface were
         * not available.  The caller must redraw the entire dirty region as represented
         * by the contents of the inOutDirty rectangle upon return from this function.
         * The caller may also pass <code>null</code> instead, in the case where the
         * entire surface should be redrawn.
         * 参数 inOutDirty ,这是一个长方形，代表调用者想重绘的无用的区域。这个功能可以用来扩展这个长方形，例如，如果这个
         * surface的尺寸重新设置了，或者先前surface上的内容无效了。调用者必须重绘整个无效的区域，因为这个无效长方形上的内容
         * 在这个方法返回后已经清空。调用者也可以传null进去，这种情况下整个surface被重绘了
         * @return A canvas for drawing into the surface.
         * 返回一个画布，用来向surface上绘制东西
         */
        @Override
        public Canvas lockCanvas(Rect inOutDirty) {
            return internalLockCanvas(inOutDirty);
        }

        private final Canvas internalLockCanvas(Rect dirty) {
            mSurfaceLock.lock();

            if (DEBUG) Log.i(TAG, "Locking canvas... stopped="
                    + mDrawingStopped + ", win=" + mWindow);

            Canvas c = null;
            if (!mDrawingStopped && mWindow != null) {
                try {
                    c = mSurface.lockCanvas(dirty);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception locking surface", e);
                }
            }

            if (DEBUG) Log.i(TAG, "Returned canvas: " + c);
            if (c != null) {
                mLastLockTime = SystemClock.uptimeMillis();
                return c;
            }

            // If the Surface is not ready to be drawn, then return null,
            // but throttle calls to this function so it isn't called more
            // than every 100ms.
            // 如果surface还不能绘制，这个方法返回null，不要总是调用这个方法，这个方法
            // 所以下一次调用要在100ms之后
            long now = SystemClock.uptimeMillis();
            long nextTime = mLastLockTime + 100;
            if (nextTime > now) {
                try {
                    Thread.sleep(nextTime-now);
                } catch (InterruptedException e) {
                }
                now = SystemClock.uptimeMillis();
            }
            mLastLockTime = now;
            mSurfaceLock.unlock();

            return null;
        }

        /**
         * Posts the new contents of the {@link Canvas} to the surface and
         * releases the {@link Canvas}.
         * 将画布上新的内容显示在surface上，并释放画布
         *
         * @param canvas The canvas previously obtained from {@link #lockCanvas}.
         *  这个canvas先前调用lockCanvas返回的canvas
         */
        @Override
        public void unlockCanvasAndPost(Canvas canvas) {
            mSurface.unlockCanvasAndPost(canvas);
            mSurfaceLock.unlock();
        }

        @Override
        public Surface getSurface() {
            return mSurface;
        }

        @Override
        public Rect getSurfaceFrame() {
            return mSurfaceFrame;
        }
    };
}
