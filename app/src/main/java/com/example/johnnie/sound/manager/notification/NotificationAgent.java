package com.example.johnnie.sound.manager.notification;

import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.example.johnnie.sound.manager.PlayManager;
import com.example.johnnie.sound.models.Song;
import com.example.johnnie.sound.service.PlayService;

/**
 * Created by johnnie on 2018/5/12.
 */

public interface NotificationAgent {

    NotificationCompat.Builder getBuilder (Context context, PlayManager manager, @PlayService.State int state, Song song);
    void afterNotify();

}