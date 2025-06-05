package com.example.android24hstream;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class VideoLooperService extends Service {
    private final IBinder binder = new LocalBinder();
    private int currentPosition = 0;
    private List<Uri> videoUris = new ArrayList<>();
    private boolean isStreaming = false;
    private YouTubeStreamer youtubeStreamer;

    public class LocalBinder extends Binder {
        VideoLooperService getService() {
            return VideoLooperService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setVideoList(List<Uri> uris) {
        videoUris.clear();
        videoUris.addAll(uris);
    }

    public void startStreaming(String accountName) {
        youtubeStreamer = new YouTubeStreamer(this, accountName);
        isStreaming = true;
        streamNextVideo();
    }

    private void streamNextVideo() {
        if (!isStreaming || videoUris.isEmpty()) return;

        Uri uri = videoUris.get(currentPosition);
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            youtubeStreamer.streamVideo(stream, "Looping Video " + (currentPosition + 1),
                    "Streamed from Android app", new YouTubeStreamer.StreamProgressListener() {
                        @Override
                        public void onComplete() {
                            currentPosition = (currentPosition + 1) % videoUris.size();
                            streamNextVideo();
                        }
                        @Override
                        public void onError(Exception e) {
                            // Handle error
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopStreaming() {
        isStreaming = false;
        stopSelf();
    }
}