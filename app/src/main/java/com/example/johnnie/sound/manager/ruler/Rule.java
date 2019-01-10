package com.example.johnnie.sound.manager.ruler;

import com.example.johnnie.sound.models.Song;

import java.util.List;

/**
 * Created by johnnie on 2018/5/13.
 */

public interface Rule {
    Song previous (Song song, List<Song> songList, boolean isUserAction);
    Song next(Song song, List<Song> songList, boolean isUserAction);
    void clear ();
}
