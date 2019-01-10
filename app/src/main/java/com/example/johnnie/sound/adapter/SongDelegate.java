package com.example.johnnie.sound.adapter;

import com.example.johnnie.sound.R;
import com.example.johnnie.sound.models.Song;
import com.nulldreams.adapter.annotation.AnnotationDelegate;
import com.nulldreams.adapter.annotation.DelegateInfo;

/**
 * Created by johnnie on 2018/5/20.
 */

@DelegateInfo(layoutID = R.layout.layout_song, holderClass = SongHolder.class)
public class SongDelegate extends AnnotationDelegate<Song> {

    private boolean isSelected = false;

    public SongDelegate(Song song) {
        super(song);
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
