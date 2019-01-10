package com.example.johnnie.sound.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.johnnie.sound.R;
import com.example.johnnie.sound.activity.VideoDetailActivity;
import com.example.johnnie.sound.models.Video;
import com.example.johnnie.sound.utils.MediaUtils;
import com.nulldreams.adapter.AbsViewHolder;
import com.nulldreams.adapter.DelegateAdapter;
public class VideoHolder extends AbsViewHolder<VideoDelegate> {
    private static ContentResolver mContentResolver;
    ImageView imageView;
    TextView textView1;
    TextView textView2;
    TextView textView3;
    public VideoHolder(View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.video_thumb);
        textView1 = itemView.findViewById(R.id.video_duration);
        textView2 = itemView.findViewById(R.id.video_title);
        textView3 = itemView.findViewById(R.id.video_date);
    }

    @Override
    public void onBindView(final Context context, VideoDelegate videoDelegate, int position, DelegateAdapter adapter) {
        final Video video = videoDelegate.getSource();
        imageView.setImageBitmap(getVideoThumbnail(video.getId(),context));
        textView1.setText(MediaUtils.formatTime((int)video.getDuration()));
        textView2.setText(video.getName());
        textView3.setText(video.getPath());
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                showVideoPlay(context,video);
                VideoDetailActivity.actionStart((Activity) context,video.getPath());
            }
        });
    }

    // 获取视频缩略图
    public Bitmap getVideoThumbnail(int id,Context context) {
        mContentResolver=context.getContentResolver();
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmap = MediaStore.Video.Thumbnails.getThumbnail(mContentResolver, id, MediaStore.Images.Thumbnails.MICRO_KIND, options);
        return bitmap;
    }
    private void showVideoPlay(Context context,Video video){
        View contentView = LayoutInflater.from(context).inflate(R.layout.layout_video_pop_window,null);
        final VideoView videoView = contentView.findViewById(R.id.video_player);
        PopupWindow popupWindow = new PopupWindow(contentView,LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.showAtLocation(contentView,Gravity.CENTER,0,0);
        popupWindow.setOutsideTouchable(false);
        videoView.setVideoPath(video.getPath());
        videoView.start();
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                videoView.stopPlayback();
            }
        });
    }
}
