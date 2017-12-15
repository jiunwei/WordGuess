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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Game;
import com.example.jiunwei.wordguess.model.Message;
import com.example.jiunwei.wordguess.model.Player;
import com.example.jiunwei.wordguess.service.BackgroundChatService;
import com.example.jiunwei.wordguess.widget.WordGuessWidget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides user interface for managing players in a room before game starts.
 */
public class CreateActivity extends AppCompatActivity implements PlayersFragment.OnRemovePlayerListener, TextWatcher {

    /** Tag for logging. */
    private static final String TAG = CreateActivity.class.getSimpleName();

    /** Key for room code intent extra. */
    public static final String EXTRA_ROOM_CODE = "room_code";

    /** Key for nickname intent extra. */
    public static final String EXTRA_NICKNAME = "nickname";

    /** Field for editing nickname. */
    private EditText mNicknameEditText;

    /** Spinner for selecting number of rounds to play. */
    private MaterialBetterSpinner mRoundsSpinner;

    /** Fragment to display players in room. */
    private PlayersFragment mPlayersFragment;

    /** Reference to database root. */
    private DatabaseReference mDatabase;

    /** Unique UID of player. */
    private String mUid;

    /** Current room code. */
    private String mRoomCode;

    /** Last known list of players. */
    private List<Player> mPlayers;

    /** Listener for whether room is still open. */
    private ValueEventListener mOpenListener;

    /** Listener for change in players. */
    private ValueEventListener mPlayersListener;

    /** Stores whether there are at least two players and all players are ready. */
    private boolean mPlayersReady;

    /** Determines whether the Start button is enabled. */
    private boolean mStartEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize view bindings.
        TextView roomCodeTextView = findViewById(R.id.room_code);
        mNicknameEditText = findViewById(R.id.nickname);
        mRoundsSpinner = findViewById(R.id.rounds);
        mPlayersFragment = (PlayersFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment);

        // Set text change listener.
        mNicknameEditText.addTextChangedListener(this);

        // Initialize spinner for number of rounds.
        final String[] ROUNDS = new String[] { "1", "2", "3" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ROUNDS);
        mRoundsSpinner.setAdapter(adapter);
        mRoundsSpinner.setText(ROUNDS[0]);

        // Initialize Firebase globals.
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            Toast.makeText(CreateActivity.this, R.string.toast_not_authenticated, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(CreateActivity.this, getString(R.string.toast_room_closed), Toast.LENGTH_SHORT).show();
                } else if (!open) {
                    Map<String, Object> multipleUpdates = new HashMap<>();
                    multipleUpdates.put("/players/" + mUid + "/room_code", mRoomCode);
                    multipleUpdates.put("/players/" + mUid + "/status", Player.STATUS_PLAYING);
                    multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + mUid + "/status", Player.STATUS_PLAYING);
                    mDatabase.updateChildren(multipleUpdates);
                    Intent intent = new Intent(CreateActivity.this, ChatActivity.class);
                    intent.putExtra(ChatActivity.EXTRA_ROOM_CODE, mRoomCode);
                    TaskStackBuilder.create(CreateActivity.this).addNextIntent(intent).startActivities();
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
                mPlayers = new ArrayList<>();
                mPlayersReady = true;
                int index = 0;
                for (DataSnapshot playerSnapshot : dataSnapshot.getChildren()) {
                    Player player = playerSnapshot.getValue(Player.class);
                    if (player == null) {
                        Log.e(TAG, "Unexpected null Player");
                        continue;
                    }
                    mPlayers.add(player);

                    CharSequence name;
                    String status;
                    boolean removable;
                    if (index == 0) {
                        if (!player.uid.equals(mUid)) {
                            finish();
                            Toast.makeText(CreateActivity.this, getString(R.string.toast_room_expired), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        name = Html.fromHtml("<b>" + Html.escapeHtml(getString(R.string.you_format,
                                player.nickname)) + "</b>");
                        status = "";
                        removable = false;
                    } else {
                        name = player.nickname;
                        status = (player.status == Player.STATUS_READY) ? getString(R.string.ready) : getString(R.string.not_ready);
                        removable = true;
                    }
                    mPlayersReady = mPlayersReady && (player.status == Player.STATUS_READY);

                    items[index] = new PlayersFragmentItem(player.uid, index, -1, name, status, -1, removable);
                    index += 1;
                }
                mPlayersFragment.changePlayers(items);
                mPlayersReady = mPlayersReady && items.length > 1;
                computeStartEnabled();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_start).setEnabled(mStartEnabled);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                destroyRoom();
                break;
            case R.id.action_start:
                mDatabase.child("/chats/" + mRoomCode).setValue(null);
                String key = mDatabase.child("chats/" + mRoomCode).push().getKey();
                Map<String, Object> multipleUpdates = new HashMap<>();
                PlayersFragmentItem[] players = mPlayersFragment.getPlayers();
                for (int i = 0; i < players.length; ++i) {
                    PlayersFragmentItem player = players[i];
                    multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + player.mId + "/color", i);
                    multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + player.mId + "/score", 0);
                }
                int rounds = Integer.valueOf(mRoundsSpinner.getText().toString());
                Message message = new Message(mUid, -1, null, null, getString(R.string.message_round, 1, rounds));
                multipleUpdates.put("/chats/" + mRoomCode + "/" + key, message);
                multipleUpdates.put("/rooms/" + mRoomCode + "/open", false);
                Map<String, Boolean> candidates = new HashMap<>();
                for (Player player : mPlayers) {
                    candidates.put(player.uid, true);
                }
                Game game = new Game(false, null, null, 1, rounds, null, candidates, null, true);
                multipleUpdates.put("/games/" + mRoomCode, game);
                mDatabase.updateChildren(multipleUpdates);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        destroyRoom();
        super.onBackPressed();
    }

    @Override
    public void onRemovePlayer(int index, String id) {
        Map<String, Object> multipleUpdates = new HashMap<>();
        multipleUpdates.put("/players/" + id + "/status", Player.STATUS_IDLE);
        multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + id, null);
        mDatabase.updateChildren(multipleUpdates);
    }

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
        }
        computeStartEnabled();
    }

    /**
     * Computes whether the Start action should be enabled and stores it in mStartEnabled.
     */
    private void computeStartEnabled() {
        mStartEnabled = mPlayersReady && mNicknameEditText.getText().toString().trim().length() > 0;
        invalidateOptionsMenu();
    }

    /**
     * Destroys current room.
     */
    private void destroyRoom() {
        mDatabase.child("rooms/" + mRoomCode).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                // Make sure room has self in it before destroying it.
                Player player = mutableData.child("players/" + mUid).getValue(Player.class);
                if (player != null) {
                    mutableData.setValue(null);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
                if (b) {
                    mDatabase.child("players/" + mUid + "/status").setValue(Player.STATUS_IDLE);
                    updateWidgetLayout();
                }
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

}
