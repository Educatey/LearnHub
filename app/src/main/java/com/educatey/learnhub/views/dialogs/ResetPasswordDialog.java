package com.educatey.learnhub.views.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.educatey.learnhub.R;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordDialog extends DialogFragment {

    private static final String TAG = "PasswordResetDialog";

    //widgets
    private EditText mEmail;

    //vars
    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_reset_password , container, false);
        mEmail = view.findViewById(R.id.reset_pass_email);
        mContext = getActivity();

        TextView confirmDialog = view.findViewById(R.id.confirm);
        confirmDialog.setOnClickListener(v -> {
            if (!isEmpty(mEmail.getText().toString())) {
                Log.d(TAG, "onClick: attempting to send reset link to: " + mEmail.getText().toString());
                sendPasswordResetEmail(mEmail.getText().toString());
                getDialog().dismiss();
            }

        });

        TextView cancelDialog = view.findViewById(R.id.cancel);
        cancelDialog.setOnClickListener(v -> getDialog().dismiss());

        return view;
    }

    /**
     * Send a password reset link to the email provided
     */
    public void sendPasswordResetEmail(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: Password Reset Email sent.");
                        Toast.makeText(mContext, "Password Reset Link Sent to Email",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "onComplete: No user associated with that email.");
                        Toast.makeText(mContext, "No Users is Associated with that Email",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Return true if the @param is null
     */
    private boolean isEmpty(String string) {
        return string.equals("");
    }
}