# FOR 24H/7D LIVE STREAM AT YOUTUBE, CHOICE A VIDEO


## Reference Article:

https://github.com/vvbhandare/YouTube-Live-Demo-Work/blob/master/MyTestProject/src/com/google/api/services/samples/youtube/cmdline/live/CreateBroadcast.java#L88
https://github.com/pedroSG94/RootEncoder/blob/master/app/src/main/java/com/pedro/streamer/file/FromFileActivity.kt
https://dev.to/theplebdev/using-android-to-stream-to-twitch-part-2-rtmp-handshake-2o44

## TODO:

        // 1. Initializing an RTMP streaming library.
        // 2. Setting up MediaExtractor to read from videoUri.
        // 3. Setting up MediaCodec for video and audio encoding.
        // 4. Feeding extracted and encoded data to the RTMP library.
        // 5. Managing threading for all these operations.
        // 6. Transitioning the YouTube broadcast state (e.g., to "live") once streaming starts.

        // Example of transitioning the broadcast once you are confident streaming will start:
        // Call this AFTER your RTMP library successfully connects and starts sending data
        // transitionBroadcastState(broadcastId, "live");



## FFmpeg Command:

Windows work: ffmpeg -stream_loop -1 -re -i [YOUR FILE NAME HERE] -pix_fmt yuvj420p -x264-params keyint=48:min-keyint=48:scenecut=-1 -b:v 4500k -b:a 128k -ar 44100 -acodec aac -vcodec libx264 -preset medium -crf 28 -threads 4 -f flv rtmp://[LIVE STREAM RTMP URL]



- [X] SELECT VIDEO PERMISSION
- [ ] YOUTUBE LIVE STREAM
  - [X] Can Choice google account but cant access google cloud console service 
  - [X] Choice Exists BoardCast
    - [ ] extract these metadata for continue BoardCast
    - [ ] Optimize UI
    - [ ] ffmpeg instead of google live stream api for save money
- [X] How to show the video i choice??


## Upload record:

[![IMAGE ALT TEXT HERE](https://img.youtube.com/vi/fQj3g6tzgKQ/0.jpg)](https://www.youtube.com/watch?v=fQj3g6tzgKQ)
