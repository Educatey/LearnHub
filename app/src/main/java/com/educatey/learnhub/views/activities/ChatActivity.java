package com.educatey.learnhub.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.educatey.learnhub.R;
import com.educatey.learnhub.adapters.ChatroomListAdapter;
import com.educatey.learnhub.data.ChatMessage;
import com.educatey.learnhub.data.Chatroom;
import com.educatey.learnhub.data.User;
import com.educatey.learnhub.views.dialogs.DeleteChatroomDialog;
import com.educatey.learnhub.views.dialogs.NewChatroomDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    public static boolean isActivityRunning;
    String userId, UserName;
    private ListView mListView;
    private FloatingActionButton mFob;
    private String TAG = "ChatActivity";
    private String mSecL;
    private ArrayList<Chatroom> mChatrooms;
    private ChatroomListAdapter mAdapter;
    private int mSecurityLevel;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mChatroomReference, mDatabase;
    private HashMap<String, String> mNumChatroomMessages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mListView = findViewById(R.id.listView);
        mFob = findViewById(R.id.fob);
        init();
        getUserDetails();
        setupFirebaseAuth();

    }

    public void init() {
        mChatrooms = new ArrayList<>();
        getUserSecurityLevel();
        getChatrooms();
        mFob.setOnClickListener(view -> {
            NewChatroomDialog dialog = new NewChatroomDialog();
            dialog.show(getSupportFragmentManager(), getString(R.string.dialog_new_chatroom));
        });

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
                mSecurityLevel = securityLevel;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void showDeleteChatroomDialog(String chatroomId) {
        DeleteChatroomDialog dialog = new DeleteChatroomDialog();
        Bundle args = new Bundle();
        args.putString(getString(R.string.field_chatroom_id), chatroomId);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), getString(R.string.dialog_delete_chatroom));
    }

    /**
     * Join a chatroom selected by the user.
     * This method is executed from the ChatroomListAdapter class
     * This method checks to make sure the chatroom exists before joining.
     *
     * @param chatroom
     */
    public void joinChatroom(final Chatroom chatroom) {
        //make sure the chatroom exists before joining
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbnode_chatrooms)).orderByKey();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();
                    if (objectMap.get(getString(R.string.field_chatroom_id)).toString()
                            .equals(chatroom.getChatroom_id())) {
                        if (mSecurityLevel >= Integer.parseInt(chatroom.getSecurity_level())) {
                            Log.d(TAG, "onItemClick: selected chatroom: " + chatroom.getChatroom_id());

                            //add user to the list of users who have joined the chatroom
                            addUserToChatroom(chatroom);

                            //navigate to the chatoom
                            Intent intent = new Intent(ChatActivity.this, ChatroomActivity.class);
                            intent.putExtra(getString(R.string.intent_chatroom), chatroom);
                            startActivity(intent);
                        } else {
                            Toast.makeText(ChatActivity.this, "insufficient security level", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }
                }
                getChatrooms();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void addUserToChatroom(Chatroom chatroom) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        reference.child(getString(R.string.dbnode_chatrooms))
                .child(chatroom.getChatroom_id())
                .child(getString(R.string.field_users))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getString(R.string.field_last_message_seen))
                .setValue(mNumChatroomMessages.get(chatroom.getChatroom_id()));

    }

    public void getChatrooms() {
        Log.d(TAG, "getChatrooms: retrieving chatrooms from firebase database.");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        mNumChatroomMessages = new HashMap<>();
        if (mAdapter != null) {
            mAdapter.clear();
            mChatrooms.clear();
        }
        Query query = reference.child(getString(R.string.dbnode_chatrooms)).orderByKey();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
//                    Log.d(TAG, "onDataChange: found chatroom: "
//                            + singleSnapshot.getValue());
                    try {
                        if (singleSnapshot.exists()) {
                            Chatroom chatroom = new Chatroom();
                            Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                            Log.d(TAG, "onDataChange: found a chatroom: "
                                    + objectMap.get(getString(R.string.field_chatroom_name)).toString());
                            chatroom.setChatroom_id(objectMap.get(getString(R.string.field_chatroom_id)).toString());
                            chatroom.setChatroom_name(objectMap.get(getString(R.string.field_chatroom_name)).toString());
                            chatroom.setCreator_id(objectMap.get(getString(R.string.field_creator_id)).toString());
                            chatroom.setSecurity_level(objectMap.get(getString(R.string.field_security_level)).toString());


                            // chatroom.setChatroom_id(singleSnapshot.getValue(Chatroom.class).getChatroom_id());
                            // chatroom.setSecurity_level(singleSnapshot.getValue(Chatroom.class).getSecurity_level());
                            // chatroom.setCreator_id(singleSnapshot.getValue(Chatroom.class).getCreator_id());
                            // chatroom.setChatroom_name(singleSnapshot.getValue(Chatroom.class).getChatroom_name());

                            //get the chatrooms messages
                            ArrayList<ChatMessage> messagesList = new ArrayList<ChatMessage>();
                            int numMessages = 0;
                            for (DataSnapshot snapshot : singleSnapshot
                                    .child(getString(R.string.field_chatroom_messages)).getChildren()) {
                                ChatMessage message = new ChatMessage();
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setUser_id(snapshot.getValue(ChatMessage.class).getUser_id());
                                message.setMessage(snapshot.getValue(ChatMessage.class).getMessage());
                                messagesList.add(message);
                                numMessages++;
                            }
                            if (messagesList.size() > 0) {
                                chatroom.setChatroom_messages(messagesList);

                                //add the number of chatrooms messages to a hashmap for reference
                                mNumChatroomMessages.put(chatroom.getChatroom_id(), String.valueOf(numMessages));
                            }

                            //get the list of users who have joined the chatroom
                            List<String> users = new ArrayList<String>();
                            for (DataSnapshot snapshot : singleSnapshot
                                    .child(getString(R.string.field_users)).getChildren()) {
                                String user_id = snapshot.getKey();
                                Log.d(TAG, "onDataChange: user currently in chatroom: " + user_id);
                                users.add(user_id);
                            }
                            if (users.size() > 0) {
                                chatroom.setUsers(users);
                            }

                            mChatrooms.add(chatroom);
                        }

                        setupChatroomList();
                    } catch (NullPointerException e) {
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupChatroomList() {
        Log.d(TAG, "setupChatroomList: setting up chatroom listview");
        mAdapter = new ChatroomListAdapter(ChatActivity.this, R.layout.layout_chatroom_listitem, mChatrooms);
        mListView.setAdapter(mAdapter);
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
                    //mNewSecurityLevel = Integer.parseInt(details.getSecurity_level());
                    //Toast.makeText(ChatActivity.this, "SL: " + mSecL, Toast.LENGTH_SHORT).show();

                    // phone = details.getPhone();
                    // email = details.getEmail();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } catch (Exception e) {
            // Toast.makeText(ChatActivity.this, "Error occured" + e.toString(), Toast.LENGTH_LONG).show();
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
                Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        };
    }

}