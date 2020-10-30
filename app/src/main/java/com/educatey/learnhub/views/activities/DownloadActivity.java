package com.educatey.learnhub.views.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.educatey.learnhub.R;
import com.educatey.learnhub.utils.CheckForSDCard;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class DownloadActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private static final int WRITE_REQUEST_CODE = 300;
    private static final String TAG = DownloadActivity.class.getSimpleName();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private String url, fileName;
    private ProgressDialog pDialog;
    private DownloadFile downloadFile;

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        verifyStoragePermissions(this);
        if (getIntent().hasExtra("url")) {
            url = getIntent().getStringExtra("url");
            fileName = getIntent().getStringExtra("file");
            downloadFile = new DownloadFile(fileName);
            Toast.makeText(this, "fileName: " + fileName, Toast.LENGTH_SHORT).show();
            if (CheckForSDCard.isSDCardPresent()) {
                //check if app has permission to write to the external storage.
                if (EasyPermissions.hasPermissions(DownloadActivity.this,
                        PERMISSIONS_STORAGE)) {
                    //Get the URL entered
                    downloadFile.execute(url);
                } else {
                    //If permission is not present request for the same.
                    EasyPermissions.requestPermissions(DownloadActivity.this,
                            getString(R.string.write_file),
                            WRITE_REQUEST_CODE, PERMISSIONS_STORAGE);
                }
            } else {
                Toast.makeText(getApplicationContext(),
                        "SD Card not found", Toast.LENGTH_LONG).show();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults,
                DownloadActivity.this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        //Download the file once permission is granted
        downloadFile.execute(url);

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "Permission has been denied");
    }

    @Override
    public void onBackPressed() {
        if (downloadFile.getStatus() == AsyncTask.Status.RUNNING) {
            downloadFile.cancel(true);
        }
        super.onBackPressed();
    }

    private class DownloadFile extends AsyncTask<String, String, String> {

        private ProgressDialog progressDialog;
        private String fileName;
        private String folder;
        private boolean isDownloaded;

        public DownloadFile(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.progressDialog = new ProgressDialog(DownloadActivity.this);
            this.progressDialog.setMessage("Downloading, please wait...");
            this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//          this.progressDialog.setCancelable(true);
//            this.progressDialog.setCanceledOnTouchOutside(true);
            progressDialog.setCancelable(true);
            this.progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (downloadFile.getStatus() == Status.RUNNING)
                        downloadFile.cancel(true);
                }
            });
            this.progressDialog.show();
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // getting file length
                int lengthOfFile = connection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                //External directory path to save file
                folder = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "LearnHub/";

                //Create LearnHub folder if it does not exist
                File directory = new File(folder);
//directory.
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // Output stream to write file
                OutputStream output = new FileOutputStream(folder + fileName);

                byte[] data = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));
                    Log.d(TAG, "Progress: " + (int) ((total * 100) / lengthOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
                return "Downloaded at: " + folder + fileName;

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return "Something went wrong";
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            progressDialog.setProgress(Integer.parseInt(progress[0]));
        }


        @Override
        protected void onPostExecute(String message) {
            // dismiss the dialog after the file was downloaded
            this.progressDialog.dismiss();

            // Display File path after downloading
            Toast.makeText(getApplicationContext(),
                    message, Toast.LENGTH_LONG).show();
            onBackPressed();
        }
    }

}


