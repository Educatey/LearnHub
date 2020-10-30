package com.educatey.learnhub.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.educatey.learnhub.R;
import com.educatey.learnhub.data.Common;
import com.educatey.learnhub.data.User;
import com.educatey.learnhub.data.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


public class HomeActivity extends AppCompatActivity {
    Button mClassroom, mQuiz, mChat, mSettings, mSignOut;
    String userId, UserName;
    private String mSecL;
    private final String TAG = "HomeActivity";
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase, reference, users;
    //DatabaseReference users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mClassroom = findViewById(R.id.classroomBtn);
        mQuiz = findViewById(R.id.quizBtn);
        mChat = findViewById(R.id.chatBtn);

        getUserSecurityLevel();
        getUserDetails();
        setupFirebaseAuth();
        users = FirebaseDatabase.getInstance().getReference("users");
        User user = new User();
        user.setSecurity_level(mSecL);

        mClassroom.setOnClickListener((v -> {
            Intent intent = new Intent(HomeActivity.this, ClassroomActivity.class);
            startActivity(intent);
        }));

        mQuiz.setOnClickListener((v -> users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Users user1 = dataSnapshot.child(userId).getValue(Users.class);
                Intent intent = new Intent(HomeActivity.this, QuizActivity.class);
                //TODO
                Common.currentUsers = user1;
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        })));

        mChat.setOnClickListener((v -> {
            Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
            startActivity(intent);
        }));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.sign_out) {
            FirebaseAuth.getInstance().signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void checkAuthenticationState() {
        Log.d("TAG", "Check Auth State: checking auth state.");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d("TAG", "checkAuthState: user is null, redirecting to Login Activity");
            Intent intent = new Intent( HomeActivity.this, LoginActivity.class);
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
                mDatabase = FirebaseDatabase.getInstance().getReference();
                getUserDetails();
                Log.d("TAG", "onAuthStateChanged:signed_in:" + user.getUid());

            } else {
                Log.d("TAG", "onAuthStateChanged:signed_out");
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        };
    }

    private void getUserDetails() {
        try {
            mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User details = dataSnapshot.getValue(User.class);
                    UserName = details.getName();
                    //get user securityLevel here
                    mSecL = details.getSecurity_level();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
        } catch (Exception e) {
            //Toast.makeText(HomeActivity.this, "Error occured" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void getUserSecurityLevel() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbnode_users))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                int securityLevel = Integer.parseInt(singleSnapshot.getValue(User.class).getSecurity_level());
                Log.d(TAG, "onDataChange: user has a security level of: " + securityLevel);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}