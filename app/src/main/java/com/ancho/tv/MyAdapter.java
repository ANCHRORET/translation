package com.ancho.tv;

import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Ancho on 2016/6/8.
 */
public class MyAdapter
        extends RecyclerView.Adapter
{
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TestViewHolder(LayoutInflater.from(parent.getContext())
                                                .inflate(R.layout.test_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TestViewHolder         viewHolder   = (TestViewHolder) holder;
        viewHolder.tv.setText("this is " + position);
    }

    @Override
    public int getItemCount() {
        return 5;
    }

    private static class TestViewHolder
            extends RecyclerView.ViewHolder
    {

        TextView tv;

        public TestViewHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView.findViewById(R.id.btn);
        }
    }
}
