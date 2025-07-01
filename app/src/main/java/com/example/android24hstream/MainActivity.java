package com.example.android24hstream;

import static com.google.android.gms.common.GooglePlayServicesUtilLight.isGooglePlayServicesAvailable;

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
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.media.MediaPlayer;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

//import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "YouTubeApp"; // Use a consistent tag for logs

    private TextView tvLoginStatus;  // Add this line
    private Uri selectedVideoUri;

    private ExecutorService executorService;
    private String googleApiKey = buildconfig.googleApiKey;
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
    private String currentRtmpUrl;
    private String currentStreamKey;

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
        setContentView(R.layout.activity_main); // Make sure you have this layout

        // Initialize the status TextView
        tvLoginStatus = findViewById(R.id.tv_login_status);


        Button selectExistingStreamButton = findViewById(R.id.btn_select_existing_stream);
        selectExistingStreamButton.setOnClickListener(v -> {
            try {
                selectExistingStream();
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


    private void selectExistingStream() throws IOException, GeneralSecurityException {
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

    /**
     * When select board cast, we will go to this method
     * @param broadcast
     */
    private void onBroadcastSelected(YouTubeBroadcast broadcast) {
        Log.d("SelectedBroadcast", "ID: " + broadcast.getId() + ", Title: " + broadcast.getTitle());
        // Example: Start streaming to this broadcast
        Toast.makeText(this, "Selected: " + broadcast.getTitle(), Toast.LENGTH_SHORT).show();

        // You can now use broadcast.getId() to bind your stream:
        // String streamId = broadcast.getId();
        // startStreaming(streamId);

        // then we create a new stream for this broadcast
        createStream();
    }



    private void createStream() {
        Log.d(TAG, "Attempting to create a new YouTube Live Stream for an existing broadcast...");
        mOutputText.setText("Attempting to create a new Live Stream...");

        if (youtubeService == null) {
            Log.e(TAG, "YouTube service not initialized for createStream.");
            mOutputText.setText("Error: YouTube service not initialized.");
            return;
        }

        executorService.execute(() -> {
            Exception currentAsyncError = null;
            String newStreamId = null;
            String newRtmpUrl = null;
            String newStreamKey = null;
            String resultString = null;

            mainHandler.post(() -> {
                mProgress.setMessage("Creating new live stream...");
                mProgress.show();
            });

            try {
                // --- Create New Live Stream ---
                LiveStream liveStream = new LiveStream();

                // Add the cdn object property to the LiveStream object.
                CdnSettings cdn = new CdnSettings();
                cdn.setFrameRate("60fps");
                cdn.setIngestionType("rtmp");
                cdn.setResolution("1080p");
                liveStream.setCdn(cdn);

                // Add the contentDetails object property to the LiveStream object.
                LiveStreamContentDetails contentDetails = new LiveStreamContentDetails();
                contentDetails.setIsReusable(true);
                liveStream.setContentDetails(contentDetails);

                // Add the snippet object property to the LiveStream object.
                LiveStreamSnippet snippet = new LiveStreamSnippet();
                snippet.setDescription("A description of your video stream. This field is optional.");
                snippet.setTitle("Your new video stream's name");
                liveStream.setSnippet(snippet);

                // Define and execute the API request
                YouTube.LiveStreams.Insert request = youtubeService.liveStreams()
                        .insert("snippet,cdn,contentDetails,status", liveStream);
                LiveStream response = request.setKey(DEVELOPER_KEY).execute();

//                YouTube.LiveStreams.Insert streamInsertRequest = youtubeService.liveStreams()
//                        .insert("snippet,cdnSettings", liveStream);
//                LiveStream createdStream = streamInsertRequest.execute();
//
//                newStreamId = createdStream.getId();
//                newRtmpUrl = createdStream.getCdn().getIngestionInfo().getIngestionAddress();
//                newStreamKey = createdStream.getCdn().getIngestionInfo().getStreamName();
//                Log.d(TAG, "New Live Stream created: " + newStreamId);
//                Log.d(TAG, "New RTMP URL: " + newRtmpUrl + "/" + newStreamKey);
//
//                resultString = "New Stream created successfully. ID: " + newStreamId +
//                        "\nRTMP: " + newRtmpUrl + "/" + newStreamKey;

                // Note: We don't bind it here. Binding happens when the user selects a broadcast.
                // The user needs to choose which broadcast to bind this new stream to.

            } catch (IOException e) {
                currentAsyncError = e;
                resultString = "Error creating new stream: " + e.getMessage();
                Log.e(TAG, "Error in createStream", e);
            } finally {
                final String finalResult = resultString;
                // ... (Update UI on mainHandler similar to startStream's finally block)
                mainHandler.post(() -> mProgress.dismiss());
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
     * @return true if Google Play Services is available and up to
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
            String currentRtmpUrl = null;
            String currentStreamKey = null;
            String currentBroadcastId = null;
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
                Log.d(TAG, "RTMP URL: " + currentRtmpUrl + "/" + currentStreamKey);

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
                final String finalRtmpUrl = currentRtmpUrl;
                final String finalStreamKey = currentStreamKey;
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
                        if (finalRtmpUrl != null && finalStreamKey != null && finalBroadcastId != null && finalSelectedVideoUri != null) { // <--- MODIFIED LINE
                            startRtmpStreaming(finalRtmpUrl, finalStreamKey, finalBroadcastId, finalSelectedVideoUri); // <--- MODIFIED LINE
                        } else {
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