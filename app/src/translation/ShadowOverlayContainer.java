/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Provides an SDK version-independent wrapper to support shadows, color overlays, and rounded
 * corners.  It's not always preferred to create a ShadowOverlayContainer, use
 * {@link ShadowOverlayHelper} instead.
 * 提供一个包裹viewGroup来支持阴影，颜色遮罩，或圆角。最好不要直接创建ShadowOverlayContainer，使用
 * ShadowOverlayHelper来代替
 *
 * <p>
 * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
 * before using shadow.  Depending on sdk version, optical bounds might be applied
 * to parent.
 * 在使用阴影之前，prepareParentForShadow(ViewGroup)必须在父容器调用。取决于sdk的版本，可见的边界或许
 * 应被应用在父布局里
 * </p>
 * <p>
 * If shadows can appear outside the bounds of the parent view, setClipChildren(false) must
 * be called on the grandparent view.
 * 如果阴影可以出现在父布局的边界外，必须爷爷布局使用setClipChildren(false)
 * </p>
 * <p>
 * {@link #initialize(boolean, boolean, boolean)} must be first called on the container.
 * Then call {@link #wrap(View)} to insert the wrapped view into the container.
 * 初始化方法initialize(boolean, boolean, boolean)，在容器（本类的对象吧）中，要第一个调用。
 * 然后调用wrap(View)，来给这个容器来插入要包裹的view
 * </p>
 * <p>
 * Call {@link #setShadowFocusLevel(float)} to control the strength of the shadow (focused shadows
 * cast stronger shadows).
 * setShadowFocusLevel(float)来控制阴影的强度（当获得焦点时，阴影会变的更强）。
 * </p>
 * <p>
 * Call {@link #setOverlayColor(int)} to control overlay color.
 * 调用setOverlayColor(int)，来控制遮盖层的颜色
 * </p>
 */
public class ShadowOverlayContainer extends FrameLayout {

    /**
     * No shadow.
     */
    public static final int SHADOW_NONE = ShadowOverlayHelper.SHADOW_NONE;

    /**
     * Shadows are fixed.
     */
    public static final int SHADOW_STATIC = ShadowOverlayHelper.SHADOW_STATIC;

    /**
     * Shadows depend on the size, shape, and position of the view.
     */
    public static final int SHADOW_DYNAMIC = ShadowOverlayHelper.SHADOW_DYNAMIC;

    private boolean mInitialized;
    private Object mShadowImpl;
    private View mWrappedView;
    private boolean mRoundedCorners;
    private int mShadowType = SHADOW_NONE;
    private float mUnfocusedZ;
    private float mFocusedZ;
    private int mRoundedCornerRadius;
    private static final Rect sTempRect = new Rect();
    private Paint mOverlayPaint;
    private int mOverlayColor;

    /**
     * Create ShadowOverlayContainer and auto select shadow type.
     * 创建一个ShadowOverlayContainer，自动选择阴影的类型
     */
    public ShadowOverlayContainer(Context context) {
        this(context, null, 0);
    }

    /**
     * Create ShadowOverlayContainer and auto select shadow type.
     * 创建一个ShadowOverlayContainer，自动选择阴影的类型
     */
    public ShadowOverlayContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Create ShadowOverlayContainer and auto select shadow type.
     * 创建一个ShadowOverlayContainer，自动选择阴影的类型
     */
    public ShadowOverlayContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        useStaticShadow();
        useDynamicShadow();
    }

    /**
     * Create ShadowOverlayContainer with specific shadowType.
     * 创建一个ShadowOverlayContainer，并选择特定的阴影类型
     */
    ShadowOverlayContainer(Context context,
                           int shadowType, boolean hasColorDimOverlay,
                           float unfocusedZ, float focusedZ, int roundedCornerRadius) {
        super(context);
        mUnfocusedZ = unfocusedZ;
        mFocusedZ = focusedZ;
        initialize(shadowType, hasColorDimOverlay, roundedCornerRadius);
    }

    /**
     * Return true if the platform sdk supports shadow.
     * 平台sdk版本是否支持阴影
     */
    public static boolean supportsShadow() {
        return StaticShadowHelper.getInstance().supportsShadow();
    }

    /**
     * Returns true if the platform sdk supports dynamic shadows.
     * 平台sdk版本是否支持动态阴影
     */
    public static boolean supportsDynamicShadow() {
        return ShadowHelper.getInstance().supportsDynamicShadow();
    }

    /**
     * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
     * before using shadow.  Depending on sdk version, optical bounds might be applied
     * to parent.
     * 在使用阴影之前，此方法必须被本容器的父布局先调用，根据sdk的版本，可见的边距可能要应用在父布局上
     */
    public static void prepareParentForShadow(ViewGroup parent) {
        StaticShadowHelper.getInstance().prepareParent(parent);
    }

    /**
     * Sets the shadow type to {@link #SHADOW_DYNAMIC} if supported.
     * 如果支持动态阴影，调用此方法来使用动态阴影
     */
    public void useDynamicShadow() {
        useDynamicShadow(getResources().getDimension(R.dimen.lb_material_shadow_normal_z),
                         getResources().getDimension(R.dimen.lb_material_shadow_focused_z));
    }

    /**
     * Sets the shadow type to {@link #SHADOW_DYNAMIC} if supported and sets the elevation/Z
     * values to the given parameteres.
     * 如果支持动态阴影，调用此方法来使用动态阴影，并且设置焦点状态和非焦点状态的z轴高度
     */
    public void useDynamicShadow(float unfocusedZ, float focusedZ) {
        if (mInitialized) {
            throw new IllegalStateException("Already initialized");
        }
        if (supportsDynamicShadow()) {
            mShadowType = SHADOW_DYNAMIC;
            mUnfocusedZ = unfocusedZ;
            mFocusedZ = focusedZ;
        }
    }

    /**
     * Sets the shadow type to {@link #SHADOW_STATIC} if supported.
     * 使用固定的阴影，如果支持的话
     */
    public void useStaticShadow() {
        if (mInitialized) {
            throw new IllegalStateException("Already initialized");
        }
        if (supportsShadow()) {
            mShadowType = SHADOW_STATIC;
        }
    }

    /**
     * Returns the shadow type, one of {@link #SHADOW_NONE}, {@link #SHADOW_STATIC}, or
     * {@link #SHADOW_DYNAMIC}.
     * 返回目前的阴影类型
     */
    public int getShadowType() {
        return mShadowType;
    }

    /**
     * Initialize shadows, color overlay.
     * @deprecated use {@link ShadowOverlayHelper#createShadowOverlayContainer(Context)} instead.
     */
    @Deprecated
    public void initialize(boolean hasShadow, boolean hasColorDimOverlay) {
        initialize(hasShadow, hasColorDimOverlay, true);
    }

    /**
     * Initialize shadows, color overlay, and rounded corners.  All are optional.
     * Shadow type are auto-selected based on {@link #useStaticShadow()} and
     * {@link #useDynamicShadow()} call.
     * @deprecated use {@link ShadowOverlayHelper#createShadowOverlayContainer(Context)} instead.
     * 初始化阴影，颜色遮盖否，以及圆角。都不是必须的。
     * 阴影类型根据useStaticShadow()和useDynamicShadow()的调用来选择
     * 已经过时了，使用ShadowOverlayHelper#createShadowOverlayContainer(Context)来替代
     */
    @Deprecated
    public void initialize(boolean hasShadow, boolean hasColorDimOverlay, boolean roundedCorners) {
        int shadowType;
        if (!hasShadow) {
            shadowType = SHADOW_NONE;
        } else {
            shadowType = mShadowType;
        }
        int roundedCornerRadius = roundedCorners ? getContext().getResources().getDimensionPixelSize(
                R.dimen.lb_rounded_rect_corner_radius) : 0;
        initialize(shadowType, hasColorDimOverlay, roundedCornerRadius);
    }

    /**
     * Initialize shadows, color overlay, and rounded corners.  All are optional.
     * 初始化阴影，颜色遮盖否，以及圆角。都不是必须的
     */
    void initialize(int shadowType, boolean hasColorDimOverlay, int roundedCornerRadius) {
        if (mInitialized) {
            throw new IllegalStateException();
        }
        mInitialized = true;
        mRoundedCornerRadius = roundedCornerRadius;
        mRoundedCorners = roundedCornerRadius > 0;
        mShadowType = shadowType;
        switch (mShadowType) {
            case SHADOW_DYNAMIC:
                mShadowImpl = ShadowHelper.getInstance().addDynamicShadow(
                        this, mUnfocusedZ, mFocusedZ, mRoundedCornerRadius);
                break;
            case SHADOW_STATIC:
                mShadowImpl = StaticShadowHelper.getInstance().addStaticShadow(this);
                break;
        }
        if (hasColorDimOverlay) {
            setWillNotDraw(false);
            mOverlayColor = Color.TRANSPARENT;
            mOverlayPaint = new Paint();
            mOverlayPaint.setColor(mOverlayColor);
            mOverlayPaint.setStyle(Paint.Style.FILL);
        } else {
            setWillNotDraw(true);
            mOverlayPaint = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mOverlayPaint != null && mOverlayColor != Color.TRANSPARENT) {
            canvas.drawRect(mWrappedView.getLeft(), mWrappedView.getTop(),
                            mWrappedView.getRight(), mWrappedView.getBottom(),
                            mOverlayPaint);
        }
    }

    /**
     * Set shadow focus level (0 to 1). 0 for unfocused, 1f for fully focused.
     * 选择焦点时，阴影的等级（0-1）。没有焦点时是0，有焦点的时候是1f
     */
    public void setShadowFocusLevel(float level) {
        if (mShadowImpl != null) {
            ShadowOverlayHelper.setShadowFocusLevel(mShadowImpl, mShadowType, level);
        }
    }

    /**
     * Set color (with alpha) of the overlay.
     * 设置遮盖的颜色（颜色是有透明度的）
     */
    public void setOverlayColor(@ColorInt int overlayColor) {
        if (mOverlayPaint != null) {
            if (overlayColor != mOverlayColor) {
                mOverlayColor = overlayColor;
                mOverlayPaint.setColor(overlayColor);
                invalidate();
            }
        }
    }

    /**
     * Inserts view into the wrapper.
     * 给本容器插入子view
     */
    public void wrap(View view) {
        if (!mInitialized || mWrappedView != null) {
            throw new IllegalStateException();
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            // if wrapped view has layout params, inherit everything but width/height.
            // Wrapped view is assigned a FrameLayout.LayoutParams with width and height only.
            // Margins, etc are assigned to the wrapper and take effect in parent container.
            // 如果被包裹的view有布局参数，除了宽高，继承所有的参数
            // 用高度和宽度给包裹的view创建一个FrameLayout.LayoutParams
            // 外边界等都会作用在本父布局上
            ViewGroup.LayoutParams wrapped_lp = new FrameLayout.LayoutParams(lp.width, lp.height);
            // Uses MATCH_PARENT for MATCH_PARENT, WRAP_CONTENT for WRAP_CONTENT and fixed size,
            // App can still change wrapped view fixed width/height afterwards.
            // MATCH_PARENT还是MATCH_PARENT，确定尺寸或包裹内容用wrap_content
            // 之后应用依然可以将被包裹view的尺寸改成固定的
            lp.width = lp.width == LayoutParams.MATCH_PARENT ?
                       LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT;
            lp.height = lp.height == LayoutParams.MATCH_PARENT ?
                        LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT;
            this.setLayoutParams(lp);
            addView(view, wrapped_lp);
        } else {
            addView(view);
        }
        if (mRoundedCorners && mShadowType == SHADOW_STATIC) {
            RoundedRectHelper.getInstance().setClipToRoundedOutline(view, true);
        }
        mWrappedView = view;
    }

    /**
     * Returns the wrapper view.
     * 返回被包裹的view
     */
    public View getWrappedView() {
        return mWrappedView;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && mWrappedView != null) {
            sTempRect.left = (int) mWrappedView.getPivotX();
            sTempRect.top = (int) mWrappedView.getPivotY();
            offsetDescendantRectToMyCoords(mWrappedView, sTempRect);
            setPivotX(sTempRect.left);
            setPivotY(sTempRect.top);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
