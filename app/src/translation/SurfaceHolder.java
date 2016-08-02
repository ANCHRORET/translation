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

import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Abstract interface to someone holding a display surface.  Allows you to
 * control the surface size and format, edit the pixels in the surface, and
 * monitor changes to the surface.  This interface is typically available
 * through the {@link SurfaceView} class.
 * 这是一个接口，持有一个展示的surface，使你可以控制surface的尺寸和格式，编辑surface上的
 * 像素点，监测surface的改变。这个借口通常用来SurfaceView类里面
 *
 * <p>When using this interface from a thread other than the one running
 * its {@link SurfaceView}, you will want to carefully read the
 * methods
 * {@link #lockCanvas} and {@link Callback#surfaceCreated Callback.surfaceCreated()}.
 * 当在一个线程中使用这个接口时，而不是创建SurfaceView的线程，你应该仔细的阅读下面的两个个回调
 */
public interface SurfaceHolder {

    /** @deprecated this is ignored, this value is set automatically when needed. */
    @Deprecated
    public static final int SURFACE_TYPE_NORMAL = 0;
    /** @deprecated this is ignored, this value is set automatically when needed. */
    @Deprecated
    public static final int SURFACE_TYPE_HARDWARE = 1;
    /** @deprecated this is ignored, this value is set automatically when needed. */
    @Deprecated
    public static final int SURFACE_TYPE_GPU = 2;
    /** @deprecated this is ignored, this value is set automatically when needed. */
    @Deprecated
    public static final int SURFACE_TYPE_PUSH_BUFFERS = 3;

    /**
     * Exception that is thrown from {@link #lockCanvas} when called on a Surface
     * whose type is SURFACE_TYPE_PUSH_BUFFERS.
     * 调用lockCanvas（）时，如果surface的类型是SURFACE_TYPE_PUSH_BUFFERS，会抛出此异常，
     */
    public static class BadSurfaceTypeException extends RuntimeException {
        public BadSurfaceTypeException() {
        }

        public BadSurfaceTypeException(String name) {
            super(name);
        }
    }

    /**
     * A client may implement this interface to receive information about
     * changes to the surface.  When used with a {@link SurfaceView}, the
     * Surface being held is only available between calls to
     * 使用者实现这个接口时，需要接收这个surface改变的信息。当和SurfaceView一起使用时，
     * 持有的surface，在这两个方法调用之间是有效的
     * {@link #surfaceCreated(SurfaceHolder)} and
     * {@link #surfaceDestroyed(SurfaceHolder)}.  The Callback is set with
     * {@link SurfaceHolder#addCallback SurfaceHolder.addCallback} method.
     * 这个回调通过SurfaceHolder.addCallback()方法来设置
     */
    public interface Callback {
        /**
         * This is called immediately after the surface is first created.
         * Implementations of this should start up whatever rendering code
         * they desire.  Note that only one thread can ever draw into
         * a {@link Surface}, so you should not draw into the Surface here
         * if your normal rendering will be in another thread.
         * surface第一次创建时，马上会调用此方法。这个方法应该开始启动，无论何时代码开始渲染
         * 注意：只有一个线程可以向Surface上绘制东西，所以你不应该在这个方法里面向surface上绘制东西
         * 通常渲染会在另外的线程
         *
         *
         * @param holder The SurfaceHolder whose surface is being created.
         *  被创建surface的持有者
         */
        public void surfaceCreated(SurfaceHolder holder);

        /**
         * This is called immediately after any structural changes (format or
         * size) have been made to the surface.  You should at this point update
         * the imagery in the surface.  This method is always called at least
         * once, after {@link #surfaceCreated}.
         * 当surface的结构（格式或者尺寸）改变之后，这个方法立刻调用。你应该在这时更新surface上的图像
         * 在surfaceCreated()之后，这个方法至少被调用一次
         *
         * @param holder The SurfaceHolder whose surface has changed.
         * @param format The new PixelFormat of the surface.
         *               surface的像素格式
         * @param width The new width of the surface.
         * @param height The new height of the surface.
         *               surface的新尺寸
         */
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height);

        /**
         * This is called immediately before a surface is being destroyed. After
         * returning from this call, you should no longer try to access this
         * surface.  If you have a rendering thread that directly accesses
         * the surface, you must ensure that thread is no longer touching the
         * Surface before returning from this function.
         * 在surface被销毁之前，这个方法先被调用。这个方法执行完后，你不应该再操作这个surface
         * 如果你有一个渲染线程来直接操作这个surface，当这个方法执行完后，你应该保证这个线程不
         * 会再操作这个surface
         *
         * @param holder The SurfaceHolder whose surface is being destroyed.
         */
        public void surfaceDestroyed(SurfaceHolder holder);
    }

    /**
     * Additional callbacks that can be received for {@link Callback}.
     * 另外的回调
     */
    public interface Callback2 extends Callback {
        /**
         * Called when the application needs to redraw the content of its
         * surface, after it is resized or for some other reason.  By not
         * returning from here until the redraw is complete, you can ensure that
         * the user will not see your surface in a bad state (at its new
         * size before it has been correctly drawn that way).  This will
         * typically be preceeded by a call to {@link #surfaceChanged}.
         * 当应用需要重绘这个surface的内容时调用，当surface尺寸改变了或者其他的什么原因
         * 不要在这里返回直到重绘完成，通过这种方式，你可以保证用户不会看到你未绘制完成的surface
         * （当你正确绘制之前，尺寸已经改变了）。这个方法一般在surfaceChanged之前。
         *
         * @param holder The SurfaceHolder whose surface has changed.
         */
        public void surfaceRedrawNeeded(SurfaceHolder holder);
    }

    /**
     * Add a Callback interface for this holder.  There can several Callback
     * interfaces associated with a holder.
     * 给这个holder添加一个回调，一个holder可以有多个回调
     *
     * @param callback The new Callback interface.
     */
    public void addCallback(Callback callback);

    /**
     * Removes a previously added Callback interface from this holder.
     * 移除先前添加的回调
     *
     * @param callback The Callback interface to remove.
     */
    public void removeCallback(Callback callback);

    /**
     * Use this method to find out if the surface is in the process of being
     * created from Callback methods. This is intended to be used with
     * {@link Callback#surfaceChanged}.
     * 通过这个方法来判断surface是否在创建的过程中，意图配合surfaceChanged（）使用
     *
     * @return true if the surface is in the process of being created.
     * surface正在被创建时返回true
     */
    public boolean isCreating();

    /**
     * Sets the surface's type.
     * 设置surface的type
     *
     * @deprecated this is ignored, this value is set automatically when needed.
     */
    @Deprecated
    public void setType(int type);

    /**
     * Make the surface a fixed size.  It will never change from this size.
     * When working with a {@link SurfaceView}, this must be called from the
     * same thread running the SurfaceView's window.
     * 使surface固定一个尺寸。尺寸不会在改变。当和surfaceView一起使用时，这个必须在
     * 创建surfaceView所在窗口的线程中调用
     *
     * @param width The surface's width.
     * @param height The surface's height.
     */
    public void setFixedSize(int width, int height);

    /**
     * Allow the surface to resized based on layout of its container (this is
     * the default).  When this is enabled, you should monitor
     * {@link Callback#surfaceChanged} for changes to the size of the surface.
     * When working with a {@link SurfaceView}, this must be called from the
     * same thread running the SurfaceView's window.
     * 允许surface随着容器改变而改变（这是默认设置）。当这个设置有效时，你应该在surfaceChanged（）
     * 方法中监听surface的尺寸变化。当和surfaceView一起使用时，这个必须在创建surfaceView所在窗口的线程中调用
     */
    public void setSizeFromLayout();

    /**
     * Set the desired PixelFormat of the surface.  The default is OPAQUE.
     * When working with a {@link SurfaceView}, this must be called from the
     * same thread running the SurfaceView's window.
     * 给surface设置一个想要的像素格式。默认是不透明的。当和SurfaceView一起使用时，
     * 这个必须在创建surfaceView所在窗口的线程中调用
     *
     * @param format A constant from PixelFormat.
     *               参数，PixelFormat里的一个常量
     *
     * @see android.graphics.PixelFormat
     */
    public void setFormat(int format);

    /**
     * Enable or disable option to keep the screen turned on while this
     * surface is displayed.  The default is false, allowing it to turn off.
     * This is safe to call from any thread.
     * 当surface展示出来时，设置是否保持screen一直打开，
     *
     * @param screenOn Set to true to force the screen to stay on, false
     * to allow it to turn off.
     */
    public void setKeepScreenOn(boolean screenOn);

    /**
     * Start editing the pixels in the surface.  The returned Canvas can be used
     * to draw into the surface's bitmap.  A null is returned if the surface has
     * not been created or otherwise cannot be edited.  You will usually need
     * to implement {@link Callback#surfaceCreated Callback.surfaceCreated}
     * to find out when the Surface is available for use.
     * 开始编辑surface上的像素。返回的画布可以用来向surface上绘制图片，如果surface还没有创建
     * 或者surface不可编辑时，返回null。你需要实现这个方法Callback.surfaceCreated（）,来判断
     * surface是否是可用的
     *
     * <p>The content of the Surface is never preserved between unlockCanvas() and
     * lockCanvas(), for this reason, every pixel within the Surface area
     * must be written. The only exception to this rule is when a dirty
     * rectangle is specified, in which case, non-dirty pixels will be
     * preserved.
     * 在unlockCanvas()和lockCanvas()之间，surface的内容不会被保留，因为这个原因，surface区域内
     * 所有的像素点需要被重写。唯一的例外是，一块无效的长方形区域被指定了。这种情况下，有效的像素会被
     * 保留
     *
     * <p>If you call this repeatedly when the Surface is not ready (before
     * {@link Callback#surfaceCreated Callback.surfaceCreated} or after
     * {@link Callback#surfaceDestroyed Callback.surfaceDestroyed}), your calls
     * will be throttled to a slow rate in order to avoid consuming CPU.
     * 在Callback.surfaceCreated（）或Callback.surfaceDestroyed（）之后，这个方法如果
     * 被调用，你的调用会被限制减慢速度，为了释放cpu
     *
     * <p>If null is not returned, this function internally holds a lock until
     * the corresponding {@link #unlockCanvasAndPost} call, preventing
     * {@link SurfaceView} from creating, destroying, or modifying the surface
     * while it is being drawn.  This can be more convenient than accessing
     * the Surface directly, as you do not need to do special synchronization
     * with a drawing thread in {@link Callback#surfaceDestroyed
     * Callback.surfaceDestroyed}.
     * 如果不为空，这个方法内部会持有一个锁，知道unlockCanvasAndPost（）同步调用，避免当
     * 只在绘制的时候，SurfaceView创建销毁或修改surface。这个会比直接获取surface方便，因为
     * 你不必专门在绘制线程保持与Callback.surfaceDestroyed（）同步
     *
     * @return Canvas Use to draw into the surface.
     */
    public Canvas lockCanvas();


    /**
     * Just like {@link #lockCanvas()} but allows specification of a dirty rectangle.
     * Every
     * pixel within that rectangle must be written; however pixels outside
     * the dirty rectangle will be preserved by the next call to lockCanvas().
     * 就像lockCanvas()方法一样，但是允许局部绘制，这个长方形内的每个像素要被重绘；这个长方形外的像素会被
     * 保留至下一次lockCanvas()的调用
     *
     * @see android.view.SurfaceHolder#lockCanvas
     *
     * @param dirty Area of the Surface that will be modified.
     * @return Canvas Use to draw into the surface.
     */
    public Canvas lockCanvas(Rect dirty);

    /**
     * Finish editing pixels in the surface.  After this call, the surface's
     * current pixels will be shown on the screen, but its content is lost,
     * in particular there is no guarantee that the content of the Surface
     * will remain unchanged when lockCanvas() is called again.
     * 完成surface的编辑，这个方法调用之后，surface上现在的像素显示到屏幕上。但是内容
     * 会遗失，尤其不能保证Surface上的内容会保持到下一次lockCanvas()调用
     *
     * @see #lockCanvas()
     *
     * @param canvas The Canvas previously returned by lockCanvas().
     */
    public void unlockCanvasAndPost(Canvas canvas);

    /**
     * Retrieve the current size of the surface.  Note: do not modify the
     * returned Rect.  This is only safe to call from the thread of
     * {@link SurfaceView}'s window, or while inside of
     * {@link #lockCanvas()}.
     * 重新获取surface的尺寸。
     * 注意：不要修改获得的Rect,只有在lockCanvas()时，或者在SurfaceView的窗口所在的
     * 线程调用是安全的（这个安全，我的理解是在其他线程，Rect也可能已经被修改）
     *
     * @return Rect The surface's dimensions.  The left and top are always 0.
     */
    public Rect getSurfaceFrame();

    /**
     * Direct access to the surface object.  The Surface may not always be
     * available -- for example when using a {@link SurfaceView} the holder's
     * Surface is not created until the view has been attached to the window
     * manager and performed a layout in order to determine the dimensions
     * and screen position of the Surface.    You will thus usually need
     * to implement {@link Callback#surfaceCreated Callback.surfaceCreated}
     * to find out when the Surface is available for use.
     * 直接得到surface对象。这个surface不能保证一定有效--例如：当使用SurfaceView时，
     * holder里的Surface还没有创建，直到view被关联到window manager或者实现surface的布局
     * 和surface的位置时。因此你需要来实现Callback.surfaceCreated（）这个方法来保证
     * Surface是有效可以使用的
     *
     *
     * <p>Note that if you directly access the Surface from another thread,
     * it is critical that you correctly implement
     * {@link Callback#surfaceCreated Callback.surfaceCreated} and
     * {@link Callback#surfaceDestroyed Callback.surfaceDestroyed} to ensure
     * that thread only accesses the Surface while it is valid, and that the
     * Surface does not get destroyed while the thread is using it.
     * 注意：如果在里一个线程直接获取Surface，你需要通过实现Callback.surfaceCreated（）和
     * Callback.surfaceDestroyed（）来确保只有这个线程能获得有效的Surface，并且保证当这个线程
     * 在使用这个surface时，surface不会被销毁，这样做是不好的。
     *
     * <p>This method is intended to be used by frameworks which often need
     * direct access to the Surface object (usually to pass it to native code).
     * 这个方法的意图是被frameworks使用，因为frameworks需要直接来获取Surface对象（通常
     * 通过本地化的代码会使用）
     *
     * @return Surface The surface.
     */
    public Surface getSurface();
}
