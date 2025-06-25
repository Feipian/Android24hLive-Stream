package com.example.android24hstream;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
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
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "YouTubeApp"; // Use a consistent tag for logs

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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Make sure you have this layout

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
    @SuppressLint("StaticFieldLeak")
    private void startStream() throws GeneralSecurityException, IOException {
        Log.d(TAG, "Attempting to start YouTube Live Stream...");
        mOutputText.setText("Attempting to start YouTube Live Stream...");

        // Ensure YouTube service is initialized with the credential
        if (youtubeService == null) {
            Log.e(TAG, "YouTube service not initialized.");
            mOutputText.setText("Error: YouTube service not initialized.");
            return;
        }

        // Define the LiveBroadcast object
        LiveBroadcast liveBroadcast = new LiveBroadcast();

        LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();
        contentDetails.setEnableClosedCaptions(true);
        contentDetails.setEnableContentEncryption(true);
        contentDetails.setEnableDvr(true);
        contentDetails.setEnableEmbed(true);
        contentDetails.setRecordFromStart(true);
        contentDetails.setStartWithSlate(true);
        liveBroadcast.setContentDetails(contentDetails);

        LiveBroadcastSnippet snippet = new LiveBroadcastSnippet();
        // Set realistic start/end times for testing or adjust as needed
        snippet.setScheduledStartTime(new DateTime(System.currentTimeMillis() + 60 * 1000)); // 1 minute from now
        snippet.setScheduledEndTime(new DateTime(System.currentTimeMillis() + 60 * 60 * 1000)); // 1 hour from now
        snippet.setTitle("My Android Test Broadcast " + System.currentTimeMillis());
        snippet.setDescription("This is a test live stream from my Android app."); // Add a description
        liveBroadcast.setSnippet(snippet);

        LiveBroadcastStatus status = new LiveBroadcastStatus();
        status.setPrivacyStatus("unlisted"); // or "private", "public"
        liveBroadcast.setStatus(status);

        // Execute the API request in an AsyncTask to avoid blocking the UI thread
        new AsyncTask<Void, Void, String>() {
            private Exception asyncError = null;

            @Override
            protected void onPreExecute() {
                mProgress.setMessage("Creating live broadcast...");
                mProgress.show();
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    YouTube.LiveBroadcasts.Insert request = youtubeService.liveBroadcasts()
                            .insert("snippet,contentDetails,status", liveBroadcast);
                    LiveBroadcast response = request.execute();
                    return "Live Broadcast created:\nID: " + response.getId() + "\nTitle: " + response.getSnippet().getTitle()
                            + "\nStatus: " + response.getStatus().getPrivacyStatus() + "\nStream URL (approx.): https://youtube.com/watch?v=" + response.getId();
                } catch (UserRecoverableAuthIOException e) {
                    asyncError = e;
                    return null;
                } catch (IOException e) {
                    asyncError = e;
                    return "Error: " + e.getMessage();
                } catch (Exception e) { // Catch all other exceptions
                    asyncError = e;
                    return "Unexpected Error: " + e.getMessage();
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            protected void onPostExecute(String result) {
                mProgress.dismiss();
                if (asyncError != null) {
                    if (asyncError instanceof GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) asyncError).getConnectionStatusCode());
                    } else if (asyncError instanceof UserRecoverableAuthIOException) {
                        authorizationLauncher.launch(((UserRecoverableAuthIOException) asyncError).getIntent());
                    } else {
                        mOutputText.setText("Error creating broadcast: " + asyncError.getMessage());
                        Log.e(TAG, "Error creating broadcast", asyncError);
                    }
                } else if (result != null) {
                    mOutputText.setText(result);
                    Log.d(TAG, result);
                } else {
                    mOutputText.setText("Failed to create broadcast for unknown reason.");
                    Log.e(TAG, "Failed to create broadcast, result was null.");
                }
            }
        }.execute();
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
         * @throws IOException
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