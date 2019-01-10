package com.example.johnnie.sound.Fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.johnnie.sound.R;
import com.example.johnnie.sound.adapter.EmptyDelegate;
import com.example.johnnie.sound.adapter.SongDecoration;
import com.example.johnnie.sound.adapter.SongDelegate;
import com.example.johnnie.sound.adapter.VideoDelegate;
import com.example.johnnie.sound.models.Song;
import com.example.johnnie.sound.models.Video;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.LayoutImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by johnnie on 2018/5/25.
 */

public class VideoFragment extends RvFragment {
    private static ContentResolver mContentResolver;
    private SongDecoration mSongDecoration;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setSongList(getVideos());
        mSongDecoration = new SongDecoration(getContext());
        getRecyclerView().addItemDecoration(mSongDecoration);
    }


    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @Override
    public CharSequence getTitle(Context context, Object... params) {
        return context.getString(R.string.title_video_list);
    }
    public void setSongList (List<Video> videoList) {
        getAdapter().clear();
        if (videoList != null && !videoList.isEmpty()) {
            getAdapter().addAll(videoList, new DelegateParser<Video>() {
                @Override
                public LayoutImpl parse(DelegateAdapter adapter, Video data) {
                    return new VideoDelegate(data);
                }
            });
        } else {
            getAdapter().add(new EmptyDelegate(getContext(), R.string.text_empty_msg_video));
        }

        getAdapter().notifyDataSetChanged();

    }
    /**
     * 获取本机视频列表
     * @return
     */
    public List<Video> getVideos() {

        List<Video> videos = new ArrayList<>();
        mContentResolver=getContext().getContentResolver();

        Cursor c = null;
        try {
            // String[] mediaColumns = { "_id", "_data", "_display_name",
            // "_size", "date_modified", "duration", "resolution" };
            c = mContentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
            while (c.moveToNext()) {
                String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));// 路径
                File file=new File(path);
                if (!file.exists()) {
                    continue;
                }

                int id = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID));// 视频的id
                String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)); // 视频名称
                String resolution = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)); //分辨率
                long size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));// 大小
                long duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));// 时长
                long date = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED));//修改时间

                Video video = new Video(id, path, name, resolution, size, date, duration);
                videos.add(video);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return videos;
    }
}
