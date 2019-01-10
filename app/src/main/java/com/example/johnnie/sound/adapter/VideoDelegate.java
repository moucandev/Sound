package com.example.johnnie.sound.adapter;

import android.os.Bundle;

import com.example.johnnie.sound.R;
import com.example.johnnie.sound.models.Video;
import com.nulldreams.adapter.annotation.AnnotationDelegate;
import com.nulldreams.adapter.annotation.DelegateInfo;

@DelegateInfo(layoutID = R.layout.item_video, holderClass = VideoHolder.class)
public class VideoDelegate extends AnnotationDelegate<Video> {
    private boolean isSelected = false;
    public VideoDelegate(Video video) {
        super(video);
    }
    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
