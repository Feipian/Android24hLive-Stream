package com.example.android24hstream;

import android.content.Context;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import java.io.InputStream;
import java.util.Collections;

public class YouTubeStreamer {
    private YouTube youtube;
    private GoogleAccountCredential credential;

    public YouTubeStreamer(Context context, String accountName) {
        credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton("https://www.googleapis.com/auth/youtube.force-ssl"));
        credential.setSelectedAccountName(accountName);

        youtube = new YouTube.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                credential)
                .setApplicationName("VideoLooper")
                .build();
    }


    private LiveStream createStream(String title) throws Exception {
        // 1. Create stream snippet
        LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
        streamSnippet.setTitle(title);

        // 2. Set stream content details (resolution, format, etc.)
        LiveStreamContentDetails contentDetails = new LiveStreamContentDetails();
        contentDetails.setIsReusable(true); // Allow stream to be reused

        // 3. Create CDN settings (ingestion info)
        CdnSettings cdnSettings = new CdnSettings();
        cdnSettings.setFormat("1080p"); // Or "720p", "480p" etc.
        cdnSettings.setIngestionType("rtmp"); // Required for live streaming

        // 4. Combine into LiveStream object
        LiveStream stream = new LiveStream();
        stream.setSnippet(streamSnippet);
        stream.setCdn(cdnSettings);
        stream.setContentDetails(contentDetails);

        // 5. Insert the stream via YouTube API
        YouTube.LiveStreams.Insert request = youtube.liveStreams()
                .insert("snippet,cdn,contentDetails", stream);

        return request.execute();
    }

    public void streamVideo(InputStream stream, String title, String description,
                            StreamProgressListener listener) {
        try {
            // 1. Create broadcast
            LiveBroadcast broadcast = createBroadcast(title, description);

            // 2. Create stream (using the new method)
            LiveStream liveStream = createStream(title);

            // 3. Bind them
            bindBroadcastToStream(broadcast.getId(), liveStream.getId());

            // 4. Get the RTMP URL (for actual streaming)
            String rtmpUrl = liveStream.getCdn().getIngestionInfo().getIngestionAddress();
            String streamKey = liveStream.getCdn().getIngestionInfo().getStreamName();

            Log.d("YouTubeStreamer", "RTMP URL: " + rtmpUrl);
            Log.d("YouTubeStreamer", "Stream Key: " + streamKey);

            // TODO: Implement actual RTMP streaming here
            // You'll need an RTMP library like 'net.ossrs:rtmp:1.0.0'

            listener.onComplete();
        } catch (Exception e) {
            listener.onError(e);
        }
    }

    private void bindBroadcastToStream(String id, String id1) {
    }


    private LiveBroadcast createBroadcast(String title, String description) throws Exception {
        LiveBroadcastSnippet snippet = new LiveBroadcastSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);
        snippet.setScheduledStartTime(new com.google.api.client.util.DateTime(System.currentTimeMillis()));

        LiveBroadcastStatus status = new LiveBroadcastStatus();
        status.setPrivacyStatus("public");

        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setSnippet(snippet);
        broadcast.setStatus(status);

        return youtube.liveBroadcasts()
                .insert("snippet,status", broadcast)
                .execute();
    }

    public interface StreamProgressListener {
        void onComplete();
        void onError(Exception e);
    }
}