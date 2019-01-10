package com.example.johnnie.sound.activity;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.johnnie.sound.Fragment.VideoFragment;
import com.example.johnnie.sound.Fragment.RvFragment;
import com.example.johnnie.sound.Fragment.SongListFragment;
import com.example.johnnie.sound.R;
import com.example.johnnie.sound.manager.PlayManager;
import com.example.johnnie.sound.manager.ruler.Rule;
import com.example.johnnie.sound.manager.ruler.Rulers;
import com.example.johnnie.sound.models.Album;
import com.example.johnnie.sound.models.Song;
import com.example.johnnie.sound.service.PlayService;
import com.example.johnnie.sound.utils.MediaUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements PlayManager.Callback, PlayManager.ProgressCallback{

    // 开启app:srcCompat语句
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    private CoordinatorLayout mCoorLayout;
    private Toolbar mTb;
    private ViewPager mVp;
    private TabLayout mTl;
    private View mMiniPanel, mSongInfoLayout;
    private ImageView mMiniThumbIv, mPlayPauseIv, mPreviousIv, mNextIv;
    private TextView mMiniTitleTv, mMiniArtistAlbumTv;
    private ProgressBar mMiniPb;

    private int mLength = 2;
    private RvFragment[] mFragmentArray = null;

    // 组件点击监听
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mPlayPauseIv.getId()) {
                PlayManager.getInstance(v.getContext()).dispatch();
            } else if (id == mMiniPanel.getId()) {
                showPlayDetail();
            } else if (id == mPreviousIv.getId()) {
                PlayManager.getInstance(v.getContext()).previous();
            } else if (id == mNextIv.getId()) {
                PlayManager.getInstance(v.getContext()).next();
            }
        }
    };

    private boolean isResumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCoorLayout = (CoordinatorLayout)findViewById(R.id.main_coordinator_layout);
        mTb = (Toolbar)findViewById(R.id.main_tool_bar);
        mVp = (ViewPager)findViewById(R.id.main_view_pager);
        mTl = (TabLayout)findViewById(R.id.main_tab_layout);

        mMiniPanel = findViewById(R.id.main_mini_panel);
        mMiniThumbIv = (ImageView)findViewById(R.id.main_mini_thumb);
        mSongInfoLayout = findViewById(R.id.main_mini_song_info_layout);
        mMiniTitleTv = (TextView)findViewById(R.id.main_mini_title);
        mMiniArtistAlbumTv = (TextView)findViewById(R.id.main_mini_artist_album);
        mPlayPauseIv = (ImageView)findViewById(R.id.main_mini_action_play_pause);
        mPreviousIv = (ImageView)findViewById(R.id.main_mini_action_previous);
        mNextIv = (ImageView)findViewById(R.id.main_mini_action_next);
        mMiniPb = (ProgressBar)findViewById(R.id.main_mini_progress_bar);

        setSupportActionBar(mTb);

        mMiniPanel.setOnClickListener(mClickListener);
        mPlayPauseIv.setOnClickListener(mClickListener);
        mPreviousIv.setOnClickListener(mClickListener);
        mNextIv.setOnClickListener(mClickListener);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        } else {
            init();
            MediaUtils.getAlbumList(this);
        }
    }

    private void init () {

        mFragmentArray = new RvFragment[mLength];
        mFragmentArray[0] = new SongListFragment();
        mFragmentArray[1] = new VideoFragment();

        mVp.setAdapter(new VpAdapter(getSupportFragmentManager()));
        mTl.setupWithViewPager(mVp);

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int id = sharedPreferences.getInt("rule", 0);
        Rule rule = null;
        switch (id) {
            case 0:
                rule = Rulers.RULER_LIST_LOOP;
                break;
            case 1:
                rule = Rulers.RULER_SINGLE_LOOP;
                break;
            case 2:
                rule = Rulers.RULER_RANDOM;
                break;
        }
        PlayManager.getInstance(this).setRule(rule);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
        Song song = PlayManager.getInstance(this).getCurrentSong();
        showSong(song);

        PlayManager.getInstance(this).registerCallback(this);
        PlayManager.getInstance(this).registerProgressCallback(this);
        //PlayManager.getInstance(this).unlockScreenControls();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayManager.getInstance(this).unregisterCallback(this);
        PlayManager.getInstance(this).unregisterProgressCallback(this);
        //PlayManager.getInstance(this).lockScreenControls();
        isResumed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showPlayDetail () {
        Intent it = new Intent(this, PlayDetailActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Pair<View, String> thumb = new Pair<View, String>(mMiniThumbIv, getString(R.string.translation_thumb));
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, thumb);
            startActivity(it, options.toBundle());
        } else {
            startActivity(it);
        }
        // 切换activity时的动画
        overridePendingTransition(R.anim.anim_bottom_in, 0);
    }

    @Override
    public void onPlayListPrepared(List<Song> songs) {

    }

    @Override
    public void onAlbumListPrepared(List<Album> albums) {

    }

    @Override
    public void onPlayStateChanged(@PlayService.State int state, Song song) {
        switch (state) {
            case PlayService.STATE_INITIALIZED:
                showSong(song);
                break;
            case PlayService.STATE_STARTED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_PAUSED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_COMPLETED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_RELEASED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mMiniPb.setProgress(0);
                break;
            case PlayService.STATE_ERROR:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mMiniPb.setProgress(0);
                break;
        }
    }

    private void showSong(Song song) {
        if (song != null) {
            mMiniTitleTv.setText(song.getTitle());
            mMiniArtistAlbumTv.setText(song.getArtistAlbum());
            Album album = song.getAlbumObj();
            if (album == null) {
                album = PlayManager.getInstance(this).getAlbum(song.getAlbumId());
            }
            if (album != null) {
                Glide.with(this).load(album.getAlbumArt()).asBitmap().placeholder(R.mipmap.ic_launcher).animate(android.R.anim.fade_in).into(mMiniThumbIv);
            }
        } else {
            mMiniTitleTv.setText(R.string.app_name);
            mMiniArtistAlbumTv.setText(R.string.text_github_name);
            Glide.with(this).load(R.drawable.sound).asBitmap().animate(android.R.anim.fade_in).into(mMiniThumbIv);
        }

    }
        @Override
    public void onShutdown() {

    }

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }

    @Override
    public void onProgress(int progress, int duration) {
        if (mMiniPb.getMax() != duration) {
            mMiniPb.setMax(duration);
        }
        mMiniPb.setProgress(progress);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
                MediaUtils.getAlbumList(this);
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.text_permission)
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            moveTaskToBack(true);
            return;
        }
        super.onBackPressed();
    }


    // Viewpager Adapter
    private class VpAdapter extends FragmentStatePagerAdapter {

        public VpAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if (mFragmentArray[0] == null) {
                        mFragmentArray[0] = new SongListFragment();
                    }
                    return mFragmentArray[0];
                case 1:
                    if (mFragmentArray[1] == null) {
                        mFragmentArray[1] = new VideoFragment();// ****暂时替代
                    }
                    return mFragmentArray[1];
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            RvFragment fragment = (RvFragment)super.instantiateItem(container, position);
            mFragmentArray[position] = fragment;
            return fragment;
        }

        @Override
        public int getCount() {
            return mFragmentArray.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentArray[position].getTitle(MainActivity.this);
        }
    }

}
