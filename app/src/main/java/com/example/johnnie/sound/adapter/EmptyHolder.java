package com.example.johnnie.sound.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.example.johnnie.sound.R;
import com.nulldreams.adapter.AbsViewHolder;
import com.nulldreams.adapter.DelegateAdapter;

/**
 * Created by johnnie on 2018/5/25.
 */

public class EmptyHolder extends AbsViewHolder<EmptyDelegate> {

    private TextView mMessageTv;

    public EmptyHolder(View itemView) {
        super(itemView);
        mMessageTv = (TextView)itemView.findViewById(R.id.empty_message);
    }

    @Override
    public void onBindView(Context context, EmptyDelegate emptyDelegate, int position, DelegateAdapter adapter) {
        String message = emptyDelegate.getSource();
        mMessageTv.setText(message);

    }
}
