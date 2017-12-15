package com.example.jiunwei.wordguess.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Player;
import com.example.jiunwei.wordguess.model.Room;
import com.example.jiunwei.wordguess.service.BackgroundChatService;
import com.example.jiunwei.wordguess.util.RoomUtil;
import com.example.jiunwei.wordguess.widget.WordGuessWidget;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 *  Provides user interface to create and join rooms.
 */
public class HomeActivity extends AppCompatActivity implements RoomCodeDialogFragment.OnJoinListener {

    /** Tag for logging. */
    private static final String TAG = HomeActivity.class.getSimpleName();

    /** Action string to create room immediately. */
    public static final String ACTION_CREATE = "create";

    /** Action string to join room immediately. */
    public static final String ACTION_JOIN = "join";

    /** Linear layout with loading screen. */
    private LinearLayout mProgress;

    /** Linear layout to retry authentication. */
    private LinearLayout mRetry;

    /** Linear layout for main content. */
    private LinearLayout mContent;

    /** Singleton instance of {@link FirebaseAuth}. */
    private FirebaseAuth mAuth;

    /** Reference to database root. */
    private DatabaseReference mDatabase;

    /** Unique UID of player. */
    private String mUid;

    /** The offset between local time and server time. */
    private long mServerOffset;

    /** Temporary storage place for the room code. */
    private String mRoomCode;

    /** Temporary storage place for the player's nickname. */
    private String mNickname;

    /** Temporary place to store the most recent error message. */
    private String mError;

    /** Number of active tasks preventing UI interaction. */
    private int mActiveTasks = 0;

    /**
     * Starts an active task that blocks UI interaction.
     */
    private void startActiveTask() {
        if (mActiveTasks == 0) {
            mProgress.setVisibility(View.VISIBLE);
            mContent.setVisibility(View.GONE);
        }
        mActiveTasks += 1;
    }

    /**
     * Stops an active task that blocks UI interaction.
     */
    private void stopActiveTask() {
        mActiveTasks -= 1;
        if (mActiveTasks == 0) {
            mProgress.setVisibility(View.GONE);
            if (mRetry.getVisibility() != View.VISIBLE) {
                mContent.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize view bindings.
        mProgress = findViewById(R.id.progress);
        mRetry = findViewById(R.id.retry);
        mContent = findViewById(R.id.content);
        Button retryButton = findViewById(R.id.retry_button);
        Button howToPlayButton = findViewById(R.id.how_to_play);
        Button createButton = findViewById(R.id.create);
        Button joinButton = findViewById(R.id.join);

        // Set OnClickListeners.
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retry();
            }
        });
        howToPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                howtoPlay();
            }
        });
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRoom();
            }
        });
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinRoom();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Remove background service if running.
        if (BackgroundChatService.sIntent != null) {
            stopService(BackgroundChatService.sIntent);
            BackgroundChatService.sIntent = null;
        }

        // Initialize Firebase globals.
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (mAuth.getCurrentUser() == null) {
            authenticate();
        } else {
            mUid = mAuth.getCurrentUser().getUid();
            getServerOffset();
            getPlayer();
        }

        updateWidgetLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (availability != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, availability, -1).show();
        }
    }

    /**
     * Starts anonymous authentication process.
     */
    private void authenticate() {
        startActiveTask();
        mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (mAuth.getCurrentUser() == null) {
                    mContent.setVisibility(View.GONE);
                    mRetry.setVisibility(View.VISIBLE);
                } else {
                    mUid = mAuth.getCurrentUser().getUid();
                    getServerOffset();
                    getPlayer();
                }
                stopActiveTask();
            }
        });
    }

    /**
     * Gets server offset and stores it in mServerOffset.
     */
    private void getServerOffset() {
        startActiveTask();
        mDatabase.child(".info/serverTimeOffset").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Long offset = snapshot.getValue(Long.class);
                if (offset != null) {
                    mServerOffset = offset;
                }
                stopActiveTask();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
                stopActiveTask();
            }
        });
    }

    /**
     * Gets details of the current player.
     */
    private void getPlayer() {
        startActiveTask();
        final DatabaseReference userReference = mDatabase.child("players/" + mUid);
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Player player = dataSnapshot.getValue(Player.class);

                // Teleport player if needed.
                if (player != null && player.status == Player.STATUS_PLAYING && player.room_code != null) {
                    Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
                    intent.putExtra(CreateActivity.EXTRA_ROOM_CODE, player.room_code);
                    intent.putExtra(CreateActivity.EXTRA_NICKNAME, player.nickname);
                    TaskStackBuilder.create(HomeActivity.this).addNextIntent(intent).startActivities();
                    return;
                }

                // Otherwise, just initialize nickname.
                if (player == null) {
                    mNickname = getString(R.string.player_prefix, mUid.substring(mUid.length() - 4).toUpperCase());
                    player = new Player(mUid, mNickname, Player.STATUS_IDLE);
                    userReference.setValue(player);
                } else {
                    mNickname = player.nickname;
                }
                stopActiveTask();

                // Check if activity was started by the widget.
                String action = getIntent().getAction();
                if (action != null) {
                    if (action.equals(ACTION_CREATE)) {
                        createRoom();
                    } else if (action.equals(ACTION_JOIN)) {
                        joinRoom();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
                stopActiveTask();
            }
        });
    }

    /**
     * Hides retry interface and retries authentication.
     */
    public void retry() {
        mRetry.setVisibility(View.GONE);
        authenticate();
    }

    /**
     * Starts process of creating a new room and launching CreateActivity.
     */
    public void createRoom() {
        startActiveTask();
        final DatabaseReference roomCodeReference = mDatabase.child("rooms");
        roomCodeReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                int length = RoomUtil.STARTING_CODE_LENGTH;
                Room room;
                while (true) {
                    mRoomCode = RoomUtil.generateRoomCode(length);
                    room = mutableData.child(mRoomCode).getValue(Room.class);
                    if (room == null || System.currentTimeMillis() + mServerOffset - room.timestamp > RoomUtil.MAX_AGE) break;
                    length += 1;
                }
                Map<String, Object> roomMap = new HashMap<>();
                roomMap.put("room_code", mRoomCode);
                roomMap.put("open", true);
                roomMap.put("timestamp", ServerValue.TIMESTAMP);
                Map<String, Player> players = new HashMap<>();
                players.put(mUid, new Player(mUid, mNickname, Player.STATUS_READY));
                roomMap.put("players", players);
                mutableData.child(mRoomCode).setValue(roomMap);
                mutableData.child(mRoomCode + "/players/" + mUid + "/timestamp").setValue(ServerValue.TIMESTAMP);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
                if (b) {
                    // Replicate player status.
                    mDatabase.child("players/" + mUid + "/status").setValue(Player.STATUS_READY);
                    // Start activity.
                    Intent intent = new Intent(HomeActivity.this, CreateActivity.class);
                    intent.putExtra(CreateActivity.EXTRA_ROOM_CODE, mRoomCode);
                    intent.putExtra(CreateActivity.EXTRA_NICKNAME, mNickname);
                    startActivity(intent);
                }
                stopActiveTask();
            }
        });
    }

    /**
     * Opens dialog for room code.
     */
    public void joinRoom() {
        AppCompatDialogFragment fragment = new RoomCodeDialogFragment();
        fragment.show(getSupportFragmentManager(), "dialog_room_code");
    }

    @Override
    public void onJoin(String roomCode) {
        startActiveTask();
        mRoomCode = roomCode;
        mDatabase.child("rooms/" + mRoomCode).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Room room = mutableData.getValue(Room.class);
                if (room == null) {
                    mError = getString(R.string.toast_room_null);
                } else if (System.currentTimeMillis() + mServerOffset - room.timestamp > RoomUtil.MAX_AGE) {
                    mError = getString(R.string.toast_room_expired);
                } else if (!room.open) {
                    mError = getString(R.string.toast_room_closed);
                } else if (room.players.size() == RoomUtil.MAX_PLAYERS) {
                    mError = getString(R.string.toast_room_full);
                } else {
                    mError = null;
                    mutableData.child("players/" + mUid).setValue(new Player(mUid, mNickname, Player.STATUS_NOT_READY));
                    mutableData.child("players/" + mUid + "/timestamp").setValue(ServerValue.TIMESTAMP);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
                if (b) {
                    // Replicate player status in room.
                    mDatabase.child("players/" + mUid + "/status").setValue(Player.STATUS_NOT_READY);
                    // Start activity.
                    if (mError == null) {
                        Intent intent = new Intent(HomeActivity.this, JoinActivity.class);
                        intent.putExtra(JoinActivity.EXTRA_ROOM_CODE, mRoomCode);
                        intent.putExtra(JoinActivity.EXTRA_NICKNAME, mNickname);
                        startActivity(intent);
                    } else {
                        Toast.makeText(HomeActivity.this, mError, Toast.LENGTH_SHORT).show();
                        mError = null;
                        AppCompatDialogFragment fragment = new RoomCodeDialogFragment();
                        fragment.show(getSupportFragmentManager(), "dialog_room_code");
                    }
                }
                stopActiveTask();
            }
        });
    }

    /**
     * Updates any widgets to display the correct layout.
     */
    private void updateWidgetLayout() {
        Intent intent = new Intent(this, WordGuessWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), WordGuessWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    /**
     * Launches "How to Play" video on YouTube.
     */
    public void howtoPlay() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=II8vPi3acPs"));
        startActivity(intent);
    }

}
