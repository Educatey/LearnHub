package com.educatey.learnhub.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.educatey.learnhub.R;
import com.educatey.learnhub.views.dialogs.ResendVerificationDialog;
import com.educatey.learnhub.views.dialogs.ResetPasswordDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static android.text.TextUtils.isEmpty;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth.AuthStateListener mAuthListener;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        firebaseAuthSetUp();

        //widgets
        TextView mSignUp = findViewById(R.id.sign_up_txt);
        TextView mForgotPassword = findViewById(R.id.forgot_password_txt);
        Button mSignIn = findViewById(R.id.sign_in_btn);
        TextView mResendEmail = findViewById(R.id.resend_v_txt);
        final EditText mEmailSignIn = findViewById(R.id.sign_in_email);
        final EditText mPasswordSignIn = findViewById(R.id.sign_in_password);
        mProgressBar = findViewById(R.id.LoginProgressBar);


        mSignUp.setOnClickListener((v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        }));

        //sign in process
        mSignIn.setOnClickListener((v -> {
            if (!isEmpty(mEmailSignIn.getText().toString()) && !isEmpty(mPasswordSignIn.getText().toString())) {
                showProgressBar();
                hideSoftKeyboard();
                FirebaseAuth.getInstance().signInWithEmailAndPassword(mEmailSignIn.getText().toString(),
                        mPasswordSignIn.getText().toString())
                        .addOnCompleteListener(task -> hideProgressBar())
                        .addOnFailureListener(e -> {
                            Toast.makeText(LoginActivity.this,
                                    " Authentication Failed!!!", Toast.LENGTH_LONG).show();
                            hideProgressBar();
                        });
            } else {
                Toast.makeText(LoginActivity.this, "Please, Fill All Fields", Toast.LENGTH_LONG).show();
            }
        }));

        mResendEmail.setOnClickListener(v -> {
            ResendVerificationDialog dialog = new ResendVerificationDialog();
            dialog.show(getSupportFragmentManager(), "resend_verification_email");
        });

        mForgotPassword.setOnClickListener(v -> {
            ResetPasswordDialog dialog = new ResetPasswordDialog();
            dialog.show(getSupportFragmentManager(), "reset_password_dialog");
        });
    }

    //Setting up Firebase
    private void firebaseAuthSetUp() {
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            Log.d("TAG", "CurrentUser: " + user);
            if (user != null) {
                if (user.isEmailVerified()) {
                    /*TODO find a way to prevent showing the login screen if the user has already logged in successfully earlier
                       -- SharedPref?????
                     */
                    Log.d("TAG", "onAuthStateChanged: signed in: " + user.getEmail());
                    Toast.makeText(LoginActivity.this, "Signed in as " + user.getEmail(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, "Please Check Your Email Inbox for Verification Link ", Toast.LENGTH_LONG).show();
                    //Quwaysim watch out for this commented code (Firebase SignOut)!!
                    //FirebaseAuth.getInstance().signOut();
                }
            } else {
                //Toast.makeText(LoginActivity.this, "Signed Out", Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }

    public void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}