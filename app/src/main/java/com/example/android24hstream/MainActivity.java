package com.example.android24hstream;

import static com.google.android.gms.common.GooglePlayServicesUtilLight.isGooglePlayServicesAvailable;

import static java.lang.Math.max;

import static kotlinx.coroutines.CoroutineScopeKt.CoroutineScope;
import static kotlinx.coroutines.DelayKt.delay;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.xuggle.xuggler.Configuration;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.media.MediaPlayer;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

//import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.javanet.NetHttpTransport; // Import this
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;



import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamContentDetails;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.Video;
import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.decoder.AudioDecoderInterface;
import com.pedro.encoder.input.decoder.VideoDecoderInterface;
import com.pedro.library.generic.GenericFromFile;
import com.pedro.library.view.OpenGlView;



import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;




public class MainActivity extends AppCompatActivity implements
        ConnectChecker,
        VideoDecoderInterface,
        AudioDecoderInterface,
        SeekBar.OnSeekBarChangeListener {

    private GenericFromFile genericFromFile;
    private ImageView bStream;
    private ImageView bSelectFile;
    private ImageView bReSync;
    private ImageView bRecord;
    private SeekBar seekBar;
    private EditText etUrl;
    private TextView tvFileName;
    private OpenGlView openGlView;

    private static YouTube youtube;
    private Uri filePath = null;
    private String recordPath = "";
    private boolean touching = false;

    private ActivityResultLauncher<String> activityResult;

    private static final String TAG = "YouTubeApp"; // Use a consistent tag for logs

    private TextView tvLoginStatus;  // Add this line
    private Uri selectedVideoUri;

    private ExecutorService executorService;
    private Handler mainHandler; // For posting results to the UI thread
    // Quickstart related fields
    private GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private ProgressDialog mProgress;

    // Request codes for old onActivityResult (now managed by launchers)
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;

    // Prefs and Scopes
    private static final String PREF_ACCOUNT_NAME = "accountName";
    // Combine quickstart and your app's scopes
    private static final List<String> SCOPES = Arrays.asList(
            YouTubeScopes.YOUTUBE_READONLY, // From quickstart (for channel info)
            "https://www.googleapis.com/auth/youtube.force-ssl" // From your app (for live stream)
    );

    // YouTube Service for both API calls
    private YouTube youtubeService;

    // Video Selection and Playback
    private ActivityResultLauncher<String[]> requestVideoPermissionLauncher;
    private ActivityResultLauncher<Intent> selectVideoIntentLauncher;
    private VideoView videoView;
    private MediaController mediaController;

    // Account Picker and Authorization Launchers
    private ActivityResultLauncher<Intent> accountPickerLauncher;
    private ActivityResultLauncher<Intent> authorizationLauncher;
    private static String currentRtmpUrl;
    private static String currentStreamKey;
    private static String  fullRtmpUrl;

    boolean streamKeyRetrieved = false; // Flag to check if stream key was retrieved

    private static final String CLIENT_SECRETS= "client_secret.json";
    private static final Collection<String> SCOPES1 =
            List.of("https://www.googleapis.com/auth/youtube.readonly");

    private static final String APPLICATION_NAME = "API code samples";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private void checkInitialRequirements() {
        // Check Google Play Services availability
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
            return;
        }

        // Check if account is selected
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
            return;
        }

        // Check network connection
        if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
            return;
        }

        // If all checks pass, you can proceed with your app initialization
        Toast.makeText(this, "All requirements met. Ready to use YouTube API.", Toast.LENGTH_SHORT).show();
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//
//        bStream = findViewById(R.id.b_start_stop);
//        bSelectFile = findViewById(R.id.select_file);
//        bReSync = findViewById(R.id.b_re_sync);
//        bRecord = findViewById(R.id.b_record);
//        etUrl = findViewById(R.id.et_rtp_url);
//        tvFileName = findViewById(R.id.tv_file_name);
//        openGlView = findViewById(R.id.surfaceView);
//        GenericFromFile genericFromFile;


        setContentView(R.layout.activity_main); // Make sure you have this layout




        // Initialize the status TextView
        seekBar = findViewById(R.id.seek_bar); // Initialize seekBar here after setContentView
        tvLoginStatus = findViewById(R.id.tv_login_status);


        Button selectExistingStreamBoardCast = findViewById(R.id.btn_select_existing_BoardCast);
        selectExistingStreamBoardCast.setOnClickListener(v -> {
            try {
                selectExistingBoardCast();
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        });

        // Initialize ExecutorService and Handler
        executorService = Executors.newFixedThreadPool(2); // Or a cached thread pool if many short tasks
        mainHandler = new Handler(Looper.getMainLooper()); // This handler is tied to the UI thread

        // Initialize UI elements from XML
        Button selectVideoButton = findViewById(R.id.btn_select_videos);
        Button startStreamBtn = findViewById(R.id.btn_start_stream);
        Button callYoutubeApiButton = findViewById(R.id.btn_call_youtube_api);
        mOutputText = findViewById(R.id.tv_output);
        videoView = findViewById(R.id.video_view);

        // Configure mOutputText
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setVerticalScrollBarEnabled(true);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Processing request...");

        // Initialize credential with combined scopes
        mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), SCOPES)
                .setBackOff(new ExponentialBackOff());

        // Restore saved account name
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        String accountName = settings.getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
        }

        // Initialize YouTube service with the credential
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        youtubeService = new YouTube.Builder(transport, jsonFactory, mCredential)
                .setApplicationName("YouTube Data API Android App") // Use a more descriptive name
                .build();

        // --- Register ActivityResultLaunchers ---

        // Launcher for account picker (REQUEST_ACCOUNT_PICKER equivalent)
        accountPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String selectedAccountName = result.getData().getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        if (selectedAccountName != null) {
                            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                            prefs.edit().putString(PREF_ACCOUNT_NAME, selectedAccountName).apply();
                            mCredential.setSelectedAccountName(selectedAccountName);
                            // After selecting an account, re-attempt the API call
                            getResultsFromApi();
                        }
                    } else {
                        Log.w(TAG, "Account selection cancelled or failed.");
                        mOutputText.setText("Account selection cancelled.");
                    }
                });

        // Launcher for authorization (REQUEST_AUTHORIZATION equivalent)
        authorizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Authorization successful, re-attempt API call
                        getResultsFromApi();
                    } else {
                        Log.e(TAG, "Authorization failed.");
                        mOutputText.setText("Authorization failed.");
                    }
                });

        // Launcher for requesting multiple permissions (e.g., GET_ACCOUNTS, video permissions)
        requestVideoPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    // Check specifically for GET_ACCOUNTS
                    if (permissions.containsKey(Manifest.permission.GET_ACCOUNTS) &&
                            !Boolean.TRUE.equals(permissions.get(Manifest.permission.GET_ACCOUNTS))) {
                        allGranted = false;
                        Log.w(TAG, "GET_ACCOUNTS permission denied.");
                        Toast.makeText(this, "Permission denied for Google accounts.", Toast.LENGTH_SHORT).show();
                    }

                    // Check for video permissions (your existing logic)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        if (!Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_VIDEO)) &&
                                !Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))) {
                            allGranted = false;
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_VIDEO))) {
                            allGranted = false;
                        }
                    } else {
                        if (!Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_EXTERNAL_STORAGE))) {
                            allGranted = false;
                        }
                    }

                    if (allGranted) {
                        Log.d(TAG, "All necessary permissions granted.");
                        // If GET_ACCOUNTS was requested and granted, proceed with chooseAccount() logic
                        if (permissions.containsKey(Manifest.permission.GET_ACCOUNTS)) {
                            // This path is usually taken after permission is granted and chooseAccount() is called again
                            // However, if the initial call to chooseAccount() failed due to permission,
                            // then after permission is granted, getResultsFromApi() will be called,
                            // which will then call chooseAccount() again, which will succeed.
                            // So, no explicit call here is needed, as the flow from getResultsFromApi() handles it.
                        }
                        // If video permissions were requested and granted, proceed to open video selection
                        if (permissions.containsKey(Manifest.permission.READ_MEDIA_VIDEO) ||
                                permissions.containsKey(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ||
                                permissions.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            openVideoSelectionIntent();
                        }
                    } else {
                        Log.w(TAG, "Some permissions denied.");
                        Toast.makeText(this, "Some permissions were denied.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Launcher for selecting video via Intent
        selectVideoIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        assert videoUri != null;
                        Log.d(TAG, "Selected video URI: " + videoUri.toString());
                        videoView.setVideoURI(videoUri); // <--- Set the video URI here!
                        this.selectedVideoUri = videoUri; // Assign video Uri to member variable

                        // Set a listener to know when the video is prepared
                        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {

                                // Start playing the video
                                videoView.start();

                                // Show the media controller once the video is prepared
                                // This is the correct place to call show()
                                mediaController.show(); // Or mediaController.show(0) for indefinitely
                            }
                        });

                        // Set an error listener (highly recommended)
                        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                            @Override
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                Log.e(TAG, "MediaPlayer Error: " + what + ", " + extra);
                                // Handle errors, e.g., show a Toast to the user
                                Toast.makeText(MainActivity.this, "Error playing video", Toast.LENGTH_SHORT).show();
                                return true; // Return true to indicate you handled the error
                            }
                        });

                        // Optional: Set a completion listener
                        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                // Video finished playing
                                Toast.makeText(MainActivity.this, "Video finished", Toast.LENGTH_SHORT).show();
                            }
                        });



                        videoView.setVisibility(View.VISIBLE); // Make sure the VideoView is visible!
                        videoView.requestFocus();

//                        videoView.setVisibility(View.VISIBLE);
//                        videoView.requestFocus();
//                        mediaController.show(); // Show controls initially

                    } else {
                        Toast.makeText(this, "Failed to get video URI", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Video selection cancelled or failed.");
                        mOutputText.setText("Video selection cancelled.");
                    }
                });


        // --- Button Click Listeners ---

        selectVideoButton.setOnClickListener(v -> checkAndRequestVideoPermission());

        startStreamBtn.setOnClickListener(v -> {
            if (mCredential.getSelectedAccount() == null) {
                chooseAccount(); // Launch account picker if no account is selected
            } else {
                try {
                    startStream(); // Already has an account, proceed
                } catch (GeneralSecurityException | IOException e) {
                    Log.e(TAG, "Error starting stream", e);
                    mOutputText.setText("Error starting stream: " + e.getMessage());
                }
            }
        });

        callYoutubeApiButton.setOnClickListener(v -> {
            callYoutubeApiButton.setEnabled(false);
            mOutputText.setText("");
            getResultsFromApi(); // Initiate the Quickstart API call
            callYoutubeApiButton.setEnabled(true); // Re-enable after initiation (AsyncTask runs in background)
        });

        // Initialize MediaController for VideoView
        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // Optional: Hide/show media controller on video view click
        videoView.setOnClickListener(v -> {
            if (videoView.isPlaying() || videoView.getCurrentPosition() > 0) {
                if (mediaController.isShowing()) {
                    mediaController.hide();
                } else {
                    mediaController.show(5000); // Show for 5 seconds
                }
            }
        });




        // Add this at the end:
        checkInitialRequirements();

        // Update login status immediately
        updateLoginStatus();
    }

    @SuppressLint("SetTextI18n")
    private void updateLoginStatus() {
        String accountName = mCredential.getSelectedAccountName();
        if (accountName != null) {
            tvLoginStatus.setText("Logged in as: " + accountName);
            tvLoginStatus.setTextColor(ContextCompat.getColor(this, R.color.green)); // Success color
        } else {
            tvLoginStatus.setText("Not logged in to Google");
            tvLoginStatus.setTextColor(ContextCompat.getColor(this, R.color.red)); // Error color
        }
    }


    /**
     * Fetches existing upcoming broadcasts and displays them in a dialog for selection.
     * @throws IOException If there's an issue communicating with the YouTube API.
     * @throws GeneralSecurityException If there's a security-related issue with the API call.
     */
    private void selectExistingBoardCast() throws IOException, GeneralSecurityException {
        Log.d("selectExistingStream", "selectExistingStream called");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                YouTube.LiveBroadcasts.List request = youtubeService.liveBroadcasts()
                        .list("snippet,contentDetails,status");
                LiveBroadcastListResponse response = request.setBroadcastStatus("upcoming")
                        .setBroadcastType("all")
                        .execute();

                // Parse broadcasts into a list
                List<YouTubeBroadcast> broadcasts = new ArrayList<>();
                if (response != null && response.getItems() != null) {
                    for (LiveBroadcast broadcast : response.getItems()) {
                        String thumbnailUrl = broadcast.getSnippet().getThumbnails().getHigh().getUrl();
                        broadcasts.add(new YouTubeBroadcast(
                                broadcast.getId(),
                                broadcast.getSnippet().getTitle(),
                                broadcast.getSnippet().getDescription(),
                                thumbnailUrl,
                                broadcast.getStatus().getLifeCycleStatus()
                        ));
                    }
                }
                // Post result back to UI thread if needed
                runOnUiThread(() -> showBroadcastSelectionDialog(broadcasts));

            } catch (IOException e) {
                Log.e("BackgroundTask", "Error fetching stream", e);
            }
        });

    }
    private void updateProgress() {
        mainHandler.post(() -> {
           Log.d("updateProgress", "updateProgress called") ;
        });
        int maxDuration = Math.max(
                (int) genericFromFile.getVideoDuration(),
                (int) genericFromFile.getAudioDuration()
        );
        mainHandler.post(() -> {
            Log.d("updateProgress", "maxDuration: " + maxDuration);
        });
        if (seekBar != null) {
            seekBar.setMax(maxDuration);
        }

        executorService.execute(() -> {
            while (genericFromFile.isStreaming() || genericFromFile.isRecording()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                if (!touching) {
                    int progress = Math.max(
                            (int) genericFromFile.getVideoTime(),
                            (int) genericFromFile.getAudioTime()
                    );

                    mainHandler.post(() -> {
                        if (seekBar != null) {
                            seekBar.setProgress(progress);
                        }
                    });
                }
            }
        });
    }


    private void delay(int i) {
    }

    private void transitionBroadcastToLive(String broadcastId) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Attempting to transition broadcast " + broadcastId + " to live");

                YouTube.LiveBroadcasts.Transition transitionRequest = youtubeService.liveBroadcasts()
                        .transition("live", broadcastId, "status");

                LiveBroadcast transitionedBroadcast = transitionRequest.execute();

                mainHandler.post(() -> {
                    String newStatus = transitionedBroadcast.getStatus().getLifeCycleStatus();
                    Toast.makeText(MainActivity.this,
                            "Broadcast is now: " + newStatus, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Transition successful. New status: " + newStatus);
                });

            } catch (GooglePlayServicesAvailabilityIOException e) {
                Log.e(TAG, "Google Play Services error", e);
                mainHandler.post(() -> showGooglePlayServicesAvailabilityErrorDialog(e.getConnectionStatusCode()));
            } catch (UserRecoverableAuthIOException e) {
                Log.e(TAG, "Auth error", e);
                mainHandler.post(() -> authorizationLauncher.launch(e.getIntent()));
            } catch (IOException e) {
                Log.e(TAG, "Network error", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Transition failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return uri.getPath(); // fallback
    }

//    private static ArrayList<IPacket> callMyDataPacket(String url, String fileName){
//        // TODO Auto-generated method stub
//        ArrayList<IPacket> arrayList = new ArrayList<IPacket>();
//        IContainer container = IContainer.make();
//        IContainerFormat containerFormat_live = IContainerFormat.make();
//        containerFormat_live.setOutputFormat("flv", url + "/" + fileName, null);
//        container.setInputBufferLength(0);
//        int retVal = container.open(url + "/" + fileName, IContainer.Type.WRITE, containerFormat_live);
//        if (retVal < 0) {
//            System.err.println("Could not open output container for live stream");
//            System.exit(1);
//        }
//        IStream stream1 = container.addNewStream(0);
//        IStreamCoder coder = stream1.getStreamCoder();
//        ICodec codec = ICodec.findEncodingCodec(ICodec.ID.CODEC_ID_H264);
//        coder.setNumPicturesInGroupOfPictures(5);
//        coder.setCodec(codec);
//        coder.setBitRate(200000);
//        coder.setPixelType(IPixelFormat.Type.YUV420P);
//        coder.setHeight(480);
//        coder.setWidth(640);
//        System.out.println("[ENCODER] video size is " + 640 + "x" + 480);
//        coder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
//        coder.setGlobalQuality(0);
//        IRational frameRate = IRational.make(24, 1);
//        coder.setFrameRate(frameRate);
//        coder.setTimeBase(IRational.make(frameRate.getDenominator(), frameRate.getNumerator()));
//        Properties props = new Properties();
////        props.setProperty("x264opts", "bitrate=10500:qpmin=7:min-keyint=5:keyint=15:cabac=1:pass=1:stats=/home/surfdogisHappyDog/Downloads/my888file.stats:no-mbtree=1");
////        props.setProperty("tune", "film");
////        props.setProperty("preset", "slow");
//        InputStream is = MainActivity.class.getClass().getResourceAsStream("/resources/libx264-normal.ffpreset");
//        try {
//            props.load(is);
//        } catch (IOException e) {
//            System.err.println("You need the libx264-normal.ffpreset file from the Xuggle distribution in your classpath.");
//            System.exit(1);
//        }
//        Configuration.configure(props, coder);
//        coder.open();
//        int retvall = container.writeHeader();
//        if (retvall < 0)
//            throw new RuntimeException("Could not write header for: .................." );
//        long firstTimeStamp = System.currentTimeMillis();
//        long lastTimeStamp = -1;
//        int i = 0;
//        java.awt.Robot robot = new java.awt.Robot();
//        while (i < 60) {
//            //long iterationStartTime = System.currentTimeMillis();
//            long now = System.currentTimeMillis();
//            //grab the screenshot
//            java.awt.image.BufferedImage image = robot.createScreenCapture(new java.awt.Rectangle(0, 0, 640, 480));
//            //convert it for Xuggler
//            java.awt.image.BufferedImage currentScreenshot = new java.awt.image.BufferedImage(image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_3BYTE_BGR);
//            currentScreenshot.getGraphics().drawImage(image, 0, 0, null);
//            //start the encoding process
//            IPacket packet = IPacket.make();
//            IConverter converter = ConverterFactory.createConverter(currentScreenshot, IPixelFormat.Type.YUV420P);
//            long timeStamp = (now - firstTimeStamp) * 1000;
//            IVideoPicture outFrame = converter.toPicture(currentScreenshot, timeStamp);
//            if (i == 0) {
//                //make first frame keyframe
//                outFrame.setKeyFrame(true);
//            }
//            outFrame.setQuality(0);
//            coder.encodeVideo(packet, outFrame, 0);
//            outFrame.delete();
//            if (packet.isComplete()) {
//                packet.setStreamIndex(0);
//                container.writePacket(packet);
//                System.out.println("[ENCODER Packet Completed] writing packet of size " + packet.getSize() + " for elapsed time " + ((timeStamp - lastTimeStamp) / 1000));
//                lastTimeStamp = timeStamp;
//                arrayList.add(packet);
//            }
//            System.out.println("[ENCODER] encoded image " + i + " in " + (System.currentTimeMillis() - now));
//            i++;
//            try {
//                Thread.sleep(Math.max((long) (1000 / frameRate.getDouble()) - (System.currentTimeMillis() - now), 0));
////        	Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        int retval = container.writeTrailer();
//        if (retval < 0)
//            throw new RuntimeException("Could not write trailer to output file");
//        return arrayList;
//    }

    private void onBroadcastSelected(YouTubeBroadcast broadcast) {
        Log.d("SelectedBroadcast", "ID: " + broadcast.getId() + ", Title: " + broadcast.getTitle());
        // Example: Start streaming to this broadcast
        Toast.makeText(this, "Selected: " + broadcast.getTitle() + ". Preparing to stream.", Toast.LENGTH_LONG).show();

        // Ensure a video is selected
        if (selectedVideoUri == null) {
            mOutputText.setText("Please select a video first using the 'Select Video' button.");
            Toast.makeText(this, "Please select a video first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Create a new stream and bind it to the selected broadcast
        final String selectedBroadcastId = broadcast.getId();
        // Removed: String currentRtmpUrl = null; and String currentStreamKey = null; as they are now static members
        executorService.execute(() -> {
            Exception currentAsyncError = null;


            mainHandler.post(() -> {
                mProgress.setMessage("Creating and binding live stream to: " + broadcast.getTitle());
                mProgress.show();
            });

            try {
//                // Define the LiveStream object, which will be uploaded as the request body.
//                LiveStream liveStream = new LiveStream();

//                // Add the cdn object property to the LiveStream object.
//                CdnSettings cdn = new CdnSettings();
//                cdn.setFrameRate("60fps");
//                cdn.setIngestionType("rtmp");
//                cdn.setResolution("1080p");
//                liveStream.setCdn(cdn);

//                // Add the contentDetails object property to the LiveStream object.
//                LiveStreamContentDetails contentDetails = new LiveStreamContentDetails();
//                contentDetails.setIsReusable(true);
//                liveStream.setContentDetails(contentDetails);

//                // Add the snippet object property to the LiveStream object.
//                LiveStreamSnippet snippet = new LiveStreamSnippet();
//                snippet.setDescription("A description of your video stream. This field is optional.");
//                snippet.setTitle("Your new video stream's name");
//                liveStream.setSnippet(snippet);

//                // Define and execute the API request
//                YouTube.LiveStreams.Insert request = youtubeService.liveStreams()
//                        .insert("snippet,cdn,contentDetails,status", liveStream);
//                LiveStream returnStream = request.setKey(BuildConfig.API_KEY).execute();
//                System.out.println(returnStream);


//                // --- NEW CODE: Extract RTMP URL and Stream Key ---
//                if (returnStream != null && returnStream.getCdn() != null && returnStream.getCdn().getIngestionInfo() != null) {
//                    currentRtmpUrl = returnStream.getCdn().getIngestionInfo().getIngestionAddress();
//                    currentStreamKey = returnStream.getCdn().getIngestionInfo().getStreamName(); // Save the stream key
//                    streamKeyRetrieved = true; // This flag confirms we got the key
//                    // set up the stream URL with the key
//                    fullRtmpUrl = currentRtmpUrl + "/" + currentStreamKey;
//                    mainHandler.post(() -> {
//                        Log.d(TAG, "Successfully created stream. RTMP URL: " + currentRtmpUrl + ", Stream Key: " + currentStreamKey);
//                        System.out.println("Successfully created stream. RTMP URL: " + currentRtmpUrl + ", Stream Key: " + currentStreamKey);
//                    });

                // When selecting an existing broadcast, we don't insert a new one.
                // We use the 'broadcast' object passed to this method (which is the selected one).
                // So, the insert operation below is not needed here.
                // YouTube.LiveBroadcasts.Insert liveBroadcastInsert =
                //         youtubeService.liveBroadcasts().insert("snippet,status", broadcast); // Use youtubeService
//                LiveBroadcast returnedBroadcast = broadcast; // The selected broadcast IS the returnedBroadcast for this context
                YouTube.LiveBroadcasts.List request = youtubeService.liveBroadcasts()
                        .list("id,snippet,contentDetails,status"); // Request 'id' as well
                LiveBroadcastListResponse response = request.setId(broadcast.getId()).execute();

//                // --- Set Scheduled Start and End Times ---
//                LiveBroadcast broadcastToUpdate = response.getItems().get(0); // Get the broadcast to update
//                LiveBroadcastSnippet snippet = broadcastToUpdate.getSnippet();
//                // Set scheduled start time to 1 minute from now
//                snippet.setScheduledStartTime(new DateTime(System.currentTimeMillis() + 60 * 1000));
//                // Set scheduled end time to 1 hour after the new start time
//                snippet.setScheduledEndTime(new DateTime(System.currentTimeMillis() + 60 * 1000 + 60 * 60 * 1000));
//                broadcastToUpdate.setSnippet(snippet);
//
//                // Update the broadcast with the new times
//                YouTube.LiveBroadcasts.Update updateRequest = youtubeService.liveBroadcasts().update("snippet", broadcastToUpdate);
//                LiveBroadcast updatedBroadcast = updateRequest.execute();



                // Print information from the API response, including the title and description.
                if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                    LiveBroadcast returnedBroadcastItem = response.getItems().get(0); // Get the first item
                    System.out.println("\n================== Returned Broadcast ==================\n");
                    System.out.println("  - Id: " + returnedBroadcastItem.getId()); // Use returnedBroadcastItem
                    System.out.println("  - Title: " + returnedBroadcastItem.getSnippet().getTitle());
                    System.out.println("  - Description: " + returnedBroadcastItem.getSnippet().getDescription());
                    System.out.println("  - Published At: " + returnedBroadcastItem.getSnippet().getPublishedAt());
                    System.out.println(
                            "  - Scheduled Start Time: " + returnedBroadcastItem.getSnippet().getScheduledStartTime());
                    System.out.println(
                            "  - Scheduled End Time: " + returnedBroadcastItem.getSnippet().getScheduledEndTime());
                    // You can also display these on the UI if needed:
                    // mainHandler.post(() -> {
                    //     mOutputText.append("\nSelected Broadcast Title: " + returnedBroadcastItem.getSnippet().getTitle());
                    //     mOutputText.append("\nSelected Broadcast Description: " + returnedBroadcastItem.getSnippet().getDescription());
                    // });
                }



                // Create a snippet with the video stream's title.
                LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
                streamSnippet.setTitle("I will become a successful Youtuber!!");

                // Define the content distribution network settings for the
                // video stream. The settings specify the stream's format and
                // ingestion type. See:
                // https://developers.google.com/youtube/v3/live/docs/liveStreams#cdn
                CdnSettings cdnSettings =  new CdnSettings();
                cdnSettings.setFormat("240p");
                cdnSettings.setFrameRate("60fps");
                cdnSettings.setResolution("1080p");
                cdnSettings.setIngestionType("rtmp");


                // continue passing video to this stream
                if (selectedVideoUri != null) {
                        String videoPath = getPathFromUri(selectedVideoUri);
                        if (videoPath != null) {
                            Video video = new Video();
                            video.set("video", videoPath);

                            LiveStream stream = new LiveStream();
                            stream.set("video", video);
                            stream.setKind("youtube#liveStream");
                            stream.setSnippet(streamSnippet);
                            stream.setCdn(cdnSettings);

                            // Construct and execute the API request to insert the stream.
                            YouTube.LiveStreams.Insert liveStreamInsert =
                                    youtubeService.liveStreams().insert("snippet,cdn", stream);
                            liveStreamInsert.set("video", videoPath);
                            LiveStream returnedStream = liveStreamInsert.execute();
                            returnedStream.set("video", videoPath);

                            // Print information from the API response.
                            System.out.println("\n================== Returned Stream ==================\n");
                            System.out.println("  - Id: " + returnedStream.getId());
                            System.out.println("  - Title: " + returnedStream.getSnippet().getTitle());
                            System.out.println("  - Description: " + returnedStream.getSnippet().getDescription());
                            System.out.println("  - Published At: " + returnedStream.getSnippet().getPublishedAt());
                            System.out.println("  - Ingestion Address: " + returnedStream.getCdn().getIngestionInfo().getIngestionAddress());


                            // Construct and execute a request to bind the new broadcast
                            // and stream.

                            YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtubeService.liveBroadcasts()
                                .bind(broadcast.getId(), "id,contentDetails"); // Use the ID of the selected broadcast

                            liveBroadcastBind.setStreamId(returnedStream.getId()); // Set the stream ID to bind

                            LiveBroadcast boundBroadcast = liveBroadcastBind.execute();


                            // Print information from the API response.
                            System.out.println("\n================== Returned Bound Broadcast ==================\n");
                            System.out.println("  - Broadcast Id: " + boundBroadcast.getId());
                            System.out.println(
                                    "  - Bound Stream Id: " + boundBroadcast.getContentDetails().getBoundStreamId());


                            String url = returnedStream.getCdn().getIngestionInfo().getIngestionAddress();
                            String fileName = returnedStream.getCdn().getIngestionInfo().getStreamName();

                            // --- Start FFmpeg Streaming ---
                            final String rtmpOutputUrl = url + "/" + fileName;
                            final String inputVideoPath = getPathFromUri(selectedVideoUri);

                            if (inputVideoPath == null) {
                                Log.e(TAG, "Cannot start FFmpeg: Input video path is null.");
                                mainHandler.post(() -> Toast.makeText(MainActivity.this, "Error: Could not get video file path for FFmpeg.", Toast.LENGTH_LONG).show());
                                return; // Exit if path is null
                            }

                            Log.d(TAG, "Starting FFmpeg stream. Input: " + inputVideoPath + ", Output: " + rtmpOutputUrl);
                            mainHandler.post(() -> mOutputText.append("\nStarting FFmpeg stream to: " + rtmpOutputUrl));

                            // Ensure FFmpeg execution is on a background thread
                            executorService.execute(() -> {
                                Process process = null;
                                try {
                                    // Loop the video indefinitely
                                    // Basic FFmpeg command (adjust as needed for your video)
                                    // -re: read input at native frame rate (important for streaming)
                                    // -i: input file
                                    // -c:v copy -c:a copy: if the video is already H.264/AAC, this avoids re-encoding (faster, less CPU)
                                    // -f flv: output format for RTMP

                                    // FFmpegKit example:
                                    // FFmpegSession session = FFmpegKit.execute("-i " + inputVideoPath + " -c:v mpeg4 " + rtmpOutputUrl);
                                    // if (ReturnCode.isSuccess(session.getReturnCode())) {
                                    //     // SUCCESS
                                    // } else if (ReturnCode.isCancel(session.getReturnCode())) {
                                    //     // CANCEL
                                    // } else {
                                    //     // FAILURE
                                    //     Log.d(TAG, String.format("Command failed with state %s and rc %s.%s", session.getState(), session.getReturnCode(), session.getFailStackTrace()));
                                    // }

                                    // -f flv: output format for RTMP
                                    // String[] cmd = {"ffmpeg", "-re", "-i", inputVideoPath, "-c:v", "libx264", "-preset", "veryfast", "-c:a", "aac", "-f", "flv", rtmpOutputUrl};
                                    // String ffmpegPath = getApplicationInfo().nativeLibraryDir + java.io.File.separator + "libffmpeg.so";
                                    // More robust command for potentially incompatible videos:
                                    String command = String.format("-stream_loop -1 -re -i \"%s\" -pix_fmt yuvj420p -g 48 -keyint_min 48 -sc_threshold 0 -b:v 4500k -b:a 128k -ar 44100 -acodec aac -vcodec h264_mediacodec -preset medium -crf 28 -threads 4 -f flv \"%s\" ", inputVideoPath, rtmpOutputUrl);


                                    // Execute FFmpeg command using FFmpegKit
                                    FFmpegSession session = FFmpegKit.executeAsync(command, session1 -> {
                                        if (ReturnCode.isSuccess(session1.getReturnCode())) {
                                            Log.d(TAG, "FFmpeg command executed successfully.");
                                            // Transition broadcast to live after FFmpeg starts successfully
                                            transitionBroadcastToLive(broadcast.getId());
                                        } else {
                                            Log.e(TAG, String.format("FFmpeg command failed with state %s and rc %s.%s", session1.getState(), session1.getReturnCode(), session1.getFailStackTrace()));
                                        }
                                    }, log -> Log.d(TAG, "FFmpeg log: " + log.getMessage()), output -> Log.d(TAG, "FFmpeg output: " + output.toString()));
                                    mainHandler.post(() -> {
                                        mOutputText.append("\nFFmpeg stream started (looping).");
                                        Log.d(TAG, "FFmpeg stream started (looping). Attempting to transition broadcast to live.");
                                    });

                                    // If you need to handle the process exit (e.g., if it crashes),
                                    // you can still use process.waitFor() but be aware it will block.
                                    // int exitCode = process.waitFor(); // This would block until ffmpeg exits

                                } catch (Exception e) { // Catch broader exceptions if FFmpegKit throws them
                                    // FFmpegKit's executeAsync handles process destruction internally on failure/cancel.
                                    // You might not need to manually destroy 'process' if using FFmpegKit.
                                    Log.e(TAG, "FFmpeg streaming error", e);
                                    mainHandler.post(() -> mOutputText.append("\nFFmpeg streaming error: " + e.getMessage()));
                                }
                            });
                            // --- End FFmpeg Streaming ---



                            // IMPORTANT: The "video" field for LiveStreams.insert is NOT for uploading the video file directly.
                            // It's a metadata field. YouTube API does not support direct video file upload via this method for live streaming.
                            // You stream the video content via RTMP.
                            // The line below is likely a misunderstanding of the API.
                            // video.set("video", videoPath); // This is incorrect for LiveStreams.insert

                            // You've already created 'returnStream'. You don't need to insert another one here
                            // unless you intend to create a completely separate stream object, which seems unlikely.

                            // If you are trying to associate the video file path with the stream for your own tracking,
                            // you could potentially use a custom property if the API supported it, or store it locally.
                            // However, the YouTube API itself doesn't use this "video" field in LiveStream for the video content.

                            // The following block for inserting another stream seems redundant and potentially incorrect
                            // based on the context of binding an existing broadcast to a new stream.
                            // LiveStream stream = new LiveStream();
                            // stream.set("video", video); // Incorrect usage
                            // stream.setKind("youtube#liveStream");
                            // stream.setSnippet(snippet); // Reusing snippet from earlier, which is fine
                            // stream.setCdn(cdn); // Reusing cdn from earlier
                            //
                            // YouTube.LiveStreams.Insert liveStreamInsert =
                            //         youtubeService.liveStreams().insert("snippet,cdn", stream); // Using youtubeService
                            // LiveStream newInsertedStream = liveStreamInsert.execute(); // This would create another new stream
                            // System.out.println("\n================== Newly Inserted Stream (Potentially Redundant) ==================\n");
                            // System.out.println("  - Id: " + newInsertedStream.getId());
                        } else {
                            Log.e(TAG, "Could not get path from selected video URI.");
                        }
                    } else {
                        Log.w(TAG, "No video selected to associate with the stream metadata (if that was the intent).");
                    }


//                    if (genericFromFile.isStreaming()) {
//                        genericFromFile.stopStream();
//                        bStream.setImageResource(R.drawable.stream_icon);
////                        if (!genericFromFile.isRecording()) ScreenOrientation.unlockScreen(this)
//                    } else if (genericFromFile.isRecording()) {
//                        if (!genericFromFile.isAudioDeviceEnabled()) genericFromFile.playAudioDevice();
//                        genericFromFile.startStream(fullRtmpUrl);
//                        bStream.setImageResource(R.drawable.stream_stop_icon);
////                        ScreenOrientation.lockScreen(this)
//                        updateProgress();
//                    } else {
//                        Toast.makeText(getApplicationContext(), "Error preparing stream, This device can't do it", Toast.LENGTH_SHORT).show();
//                    }


                } catch (IOException e) {
                throw new RuntimeException(e);

//              Bind the stream to the boardcast
//                YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtubeService.liveBroadcasts()
//                        .bind(broadcast.getId(), "id,contentDetails"); // Use the ID of the selected broadcast

//                liveBroadcastBind.setStreamId(returnStream.getId());
//
//                LiveBroadcast returnedBroadcast = liveBroadcastBind.execute();

//                YouTube.LiveBroadcasts.List requestLiveBroadcasts = youtubeService.liveBroadcasts()
//                        .list("snippet,contentDetails,status");
//                LiveBroadcastListResponse response = requestLiveBroadcasts.setId(broadcast.getId()).execute();
//                System.out.println(response);


                // send video to stream

                // Insert video to stream

//
//                YouTube.LiveBroadcasts.Transition tansationrequest = youtubeService.liveBroadcasts()
//                        .transition("live", broadcast.getId(), "status");

//                System.out.println(tansationrequest);


//                LiveBroadcastListResponse response1 = requestLiveBroadcasts.setId(broadcast.getId()).execute();
//                System.out.println(response1);

//                // Check if the broadcast status is "noData" and if a video is selected
//                if (response != null && !response.getItems().isEmpty()) {
//                    LiveBroadcast currentBroadcast = response.getItems().get(0);
//                    if (currentBroadcast.getStatus() != null && "noData".equals(currentBroadcast.getStatus().getLifeCycleStatus()) && selectedVideoUri != null && streamKeyRetrieved) {
//                        mainHandler.post(() -> {
//                            Toast.makeText(MainActivity.this, "Broadcast has no data. Attempting to stream selected video.", Toast.LENGTH_LONG).show();
//                            // Ensure you have the fullRtmpUrl (rtmpUrl + "/" + streamKey)
//                            // and the broadcast ID (broadcast.getId())
//                            startRtmpStreaming(currentRtmpUrl, currentStreamKey, broadcast.getId(), selectedVideoUri);
//                        });
//                    }
//                }


//                assert response != null;
//                bindRequest.setStreamId(response.getId()); // Use the ID of the newly created stream
//                LiveBroadcast boundBroadcast = bindRequest.execute();
                // After binding, update UI to show success

//                mainHandler.post(() -> {
//                    if (boundBroadcast != null && boundBroadcast.getId() != null) {
//                        Log.d(TAG, "Successfully bound broadcast " + boundBroadcast.getId() + " to stream " + response.getId());
//                        Toast.makeText(MainActivity.this, "Broadcast '" + broadcast.getTitle() + "' successfully bound to new stream!", Toast.LENGTH_LONG).show();
//                        mOutputText.append("\nBroadcast '" + broadcast.getTitle() + "' bound to stream: " + response.getId());
//                    } else {
//                        Log.e(TAG, "Failed to bind broadcast to stream. BoundBroadcast object is null or has no ID.");
//                        Toast.makeText(MainActivity.this, "Error: Could not confirm broadcast binding.", Toast.LENGTH_LONG).show();
//                    }
//                });

                // After successful binding, check the broadcast status
//                mainHandler.post(() -> {
//                    // You can call a method here to fetch and display the broadcast status
//                    // For example:
//                    // checkBroadcastStatus(selectedBroadcastId);
//                    // Or directly use the status if available from 'boundBroadcast', after null check
//                    if (boundBroadcast != null && boundBroadcast.getStatus() != null && boundBroadcast.getStatus().getLifeCycleStatus() != null) {
//                        Log.d("BroadcastStatus", "Status: " + boundBroadcast.getStatus().getLifeCycleStatus());
//                        Toast.makeText(MainActivity.this, "Broadcast status: " + boundBroadcast.getStatus().getLifeCycleStatus(), Toast.LENGTH_LONG).show();
//                    } else {
//                        Log.e("BroadcastStatus", "Could not get broadcast status. Broadcast or status is null.");
//                        Toast.makeText(MainActivity.this, "Could not retrieve broadcast status.", Toast.LENGTH_LONG).show();
//                    }
//                });



//                // Initialize the streamer if not already done
//                mainHandler.post(() -> {
//                    Log.d(TAG, "Initializing GenericFromFile");
//                    System.out.println("Initializing GenericFromFile");
//                });
//
//                if (genericFromFile == null) {
//                    OpenGlView openGlView = findViewById(R.id.surfaceView); // Make sure you have this in your layout
//                    genericFromFile = new GenericFromFile(openGlView, this, this, this);
//                }
//
//                try {
//                    // Prepare the video file
//                    mainHandler.post(() -> {
//                        Log.d(TAG, "Preparing video from URI: " + selectedVideoUri);
//                        System.out.println("Preparing video from URI: " + selectedVideoUri);
//                    });
//                    String videoPath = getPathFromUri(selectedVideoUri);
//                    if (videoPath != null) {
//                        mainHandler.post(() -> {
//
//                            Log.d(TAG, "Video path resolved: " + videoPath);
//                            System.out.println("Video path resolved: " + videoPath);
//                        });
//                        genericFromFile.prepareVideo(videoPath, 1920, 1080, 30, 3000 * 1024);
//                        mainHandler.post(() -> {
//                            Log.d(TAG, "Video prepared successfully");
//                        });
//
//                        // Start streaming
//                        if (!genericFromFile.isStreaming()) {
//                            genericFromFile.startStream(fullRtmpUrl);
//                            mainHandler.post(() -> {
//                                Log.d(TAG, "Starting stream to: " + fullRtmpUrl);
//                            });
//
//                            mainHandler.post(() -> {
//                                Toast.makeText(this, "Streaming started!", Toast.LENGTH_SHORT).show();
//                            });
//                            // Transition broadcast to "live" state
//                            transitionBroadcastState(broadcast.getId(), "live");
//
//                            // Start progress updater
//                            updateProgress();
//                            // In your streaming code after starting the stream:
//                            if (genericFromFile.isStreaming()) {
//                                transitionBroadcastToLive(broadcast.getId());
//                            }
//                        }
//                    } else {
//                        Toast.makeText(this, "Could not get video file path", Toast.LENGTH_SHORT).show();
//                    }
//                } catch (Exception e) {
//                    mainHandler.post(() -> {
//                        Log.e(TAG, "Error starting stream", e);
//                    });
//                    Toast.makeText(this, "Error starting stream: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                }

//



//                // --- 3. Bind Live Broadcast to Live Stream ---
//                YouTube.LiveBroadcasts.Bind bindRequest = youtubeService.liveBroadcasts()
//                        .bind(selectedBroadcastId, "id,contentDetails"); // Use the ID of the selected broadcast
//                bindRequest.setStreamId(createdStream.getId());
//                LiveBroadcast boundBroadcast = bindRequest.execute();
//                Log.d(TAG, "Existing broadcast " + boundBroadcast.getId() + " bound to new stream: " + createdStream.getId());

            } catch (Exception e) {
                currentAsyncError = e;
                Log.e(TAG, "Unexpected exception during stream creation/binding for existing broadcast", e);
            } finally {
                final String capturedRtmpUrl = currentRtmpUrl; // Capture current state for the lambda
                final String capturedStreamKey = currentStreamKey; // Capture current state for the lambda
                final Exception capturedError = currentAsyncError; // Capture current state for the lambda
                final boolean finalStreamKeyRetrieved = streamKeyRetrieved;
                final Uri finalSelectedVideoUri = selectedVideoUri;

                mainHandler.post(() -> {
                    mProgress.dismiss();
                    if (capturedError != null) { // Use capturedError instead of finalError

                        mOutputText.setText("Error setting up stream for selected broadcast: " + capturedError.getMessage());
                        Log.e(TAG, "Error setting up stream for selected broadcast", capturedError);

                    } else if (capturedRtmpUrl != null && capturedStreamKey != null && finalSelectedVideoUri != null && finalStreamKeyRetrieved) {
                        // Update the UI and also store the stream key and RTMP URL globally if needed for other activities
                        // currentRtmpUrl and currentStreamKey are already static and updated
                        mOutputText.setText("Stream ready for broadcast '" + broadcast.getTitle() +
                                            "'. RTMP: " + capturedRtmpUrl + "/" + capturedStreamKey +
                                            "\nStream Key (for StreamActivity): " + capturedStreamKey);
                        // startRtmpStreaming is now implicitly handled by the FFmpeg command within the try block
                    } else {
                        mOutputText.setText("Failed to get RTMP details or video not selected for broadcast '" + broadcast.getTitle() + "'.");
                        Log.e(TAG, "Failed to get RTMP details for selected broadcast.");
                    }
                });
            }
        });
    }


    private void showBroadcastSelectionDialog(List<YouTubeBroadcast> broadcasts) {
        if (broadcasts.isEmpty()) {
            Toast.makeText(this, "No broadcasts found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert broadcasts to displayable strings
        String[] titles = new String[broadcasts.size()];
        for (int i = 0; i < broadcasts.size(); i++) {
            titles[i] = broadcasts.get(i).getTitle() + " (" + broadcasts.get(i).getStatus() + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Broadcast")
                .setItems(titles, (dialog, which) -> {
                    YouTubeBroadcast selected = broadcasts.get(which);
                    onBroadcastSelected(selected); // Handle selection
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor service when the activity is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Attempt to stop all executing tasks
        }
    }


    @SuppressLint("SetTextI18n")
    private void getResultsFromApiAsync() {
        mOutputText.setText("");
        mProgress.setMessage("Calling YouTube Data API (Quickstart)...");
        mProgress.show();

        executorService.execute(() -> { // This code runs on a background thread
            List<String> output = null;
            Exception currentError = null;

            try {
                YouTube mService = youtubeService; // Use the initialized service
                List<String> channelInfo = new ArrayList<>();
                ChannelListResponse result = mService.channels().list("snippet,contentDetails,statistics")
                        .setForUsername("GoogleDevelopers")
                        .execute();
                List<Channel> channels = result.getItems();
                if (channels != null && !channels.isEmpty()) {
                    Channel channel = channels.get(0);
                    channelInfo.add("This channel's ID is " + channel.getId() + ". " +
                            "Its title is '" + channel.getSnippet().getTitle() + "', " +
                            "and it has " + channel.getStatistics().getViewCount() + " views.");
                }
                output = channelInfo;

            } catch (GooglePlayServicesAvailabilityIOException e) {
                currentError = e;
                // Post to main thread to show dialog
                mainHandler.post(() -> {
                    showGooglePlayServicesAvailabilityErrorDialog(e.getConnectionStatusCode());
                });
            } catch (UserRecoverableAuthIOException e) {
                currentError = e;
                // Post to main thread to launch authorization intent
                mainHandler.post(() -> {
                    authorizationLauncher.launch(e.getIntent());
                });
            } catch (IOException e) {
                currentError = e;
                // Post error message to main thread
                mainHandler.post(() -> {
                    mOutputText.setText("The following error occurred (Quickstart API):\n" + e.getMessage());
                    Log.e(TAG, "Quickstart API error", e);
                });
            } catch (Exception e) {
                currentError = e;
                // Catch any other unexpected exceptions
                mainHandler.post(() -> {
                    mOutputText.setText("An unexpected error occurred (Quickstart API):\n" + e.getMessage());
                    Log.e(TAG, "Quickstart API unexpected error", e);
                });
            } finally {
                // Dismiss progress dialog and update UI on main thread
                Exception finalCurrentError = currentError;
                List<String> finalOutput = output;
                mainHandler.post(() -> {
                    mProgress.hide();
                    if (finalCurrentError == null && finalOutput != null) {
                        if (finalOutput.isEmpty()) {
                            mOutputText.setText("No results returned from Quickstart API.");
                        } else {
                            finalOutput.add(0, "Data retrieved using the YouTube Data API (Quickstart):");
                            mOutputText.setText(TextUtils.join("\n", finalOutput));
                        }
                    } else if (finalCurrentError == null) { // This case handles cancel, or if output is null for other reasons not caught
                        mOutputText.setText("Quickstart API Request cancelled or no result.");
                    }
                });
            }
        });
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute(); // Quickstart API call
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that setting the account
     * requires GET_ACCOUNTS permission.
     */
    private void chooseAccount() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi(); // Try API call again with selected account
            } else {
                // Start a dialog from which the user can choose an account
                accountPickerLauncher.launch(mCredential.newChooseAccountIntent());
            }
        } else {
            // Request the GET_ACCOUNTS permission
            requestVideoPermissionLauncher.launch(new String[]{Manifest.permission.GET_ACCOUNTS});
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is a`vailable and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     * Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES // Use old request code as a tag for the dialog
        );
        dialog.show();
    }


    /**
     * Performs a YouTube Live Broadcast insertion.
     * This method is called when the "Start Stream" button is clicked.
     */
    @SuppressLint({"StaticFieldLeak", "SetTextI18n"})
    private void startStream() throws GeneralSecurityException, IOException {
        Log.d(TAG, "Attempting to start YouTube Live Stream...");
        mOutputText.setText("Attempting to start YouTube Live Stream...");

        if (youtubeService == null) {
            Log.e(TAG, "YouTube service not initialized.");
            mOutputText.setText("Error: YouTube service not initialized.");
            return;
        }

        // Add a check to ensure a video has been selected before attempting to stream
        if (selectedVideoUri == null) { // <--- ADD THIS CHECK
            mOutputText.setText("Please select a video first using the 'Select Video' button.");
            Toast.makeText(this, "Please select a video first.", Toast.LENGTH_LONG).show();
            return;
        }

        executorService.execute(() -> {
            Exception currentAsyncError = null;
            // Removed: String currentRtmpUrl = null; and String currentStreamKey = null; as they are now static members
            String currentBroadcastId = null; // Local variable for this specific broadcast
            String resultString = null;

            mainHandler.post(() -> {
                mProgress.setMessage("Creating live broadcast and stream...");
                mProgress.show();
            });

            try {
                // ... (your existing API calls for creating broadcast and stream) ...

                // --- 1. Create Live Broadcast ---
                LiveBroadcast liveBroadcast = new LiveBroadcast();
                LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
                broadcastSnippet.setScheduledStartTime(new DateTime(System.currentTimeMillis() + 60 * 1000));
                broadcastSnippet.setScheduledEndTime(new DateTime(System.currentTimeMillis() + 60 * 60 * 1000));
                broadcastSnippet.setTitle("My Android Video Stream - " + System.currentTimeMillis());
                broadcastSnippet.setDescription("Streaming a pre-recorded video from Android.");
                liveBroadcast.setSnippet(broadcastSnippet);

                LiveBroadcastStatus broadcastStatus = new LiveBroadcastStatus();
                broadcastStatus.setPrivacyStatus("unlisted");
                liveBroadcast.setStatus(broadcastStatus);

                LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();
                contentDetails.setEnableClosedCaptions(true);
                contentDetails.setEnableContentEncryption(true);
                contentDetails.setEnableDvr(true);
                contentDetails.setEnableEmbed(true);
                contentDetails.setRecordFromStart(true);
                contentDetails.setStartWithSlate(true);
                liveBroadcast.setContentDetails(contentDetails);

                YouTube.LiveBroadcasts.Insert broadcastInsertRequest = youtubeService.liveBroadcasts()
                        .insert("snippet,contentDetails,status", liveBroadcast);
                LiveBroadcast createdBroadcast = broadcastInsertRequest.execute();
                currentBroadcastId = createdBroadcast.getId();
                Log.d(TAG, "Broadcast created: " + createdBroadcast.getId());

                // --- 2. Create Live Stream ---
                LiveStream liveStream = new LiveStream();
                LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
                streamSnippet.setTitle("Android Stream Key for " + createdBroadcast.getSnippet().getTitle());
                liveStream.setSnippet(streamSnippet);

                CdnSettings cdnSettings = new CdnSettings();
                cdnSettings.setFormat("1080p");
                cdnSettings.setIngestionType("rtmp");
                liveStream.setCdn(cdnSettings); // Corrected this previously

                YouTube.LiveStreams.Insert streamInsertRequest = youtubeService.liveStreams()
                        .insert("snippet,cdnSettings", liveStream);
                LiveStream createdStream = streamInsertRequest.execute();
                Log.d(TAG, "Live Stream created: " + createdStream.getId());

                // Get Ingestion Info (RTMP URL and Stream Key)
                currentRtmpUrl = createdStream.getCdn().getIngestionInfo().getIngestionAddress(); // Corrected this previously
                currentStreamKey = createdStream.getCdn().getIngestionInfo().getStreamName();   // Corrected this previously
                fullRtmpUrl = currentRtmpUrl + "/" + currentStreamKey;
                Log.d(TAG, "Full RTMP URL: " + fullRtmpUrl);
                // Now you can pass fullRtmpUrl to another function


                // --- 3. Bind Live Broadcast to Live Stream ---
                YouTube.LiveBroadcasts.Bind bindRequest = youtubeService.liveBroadcasts()
                        .bind(createdBroadcast.getId(), "id,contentDetails");
                bindRequest.setStreamId(createdStream.getId());
                LiveBroadcast boundBroadcast = bindRequest.execute();
                Log.d(TAG, "Broadcast bound to stream: " + boundBroadcast.getId());

                resultString = "Broadcast and Stream created. Ready to stream to: " + currentRtmpUrl + "/" + currentStreamKey;

            } catch (GooglePlayServicesAvailabilityIOException e) {
                currentAsyncError = e;
                mainHandler.post(() -> showGooglePlayServicesAvailabilityErrorDialog(e.getConnectionStatusCode()));
            } catch (UserRecoverableAuthIOException e) {
                currentAsyncError = e;
                mainHandler.post(() -> authorizationLauncher.launch(e.getIntent()));
            } catch (IOException e) {
                currentAsyncError = e;
                resultString = "Error: " + e.getMessage();
            } catch (Exception e) {
                currentAsyncError = e;
                resultString = "Unexpected Error: " + e.getMessage();
            } finally {
                final String finalResult = resultString;
                final Exception finalError = currentAsyncError;
                // Removed: final String finalRtmpUrl = currentRtmpUrl;
                // Removed: final String finalStreamKey = currentStreamKey;
                final String finalBroadcastId = currentBroadcastId;
                // Capture selectedVideoUri for use in the lambda
                final Uri finalSelectedVideoUri = selectedVideoUri; // <--- NEW LINE

                mainHandler.post(() -> {
                    mProgress.dismiss();
                    if (finalError != null) {
                        if (!(finalError instanceof UserRecoverableAuthIOException)) {
                            mOutputText.setText("Error creating broadcast: " + finalError.getMessage());
                            Log.e(TAG, "Error creating broadcast", finalError);
                        }
                    } else if (finalResult != null) {
                        mOutputText.setText(finalResult);
                        Log.d(TAG, finalResult);

                        // Use the captured finalSelectedVideoUri
                        if (currentRtmpUrl != null && currentStreamKey != null && finalBroadcastId != null && finalSelectedVideoUri != null) { // <--- MODIFIED LINE
                            // currentRtmpUrl and currentStreamKey are already static and updated

                            mOutputText.append("\nStream Key (for StreamActivity): " + currentStreamKey); // Show stream key in UI
                            startRtmpStreaming(currentRtmpUrl, currentStreamKey, finalBroadcastId, finalSelectedVideoUri);
                        } else { // Check if stream key was not retrieved
                            mOutputText.append("\nFailed to get RTMP details or video URI (Is video selected?).");
                        }
                    } else {
                        mOutputText.setText("Failed to create broadcast for unknown reason.");
                        Log.e(TAG, "Failed to create broadcast, result was null.");
                    }
                });
            }
        });


    }


    /**
     * Placeholder method to initiate RTMP streaming of the selected video.
     * This method will contain the complex logic for media extraction, encoding,
     * and sending data via an RTMP streaming library.
     *
     * @param rtmpUrl The base RTMP ingest URL (e.g., "rtmp://a.rtmp.youtube.com/live2")
     * @param streamKey The unique stream key for your YouTube Live Stream
     * @param broadcastId The ID of the YouTube Live Broadcast
     * @param videoUri The URI of the video file to stream
     */
    private void startRtmpStreaming(String rtmpUrl, String streamKey, String broadcastId, Uri videoUri) {
        Log.d(TAG, "startRtmpStreaming called!");
        Log.d(TAG, "RTMP URL: " + rtmpUrl);
        Log.d(TAG, "Stream Key: " + streamKey);
        Log.d(TAG, "Broadcast ID: " + broadcastId);
        Log.d(TAG, "Video URI: " + videoUri.toString());

        mOutputText.append("\n\n--- Initiating RTMP Streaming ---");
        mOutputText.append("\nThis is where the complex video processing and streaming logic will go.");
        mOutputText.append("\nLook at previous explanations for: MediaExtractor, MediaCodec, and RTMP library integration.");
        mOutputText.append("\nNote: Actual streaming is not implemented in this placeholder.");

        // --- YOUR IMPLEMENTATION GOES HERE ---
        // This will involve:
        // 1. Initializing an RTMP streaming library.
        // 2. Setting up MediaExtractor to read from videoUri.
        // 3. Setting up MediaCodec for video and audio encoding.
        // 4. Feeding extracted and encoded data to the RTMP library.
        // 5. Managing threading for all these operations.
        // 6. Transitioning the YouTube broadcast state (e.g., to "live") once streaming starts.

        // Example of transitioning the broadcast once you are confident streaming will start:
        // Call this AFTER your RTMP library successfully connects and starts sending data
        // transitionBroadcastState(broadcastId, "live");
    }

    private void transitionBroadcastState(String broadcastId, String newState) {
        executorService.execute(() -> {
            Exception currentError = null;
            String resultString = null;

            try {
                YouTube.LiveBroadcasts.Transition transitionRequest = youtubeService.liveBroadcasts()
                        .transition(newState, broadcastId, "status");
                LiveBroadcast transitionedBroadcast = transitionRequest.execute();
                resultString = "Broadcast " + broadcastId + " transitioned to: " + transitionedBroadcast.getStatus().getLifeCycleStatus();
            } catch (IOException e) {
                currentError = e;
                resultString = "Error transitioning broadcast: " + e.getMessage();
            } finally {
                final String finalResult = resultString;
                final Exception finalError = currentError;
                mainHandler.post(() -> {
                    if (finalError != null) {
                        Log.e(TAG, "Transition error", finalError);
                        mOutputText.append("\n" + finalResult);
                    } else {
                        mOutputText.append("\n" + finalResult);
                        Log.d(TAG, finalResult);
                    }
                });
            }
        });
    }


    /**
     * Checks for and requests the necessary video media permissions based on Android version.
     */
    private void checkAndRequestVideoPermission() {
        Log.d(TAG, "Checking and requesting video permissions.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            boolean hasVideoPermission = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            boolean hasPartialPermission = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED;

            if (hasVideoPermission || hasPartialPermission) {
                openVideoSelectionIntent();
            } else {
                requestVideoPermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                });
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            ) {
                openVideoSelectionIntent();
            } else {
                requestVideoPermissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_VIDEO});
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ) {
                openVideoSelectionIntent();
            } else {
                requestVideoPermissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        }
    }

    /**
     * Launches an Intent to pick a video from storage.
     */
    private void openVideoSelectionIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        selectVideoIntentLauncher.launch(intent);
    }

    @Override
    public void onConnectionStarted(@NonNull String s) {

    }

    @Override
    public void onConnectionSuccess() {

    }

    @Override
    public void onConnectionFailed(@NonNull String s) {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onAuthError() {

    }

    @Override
    public void onAuthSuccess() {

    }

    @Override
    public void onAudioDecoderFinished() {

    }

    @Override
    public void onVideoDecoderFinished() {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public Uri getFilePath() {
        return filePath;
    }

    public void setFilePath(Uri filePath) {
        this.filePath = filePath;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public boolean isTouching() {
        return touching;
    }

    public void setTouching(boolean touching) {
        this.touching = touching;
    }

    /**
     * An asynchronous task that handles the YouTube Data API call for the Quickstart.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    @SuppressLint("StaticFieldLeak")
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private YouTube mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            // Re-use the existing youtubeService for consistency, or create a new one if scopes differ significantly
            mService = youtubeService; // Assuming 'youtubeService' is correctly initialized with 'mCredential'
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch information about the "GoogleDevelopers" YouTube channel.
         * @return List of Strings containing information about the channel.
         */
        private List<String> getDataFromApi() throws IOException {
            List<String> channelInfo = new ArrayList<>();
            ChannelListResponse result = mService.channels().list("snippet,contentDetails,statistics")
                    .setForUsername("GoogleDevelopers")
                    .execute();
            List<Channel> channels = result.getItems();
            if (channels != null && !channels.isEmpty()) {
                Channel channel = channels.get(0);
                channelInfo.add("This channel's ID is " + channel.getId() + ". " +
                        "Its title is '" + channel.getSnippet().getTitle() + "', " +
                        "and it has " + channel.getStatistics().getViewCount() + " views.");
            }
            return channelInfo;
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.setMessage("Calling YouTube Data API (Quickstart)...");
            mProgress.show();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.isEmpty()) {
                mOutputText.setText("No results returned from Quickstart API.");
            } else {
                output.add(0, "Data retrieved using the YouTube Data API (Quickstart):");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    authorizationLauncher.launch(
                            ((UserRecoverableAuthIOException) mLastError).getIntent());
                } else {
                    mOutputText.setText("The following error occurred (Quickstart API):\n"
                            + mLastError.getMessage());
                    Log.e(TAG, "Quickstart API error", mLastError);
                }
            } else {
                mOutputText.setText("Quickstart API Request cancelled.");
            }
        }
    }
}