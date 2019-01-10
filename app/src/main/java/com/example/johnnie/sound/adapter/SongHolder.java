package com.example.johnnie.sound.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.johnnie.sound.R;
import com.example.johnnie.sound.manager.PlayManager;
import com.example.johnnie.sound.models.Album;
import com.example.johnnie.sound.models.Song;
import com.example.johnnie.sound.utils.MediaUtils;
import com.nulldreams.adapter.AbsViewHolder;
import com.nulldreams.adapter.DelegateAdapter;

/**
 * Created by johnnie on 2018/5/24.
 */

public class SongHolder extends AbsViewHolder<SongDelegate> {

    private ImageView thumbIv;
    private TextView titleTv, artistAlbumTv, durationTv;

    public SongHolder(View itemView) {
        super(itemView);
        thumbIv = (ImageView) findViewById(R.id.song_thumb);
        titleTv = (TextView) findViewById(R.id.song_title);
        artistAlbumTv = (TextView) findViewById(R.id.song_artist_album);
        durationTv = (TextView) findViewById(R.id.song_duration);
    }

    @Override
    public void onBindView(final Context context, SongDelegate songDelegate, int position, DelegateAdapter adapter) {
        final Song song = songDelegate.getSource();
        titleTv.setText(song.getTitle());
        artistAlbumTv.setText(song.getArtistAlbum());
        Album album = song.getAlbumObj();
        if (album != null) {
            Glide.with(context).load(album.getAlbumArt()).placeholder(R.mipmap.ic_launcher).into(thumbIv);
        } else {
            Glide.with(context).load("").placeholder(R.mipmap.ic_launcher).into(thumbIv);
        }
        durationTv.setText(MediaUtils.formatTime(song.getDuration()));
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayManager.getInstance(context).dispatch(song, "item click");
            }
        });
    }

    @Override
    public void onViewRecycled(Context context) {
        super.onViewRecycled(context);
        Glide.clear(thumbIv);
    }
}

