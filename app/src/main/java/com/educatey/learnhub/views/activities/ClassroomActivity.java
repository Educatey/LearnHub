package com.educatey.learnhub.views.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.educatey.learnhub.R;
import com.educatey.learnhub.adapters.FilesAdapter;
import com.educatey.learnhub.data.Files;
import com.educatey.learnhub.data.User;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class ClassroomActivity extends AppCompatActivity {

    private final int PICK_IMAGE_REQUEST = 71;
    ProgressBar mProgressBar;
    String UserName, userId, FileUrl, fileName, FilePath;
    ArrayList<Files> list;
    FilesAdapter adapter;

    FloatingActionButton select_file;
    private String mSecL;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private Uri mSelectedFileUrl;
    private RecyclerView mRecyclerView;
    private DatabaseReference mDatabaseReference, reference;
    private long date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom);
        mProgressBar = findViewById(R.id.upload_progressBar);
        select_file = findViewById(R.id.select_file_fab);
        mRecyclerView = findViewById(R.id.rv_row);
        mRecyclerView = findViewById(R.id.rv_row);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        date = new Date().getTime();

        setupFirebaseAuth();
//        initImageLoader();
        getFiles();


        select_file.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("*/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_IMAGE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mSelectedFileUrl = data.getData();
            fileName = getFileName(mSelectedFileUrl);
            executeUploadTask();
        }
    }

    //get the actual name of the file to be uploaded from its Uri
    public String getFileName(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            assert result != null;
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    //fetch files from the RTDB
    //expensive operation - called everytime onCreate() gets called :(
    //TODO persistent library needed
    private void getFiles() {
        showProgressBar();
        mDatabaseReference.child("files").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                hideProgressBar();
                list = new ArrayList<>();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    Files s = dataSnapshot1.getValue(Files.class);
                    list.add(s);
                }
                adapter = new FilesAdapter(ClassroomActivity.this, list);
                mRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                hideProgressBar();
                Toast.makeText(ClassroomActivity.this,
                        "Opsss.... Something is wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //This method is used to start the upload task to the firebase storage
    private void executeUploadTask() {
        //specify where the photo will be stored
        final StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("classroom_files/" + fileName);

        final UploadTask uploadTask = ref.putFile(mSelectedFileUrl);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                    .getTotalByteCount());
            Snackbar.make(findViewById(R.id.relativeLayout),
                    "Uploaded " + (int) progress + "%", Snackbar.LENGTH_SHORT)
                    .setAction("Cancel", v -> uploadTask.cancel()).show();
        }).addOnCanceledListener(() -> Toast.makeText(ClassroomActivity.this, "Upload cancelled", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(ClassroomActivity.this, "Upload failed", Toast.LENGTH_SHORT).show());

        //generating a random string
        Random r = new java.util.Random();
        final String s = Long.toString(r.nextLong() & Long.MAX_VALUE, 36);

        Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            // Continue with the task to get the download URL
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                //saving file details to the RTDB
                Files file = new Files();
                file.setFileName(fileName);
                file.setUserName(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail());
                assert downloadUri != null;
                file.setFileUrl(downloadUri.toString());
                file.setFilePath(" ");
                file.setDate("");
                mDatabaseReference.child("files").child(s).setValue(file);
            } else {
                Toast.makeText(ClassroomActivity.this,
                        "File upload did not complete successfully, please, re-upload", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private void initImageLoader() {
//        UniversalImageLoader imageLoader = new UniversalImageLoader(ClassroomActivity.this);
//        ImageLoader.getInstance().init(imageLoader.getConfig());
//    }

    private void checkAuthenticationState() {
        Log.d("TAG", "Check Auth State: checking auth state.");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d("TAG", "checkAuthState: user is null, redirecting to Login Activity");
            Intent intent = new Intent(ClassroomActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Log.d("TAG", "Users is Authenticated.");

        }
    }

    private void setupFirebaseAuth() {
        Log.d("TAG", "setupFirebaseAuth: started.");

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                userId = user.getUid();
                getUserDetails();
                Log.d("TAG", "onAuthStateChanged:signed_in:" + user.getUid());

            } else {
                Log.d("TAG", "onAuthStateChanged:signed_out");
                Intent intent = new Intent(ClassroomActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        };
    }

    private void getUserDetails() {
        try {
            mDatabaseReference.child("users").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User details = dataSnapshot.getValue(User.class);
                    if (details != null) {
                        UserName = details.getName();
                        mSecL = details.getSecurity_level();
                    }
                    //get user securityLevel here
                    if (!mSecL.equals("2")) {
                        select_file.hide();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        } catch (Exception e) {
            Toast.makeText(ClassroomActivity.this,
                    "Error occured" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        //isActivityRunning = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        //isActivityRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticationState();
    }

    public void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

}