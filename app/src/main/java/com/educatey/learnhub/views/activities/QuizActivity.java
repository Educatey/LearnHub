package com.educatey.learnhub.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.educatey.learnhub.R;
import com.educatey.learnhub.data.User;
import com.educatey.learnhub.views.fragments.CategoryFragment;
import com.educatey.learnhub.views.fragments.RankingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class QuizActivity extends AppCompatActivity {

    FloatingActionButton newQuiz;
    BottomNavigationView bottomNavigationView;
    String userId, UserName;
    private ListView mListView;
    private String TAG = "QuizActivity";
    /*
    New
     */
    private String mSecL;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase, reference, users;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        newQuiz = findViewById(R.id.new_quiz_fab);
        mListView = findViewById(R.id.listView);

        initImageLoader();
        getUserSecurityLevel();
        getUserDetails();
        setupFirebaseAuth();
        users = FirebaseDatabase.getInstance().getReference("users");
        User user = new User();
        user.setSecurity_level(mSecL);

        newQuiz.setOnClickListener((v -> {
            Intent intent = new Intent(QuizActivity.this, AddQuizActivity.class);
            startActivity(intent);
        }));


        bottomNavigationView = findViewById(R.id.navigation);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, CategoryFragment.newInstance());
        fragmentTransaction.commit();
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectFragment = null;
                switch (item.getItemId()) {
                    case R.id.category:
                        selectFragment = CategoryFragment.newInstance();
                        break;
                    case R.id.ranking:
                        selectFragment = RankingFragment.rankingFragment();
                        break;
                }
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frame_layout, selectFragment);
                fragmentTransaction.commit();
                return true;

            }
        });
    }


    /*
    New
     */
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
            Intent intent = new Intent(QuizActivity.this, LoginActivity.class);
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
                Intent intent = new Intent(QuizActivity.this, LoginActivity.class);
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
                    //TODO
                    //get user securityLevel here
                    mSecL = details.getSecurity_level();
                    if (!mSecL.equals("2")) {
                        newQuiz.hide();
                    }
                    //mNewSecurityLevel = Integer.parseInt(details.getSecurity_level());
                    //Toast.makeText(QuizActivity.this, "SL Quiz: " + mSecL, Toast.LENGTH_SHORT).show();
                    // phone = details.getPhone();
                    // email = details.getEmail();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        } catch (Exception e) {
            //Toast.makeText(HomeActivity.this, "Error occured" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * init universal image loader
     */
    private void initImageLoader() {
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(getBaseContext()));
    }


    private void getUserSecurityLevel() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbnode_users))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: datasnapshot: " + dataSnapshot);
                DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                int securityLevel = Integer.parseInt(singleSnapshot.getValue(User.class).getSecurity_level());
                Log.d(TAG, "onDataChange: user has a security level of: " + securityLevel);
                //mSecurityLevel = securityLevel;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}