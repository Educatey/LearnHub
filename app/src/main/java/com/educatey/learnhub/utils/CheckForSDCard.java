package com.educatey.learnhub.utils;

import android.os.Environment;

public class CheckForSDCard {
    //Method to Check If SD Card is mounted or not
    public static boolean isSDCardPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}