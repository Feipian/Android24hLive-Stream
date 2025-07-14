# FOR 24H/7D LIVE STREAM AT YOUTUBE, CHOICE A VIDEO


# Android24hStream

**Stream local video files from your Android device to YouTube Live, aiming for continuous (24/7) streaming capabilities.**

This project demonstrates how to use the YouTube Data API v3 for creating and managing live broadcasts and FFmpeg (via FFmpegKit) for the actual video streaming over RTMP.

## Features

*   **YouTube Live Integration:**
  *   OAuth 2.0 authentication with Google Sign-In.
  *   Create new YouTube Live broadcasts.
  *   Select existing YouTube Live broadcasts.
  *   Transition broadcast states (e.g., to "testing", "live").
  *   Retrieve RTMP ingest URL and stream key.
*   **Video Streaming:**
  *   Select a video file from the device's local storage.
  *   Stream the selected video file to the configured YouTube Live event.
  *   Utilizes FFmpeg for robust video processing and RTMP streaming.(You have to use linux or macos build ffmpegKit getting [ffmpeg-kit.aar](libs/ffmpeg-kit.aar) first)
  *   (Potentially) Configured for continuous looping of the selected video.
*   **User Interface:**
  *   Buttons for selecting videos, creating/selecting broadcasts, and starting/stopping the stream.
  *   Displays output logs and status messages.
  *   Video preview of the selected file.

## Prerequisites

*   **Android Studio:** Latest stable version recommended.
*   **Android Device/Emulator:** Running Android API level 24 (Nougat) or higher.
*   **Google Cloud Project:**
  *   YouTube Data API v3 enabled.
  *   OAuth 2.0 Client ID configured for Android. You'll need the `SHA-1` fingerprint of your signing certificate.
*   **YouTube Channel:** With live streaming enabled (can take up to 24 hours to activate after the first request).

## Setup

1.  **Clone the Repository:**
  Fork this project on GitHub and clone it to your local device(Computer).

2.  **Google Cloud Project & API Key:**
  *   Go to the [Google Cloud Console](https://console.cloud.google.com/).
  *   Create a new project or select an existing one.
  *   Enable the "YouTube Data API v3".
  *   Go to "Credentials":
    *   Create an "OAuth 2.0 Client ID".
    *   Select "Android" as the application type.
    *   Provide your app's package name (e.g., `com.example.android24hstream`).
    *   Generate the SHA-1 fingerprint for your debug keystore:

(For release, use your release keystore).
*   Add the SHA-1 fingerprint to your OAuth 2.0 Client ID configuration.
*   You might also want to create an API Key if parts of your app use it (though for user data like broadcasting, OAuth 2.0 is primary). If you do, restrict its usage appropriately.

3.  **Configure API Key (if used and how it's set in your project):**
    (Use for Google Cloud Project Project's API Key, it near the OAUTH)
  *   Create a `local.properties` file in the root directory of your Android Studio project (it should be next to `build.gradle.kts` and `settings.gradle.kts`).
  *   Add your YouTube Data API Key (if you're using one for non-OAuth calls, otherwise this step might be related to your `BuildConfigField` setup) to `local.properties`:


*Note: Based on your `build.gradle.kts`, you have `buildConfigField("String", "API_KEY", localProperties.getProperty("API_KEY"))`. Explain if this `API_KEY` is for the YouTube Data API or another service.*

4.  **Open in Android Studio:**
  *   Open the cloned project in Android Studio.
  *   Let Gradle sync and download dependencies.

## Building and Running

1.  **Select Build Variant:** Choose a build variant (e.g., `debug`).
2.  **Connect Device/Start Emulator:** Ensure your Android device (with USB debugging enabled) is connected or an emulator is running.
3.  **Run the App:** Click the "Run" button in Android Studio.

## How to Use

1.  **Authenticate:**
  *   The app will likely prompt you to sign in with a Google account.
  *   Choose the account associated with the YouTube channel you want to stream to.
  *   Grant the necessary permissions for managing YouTube Live broadcasts.
2.  **Select/Create Broadcast:**
  *   Use the "Create New Broadcast" button to set up a new live event on YouTube.
    *   You'll be prompted to enter a title, description, and privacy status.
  *   Or, use the "Select Existing Broadcast" button to choose from your channel's existing live events.
3.  **Select Video:**
  *   Click the "Select Video" button.
  *   Browse your device storage and choose the video file you want to stream.
4.  **Start Streaming:**
  *   Once a broadcast is configured and a video is selected, click the "Start Stream" button.
  *   FFmpeg will begin processing the video and sending it to YouTube's RTMP ingest server.
  *   You should see logs in the app and be able to monitor the stream on YouTube Studio.
5.  **Monitor Output:**
  *   The app's output text view will display logs from the YouTube API calls and FFmpeg.

## Code Overview

*   **`MainActivity.java`**: The main screen of the application, handling:
  *   Google Sign-In and YouTube API authorization.
  *   UI interactions (button clicks, video selection).
  *   Calls to create, list, and transition YouTube Live broadcasts.
  *   Initiating the FFmpeg streaming process.
*   **YouTube Data API Calls:**
  *   Uses the `com.google.api.services.youtube.YouTube` client library.
  *   Handles creation of `LiveBroadcast` and `LiveStream` objects.
  *   Transitions broadcast states (e.g., `testing` -> `live`).
*   **FFmpeg Streaming:**
  *   Uses `com.arthenica:ffmpeg-kit-full` (or your specific FFmpeg library).
  *   Constructs and executes FFmpeg commands to:
    *   Read the input video file.
    *   Re-encode or remux the video as needed for RTMP.
    *   Set video/audio codecs (e.g., `h264_mediacodec` for hardware acceleration, `aac`).
    *   Stream to the RTMP URL provided by the YouTube API.
*   **Permissions:**
  *   `INTERNET`: For network access (API calls, streaming).
  *   `ACCESS_NETWORK_STATE`: To check network connectivity.
  *   `READ_EXTERNAL_STORAGE` / `READ_MEDIA_VIDEO`: To access video files from storage.
  *   `GET_ACCOUNTS` / Google Sign-In permissions for authentication.
*   **Key Dependencies:**
  *   `com.google.apis:google-api-services-youtube`
  *   `com.google.android.gms:play-services-auth`
  *   `com.arthenica:ffmpeg-kit-full` (For android mobile, we need to build ffmpegKit first, let it become .aar file.)

## Key FFmpeg Command Example

(Located in `MainActivity.java` within the `startRtmpStreamingWithFFmpeg` method or similar)


*Note: The exact command might differ in your current code. Update this section to reflect your actual command and explain its components.*

## Troubleshooting

*   **"403 Forbidden" errors for YouTube API calls:**
  *   Ensure your Google Cloud Project has the YouTube Data API v3 enabled.
  *   Verify your OAuth 2.0 Client ID is correctly configured with the right package name and SHA-1 fingerprint.
  *   Make sure the authenticated Google account has live streaming enabled on its YouTube channel and has permissions to manage broadcasts.
*   **FFmpeg: "Unknown encoder 'h264_mediacodec'"**:
  *   You might be using an FFmpeg build that doesn't include MediaCodec support. Ensure you're using a "full" version of FFmpegKit or one that explicitly supports hardware acceleration. Consider updating your FFmpegKit dependency.
  *   As a fallback for testing, you can try a software encoder like `libx264` (e.g., `-c:v libx264 -preset veryfast`).
*   **FFmpeg: Fails to connect to RTMP URL:**
  *   Double-check that the RTMP URL and stream key are correctly retrieved and combined.
  *   Ensure your device has a stable internet connection.
  *   Check if any firewall or network restriction is blocking outgoing RTMP traffic (usually on TCP port 1935).
*   **Video playback issues on YouTube:**
  *   Adjust FFmpeg encoding parameters (`-b:v`, `-g`, `-preset` if using libx264, etc.) to match YouTube's recommended settings for live streaming.
  *   Ensure your source video is not corrupted.

## Future Improvements / To-Do

*(Optional: List things you plan to add or areas that could be improved)*

*   More robust error handling and user feedback.
*   Background service for long-running streams.
*   Dynamic bitrate adjustment based on network conditions.
*   UI for configuring FFmpeg parameters.
*   Support for streaming from the device camera.

## Contributing

*(Optional: If you're open to contributions)*

Contributions are welcome! Please feel free to submit pull requests or open issues.
1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## License

*(Choose a license, e.g., MIT, Apache 2.0)*

Distributed under the MIT License. See `LICENSE.txt` for more information.

## Acknowledgements

*   [Google API Client Library for Java](https://github.com/googleapis/google-api-java-client)
*   [FFmpegKit](https://github.com/arthenica/ffmpeg-kit)
*   [YouTube Live Streaming API Documentation](https://developers.google.com/youtube/v3/live/getting-started)

---

**Fill in the blanks (especially `YOUR_YOUTUBE_DATA_API_KEY` explanation, repository URL, license) and adjust any details specific to your project's current state.** This document should give anyone a good starting point for using your code.



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
