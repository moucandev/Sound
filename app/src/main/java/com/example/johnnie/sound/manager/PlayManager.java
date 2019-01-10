package com.example.johnnie.sound.manager;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.example.johnnie.sound.manager.notification.NotificationAgent;
import com.example.johnnie.sound.manager.ruler.Rule;
import com.example.johnnie.sound.manager.ruler.Rulers;
import com.example.johnnie.sound.models.Album;
import com.example.johnnie.sound.models.Song;
import com.example.johnnie.sound.receiver.RemoteControlReceiver;
import com.example.johnnie.sound.receiver.SimpleBroadcastReceiver;
import com.example.johnnie.sound.service.PlayService;
import com.example.johnnie.sound.utils.MediaUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by johnnie on 2018/5/11.
 */

public class PlayManager implements PlayService.PlayStateChangeListener {

    private static final String TAG = PlayManager.class.getSimpleName();

    private static PlayManager sManager = null;

    // 单例化，通过getInstance调用，每次返回同一个对象
    public synchronized static PlayManager getInstance (Context context) {
        if (sManager == null) {
            sManager = new PlayManager(context.getApplicationContext());
        }
        return sManager;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((PlayService.PlayBinder)service).getService();
            mService.setPlayStateChangeListener(PlayManager.this);
            Log.v(TAG, "onServiceConnected " + mSong);
            startRemoteControl();
            if (!isPlaying()) {
                dispatch(mSong, "onServiceConnected");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "onServiceDisconnected " + name);
            mService.setPlayStateChangeListener(null);
            mService = null;

            startPlayService();
            bindPlayService();
        }
    };

    // 处理意外情况
    private SimpleBroadcastReceiver mNoisyReceiver = new SimpleBroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
                pause(false);
            }
        }

    };

    // Audio占用情况改变监控
    private AudioManager.OnAudioFocusChangeListener mAfListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.v(TAG, "onAudioFocusChange = " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                if (isPlaying()) {
                    pause(false);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {

            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (isPaused() && !isPausedByUser()) {
                    resume();
                }
            }
        }
    };

    private int mPeriod = 1000;
    private boolean isProgressUpdating = false;
    private Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCallbacks != null && !mCallbacks.isEmpty()
                    && mService != null && mSong != null && mService.isStarted()) {
                for (ProgressCallback callback : mProgressCallbacks) {
                    callback.onProgress(mService.getPosition(), mSong.getDuration());
                }
                mHandler.postDelayed(this, mPeriod);
                isProgressUpdating = true;
            } else {
                isProgressUpdating = false;
            }
        }
    };

    private Handler mHandler = null;

    private List<Callback> mCallbacks;
    private List<ProgressCallback> mProgressCallbacks;

    private Context mContext;
    private List<Album> mTotalAlbumList;
    private List<Song> mTotalList;
    private List<Song> mCurrentList;
    private Album mCurrentAlbum;
    private Song mSong = null;
    private int mState = PlayService.STATE_IDLE;
    private PlayService mService;

    private Rule mPlayRule = Rulers.RULER_LIST_LOOP;

    private boolean isPausedByUser = false;

    private NotificationAgent mNotifyAgent = null;

    private MediaSessionCompat mMediaSessionCompat;

    private PlayManager (Context context) {
        mContext = context;
        mCallbacks = new ArrayList<>();
        mProgressCallbacks = new ArrayList<>();
        mHandler = new Handler();

    }

    public void getTotalListAsync(final Callback callback) {
        new AsyncTask<Context, Integer, List<Song>>() {

            @Override
            protected List<Song> doInBackground(Context... params) {
                Context context = params[0];
                List<Song> songs = MediaUtils.getAudioList(context);
                mTotalAlbumList = MediaUtils.getAlbumList(context);
                if (songs != null) {
                    for (Song song : songs) {
                        song.setAlbumObj(getAlbum(song.getAlbumId()));
                    }
                }
                return songs;
            }

            @Override
            protected void onPostExecute(List<Song> songs) {
                mTotalList = songs;
                mCurrentList = mTotalList;
                bindPlayService();
                startPlayService();
                for (Callback callback : mCallbacks) {
                    callback.onPlayListPrepared(songs);
                    callback.onAlbumListPrepared(mTotalAlbumList);
                }
                callback.onPlayListPrepared(songs);
                callback.onAlbumListPrepared(mTotalAlbumList);
            }
        }.execute(mContext);
    }

    public List<Song> getTotalList () {
        return mTotalList;
    }

    public List<Song> getAlbumSongList (int albumId) {
        List<Song> songs = MediaUtils.getAlbumSongList(mContext, albumId);
        for (Song song : songs) {
            song.setAlbumObj(getAlbum(song.getAlbumId()));
        }
        return songs;
    }

    public List<Album> getAlbumList () {
        return mTotalAlbumList;
    }

    public Album getAlbum (int albumId) {
        for (Album album : mTotalAlbumList) {
            if (album.getId() == albumId) {
                return album;
            }
        }
        return null;
    }

    // 开关服务
    private void bindPlayService () {
        mContext.bindService(new Intent(mContext, PlayService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    private void unbindPlayService () {
        if (mService != null) {
            mContext.unbindService(mConnection);
        }
    }
    private void startPlayService () {
        mContext.startService(new Intent(mContext, PlayService.class));
    }
    private void stopPlayService () {
        mContext.stopService(new Intent(mContext, PlayService.class));
    }

    /*public void dispatch (Album album, Song song) {
        if (album != null) {
            mCurrentList = getAlbumSongList(album.getId());
        } else {
            mCurrentList = mTotalList;
        }
        dispatch(song);
        mCurrentAlbum = album;
    }
    public void dispatch (Album album) {
        dispatch(album, mPlayRule.next(mSong, mCurrentList, true));
    }*/

    // dispatch:调度

    /**
     *  dispatch the current song
     */
    public void dispatch () {
        dispatch(mSong, "dispatch ()");
    }

    /**
     * dispatch a song.If the song is paused, then resume.If the song is not started, then start it.If the song is playing, then pause it.
     * {@link PlayService#STATE_COMPLETED}
     * @param song the song you want to dispatch, if null, dispatch a song from {@link Rule}.
     * @see Song;
     * @see com.example.johnnie.sound.manager.ruler.Rule;#next(Song, List, boolean);
     */
    public void dispatch(final Song song, String by) {
        Log.v(TAG, "dispatch BY=" + by);
        Log.v(TAG, "dispatch song=" + song);
        Log.v(TAG, "dispatch getAudioFocus mService=" + mService);
        if (mCurrentList == null || mCurrentList.isEmpty() || song == null) {
            return;
        }
        if (mService != null) {
            if (song == null && mSong == null) {
                Song defaultSong = mPlayRule.next(song, mCurrentList, false);
                dispatch(defaultSong, "dispatch(final Song song, String by)");
            } else if (song.equals(mSong)) {
                if (mService.isStarted()) {
                    pause();
                } else if (mService.isPaused()){
                    resume();
                } else {
                    mService.releasePlayer();
                    if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
                        mSong = song;
                        mService.startPlayer(song.getPath());
                    }
                }
            } else {
                mService.releasePlayer();
                if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
                    mSong = song;
                    mService.startPlayer(song.getPath());
                }
            }

        } else {
            Log.v(TAG, "dispatch mService == null");
            mSong = song;
            bindPlayService();
            startPlayService();
        }

    }

    /**
     * you can set a custom {@link Rule} by this
     * @param rule
     */
    public void setRule (@NonNull Rule rule) {
        mPlayRule = rule;
        for (Callback callback : mCallbacks) {
            callback.onPlayRuleChanged(mPlayRule);
        }
    }

    /**
     *
     * @return the current {@link Rule}
     */
    public Rule getRule () {
        return mPlayRule;
    }

    /**
     * next song by user action
     */
    public void next() {
        next(true);
    }

    /**
     * next song triggered by {@link #onStateChanged(int)} and {@link PlayService#STATE_COMPLETED}
     * @param isUserAction
     */
    private void next(boolean isUserAction) {
        dispatch(mPlayRule.next(mSong, mCurrentList, isUserAction), "next(boolean isUserAction)");
    }

    /**
     * previous song by user action
     */
    public void previous () {
        previous(true);
    }

    private void previous (boolean isUserAction) {
        dispatch(mPlayRule.previous(mSong, mCurrentList, isUserAction), "previous (boolean isUserAction)");
    }

    /**
     * resume play
     */
    public void resume () {
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
            mService.resumePlayer();
        }
    }

    /**
     * pause a playing song by user action
     */
    public void pause () {
        pause(true);
    }

    /**
     * pause a playing song
     * @param isPausedByUser false if triggered by {@link AudioManager#AUDIOFOCUS_LOSS} or
     *                       {@link AudioManager#AUDIOFOCUS_LOSS_TRANSIENT}
     */
    private void pause (boolean isPausedByUser) {
        mService.pausePlayer();
        this.isPausedByUser = isPausedByUser;
    }

    public void stop () {
        mService.stopPlayer();
    }

    /**
     * release a playing song
     */
    public void release () {
        mService.releasePlayer();
        unbindPlayService();
        stopPlayService();

        mService.setPlayStateChangeListener(null);
        mService = null;
    }

    public MediaSessionCompat getMediaSessionCompat () {
        return mMediaSessionCompat;
    }

    private void startRemoteControl() {
        ComponentName mediaButtonReceiver = new ComponentName(mContext, RemoteControlReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(mContext, TAG, mediaButtonReceiver, null);
        mMediaSessionCompat.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mMediaSessionCompat.setCallback(mSessionCallback);
        setSessionActive(true);
        changeMediaSessionState(PlayService.STATE_IDLE);
    }

    private void setSessionActive (boolean active) {
        mMediaSessionCompat.setActive(active);
    }

    private void stopRemoteControl () {
        if (mMediaSessionCompat != null) {
            mMediaSessionCompat.release();
        }
    }

    private void changeMediaSessionMetadata (@PlayService.State int state) {
        if (mMediaSessionCompat == null || !mMediaSessionCompat.isActive()) {
            return;
        }
        final boolean hasSong = mSong != null;
        String title = hasSong ? mSong.getTitle() : "";
        String album = hasSong ? mSong.getAlbum() : "";
        String artist = hasSong ? mSong.getArtist() : "";
        Bitmap bmp = null;

        if (hasSong) {
            Album albumObj = mSong.getAlbumObj();
            if (albumObj != null) {
                String cover = albumObj.getAlbumArt();
                if (!TextUtils.isEmpty(cover) && new File(cover).exists()) {
                    bmp = BitmapFactory.decodeFile(mSong.getAlbumObj().getAlbumArt());
                }
            }

        }
        mMediaSessionCompat.setMetadata(
                new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bmp)
                        .build()
        );
        changeMediaSessionState(state);
    }

    private void changeMediaSessionState(@PlayService.State int state) {
        if (mMediaSessionCompat == null || !mMediaSessionCompat.isActive()) {
            return;
        }
        final int playState = isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        mMediaSessionCompat.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setState(playState, mService.getPosition(), 0)
                        .setActions(
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        )
                        .build()
        );
    }

    /**
     *
     * @return
     */
    public boolean isPlaying () {
        return mService != null && mService.isStarted();
    }

    public boolean isPaused () {
        return  mService != null && mService.isPaused();
    }

    public boolean isPausedByUser () {
        return isPausedByUser;
    }

    public void seekTo (int position) {
        if (mService != null) {
            mService.seekTo(position);
        }
    }

    /**
     *
     * @return a song current playing or paused, may be null
     */
    public Song getCurrentSong () {
        return mSong;
    }

    private int requestAudioFocus () {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        Log.v(TAG, "requestAudioFocus by ");
        return audioManager.requestAudioFocus(
                mAfListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private int releaseAudioFocus () {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        Log.v(TAG, "releaseAudioFocus by ");
        return audioManager.abandonAudioFocus(mAfListener);
    }

    public void registerCallback (Callback callback) {
        registerCallback(callback, false);
    }

    public void registerCallback (Callback callback, boolean updateOnceNow) {
        if (mCallbacks.contains(callback)) {
            return;
        }
        mCallbacks.add(callback);
        if (updateOnceNow) {
            callback.onPlayListPrepared(mTotalList);
            callback.onPlayRuleChanged(mPlayRule);
            callback.onPlayStateChanged(mState, mSong);
        }
    }

    public void unregisterCallback (Callback callback) {
        if (mCallbacks.contains(callback)) {
            mCallbacks.remove(callback);
        }
    }

    private void startUpdateProgressIfNeed () {
        if (!isProgressUpdating) {
            mHandler.post(mProgressRunnable);
        }
    }

    public void registerProgressCallback (ProgressCallback callback) {
        if (mProgressCallbacks.contains(callback)) {
            return;
        }
        mProgressCallbacks.add(callback);
        startUpdateProgressIfNeed();
    }

    public void unregisterProgressCallback (ProgressCallback callback) {
        if (mProgressCallbacks.contains(callback)) {
            mProgressCallbacks.remove(callback);
        }
    }

    private void registerNoisyReceiver () {
        mNoisyReceiver.register(mContext, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    private void unregisterNoisyReceiver () {
        mNoisyReceiver.unregister(mContext);
    }

    @Override
    public void onStateChanged(@PlayService.State int state) {
        mState = state;
        switch (state) {
            case PlayService.STATE_IDLE:
                isPausedByUser = false;
                break;
            case PlayService.STATE_INITIALIZED:
                isPausedByUser = false;
                changeMediaSessionMetadata(state);
                break;
            case PlayService.STATE_PREPARING:
                isPausedByUser = false;
                break;
            case PlayService.STATE_PREPARED:
                isPausedByUser = false;
                break;
            case PlayService.STATE_STARTED:
                registerNoisyReceiver();
                notification(state);
                setSessionActive(true);
                changeMediaSessionState(state);
                startUpdateProgressIfNeed();
                isPausedByUser = false;
                break;
            case PlayService.STATE_PAUSED:
                unregisterNoisyReceiver();
                notification(state);
                changeMediaSessionState(state);
                break;
            case PlayService.STATE_ERROR:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                isPausedByUser = false;
                break;
            case PlayService.STATE_STOPPED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                //notification(state);
                changeMediaSessionState(state);
                setSessionActive(false);
                isPausedByUser = false;
                break;
            case PlayService.STATE_COMPLETED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                changeMediaSessionState(state);
                isPausedByUser = false;
                next(false);
                break;
            case PlayService.STATE_RELEASED:
                Log.v(TAG, "onStateChanged STATE_RELEASED");
                unregisterNoisyReceiver();
                releaseAudioFocus();
                isPausedByUser = false;
                break;
        }
        for (Callback callback : mCallbacks) {
            callback.onPlayStateChanged(state, mSong);
        }
    }

    @Override
    public void onShutdown() {
        releaseAudioFocus();
        stopRemoteControl();
        mService.stopForeground(true);
        NotificationManagerCompat notifyManager = NotificationManagerCompat.from(mContext);
        notifyManager.cancelAll();
        for (Callback callback : mCallbacks) {
            callback.onShutdown();
        }
    }

    /**
     * you can custom a {@link Notification} by the {@link NotificationAgent}
     * @param agent
     */
    public void setNotificationAgent (NotificationAgent agent) {
        this.mNotifyAgent = agent;
    }

    public NotificationAgent getNotificationAgent () {
        return mNotifyAgent;
    }

    //private int mLastNotificationId;
    private void notification (@PlayService.State int state) {
        if (mNotifyAgent == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationLollipop(state);
        } else {
            notificationPreLollipop(state);
        }
    }

    private void notificationPreLollipop (@PlayService.State int state) {
        NotificationCompat.Builder builder = mNotifyAgent.getBuilder(mContext, this, state, mSong);
        mService.startForeground(1, builder.build());
    }

    private void notificationLollipop (@PlayService.State int state) {
        NotificationManagerCompat notifyManager = NotificationManagerCompat.from(mContext);
        NotificationCompat.Builder builder = mNotifyAgent.getBuilder(mContext, this, state, mSong);
        notifyManager.notify(1, builder.build());
    }

    private MediaSessionCompat.Callback mSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
            Log.v(TAG, "mSessionCallback onCommand command=" + command);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.v(TAG, "mSessionCallback onMediaButtonEvent mediaButtonEvent=" + mediaButtonEvent.getAction());
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPrepare() {
            super.onPrepare();
            Log.v(TAG, "mSessionCallback onPrepare");
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
            Log.v(TAG, "mSessionCallback onPrepareFromMediaId mediaId=" + mediaId);
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            super.onPrepareFromSearch(query, extras);
            Log.v(TAG, "mSessionCallback onPrepareFromSearch query=" + query);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
            Log.v(TAG, "mSessionCallback onPrepareFromUri uri=" + uri.toString());
        }

        @Override
        public void onPlay() {
            super.onPlay();
            dispatch();
            Log.v(TAG, "mSessionCallback onPlay");
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.v(TAG, "mSessionCallback onPlayFromMediaId mediaId=" + mediaId);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
            Log.v(TAG, "mSessionCallback onPlayFromSearch query=" + query);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
            Log.v(TAG, "mSessionCallback onPlayFromUri uri=" + uri.toString());
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
            Log.v(TAG, "mSessionCallback onSkipToQueueItem id=" + id);
        }

        @Override
        public void onPause() {
            pause(true);
            Log.v(TAG, "mSessionCallback onPause");
        }

        @Override
        public void onSkipToNext() {
            next();
            Log.v(TAG, "mSessionCallback onSkipToNext");
        }

        @Override
        public void onSkipToPrevious() {
            previous();
            Log.v(TAG, "mSessionCallback onSkipToPrevious");
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            Log.v(TAG, "mSessionCallback onFastForward");
        }

        @Override
        public void onRewind() {
            super.onRewind();
            Log.v(TAG, "mSessionCallback onRewind");
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.v(TAG, "mSessionCallback onStop");
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            Log.v(TAG, "mSessionCallback onSeekTo pos=" + pos);
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            super.onSetRating(rating);
            Log.v(TAG, "mSessionCallback onSetRating rating=" + rating.toString());
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            Log.v(TAG, "mSessionCallback onCustomAction action=" + action);
        }
    };

    public interface Callback {
        void onPlayListPrepared (List<Song> songs);
        void onAlbumListPrepared (List<Album> albums);
        void onPlayStateChanged (@PlayService.State int state, Song song);
        void onShutdown ();
        void onPlayRuleChanged (Rule rule);
    }

    public interface ProgressCallback {
        void onProgress (int progress, int duration);
    }

}
