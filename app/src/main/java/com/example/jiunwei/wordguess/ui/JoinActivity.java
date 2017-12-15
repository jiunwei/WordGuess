package com.example.jiunwei.wordguess.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Player;
import com.example.jiunwei.wordguess.service.BackgroundChatService;
import com.example.jiunwei.wordguess.widget.WordGuessWidget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides user interface for waiting in a room before game starts.
 */
public class JoinActivity extends AppCompatActivity implements PlayersFragment.OnRemovePlayerListener, TextWatcher {

    /** Tag for logging. */
    private static final String TAG = JoinActivity.class.getSimpleName();

    /** Key for room code intent extra. */
    public static final String EXTRA_ROOM_CODE = "room_code";

    /** Key for nickname intent extra. */
    public static final String EXTRA_NICKNAME = "nickname";

    /** Field for editing nickname. */
    private EditText mNicknameEditText;

    /** Button for player to indicate he/she is ready. */
    private Button mReadyButton;

    /** Fragment to display players in room. */
    private PlayersFragment mPlayersFragment;

    /** Reference to database root. */
    private DatabaseReference mDatabase;

    /** Unique UID of player. */
    private String mUid;

    /** Current room code. */
    private String mRoomCode;

    /** Listener for whether room is still open. */
    private ValueEventListener mOpenListener;

    /** Listener for change in players. */
    private ValueEventListener mPlayersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize view bindings.
        TextView roomCodeTextView = findViewById(R.id.room_code);
        mNicknameEditText = findViewById(R.id.nickname);
        mReadyButton = findViewById(R.id.ready);
        mPlayersFragment = (PlayersFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment);

        // Set OnClickListeners.
        mReadyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ready();
            }
        });

        // Set text change listener.
        mNicknameEditText.addTextChangedListener(this);

        // Initialize Firebase globals.
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            Toast.makeText(JoinActivity.this, getString(R.string.toast_not_authenticated), Toast.LENGTH_SHORT).show();
            return;
        }
        mUid = user.getUid();

        // Initialize view contents if needed.
        mRoomCode = getIntent().getStringExtra(EXTRA_ROOM_CODE);
        roomCodeTextView.setText(mRoomCode);
        if (savedInstanceState == null) {
            mNicknameEditText.setText(getIntent().getStringExtra(EXTRA_NICKNAME));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Remove background service if running.
        if (BackgroundChatService.sIntent != null) {
            stopService(BackgroundChatService.sIntent);
            BackgroundChatService.sIntent = null;
        }

        // Detect if game starts or if room is ever deleted.
        mOpenListener = mDatabase.child("rooms/" + mRoomCode + "/open").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean open = dataSnapshot.getValue(Boolean.class);
                if (open == null) {
                    finish();
                    Toast.makeText(JoinActivity.this, getString(R.string.toast_room_closed), Toast.LENGTH_SHORT).show();
                } else if (!open) {
                    Map<String, Object> multipleUpdates = new HashMap<>();
                    multipleUpdates.put("/players/" + mUid + "/room_code", mRoomCode);
                    multipleUpdates.put("/players/" + mUid + "/status", Player.STATUS_PLAYING);
                    multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + mUid + "/status", Player.STATUS_PLAYING);
                    mDatabase.updateChildren(multipleUpdates);
                    Intent intent = new Intent(JoinActivity.this, ChatActivity.class);
                    intent.putExtra(ChatActivity.EXTRA_ROOM_CODE, mRoomCode);
                    TaskStackBuilder.create(JoinActivity.this).addNextIntent(intent).startActivities();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
            }
        });

        // Keep track of players and their states.
        mPlayersListener = mDatabase.child("rooms/" + mRoomCode + "/players").orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PlayersFragmentItem[] items = new PlayersFragmentItem[(int)dataSnapshot.getChildrenCount()];
                boolean uidFound = false;
                int index = 0;
                for (DataSnapshot playerSnapshot : dataSnapshot.getChildren()) {
                    Player player = playerSnapshot.getValue(Player.class);
                    if (player == null) {
                        Log.e(TAG, "Unexpected null Player");
                        continue;
                    }

                    CharSequence name;
                    String status = (player.status == Player.STATUS_READY) ? getString(R.string.ready) : getString(R.string.not_ready);
                    if (player.uid.equals(mUid)) {
                        name = Html.fromHtml("<b>" + Html.escapeHtml(getString(R.string.you_format,
                                player.nickname)) + "</b>");
                        uidFound = true;
                    } else {
                        name = player.nickname;
                    }

                    items[index] = new PlayersFragmentItem(player.uid, index, -1, name, status, -1, false);
                    index += 1;
                }
                if (!uidFound) {
                    finish();
                    Toast.makeText(JoinActivity.this, R.string.toast_removed, Toast.LENGTH_SHORT).show();
                    return;
                }
                mPlayersFragment.changePlayers(items);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
            }
        });

        updateWidgetLayout();
    }

    @Override
    protected void onStop() {
        if (mOpenListener != null) {
            mDatabase.child("rooms/" + mRoomCode + "/open").removeEventListener(mOpenListener);
            mOpenListener = null;
        }
        if (mPlayersListener != null) {
            mDatabase.child("rooms/" + mRoomCode + "/players").removeEventListener(mPlayersListener);
            mPlayersListener = null;
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            leaveRoom();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        leaveRoom();
        super.onBackPressed();
    }

    @Override
    public void onRemovePlayer(int index, String id) { }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.toString().trim().length() > 0) {
            Map<String, Object> multipleUpdates = new HashMap<>();
            multipleUpdates.put("/players/" + mUid + "/nickname", s.toString());
            multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + mUid + "/nickname", s.toString());
            mDatabase.updateChildren(multipleUpdates);
            mReadyButton.setEnabled(true);
        } else {
            mReadyButton.setEnabled(false);
        }
    }

    /**
     * Post to database that player is ready.
     */
    public void ready() {
        mReadyButton.setText(R.string.waiting);
        mReadyButton.setEnabled(false);
        mNicknameEditText.setEnabled(false);
        Map<String, Object> multipleUpdates = new HashMap<>();
        multipleUpdates.put("/players/" + mUid + "/status", Player.STATUS_READY);
        multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + mUid + "/status", Player.STATUS_READY);
        mDatabase.updateChildren(multipleUpdates);
    }

    /**
     * Leaves the current room.
     */
    private void leaveRoom() {
        Map<String, Object> multipleUpdates = new HashMap<>();
        multipleUpdates.put("/players/" + mUid + "/status", Player.STATUS_IDLE);
        multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + mUid, null);
        mDatabase.updateChildren(multipleUpdates);
        updateWidgetLayout();
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

}
