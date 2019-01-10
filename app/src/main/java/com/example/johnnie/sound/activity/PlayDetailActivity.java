package com.example.johnnie.sound.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;


import com.example.johnnie.sound.R;
import com.example.johnnie.sound.adapter.SongDelegate;
import com.example.johnnie.sound.manager.PlayManager;
import com.example.johnnie.sound.manager.ruler.Rule;
import com.example.johnnie.sound.manager.ruler.Rulers;
import com.example.johnnie.sound.models.Album;
import com.example.johnnie.sound.models.Song;
import com.example.johnnie.sound.service.PlayService;
import com.example.johnnie.sound.utils.MediaUtils;

import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.LayoutImpl;

import java.io.File;
import java.util.List;

public class PlayDetailActivity extends AppCompatActivity
        implements PlayManager.Callback, PlayManager.ProgressCallback{

    private static final String TAG = PlayDetailActivity.class.getSimpleName();

    private TextView mTitleTv, mArtistTv, mAlbumTv, mPositionTv, mDurationTv;
    private ImageView mThumbIv, mPlayPauseIv, mPreviousIv, mNextIv, mRuleIv, mPlayListIv;
    private View mPanel;
    private SeekBar mSeekBar;
    private Toolbar mToolbar;

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mPlayPauseIv.getId()) {
                PlayManager.getInstance(v.getContext()).dispatch();
            } else if (id == mPreviousIv.getId()) {
                PlayManager.getInstance(v.getContext()).previous();
            } else if (id == mNextIv.getId()) {
                PlayManager.getInstance(v.getContext()).next();
            } else if (id == mRuleIv.getId()) {
                PlayManager manager = PlayManager.getInstance(v.getContext());
                Rule rule = manager.getRule();
                if (rule == Rulers.RULER_LIST_LOOP) {
                    manager.setRule(Rulers.RULER_SINGLE_LOOP);
                } else if (rule == Rulers.RULER_SINGLE_LOOP) {
                    manager.setRule(Rulers.RULER_RANDOM);
                } else if (rule == Rulers.RULER_RANDOM) {
                    manager.setRule(Rulers.RULER_LIST_LOOP);
                }
            } else if (id == mPlayListIv.getId()) {
                showQuickList();
            }
        }
    };

    private void showQuickList () {
        List<Song> songs = PlayManager.getInstance(this).getTotalList();
        if (songs != null && !songs.isEmpty()) {      // !=null:有songs  !songs.isEmpty():songs有元素
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            RecyclerView rv = new RecyclerView(this);
            rv.setLayoutManager(new LinearLayoutManager(this));

            DelegateAdapter adapter = new DelegateAdapter(this);
            adapter.addAll(songs, new DelegateParser<Song>() {
                @Override
                public LayoutImpl parse(DelegateAdapter adapter, Song data) {
                    return new SongDelegate(data);
                }
            });
            rv.setAdapter(adapter);

            dialog.setContentView(rv);
            dialog.show();
        }
    }

    // 滑动条监听
    private boolean isSeeking = false;
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mPositionTv.setText(MediaUtils.formatTime(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeeking = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeeking = false;
            PlayManager.getInstance(seekBar.getContext()).seekTo(seekBar.getProgress());
        }
    };

    private int mLastColor = 0x00000000;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_detail);

        mToolbar = (Toolbar)findViewById(R.id.play_detail_tool_bar);

        mTitleTv = (TextView)findViewById(R.id.play_detail_title);
        mArtistTv = (TextView)findViewById(R.id.play_detail_artist);
        mAlbumTv = (TextView)findViewById(R.id.play_detail_album);
        mPositionTv = (TextView)findViewById(R.id.play_detail_position);
        mDurationTv = (TextView)findViewById(R.id.play_detail_duration);

        mPanel = findViewById(R.id.play_detail_panel);

        mThumbIv = (ImageView)findViewById(R.id.play_detail_thumb);
        mSeekBar = (SeekBar)findViewById(R.id.play_detail_seek_bar);
        mPlayPauseIv = (ImageView)findViewById(R.id.play_detail_play_pause);
        mPreviousIv = (ImageView)findViewById(R.id.play_detail_previous);
        mNextIv = (ImageView)findViewById(R.id.play_detail_next);
        mRuleIv = (ImageView)findViewById(R.id.play_detail_rule_change);
        mPlayListIv = (ImageView)findViewById(R.id.play_detail_play_list);

        // 裁剪图片
        final int width = getResources().getDisplayMetrics().widthPixels;
        final int height = getResources().getDisplayMetrics().heightPixels;
        final int size = Math.min(width, height);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mThumbIv.getLayoutParams();
        if (params == null) {
            params = new LinearLayout.LayoutParams(size, size);
        } else {
            params.width = size;
            params.height = size;
        }
        mThumbIv.setLayoutParams(params);

        // 设置Toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);

        // 增加点击事件监听
        mPlayPauseIv.setOnClickListener(mClickListener);
        mPreviousIv.setOnClickListener(mClickListener);
        mNextIv.setOnClickListener(mClickListener);
        mRuleIv.setOnClickListener(mClickListener);
        mPlayListIv.setOnClickListener(mClickListener);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);

        // 获得初始化信息
        Song song = PlayManager.getInstance(this).getCurrentSong();
        mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
        onPlayRuleChanged(PlayManager.getInstance(this).getRule());
        showSong(song);

    }

    private void showSong (Song song) {
        if (song == null) {
            mTitleTv.setText(R.string.app_name);
            mArtistTv.setText(R.string.text_github_name);
            mAlbumTv.setText(R.string.text_github_name);
            mSeekBar.setEnabled(false);
            Glide.with(this).load(R.drawable.sound).animate(android.R.anim.fade_in).into(mThumbIv);
            unregisterForContextMenu(mThumbIv);
        } else {
            mTitleTv.setText(song.getTitle());
            mArtistTv.setText(song.getArtist());
            mAlbumTv.setText(song.getAlbum());
            mSeekBar.setEnabled(true);
            Album album = song.getAlbumObj();
            if (album == null) {
                album = PlayManager.getInstance(this).getAlbum(song.getAlbumId());
            }
            if (album != null) {
                String albumArt = album.getAlbumArt();
                if (!TextUtils.isEmpty(albumArt)) {
                    File file = new File(albumArt);
                    if (!TextUtils.isEmpty(albumArt) && file.exists()) {
                        registerForContextMenu(mThumbIv);
                    } else {
                        unregisterForContextMenu(mThumbIv);
                    }
                    Glide.with(this).load(albumArt).asBitmap().placeholder(R.mipmap.ic_launcher).animate(android.R.anim.fade_in)
                            .placeholder(R.mipmap.ic_launcher).into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            mThumbIv.setImageBitmap(resource);
                            Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    Palette.Swatch swatch = palette.getDarkMutedSwatch();
                                    if (swatch != null) {
                                        animColor(swatch.getRgb());// 替换背景颜色
                                    }
                                }
                            });
                        }
                    });
                }
            } else {
                Glide.with(this).load(R.drawable.sound).animate(android.R.anim.fade_in).into(mThumbIv);
                unregisterForContextMenu(mThumbIv);
            }
        }
    }

    private void animColor (int newColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ObjectAnimator animator = ObjectAnimator.ofArgb(mPanel, "backgroundColor", mLastColor, newColor);
            animator.start();
        } else {
            mPanel.setBackgroundColor (newColor);
        }
        mLastColor = newColor;
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayManager.getInstance(this).registerCallback(this);
        PlayManager.getInstance(this).registerProgressCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayManager.getInstance(this).unregisterCallback(this);
        PlayManager.getInstance(this).unregisterProgressCallback(this);
    }

    @Override
    public void onPlayListPrepared(List<Song> songs) {

    }

    @Override
    public void onAlbumListPrepared(List<Album> albums) {

    }

    // 实现Callback接口
    @Override
    public void onPlayStateChanged(@PlayService.State int state, Song song) {
        switch (state) {
            case PlayService.STATE_INITIALIZED:
                closeContextMenu();
                showSong(song);
                break;
            case PlayService.STATE_STARTED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_PAUSED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_COMPLETED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_RELEASED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mSeekBar.setProgress(0);
                break;
            case PlayService.STATE_ERROR:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mSeekBar.setProgress(0);
                break;
        }
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onPlayRuleChanged(Rule rule) {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int id = 0;
        if (rule == Rulers.RULER_LIST_LOOP) {
            mRuleIv.setImageResource(R.drawable.ic_repeat);
            id = 0;
        } else if (rule == Rulers.RULER_SINGLE_LOOP) {
            mRuleIv.setImageResource(R.drawable.ic_repeat_once);
            id = 1;
        } else if (rule == Rulers.RULER_RANDOM) {
            mRuleIv.setImageResource(R.drawable.ic_shuffle_white_36dp);
            id = 2;
        }
        editor.putInt("rule", id);
        editor.apply();
    }

    @Override
    public void onProgress(int progress, int duration) {
        if (isSeeking) {
            return;
        }
        if (mSeekBar.getMax() != duration) {
            mSeekBar.setMax(duration);
            mDurationTv.setText(MediaUtils.formatTime(duration));
        }
        mSeekBar.setProgress(progress);
        mPositionTv.setText(MediaUtils.formatTime(progress));
    }
}
