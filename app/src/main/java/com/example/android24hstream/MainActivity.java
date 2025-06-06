package com.example.android24hstream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import android.view.View; // Import View

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> selectVideoIntentLauncher;
    private VideoView videoView; // Declare VideoView
    private MediaController mediaController; // Declare MediaController

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure you have a layout

        videoView = findViewById(R.id.video_view); // Initialize VideoView

        // Initialize MediaController
        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView); // Attach MediaController to VideoView
        videoView.setMediaController(mediaController); // Set MediaController for VideoView

        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying() || videoView.getCurrentPosition() > 0) { // Only show if video is loaded/playing
                    if (mediaController.isShowing()) {
                        mediaController.hide();
                    } else {
                        // Show for 5 seconds (5000 milliseconds)
                        mediaController.show(5000);
                    }
                }
            }
        });


        // Initialize the Activity Result Launcher for requesting permissions
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allGranted = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // For Android 14+, check for both READ_MEDIA_VIDEO and READ_MEDIA_VISUAL_USER_SELECTED
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_VIDEO)) ||
                        Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))) {
                    allGranted = true; // One of them is sufficient for initial access
                } else {
                    allGranted = false;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_VIDEO))) {
                    allGranted = true;
                } else {
                    allGranted = false;
                }
            } else {
                // For Android 12L and lower
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_EXTERNAL_STORAGE))) {
                    allGranted = true;
                } else {
                    allGranted = false;
                }
            }

            if (allGranted) {
                Log.d("Permission", "Video permissions granted.");
                openVideoSelectionIntent();
            } else {
                Log.w("Permission", "Video permissions denied.");
                Toast.makeText(MainActivity.this, "Permission denied to read videos.", Toast.LENGTH_SHORT).show();
                // Handle permission denial: disable feature, show explanation, etc.
            }
        });

        // Initialize the Activity Result Launcher for selecting video via Intent
        selectVideoIntentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri videoUri = result.getData().getData();
                Log.d("VideoSelection", "Selected video URI: " + videoUri);
                if (videoUri != null) {
                    Log.d("VideoSelection", "Selected video URI: " + videoUri);
                    // Process the selected video URI here
                    videoView.setVideoURI(videoUri); // Set the video source
                    videoView.setVisibility(View.VISIBLE); // Make the VideoView visible
//                    videoView.start(); // Start playing the video
                    // Optionally, you can set focus to the videoView so controls appear immediately
                    videoView.requestFocus();

//                    mediaController.show();

                }
            } else {
                Log.d("VideoSelection", "Video selection cancelled or failed.");
            }
        });

        // Example: Call this method from a button click
        Button selectVideoButton = findViewById(R.id.btn_select_videos);
        if (selectVideoButton != null) {
            selectVideoButton.setOnClickListener(view -> checkAndRequestVideoPermission());
        }
    }

    /**
     * Checks for and requests the necessary video media permissions based on Android version.
     */
    private void checkAndRequestVideoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14 (API 34) and higher
            boolean hasVideoPermission = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            boolean hasPartialPermission = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED;

            if (hasVideoPermission || hasPartialPermission) {
                // Already have full or partial access
                openVideoSelectionIntent();
            } else {
                // Request both, as user might choose partial access
                requestPermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                });
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33)
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            ) {
                openVideoSelectionIntent();
            } else {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_VIDEO});
            }
        } else {
            // Android 12L (API 32) and lower
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ) {
                openVideoSelectionIntent();
            } else {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        }
    }

    /**
     * Launches an Intent to pick a video from the device's media store.
     * This method assumes permissions have already been granted.
     */
    private void openVideoSelectionIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*"); // Ensures only video files are shown
        selectVideoIntentLauncher.launch(intent);
    }
}