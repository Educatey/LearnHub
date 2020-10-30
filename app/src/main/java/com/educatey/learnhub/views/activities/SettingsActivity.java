package com.educatey.learnhub.views.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.educatey.learnhub.R;
import com.educatey.learnhub.data.User;
import com.educatey.learnhub.views.dialogs.ChangePhotoDialog;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity implements ChangePhotoDialog.OnPhotoReceivedListener {

    private static final String TAG = "SettingsActivityTag";
    private static final double MB_THRESHHOLD = 5.0;
    private static final double MB = 1000000.0;
    private static final int REQUEST_CODE = 1234;
    public static boolean isActivityRunning;
    public String lecturerProfilePic = "https://firebasestorage.googleapis.com/v0/b/smis-1234.appspot.com/o/profile_pics%2Flecturer.png?alt=media&token=8ef5d5d1-9b77-4378-bc89-ee8f00cd9639";
    public String studentProfilePic = "https://firebasestorage.googleapis.com/v0/b/smis-1234.appspot.com/o/profile_pics%2Fstudents.jpg?alt=media&token=a6ac3b2a-995f-460b-8afe-cec8d2a7a203";
    //widgets
    EditText mNames, mPhone, mEmail, mPassword;
    Button mSave;
    ImageView mProfile_image;
    ProgressBar mProgressBar;
    Uri uriNew;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private byte[] mBytes;
    private double progress;
    private boolean mStoragePermissions;
    private Uri mSelectedImageUri;
    private Bitmap mSelectedImageBitmap;
    private String profile_image_url;

    // convert from bitmap to byte array
    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    @Override
    public void getImagePath(Uri imagePath) {
        if (!imagePath.toString().equals("")) {
            mSelectedImageBitmap = null;
            mSelectedImageUri = imagePath;
            Log.d("TAG", "getImagePath: got the image uri: " + mSelectedImageUri);
            ImageLoader.getInstance().displayImage(imagePath.toString(), mProfile_image);
        }
    }

    @Override
    public void getImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            mSelectedImageUri = null;
            mSelectedImageBitmap = bitmap;
            Log.d("TAG", "getImageBitmap: got the image bitmap" + mSelectedImageBitmap);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //referencing the widgets
        mNames = findViewById(R.id.settings_name);
        mPhone = findViewById(R.id.settings_phone);
        mEmail = findViewById(R.id.settings_email);
        mPassword = findViewById(R.id.settings_confirm_password);
        mSave = findViewById(R.id.settings_save_btn);
        mProfile_image = findViewById(R.id.profile_image);
        mProgressBar = findViewById(R.id.progressBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initImageLoader();
        verifyStoragePermissions();
        setupFirebaseAuth();
        init();
        hideSoftKeyboard();
    }

    private void init() {
        //get user account data from Firebase Database and Auth
        getUserAccountData();

        mProfile_image.setOnClickListener((v -> {
            if (mStoragePermissions) {
                ChangePhotoDialog dialog = new ChangePhotoDialog();
                dialog.show(getSupportFragmentManager(), getString(R.string.dialog_change_photo));
            } else {
                verifyStoragePermissions();
            }
        }));

        mSave.setOnClickListener((v -> {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            /*
            ------ Change Name -----
             */
            if (!mNames.getText().toString().equals("")) {
                reference.child(getString(R.string.dbnode_users))
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(getString(R.string.field_name))
                        .setValue(mNames.getText().toString());
            }
            //Change Phone Number
            if (!mPhone.getText().toString().equals("")) {
                reference.child(getString(R.string.dbnode_users))
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(getString(R.string.field_phone))
                        .setValue(mPhone.getText().toString());
            }
            //Upload the New Photo
            if (mSelectedImageUri != null) {
                uploadNewPhoto(mSelectedImageUri);
            } else if (mSelectedImageBitmap != null) {
                uploadNewPhoto(mSelectedImageBitmap);
            }

            Toast.makeText(SettingsActivity.this, "Saved", Toast.LENGTH_SHORT).show();
        }));
    }

    /*
    Section1
    */
    //Uploads a new profile photo to Firebase Storage using a @param **imageUri***
    public void uploadNewPhoto(Uri imageUri) {
        //upload a new profile photo to firebase storage
        Log.d(TAG, "uploadNewPhoto: uploading new profile photo to firebase storage.");

        //Only accept image sizes that are compressed to under 5MB. If that's not possible
        //then do not allow image to be uploaded
        BackgroundImageResize resize = new BackgroundImageResize(null);
        resize.execute(imageUri);
    }

    //Uploads a new profile photo to Firebase Storage using a @param ***imageBitmap***
    public void uploadNewPhoto(Bitmap imageBitmap) {
        //upload a new profile photo to firebase storage
        Log.d(TAG, "uploadNewPhoto: uploading new profile photo to firebase storage.");

        //Only accept image sizes that are compressed to under 5MB. If thats not possible
        //then do not allow image to be uploaded
        BackgroundImageResize resize = new BackgroundImageResize(imageBitmap);
        Uri uri = null;
        resize.execute(uri);
    }

    private void executeUploadTask() {
        //showDialog();
        //FilePaths filePaths = new FilePaths();
        //specify where the photo will be stored
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("images/users" + FirebaseAuth.getInstance().getCurrentUser().getUid()
                        + "/profile_image"); //just replace the old image with the new one

        if (mBytes.length / MB < MB_THRESHHOLD) {

            // Create file metadata including the content type
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpg")
                    .setContentLanguage("en") //see nodes below
                    .setCustomMetadata("profile pic", "****")
                    .build();
            //if the image size is valid then we can submit to database
            UploadTask uploadTask;
            uploadTask = storageReference.putBytes(mBytes, metadata);
            //uploadTask = storageReference.putBytes(mBytes); //without metadata


            uploadTask.addOnSuccessListener(taskSnapshot -> {
                //Now insert the download url into the firebase database

                Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    // Continue with the task to get the download URL
                    return storageReference.getDownloadUrl();
                }).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        //saving file details to the RTDB
                        FirebaseDatabase.getInstance().getReference()
                                .child(getString(R.string.dbnode_users))
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .child(getString(R.string.field_profile_image))
                                .setValue(downloadUri.toString());
                        Log.d(TAG, "onSuccess: firebase download url = " + downloadUri.toString());
                    }
                });
                hideDialog();
            }).addOnFailureListener(exception -> {
                Toast.makeText(SettingsActivity.this, "could not upload photo", Toast.LENGTH_SHORT).show();
                hideDialog();

            }).addOnProgressListener(taskSnapshot -> {
                double currentProgress = (100 * taskSnapshot.getBytesTransferred())
                        / taskSnapshot.getTotalByteCount();
                if (currentProgress > (progress + 15)) {
                    progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "onProgress: Upload is " + progress + "% done");
                    Toast.makeText(SettingsActivity.this,
                            progress + "%", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Image is too Large", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: requestCode: " + requestCode);
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: Users has allowed permission to access: " + permissions[0]);
                }
                break;
        }
    }

    /*
    Section2
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticationState();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        isActivityRunning = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        isActivityRunning = false;
    }

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // Users is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                //toastMessage("Successfully signed in with: " + user.getEmail());


            } else {
                // Users is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out");
                Toast.makeText(SettingsActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
            // ...
        };


    }

    private void checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state.");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.");

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated.");
        }
    }

    /*
    Section3
     */
    private void getUserAccountData() {
        Log.d("TAG", "getUserAccountData: getting the user's account information");

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        /*
            ---------- QUERY Method 1 ----------
         */
        Query query1 = reference.child(getString(R.string.dbnode_users))
                .orderByKey()
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //this loop will return a single result
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Log.d("TAG", "onDataChange: (QUERY METHOD 1) found user: "
                            + singleSnapshot.getValue(User.class).toString());
                    User user = singleSnapshot.getValue(User.class);
                    mNames.setText(user.getName());
                    mPhone.setText(user.getPhone());
                    profile_image_url = user.getProfile_image();

                    ImageLoader.getInstance().displayImage(profile_image_url, mProfile_image);
//                    try {
//                        if (user.getSecurity_level().equals("2")) {
//                        } else {
//                            ImageLoader.getInstance().displayImage(studentProfilePic, mProfile_image);
//                        }
//                    } catch (Exception e) {
//                        ImageLoader.getInstance().displayImage(uriNew.toString(), mProfile_image);
//                    }
                    Log.d("profile", "Users data: " + user.getSecurity_level());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        mEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());

    }

    /**
     * Generalized method for asking permission. Can pass any array of permissions
     */
    public void verifyStoragePermissions() {
        Log.d("TAG", "verifyPermissions: asking user for permissions.");
        String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[2]) == PackageManager.PERMISSION_GRANTED) {
            mStoragePermissions = true;
        } else {
            ActivityCompat.requestPermissions(
                    SettingsActivity.this,
                    permissions,
                    REQUEST_CODE
            );
        }
    }

    private void showDialog() {
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void hideDialog() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * init universal image loader
     */
    private void initImageLoader() {
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(getBaseContext()));
    }

    /**
     * 1) doinBackground takes an imageUri and returns the byte array after compression
     * 2) onPostExecute will print the % compression to the log once finished
     */
    public class BackgroundImageResize extends AsyncTask<Uri, Integer, byte[]> {

        Bitmap mBitmap;

        public BackgroundImageResize(Bitmap bm) {
            if (bm != null) {
                mBitmap = bm;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog();
        }

        @Override
        protected byte[] doInBackground(Uri... params) {
            Log.d(TAG, "doInBackground: started.");

            if (mBitmap == null) {
                InputStream iStream = null;
                try {
                    iStream = SettingsActivity.this.getContentResolver().openInputStream(params[0]);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                int len = 0;
                try {
                    while ((len = iStream.read(buffer)) != -1) {
                        byteBuffer.write(buffer, 0, len);
                    }
                    iStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return byteBuffer.toByteArray();
            } else {
                int size = mBitmap.getRowBytes() * mBitmap.getHeight();
                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                mBitmap.copyPixelsToBuffer(byteBuffer);
                byte[] bytes = byteBuffer.array();
                byteBuffer.rewind();
                return bytes;
            }
        }


        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            hideDialog();
            mBytes = bytes;
            //execute the upload
            executeUploadTask();
        }
    }
}