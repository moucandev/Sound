package com.example.johnnie.sound.adapter;

import android.content.Context;
import android.support.annotation.StringRes;

import com.example.johnnie.sound.R;
import com.nulldreams.adapter.annotation.AnnotationDelegate;
import com.nulldreams.adapter.annotation.DelegateInfo;

/**
 * Created by johnnie on 2018/5/26.
 */
@DelegateInfo(layoutID = R.layout.layout_empty, holderClass = EmptyHolder.class)
public class EmptyDelegate extends AnnotationDelegate<String> {

    public EmptyDelegate(String s) {
        super(s);
    }

    public EmptyDelegate(Context context, @StringRes int stringRes) {
        this(context.getString(stringRes));
    }
}
