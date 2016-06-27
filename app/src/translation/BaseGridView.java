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
package com.ancho.translation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.ItemAlignmentFacet;
import android.support.v17.leanback.widget.OnChildLaidOutListener;
import android.support.v17.leanback.widget.OnChildSelectedListener;
import android.support.v17.leanback.widget.OnChildViewHolderSelectedListener;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 *
 * 为横向或纵向可滚动条目准备的抽象积累view,继承自recyclerView,不要直接创建此类的实例,请使用VerticalGridView
 * 和HorizontalGridView
 * An abstract base class for vertically and horizontally scrolling lists. The items come
 * from the {@link Adapter} associated with this view.
 * Do not directly use this class, use {@link VerticalGridView} and {@link HorizontalGridView}.
 * @hide
 */
abstract class BaseGridView extends RecyclerView {

    /**
     * Always keep focused item at a aligned position.  Developer can use
     * WINDOW_ALIGN_XXX and ITEM_ALIGN_XXX to define how focused item is aligned.
     * In this mode, the last focused position will be remembered and restored when focus
     * is back to the view.
     * 保持焦点所在条目有某一个位置对齐,开发者可以使用WINDOW_ALIGN_XXX或者ITEM_ALIGN_XXX指定对齐某个位置.
     * 此种模式下,本view里的最后一个持有焦点的item会被记录下来,用来当本view重新获得焦点时,还原焦点位置
     */
    public final static int FOCUS_SCROLL_ALIGNED = 0;

    /**
     * Scroll to make the focused item inside client area.
     * 滚动使得焦点条目进入可视区域
     */
    public final static int FOCUS_SCROLL_ITEM = 1;

    /**
     * Scroll a page of items when focusing to item outside the client area.
     * The page size matches the client area size of RecyclerView.
     * 当焦点条目在可是区域外时,滚动一个页面,一个页面的大小匹配RecyclerView的尺寸
     */
    public final static int FOCUS_SCROLL_PAGE = 2;

    /**
     * The first item is aligned with the low edge of the viewport. When
     * navigating away from the first item, the focus maintains a middle
     * location.
     * <p>
     * For HorizontalGridView, low edge refers to left edge when RTL is false or
     * right edge when RTL is true.
     * For VerticalGridView, low edge refers to top edge.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     *
     * ViewPort:视图窗口,我的理解是可滑动的view显示部分,可以认为是通过窗口(ViewPort)来看
     * 一个滚动的视图(Scroll view),所以ViewPort就是scroll view就是此view在屏幕上的区域
     *
     * 第一个条目与viewport低边缘对齐,当焦点离开list的开始区域,焦点维持在中间位置
     * <p>
     * todo ancho
     * 对于HorizontalGridView,当RTL(是否反转显示)=false,低边缘是左边;RTL(是否反转显示)=true,
     * 低边缘在右边.
     * 对于VerticalGridView,低边缘在上边
     * <p>
     * 中间位置是通过"windowAlignOffset"和"windowAlignOffsetPercent"计算的,如果这两项都没
     * 设置,默认在viewport的中间位置
     */
    public final static int WINDOW_ALIGN_LOW_EDGE = 1;

    /**
     * The last item is aligned with the high edge of the viewport when
     * navigating to the end of list. When navigating away from the end, the
     * focus maintains a middle location.
     * <p>
     * For HorizontalGridView, high edge refers to right edge when RTL is false or
     * left edge when RTL is true.
     * For VerticalGridView, high edge refers to bottom edge.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     *
     * 当移动到list的后面,最后一个条目与viewport的高边缘对齐.当焦点离开list的末尾
     * 区域,焦点维持在中间位置
     * <p>
     * 同上
     */
    public final static int WINDOW_ALIGN_HIGH_EDGE = 1 << 1;

    /**
     * The first item and last item are aligned with the two edges of the
     * viewport. When navigating in the middle of list, the focus maintains a
     * middle location.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     *
     * 第一个条目和最后一个条目与viewport的上下边界对齐.当移动到中间位置,焦点保持在
     * 中间位置.
     * <p>
     * 同上
     */
    public final static int WINDOW_ALIGN_BOTH_EDGE =
            WINDOW_ALIGN_LOW_EDGE | WINDOW_ALIGN_HIGH_EDGE;

    /**
     * The focused item always stays in a middle location.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     *
     * 焦点条目总是处于中间位置
     * <p>
     * 同上
     */
    public final static int WINDOW_ALIGN_NO_EDGE = 0;

    /**
     * Value indicates that percent is not used.
     *
     * WINDOW_ALIGN_OFFSET_PERCENT的默认值
     * 表示这个值没有设置,是无效的
     */
    public final static float WINDOW_ALIGN_OFFSET_PERCENT_DISABLED = -1;

    /**
     * Value indicates that percent is not used.
     * ITEM_ALIGN_OFFSET_PERCENT的默认值
     * 表示这个值没有设置,是无效的
     */
    public final static float ITEM_ALIGN_OFFSET_PERCENT_DISABLED =
            ItemAlignmentFacet.ITEM_ALIGN_OFFSET_PERCENT_DISABLED;

    /**
     * Dont save states of any child views.
     * 不记录任何子条目的状态
     */
    public static final int SAVE_NO_CHILD = 0;

    /**
     * Only save on screen child views, the states are lost when they become off screen.
     * 记录显示在屏幕里的子条目的状态,当移出屏幕时,状态丢失
     */
    public static final int SAVE_ON_SCREEN_CHILD = 1;

    /**
     * Save on screen views plus save off screen child views states up to
     * {@link #getSaveChildrenLimitNumber()}.
     * 记录显示在屏幕里的子条目,也记录一定数目的移出屏幕的子条目的状态
     */
    public static final int SAVE_LIMITED_CHILD = 2;

    /**
     * Save on screen views plus save off screen child views without any limitation.
     * This might cause out of memory, only use it when you are dealing with limited data.
     * 记录屏幕内显示的子条目和屏幕外的条目,不设限制.可能会导致内存溢出,数据量比较少的时候采用
     *
     */
    public static final int SAVE_ALL_CHILD = 3;

    /**
     * Listener for intercepting touch dispatch events.
     * 监听触摸事件,并判断是否打断事件传递
     */
    public interface OnTouchInterceptListener {
        /**
         * Returns true if the touch dispatch event should be consumed.
         * return true代表事件被消耗
         */
        public boolean onInterceptTouchEvent(MotionEvent event);
    }

    /**
     * Listener for intercepting generic motion dispatch events.
     * 监听一般的事件,并判断是否打断事件传递
     */
    public interface OnMotionInterceptListener {
        /**
         * Returns true if the touch dispatch event should be consumed.
         * return true代表事件被消耗
         */
        public boolean onInterceptMotionEvent(MotionEvent event);
    }

    /**
     * Listener for intercepting key dispatch events.
     * 监听键盘事件,并判断是否打断事件传递
     */
    public interface OnKeyInterceptListener {
        /**
         * Returns true if the key dispatch event should be consumed.
         * return true代表事件被消耗
         */
        public boolean onInterceptKeyEvent(KeyEvent event);
    }

    public interface OnUnhandledKeyListener {
        /**
         * Returns true if the key event should be consumed.
         * return true代表事件被消耗
         */
        public boolean onUnhandledKey(KeyEvent event);
    }

    final GridLayoutManager mLayoutManager;

    /**
     * Animate layout changes from a child resizing or adding/removing a child.
     * 当重新绘制子条目的尺寸以及添加删除子条目时,动画显示布局的改变
     */
    private boolean mAnimateChildLayout = true;

    private boolean mHasOverlappingRendering = true;

    private ItemAnimator mSavedItemAnimator;

    private OnTouchInterceptListener mOnTouchInterceptListener;
    private OnMotionInterceptListener mOnMotionInterceptListener;
    private OnKeyInterceptListener mOnKeyInterceptListener;
    private RecyclerListener mChainedRecyclerListener;
    private OnUnhandledKeyListener mOnUnhandledKeyListener;

    public BaseGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLayoutManager = new GridLayoutManager(this);
        setLayoutManager(mLayoutManager);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setHasFixedSize(true);
        setChildrenDrawingOrderEnabled(true);
        setWillNotDraw(true);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        // Disable change animation by default on leanback.
        //默认不显示动画
        // Change animation will create a new view and cause undesired
        // focus animation between the old view and new view.
//        在创建一个新的view的时候显示动画,焦点动画显示在旧的view,还是新创建的view,这是不可控的
        ((SimpleItemAnimator)getItemAnimator()).setSupportsChangeAnimations(false);
        super.setRecyclerListener(new RecyclerListener() {
            @Override
            public void onViewRecycled(ViewHolder holder) {
                mLayoutManager.onChildRecycled(holder);
                if (mChainedRecyclerListener != null) {
                    mChainedRecyclerListener.onViewRecycled(holder);
                }
            }
        });
    }

    protected void initBaseGridViewAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbBaseGridView);
        boolean throughFront = a.getBoolean(R.styleable.lbBaseGridView_focusOutFront, false);
        boolean throughEnd = a.getBoolean(R.styleable.lbBaseGridView_focusOutEnd, false);
        mLayoutManager.setFocusOutAllowed(throughFront, throughEnd);
        boolean throughSideStart = a.getBoolean(R.styleable.lbBaseGridView_focusOutSideStart, true);
        boolean throughSideEnd = a.getBoolean(R.styleable.lbBaseGridView_focusOutSideEnd, true);
        mLayoutManager.setFocusOutSideAllowed(throughSideStart, throughSideEnd);
        mLayoutManager.setVerticalMargin(
                a.getDimensionPixelSize(R.styleable.lbBaseGridView_verticalMargin, 0));
        mLayoutManager.setHorizontalMargin(
                a.getDimensionPixelSize(R.styleable.lbBaseGridView_horizontalMargin, 0));
        if (a.hasValue(R.styleable.lbBaseGridView_android_gravity)) {
            setGravity(a.getInt(R.styleable.lbBaseGridView_android_gravity, Gravity.NO_GRAVITY));
        }
        a.recycle();
    }

    /**
     * Sets the strategy used to scroll in response to item focus changing:
     * 设置一个显示方式用于滑动导致的焦点变化
     * 默认对齐
     * <ul>
     * <li>{@link #FOCUS_SCROLL_ALIGNED} (default) </li>
     * <li>{@link #FOCUS_SCROLL_ITEM}</li>
     * <li>{@link #FOCUS_SCROLL_PAGE}</li>
     * </ul>
     */
    public void setFocusScrollStrategy(int scrollStrategy) {
        if (scrollStrategy != FOCUS_SCROLL_ALIGNED && scrollStrategy != FOCUS_SCROLL_ITEM
                && scrollStrategy != FOCUS_SCROLL_PAGE) {
            throw new IllegalArgumentException("Invalid scrollStrategy");
        }
        mLayoutManager.setFocusScrollStrategy(scrollStrategy);
        requestLayout();
    }

    /**
     * Returns the strategy used to scroll in response to item focus changing.
     * 返回焦点变化显示方式
     * <ul>
     * <li>{@link #FOCUS_SCROLL_ALIGNED} (default) </li>
     * <li>{@link #FOCUS_SCROLL_ITEM}</li>
     * <li>{@link #FOCUS_SCROLL_PAGE}</li>
     * </ul>
     */
    public int getFocusScrollStrategy() {
        return mLayoutManager.getFocusScrollStrategy();
    }

    /**
     * Sets the method for focused item alignment in the view.
     * 设定焦点条目的对齐方式
     * @param windowAlignment {@link #WINDOW_ALIGN_BOTH_EDGE},
     *        {@link #WINDOW_ALIGN_LOW_EDGE}, {@link #WINDOW_ALIGN_HIGH_EDGE} or
     *        {@link #WINDOW_ALIGN_NO_EDGE}.
     */
    public void setWindowAlignment(int windowAlignment) {
        mLayoutManager.setWindowAlignment(windowAlignment);
        requestLayout();
    }

    /**
     * Returns the method for focused item alignment in the view.
     * 返回焦点条目的对齐方式
     *
     * @return {@link #WINDOW_ALIGN_BOTH_EDGE}, {@link #WINDOW_ALIGN_LOW_EDGE},
     *         {@link #WINDOW_ALIGN_HIGH_EDGE} or {@link #WINDOW_ALIGN_NO_EDGE}.
     */
    public int getWindowAlignment() {
        return mLayoutManager.getWindowAlignment();
    }

    /**
     * Sets the offset in pixels for window alignment.
     * 焦点条目对齐时的偏移量
     *
     * @param offset The number of pixels to offset.  If the offset is positive,
     *        it is distance from low edge (see {@link #WINDOW_ALIGN_LOW_EDGE});
     *        if the offset is negative, the absolute value is distance from high
     *        edge (see {@link #WINDOW_ALIGN_HIGH_EDGE}).
     *        Default value is 0.
     *              补偿多少个像素，如果补偿值为正，补偿的是viewport的低边缘
     *       如果补偿值为负，补偿的是viewport的高边缘，默认为零
     */
    public void setWindowAlignmentOffset(int offset) {
        mLayoutManager.setWindowAlignmentOffset(offset);
        requestLayout();
    }

    /**
     * Returns the offset in pixels for window alignment.
     * 返回焦点条目对齐时的偏移量
     *
     * @return The number of pixels to offset.  If the offset is positive,
     *        it is distance from low edge (see {@link #WINDOW_ALIGN_LOW_EDGE});
     *        if the offset is negative, the absolute value is distance from high
     *        edge (see {@link #WINDOW_ALIGN_HIGH_EDGE}).
     *        Default value is 0.
     */
    public int getWindowAlignmentOffset() {
        return mLayoutManager.getWindowAlignmentOffset();
    }

    /**
     * Sets the offset percent for window alignment in addition to {@link
     * #getWindowAlignmentOffset()}.
     *  设置补偿百分比（减去已经设置的绝对的像素偏移量）
     * @param offsetPercent Percentage to offset. E.g., 40 means 40% of the
     *        width from low edge. Use
     *        {@link #WINDOW_ALIGN_OFFSET_PERCENT_DISABLED} to disable.
     *        Default value is 50.
     *        补偿量百分值，例如，40代表给viewport底边缘补偿40%的viewport的宽度
     *        设置WINDOW_ALIGN_OFFSET_PERCENT_DISABLED可以废弃这个参数
     *        默认是50%
     */
    public void setWindowAlignmentOffsetPercent(float offsetPercent) {
        mLayoutManager.setWindowAlignmentOffsetPercent(offsetPercent);
        requestLayout();
    }

    /**
     * Returns the offset percent for window alignment in addition to
     * {@link #getWindowAlignmentOffset()}.
     * 返回补偿的百分比
     *
     * @return Percentage to offset. E.g., 40 means 40% of the width from the
     *         low edge, or {@link #WINDOW_ALIGN_OFFSET_PERCENT_DISABLED} if
     *         disabled. Default value is 50.
     */
    public float getWindowAlignmentOffsetPercent() {
        return mLayoutManager.getWindowAlignmentOffsetPercent();
    }

    /**
     * Sets the absolute offset in pixels for item alignment.
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link ViewHolder} or {@link FacetProviderAdapter}.
     * 设置绝对补偿值用于子条目对齐。当ViewHolder提供的是“ItemAlignmentFacet”或者用的适配器是
     * FacetProviderAdapter，此设置是无效的
     *
     * @param offset The number of pixels to offset. Can be negative for
     *        alignment from the high edge, or positive for alignment from the
     *        low edge.
     *               绝对的像素补偿值，为负时，如果补偿值为正，补偿的是viewport的低边缘
     *       如果补偿值为负，补偿的是viewport的高边缘
     */
    public void setItemAlignmentOffset(int offset) {
        mLayoutManager.setItemAlignmentOffset(offset);
        requestLayout();
    }

    /**
     * Returns the absolute offset in pixels for item alignment.
     * 返回item的绝对像素补偿值
     *
     * @return The number of pixels to offset. Will be negative for alignment
     *         from the high edge, or positive for alignment from the low edge.
     *         Default value is 0.
     */
    public int getItemAlignmentOffset() {
        return mLayoutManager.getItemAlignmentOffset();
    }

    /**
     * Set to true if include padding in calculating item align offset.
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link ViewHolder} or {@link FacetProviderAdapter}.
     * 计算条目补偿时，是否包括padding，当ViewHolder提供的是“ItemAlignmentFacet”或者用的适配器是
     * FacetProviderAdapter，此设置是无效的
     *
     * @param withPadding When it is true: we include left/top padding for positive
     *          item offset, include right/bottom padding for negative item offset.
     *   为真时，offset += left/top padding，offset+=right/bottom padding;
     */
    public void setItemAlignmentOffsetWithPadding(boolean withPadding) {
        mLayoutManager.setItemAlignmentOffsetWithPadding(withPadding);
        requestLayout();
    }

    /**
     * Returns true if include padding in calculating item align offset.
     * 如果补偿值包括padding,则返回true
     */
    public boolean isItemAlignmentOffsetWithPadding() {
        return mLayoutManager.isItemAlignmentOffsetWithPadding();
    }

    /**
     * Sets the offset percent for item alignment in addition to {@link
     * #getItemAlignmentOffset()}.
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link ViewHolder} or {@link FacetProviderAdapter}.
     *
     * @param offsetPercent Percentage to offset. E.g., 40 means 40% of the
     *        width from the low edge. Use
     *        {@link #ITEM_ALIGN_OFFSET_PERCENT_DISABLED} to disable.
     */
    public void setItemAlignmentOffsetPercent(float offsetPercent) {
        mLayoutManager.setItemAlignmentOffsetPercent(offsetPercent);
        requestLayout();
    }

    /**
     * Returns the offset percent for item alignment in addition to {@link
     * #getItemAlignmentOffset()}.
     *
     * @return Percentage to offset. E.g., 40 means 40% of the width from the
     *         low edge, or {@link #ITEM_ALIGN_OFFSET_PERCENT_DISABLED} if
     *         disabled. Default value is 50.
     */
    public float getItemAlignmentOffsetPercent() {
        return mLayoutManager.getItemAlignmentOffsetPercent();
    }

    /**
     * Sets the id of the view to align with. Use {@link View#NO_ID} (default)
     * for the item view itself.
     * 设置view对齐的ID,默认使用no_id表示有自己对齐
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link ViewHolder} or {@link FacetProviderAdapter}.
     * 如果viewHolder提供的是itemAlignmentFacet或者用的适配器是FacetProviderAdaper,则此参数无效
     */
    public void setItemAlignmentViewId(int viewId) {
        mLayoutManager.setItemAlignmentViewId(viewId);
    }

    /**
     * Returns the id of the view to align with, or zero for the item view itself.
     * 返回view要对齐的ID,如果对齐自己则返回0
     */
    public int getItemAlignmentViewId() {
        return mLayoutManager.getItemAlignmentViewId();
    }

    /**
     * Sets the margin in pixels between two child items.
     * 设置两个子条目的外间距
     */
    public void setItemMargin(int margin) {
        mLayoutManager.setItemMargin(margin);
        requestLayout();
    }

    /**
     * Sets the margin in pixels between two child items vertically.
     * 设置子条目之间纵向的外间距
     */
    public void setVerticalMargin(int margin) {
        mLayoutManager.setVerticalMargin(margin);
        requestLayout();
    }

    /**
     * Returns the margin in pixels between two child items vertically.
     * 返回子条目之间纵向的外间距
     */
    public int getVerticalMargin() {
        return mLayoutManager.getVerticalMargin();
    }

    /**
     * Sets the margin in pixels between two child items horizontally.
     * 设置子条目之间横向的外间距
     */
    public void setHorizontalMargin(int margin) {
        mLayoutManager.setHorizontalMargin(margin);
        requestLayout();
    }

    /**
     * Returns the margin in pixels between two child items horizontally.
     * 返回子条目之间横向的外间距
     */
    public int getHorizontalMargin() {
        return mLayoutManager.getHorizontalMargin();
    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
     * been laid out.
     * 注册一个回调,当一个子项目布局好的时候调用
     *
     * @param listener The listener to be invoked.
     */
    public void setOnChildLaidOutListener(OnChildLaidOutListener listener) {
        mLayoutManager.setOnChildLaidOutListener(listener);
    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
     * been selected.  Note that the listener may be invoked when there is a
     * layout pending on the view, affording the listener an opportunity to
     * adjust the upcoming layout based on the selection state.
     * 注册一个回调,当一个子项目被选中的时候调用.
     * 注意:当布局这个view上时,这个回调可能被调用,这样这个view可以根据初始的selection状态
     * 来调整布局
     *
     *
     * @param listener The listener to be invoked.
     */
    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mLayoutManager.setOnChildSelectedListener(listener);
    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
     * been selected.  Note that the listener may be invoked when there is a
     * layout pending on the view, affording the listener an opportunity to
     * adjust the upcoming layout based on the selection state.
     * This method will clear all existing listeners added by
     * {@link #addOnChildViewHolderSelectedListener}.
     * 注册一个回调,当一个条目被选中的时候调用.
     * 注意:当布局这个view上时,这个回调可能被调用,样这个view可以根据初始的selection状态
     * 来调整布局.调用这个方法时,之前通过addOnChildViewHolderSelectedListener()方法设置的
     * 监听器会被全部清除
     *
     * @param listener The listener to be invoked.
     */
    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        mLayoutManager.setOnChildViewHolderSelectedListener(listener);
    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
     * been selected.  Note that the listener may be invoked when there is a
     * layout pending on the view, affording the listener an opportunity to
     * adjust the upcoming layout based on the selection state.
     * 注册一个回调,当一个条目被选中的时候调用.
     * 注意:当布局这个view上时,这个回调可能被调用,样这个view可以根据初始的selection状态
     * 来调整布局.
     *
     * @param listener The listener to be invoked.
     */
    public void addOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        mLayoutManager.addOnChildViewHolderSelectedListener(listener);
    }

    /**
     * Remove the callback invoked when an item in BaseGridView has been selected.
     * 移除这个监听器
     *
     * @param listener The listener to be removed.
     */
    public void removeOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener)
    {
        mLayoutManager.removeOnChildViewHolderSelectedListener(listener);
    }

    /**
     * Changes the selected item immediately without animation.
     * 马上改变被选中的item,动画不会被调用
     */
    public void setSelectedPosition(int position) {
        mLayoutManager.setSelection(this, position, 0);
    }

    /**
     * Changes the selected item and/or subposition immediately without animation.
     * 动画不会被调用
     */
    public void setSelectedPositionWithSub(int position, int subposition) {
        mLayoutManager.setSelectionWithSub(this, position, subposition, 0);
    }

    /**
     * Changes the selected item immediately without animation, scrollExtra is
     * applied in primary scroll direction.  The scrollExtra will be kept until
     * another {@link #setSelectedPosition} or {@link #setSelectedPositionSmooth} call.
     * 马上改变被选中的item,动画不会被调用,额外的滚动会执行,这个动作会持续到setSelectedPosition()或
     * setSelectedPositionSmooth()在此被调用
     */
    public void setSelectedPosition(int position, int scrollExtra) {
        mLayoutManager.setSelection(this, position, scrollExtra);
    }

    /**
     * Changes the selected item and/or subposition immediately without animation, scrollExtra is
     * applied in primary scroll direction.  The scrollExtra will be kept until
     * another {@link #setSelectedPosition} or {@link #setSelectedPositionSmooth} call.
     */
    public void setSelectedPositionWithSub(int position, int subposition, int scrollExtra) {
        mLayoutManager.setSelectionWithSub(this, position, subposition, scrollExtra);
    }

    /**
     * Changes the selected item and run an animation to scroll to the target
     * position.
     * 平滑的移动到被选中的条目
     */
    public void setSelectedPositionSmooth(int position) {
        mLayoutManager.setSelectionSmooth(this, position);
    }

    /**
     * Changes the selected item and/or subposition, runs an animation to scroll to the target
     * position.
     */
    public void setSelectedPositionSmoothWithSub(int position, int subposition) {
        mLayoutManager.setSelectionSmoothWithSub(this, position, subposition);
    }

    /**
     * Perform a task on ViewHolder at given position after smooth scrolling to it.
     * 平滑滚动到某个item时,在这个viewHolder做一些操作
     * @param position Position of item in adapter.
     * @param task Task to executed on the ViewHolder at a given position.
     */
    public void setSelectedPositionSmooth(final int position, final ViewHolderTask task) {
        if (task != null) {
            ViewHolder vh = findViewHolderForPosition(position);
            if (vh == null) {
                addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
                    public void onChildViewHolderSelected(RecyclerView parent,
                                                          ViewHolder child, int selectedPosition, int subposition) {
                        if (selectedPosition == position) {
                            removeOnChildViewHolderSelectedListener(this);
                            task.run(child);
                        }
                    }
                });
            } else {
                task.run(vh);
            }
        }
        setSelectedPositionSmooth(position);
    }

    /**
     * Perform a task on ViewHolder at given position after scroll to it.
     * 同上,不是平滑的移动
     * @param position Position of item in adapter.
     * @param task Task to executed on the ViewHolder at a given position.
     */
    public void setSelectedPosition(final int position, final ViewHolderTask task) {
        if (task != null) {
            ViewHolder vh = findViewHolderForPosition(position);
            if (vh == null) {
                addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
                    public void onChildViewHolderSelected(RecyclerView parent,
                                                          ViewHolder child, int selectedPosition, int subposition) {
                        if (selectedPosition == position) {
                            removeOnChildViewHolderSelectedListener(this);
                            task.run(child);
                        }
                    }
                });
            } else {
                task.run(vh);
            }
        }
        setSelectedPosition(position);
    }

    /**
     * Returns the selected item position.
     * 得到当前被选中的条目位置
     */
    public int getSelectedPosition() {
        return mLayoutManager.getSelection();
    }

    /**
     * subposition与ItemAlignmentFacet有关
     * Returns the sub selected item position started from zero.  An item can have
     * multiple {@link ItemAlignmentFacet}s provided by {@link ViewHolder}
     * or {@link FacetProviderAdapter}.  Zero is returned when no {@link ItemAlignmentFacet}
     * is defined.
     */
    public int getSelectedSubPosition() {
        return mLayoutManager.getSubSelection();
    }

    /**
     * Sets whether an animation should run when a child changes size or when adding
     * or removing a child.
     * 当子条目的尺寸改变时,或者添加或删除一个子条目时,是否应该有动画
     * <p><i>Unstable API, might change later.</i>
     * 不是确定的API,后面可能改变
     */
    public void setAnimateChildLayout(boolean animateChildLayout) {
        if (mAnimateChildLayout != animateChildLayout) {
            mAnimateChildLayout = animateChildLayout;
            if (!mAnimateChildLayout) {
                mSavedItemAnimator = getItemAnimator();
                super.setItemAnimator(null);
            } else {
                super.setItemAnimator(mSavedItemAnimator);
            }
        }
    }

    /**
     * Returns true if an animation will run when a child changes size or when
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public boolean isChildLayoutAnimated() {
        return mAnimateChildLayout;
    }

    /**
     * Sets the gravity used for child view positioning. Defaults to
     * GRAVITY_TOP|GRAVITY_START.
     * 设置子View放置的位置
     *
     * @param gravity See {@link Gravity}
     */
    public void setGravity(int gravity) {
        mLayoutManager.setGravity(gravity);
        requestLayout();
    }

    @Override
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return mLayoutManager.gridOnRequestFocusInDescendants(this, direction,
                                                              previouslyFocusedRect);
    }

    /**
     * Returns the x/y offsets to final position from current position if the view
     * is selected.
     * 得到当某个view被选中时,当前view距离最后最后位置的偏移值
     *
     * @param view The view to get offsets.
     * @param offsets offsets[0] holds offset of X, offsets[1] holds offset of Y.
     * offsets[0]=x,offsets[1]=y
     */
    public void getViewSelectedOffsets(View view, int[] offsets) {
        mLayoutManager.getViewSelectedOffsets(view, offsets);
    }

    @Override
    public int getChildDrawingOrder(int childCount, int i) {
        return mLayoutManager.getChildDrawingOrder(this, childCount, i);
    }

    final boolean isChildrenDrawingOrderEnabledInternal() {
        return isChildrenDrawingOrderEnabled();
    }

    @Override
    public View focusSearch(int direction) {
        if (isFocused()) {
            // focusSearch(int) is called when GridView itself is focused.
        	// 当当前的gridView有焦点的时候,focusSearch才会被调用
            // Calling focusSearch(view, int) to get next sibling of current selected child.
        	// 查找当前选中条目的下一个条目
            View view = mLayoutManager.findViewByPosition(mLayoutManager.getSelection());
            if (view != null) {
                return focusSearch(view, direction);
            }
        }
        // otherwise, go to mParent to perform focusSearch
        return super.focusSearch(direction);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        mLayoutManager.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    /**
     * Disables or enables focus search.
     * 当前view是否可以查找下一个子条目
     */
    public final void setFocusSearchDisabled(boolean disabled) {
        // LayoutManager may detachView and attachView in fastRelayout, it causes RowsFragment
        // re-gain focus after a BACK key pressed, so block children focus during transition.
        setDescendantFocusability(disabled ? FOCUS_BLOCK_DESCENDANTS: FOCUS_AFTER_DESCENDANTS);
        mLayoutManager.setFocusSearchDisabled(disabled);
    }

    /**
     * Returns true if focus search is disabled.
     * 返回
     */
    public final boolean isFocusSearchDisabled() {
        return mLayoutManager.isFocusSearchDisabled();
    }

    /**
     * Enables or disables layout.  All children will be removed when layout is
     * disabled.
     * 布局是否可用,如果不可用,则会移除所有的子条目
     */
    public void setLayoutEnabled(boolean layoutEnabled) {
        mLayoutManager.setLayoutEnabled(layoutEnabled);
    }

    /**
     * Changes and overrides children's visibility.
     * children是否可见
     */
    public void setChildrenVisibility(int visibility) {
        mLayoutManager.setChildrenVisibility(visibility);
    }

    /**
     * Enables or disables pruning of children.  Disable is useful during transition.
     * 子条目是否可以删减,设置为disable对过渡有用
     */
    public void setPruneChild(boolean pruneChild) {
        mLayoutManager.setPruneChild(pruneChild);
    }

    /**
     * Enables or disables scrolling.  Disable is useful during transition.
     * 是否可以滑动,设置为disable对过渡有用
     */
    public void setScrollEnabled(boolean scrollEnabled) {
        mLayoutManager.setScrollEnabled(scrollEnabled);
    }

    /**
     * Returns true if scrolling is enabled.
     */
    public boolean isScrollEnabled() {
        return mLayoutManager.isScrollEnabled();
    }

    /**
     * Returns true if the view at the given position has a same row sibling
     * in front of it.  This will return true if first item view is not created.
     * So application should check in both {@link OnChildSelectedListener} and {@link
     * OnChildLaidOutListener}.
     * 如果在同一行的前面还有兄弟,则返回true.
     * 如果第一个条目还没有创建,会返回true
     * 所以应用要在这两个方法中都判断一下
     *
     * @param position Position in adapter.
     */
    public boolean hasPreviousViewInSameRow(int position) {
        return mLayoutManager.hasPreviousViewInSameRow(position);
    }

    /**
     * Enables or disables the default "focus draw at last" order rule.
     * 设定"最后绘制焦点"这条规则是否有效
     */
    public void setFocusDrawingOrderEnabled(boolean enabled) {
        super.setChildrenDrawingOrderEnabled(enabled);
    }

    /**
     * Returns true if default "focus draw at last" order rule is enabled.
     * "最后绘制焦点"这条规则如果有效,则返回true
     */
    public boolean isFocusDrawingOrderEnabled() {
        return super.isChildrenDrawingOrderEnabled();
    }

    /**
     * Sets the touch intercept listener.
     * 设置触摸事件是否打断的监听器
     */
    public void setOnTouchInterceptListener(OnTouchInterceptListener listener) {
        mOnTouchInterceptListener = listener;
    }

    /**
     * Sets the generic motion intercept listener.
     * 设置一般事件是否打断的监听器
     */
    public void setOnMotionInterceptListener(OnMotionInterceptListener listener) {
        mOnMotionInterceptListener = listener;
    }

    /**
     * Sets the key intercept listener.
     * 设置键盘事件是否打断的监听器
     */
    public void setOnKeyInterceptListener(OnKeyInterceptListener listener) {
        mOnKeyInterceptListener = listener;
    }

    /**
     * Sets the unhandled key listener.
     * 设置未处理键盘事件的监听器
     */
    public void setOnUnhandledKeyListener(OnUnhandledKeyListener listener) {
        mOnUnhandledKeyListener = listener;
    }

    /**
     * Returns the unhandled key listener.
     * 返回未处理键盘事件的监听器
     */
    public OnUnhandledKeyListener getOnUnhandledKeyListener() {
        return mOnUnhandledKeyListener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mOnKeyInterceptListener != null && mOnKeyInterceptListener.onInterceptKeyEvent(event)) {
            return true;
        }
        if (super.dispatchKeyEvent(event)) {
            return true;
        }
        if (mOnUnhandledKeyListener != null && mOnUnhandledKeyListener.onUnhandledKey(event)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mOnTouchInterceptListener != null) {
            if (mOnTouchInterceptListener.onInterceptTouchEvent(event)) {
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchGenericFocusedEvent(MotionEvent event) {
        if (mOnMotionInterceptListener != null) {
            if (mOnMotionInterceptListener.onInterceptMotionEvent(event)) {
                return true;
            }
        }
        return super.dispatchGenericFocusedEvent(event);
    }

    /**
     * Returns the policy for saving children.
     * 获取保存子条目的方式
     *
     * @return policy, one of {@link #SAVE_NO_CHILD}
     * {@link #SAVE_ON_SCREEN_CHILD} {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}.
     */
    public final int getSaveChildrenPolicy() {
        return mLayoutManager.mChildrenStates.getSavePolicy();
    }

    /**
     * Returns the limit used when when {@link #getSaveChildrenPolicy()} is
     *         {@link #SAVE_LIMITED_CHILD}
     * 当缓存方针为SAVE_LIMITED_CHILD时,返回限制数目
     */
    public final int getSaveChildrenLimitNumber() {
        return mLayoutManager.mChildrenStates.getLimitNumber();
    }

    /**
     * Sets the policy for saving children.
     * @param savePolicy One of {@link #SAVE_NO_CHILD} {@link #SAVE_ON_SCREEN_CHILD}
     * {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}.
     * 设置保存子view的方式
     */
    public final void setSaveChildrenPolicy(int savePolicy) {
        mLayoutManager.mChildrenStates.setSavePolicy(savePolicy);
    }

    /**
     * Sets the limit number when {@link #getSaveChildrenPolicy()} is {@link #SAVE_LIMITED_CHILD}.
     * 当缓存方针为SAVE_LIMITED_CHILD时,设置限制数目
     */
    public final void setSaveChildrenLimitNumber(int limitNumber) {
        mLayoutManager.mChildrenStates.setLimitNumber(limitNumber);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return mHasOverlappingRendering;
    }

    public void setHasOverlappingRendering(boolean hasOverlapping) {
        mHasOverlappingRendering = hasOverlapping;
    }

    /**
     * Notify layout manager that layout directionality has been updated
     * 通知布局管理器,rtl属性改变了
     */
    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        mLayoutManager.onRtlPropertiesChanged(layoutDirection);
    }

    @Override
    public void setRecyclerListener(RecyclerListener listener) {
        mChainedRecyclerListener = listener;
    }

    /**
     * Sets pixels of extra space for layout child in invisible area.
     * 在可视区域设立一些额外的空间来布局子对象
     * @param extraLayoutSpace  Pixels of extra space for layout invisible child.
     *                          Must be bigger or equals to 0.
     * @hide
     */
    public void setExtraLayoutSpace(int extraLayoutSpace) {
        mLayoutManager.setExtraLayoutSpace(extraLayoutSpace);
    }

    /**
     * Returns pixels of extra space for layout child in invisible area.
     * 返回设定的额外空间大小
     * @hide
     */
    public int getExtraLayoutSpace() {
        return mLayoutManager.getExtraLayoutSpace();
    }

}
