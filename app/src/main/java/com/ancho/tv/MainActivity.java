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

package com.ancho.tv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.ShadowOverlayContainer;
import android.support.v17.leanback.widget.ShadowOverlayHelper;
import android.support.v17.leanback.widget.VerticalGridView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity
        extends Activity
{
    private ShadowOverlayHelper mHelper;

    //    private VerticalGridView   mHgv1;
    //    private VerticalGridView mHgv2;

    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);
        initView();
    }

    private void initView() {
        ViewGroup           parent  = (ViewGroup) findViewById(R.id.container_parent);
        mHelper = new ShadowOverlayHelper.Builder()
                            .needsOverlay(false).needsRoundedCorner(false)
                            .needsShadow(true)
                            .build(this);
        mHelper.prepareParentForShadow(parent); // apply optical-bounds for 9-patch shadow.

        ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.bm_hot_album_default_0);
        img.setFocusable(true);
        img.setFocusableInTouchMode(true);
        img.setAdjustViewBounds(true);
//        img.setPadding(20,10,20,10);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                       ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(0,50,0,0);
        img.setLayoutParams(params);

        View view = initializeView(img);
        parent.addView(view);

//        mHelper.setOverlayColor(view, getResources().getColor(R.color.btn_color));
        mHelper.setShadowFocusLevel(view, 0.8f);

        //        mHgv1 = (VerticalGridView) findViewById(R.id.hgv1);
        //        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
        //        ll.setOrientation(LinearLayout.VERTICAL);
        //        ll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        //        mHgv1.setAdapter(new MyAdapter());
        //        mHgv1.setf
        //        mHgv1.setFadingRightEdge(true);

        //        mHgv2 = (VerticalGridView) findViewById(R.id.hgv2);
        //        mHgv2.setAdapter(new MyAdapter());
        img.requestFocus();
    }


     public View initializeView(View view) {
         if (mHelper.needsWrapper()) {
              ShadowOverlayContainer wrapper = mHelper.createShadowOverlayContainer(this);
              wrapper.wrap(view);
              return wrapper;
          } else {
              mHelper.onViewCreated(view);
              return view;
          }
     }

}
