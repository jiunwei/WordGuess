package com.example.jiunwei.wordguess.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Game;
import com.example.jiunwei.wordguess.model.Message;
import com.example.jiunwei.wordguess.model.Player;
import com.example.jiunwei.wordguess.model.Room;
import com.example.jiunwei.wordguess.service.BackgroundChatService;
import com.example.jiunwei.wordguess.util.WordUtil;
import com.example.jiunwei.wordguess.widget.WordGuessWidget;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.KeyEvent.KEYCODE_ENTER;

/**
 * Provides a chat user interface for game to take place.
 */
public class ChatActivity extends AppCompatActivity implements PlayersFragment.OnRemovePlayerListener, TextWatcher {

    /** Tag for logging. */
    private static final String TAG = ChatActivity.class.getSimpleName();

    /** Key for room code intent extra. */
    public static final String EXTRA_ROOM_CODE = "room_code";

    /** Background drawables for each color. */
    private static final int[] CHATS = {
            R.drawable.chat_red,
            R.drawable.chat_blue,
            R.drawable.chat_green,
            R.drawable.chat_orange,
            R.drawable.chat_brown,
            R.drawable.chat_purple
    };

    /** Linear layout with loading screen. */
    private LinearLayout mProgress;

    /** Layout for scores display. */
    private SlidingUpPanelLayout mPanelLayout;

    /** Icon for scores display. */
    private ImageView mPanelIcon;

    /** View for displaying player's score. */
    private TextView mScoreTextView;

    /** View for displaying player's rank. */
    private TextView mRankTextView;

    /** Linear layout with red background for Insider player. */
    private LinearLayout mInsider;

    /** View for displaying secret word. */
    private TextView mWordTextView;

    /** View for displaying secret word's definition. */
    private TextView mDefinitionTextView;

    /** View for displaying chat messages. */
    private RecyclerView mChatRecyclerView;

    /** EditText for composing messages. */
    private EditText mMessageEditText;

    /** Button to send message. */
    private Button mSendButton;

    /** Fragment to display scores. */
    private PlayersFragment mPlayersFragment;

    /** Reference to database root. */
    private DatabaseReference mDatabase;

    /** Unique UID of player. */
    private String mUid;

    /** Current room code. */
    private String mRoomCode;

    /** Current player. */
    private Player mPlayer;

    /** Last known list of players. */
    private List<Player> mPlayers;

    /** Last known game state. */
    private Game mGame;

    /** Convenience field to store whether current player is the Insider. */
    private boolean mIsInsider;

    /** Whether secret word loading is in progress. */
    private boolean mWordLoading;

    /** Whether the current onStop() call is intentional. */
    private boolean mIntentionalExit;

    /** Layout manager for {@link RecyclerView} of chat messages. */
    private LinearLayoutManager mManager;

    /** Adapter for {@link RecyclerView} of chat messages. */
    private FirebaseRecyclerAdapter mAdapter;

    /** Listener for change in players. */
    private ValueEventListener mPlayersListener;

    /** Listener for change in game state. */
    private ValueEventListener mGameListener;

    /** Listener to detect secret word in chat. */
    private ChildEventListener mChatListener;

    /** Number of active tasks preventing UI interaction. */
    private int mActiveTasks = 0;

    /**
     * Starts an active task that blocks UI interaction.
     */
    private void startActiveTask() {
        if (mActiveTasks == 0) {
            mProgress.setVisibility(View.VISIBLE);
            mPanelLayout.setVisibility(View.GONE);
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
            mPanelLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize view bindings.
        mProgress = findViewById(R.id.progress);
        mPanelLayout = findViewById(R.id.panel_layout);
        mPanelIcon = findViewById(R.id.panel_icon);
        mScoreTextView = findViewById(R.id.score);
        mRankTextView = findViewById(R.id.rank);
        mInsider = findViewById(R.id.insider);
        mWordTextView = findViewById(R.id.secret_word);
        mDefinitionTextView = findViewById(R.id.definition);
        mChatRecyclerView = findViewById(R.id.chat);
        mMessageEditText = findViewById(R.id.message);
        mSendButton = findViewById(R.id.send);
        mPlayersFragment = (PlayersFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment);

        // Set OnClickListeners.
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        // Set scores to display at bottom.
        mPlayersFragment.setStackFromEnd(true);

        // Set text change listener.
        mMessageEditText.addTextChangedListener(this);

        // Set on key listener.
        mMessageEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KEYCODE_ENTER && mSendButton.isEnabled()) {
                    send();
                    return true;
                }
                return false;
            }
        });

        // Set SlidingUpPanelLayout listener.
        mPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) { }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (previousState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    mPanelIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_expand_more_black_24dp));
                } else if (previousState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    mPanelIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_expand_less_black_24dp));
                }
            }
        });

        // Initialize Firebase globals.
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            mIntentionalExit = true;
            Intent intent = new Intent(ChatActivity.this, HomeActivity.class);
            TaskStackBuilder.create(ChatActivity.this).addNextIntent(intent).startActivities();
            Toast.makeText(ChatActivity.this, getString(R.string.toast_not_authenticated), Toast.LENGTH_SHORT).show();
            return;
        }
        mUid = user.getUid();

        // Initialize room code from Intent.
        mRoomCode = getIntent().getStringExtra(EXTRA_ROOM_CODE);

        // Initialize chat adapter.
        FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>()
                .setQuery(mDatabase.child("chats/" + mRoomCode), Message.class).build();
        mAdapter = new FirebaseRecyclerAdapter<Message, ChatViewHolder>(options) {
            @Override
            public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat, parent, false);
                return new ChatViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(ChatViewHolder holder, int position, Message model) {
                holder.mYou.setVisibility(View.GONE);
                holder.mOthers.setVisibility(View.GONE);
                holder.mSystem.setVisibility(View.GONE);
                if (model.color == -1) {
                    holder.mSystem.setText(model.message);
                    holder.mSystem.setVisibility(View.VISIBLE);
                    return;
                }
                TextView view;
                String nickname;
                if (model.uid.equals(mUid)) {
                    view = holder.mYou;
                    nickname = getString(R.string.you);
                } else {
                    view = holder.mOthers;
                    nickname = model.nickname;
                }
                view.setBackgroundResource(CHATS[model.color]);
                if (model.secret_length == null) {
                    view.setText(Html.fromHtml(getString(R.string.message, Html.escapeHtml(nickname), Html.escapeHtml(model.message))));
                } else {
                    view.setText(Html.fromHtml(getString(R.string.message, Html.escapeHtml(nickname), WordUtil.underlineWords(model.message, model.secret_length))));
                }
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (mManager.findLastVisibleItemPosition() == getItemCount() - 2) {
                    mChatRecyclerView.smoothScrollToPosition(getItemCount() - 1);
                }
            }
        };
        mManager = new LinearLayoutManager(this);
        mManager.setStackFromEnd(true);
        mChatRecyclerView.setLayoutManager(mManager);
        mChatRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (BackgroundChatService.sIntent != null) {
            stopService(BackgroundChatService.sIntent);
            BackgroundChatService.sIntent = null;
        }
        mIntentionalExit = false;

        // Get initial values before adding listeners.
        startActiveTask();
        mDatabase.child("rooms/" + mRoomCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Room room = dataSnapshot.getValue(Room.class);
                if (room != null) {
                    List<Player> players = new ArrayList<>();
                    players.addAll(room.players.values());
                    mPlayers = players;
                    mPlayer = room.players.get(mUid);
                }
                startActiveTask();
                mDatabase.child("games/" + mRoomCode).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mGame = dataSnapshot.getValue(Game.class);
                        if (mGame == null) {
                            mIntentionalExit = true;
                            mDatabase.child("players/" + mUid + "/status").setValue(Player.STATUS_IDLE);
                            Intent intent = new Intent(ChatActivity.this, HomeActivity.class);
                            TaskStackBuilder.create(ChatActivity.this).addNextIntent(intent).startActivities();
                            Toast.makeText(ChatActivity.this, getString(R.string.toast_room_expired), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mIsInsider = mUid.equals(mGame.insider);
                        if (mIsInsider && !mGame.insider_finished) {
                            loadWord();
                        }

                        // Keep track of players and their states.
                        addPlayersListener();

                        // Keep track of game state.
                        addGameListener();

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

        mAdapter.startListening();
        updateWidgetLayout();
    }

    @Override
    protected void onStop() {
        if (!mIntentionalExit) {
            Intent intent = new Intent(this, BackgroundChatService.class);
            intent.putExtra(BackgroundChatService.EXTRA_ROOM_CODE, mRoomCode);
            intent.putExtra(BackgroundChatService.EXTRA_NICKNAME, mPlayer.nickname);
            startService(intent);
        }
        if (mPlayersListener != null) {
            mDatabase.child("rooms/" + mRoomCode + "/players").removeEventListener(mPlayersListener);
            mPlayersListener = null;
        }
        if (mGameListener != null) {
            mDatabase.child("games/" + mRoomCode).removeEventListener(mGameListener);
            mGameListener = null;
        }
        if (mChatListener != null) {
            mDatabase.child("chats/" + mRoomCode).removeEventListener(mChatListener);
            mChatListener = null;
        }
        mAdapter.stopListening();
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_skip).setEnabled(mIsInsider);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_resign:
                resign();
                break;
            case R.id.action_skip:
                Map<String, Object> multipleUpdates = new HashMap<>();
                multipleUpdates.put("/games/" + mRoomCode + "/insider_finished", true);
                Message systemMessage = new Message(mUid, -1, null, null, mPlayer.nickname + " skipped turn as Insider.");
                addMessage(multipleUpdates, systemMessage);
                multipleUpdates.put("/games/" + mRoomCode + "/word", null);
                multipleUpdates.put("/games/" + mRoomCode + "/definition", null);
                mDatabase.updateChildren(multipleUpdates);
                mGame.insider_finished = true;
                mGame.word = null;
                mGame.definition = null;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRemovePlayer(int index, String id) { }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        boolean isBlank = mMessageEditText.getText().toString().trim().length() == 0;
        boolean hasWord = mIsInsider && mGame.word != null && mMessageEditText.getText().toString().contains(mGame.word);
        mSendButton.setEnabled(!isBlank && !hasWord);
    }

    /**
     * Posts the contents of mMessageEditText to chat and clears the field.
     */
    public void send() {
        Map<String, Object> multipleUpdates = new HashMap<>();
        Integer length = (mIsInsider || mGame.word == null) ? null : mGame.word.length();
        Message message = new Message(mUid, mPlayer.color, length, mPlayer.nickname, mMessageEditText.getText().toString());
        addMessage(multipleUpdates, message);
        mDatabase.updateChildren(multipleUpdates);
        mMessageEditText.setText("");
    }

    /**
     * Inserts the correct timestamp when posting a message with the given simultaneous updates.
     *
     * @param multipleUpdates Map of simultaneous updates to make
     * @param message Message to post.
     */
    private void addMessage(Map<String, Object> multipleUpdates, Message message) {
        String key = mDatabase.child("chats/" + mRoomCode).push().getKey();
        multipleUpdates.put("/chats/" + mRoomCode + "/" + key + "/uid", message.uid);
        multipleUpdates.put("/chats/" + mRoomCode + "/" + key + "/color", message.color);
        multipleUpdates.put("/chats/" + mRoomCode + "/" + key + "/secret_length", message.secret_length);
        multipleUpdates.put("/chats/" + mRoomCode + "/" + key + "/nickname", message.nickname);
        multipleUpdates.put("/chats/" + mRoomCode + "/" + key + "/message", message.message);
        multipleUpdates.put("/chats/" + mRoomCode + "/" + key + "/timestamp", ServerValue.TIMESTAMP);
    }

    /**
     * Resigns player from the game.
     */
    private void resign() {
        if (mPlayers.size() == 2) {
            Map<String, Object> multipleUpdates = new HashMap<>();
            multipleUpdates.put("/games/" + mRoomCode + "/game_over", true);
            Message message = new Message(mUid, -1, null, null, getString(R.string.message_game_over_resigned, mPlayer.nickname));
            addMessage(multipleUpdates, message);
            mDatabase.updateChildren(multipleUpdates);
            mGame.game_over = true;
        } else {
            Map<String, Object> multipleUpdates = new HashMap<>();
            multipleUpdates.put("/players/" + mUid + "/status", Player.STATUS_IDLE);
            multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + mUid, null);
            multipleUpdates.put("/games/" + mRoomCode + "/insider_candidates/" + mUid, null);
            if (mIsInsider) {
                multipleUpdates.put("/games/" + mRoomCode + "/insider_finished", true);
            }
            Message message = new Message(mUid, -1, null, null, getString(R.string.message_resigned, mPlayer.nickname));
            addMessage(multipleUpdates, message);
            mDatabase.updateChildren(multipleUpdates);
            mGame.insider_finished = true;
        }
    }

    /**
     * Advances to the game over screen.
     */
    private void gameOver() {
        mIntentionalExit = true;
        mDatabase.child("players/" + mUid + "/status").setValue(Player.STATUS_IDLE);
        Intent intent = new Intent(ChatActivity.this, GameOverActivity.class);
        intent.putExtra(GameOverActivity.EXTRA_ROOM_CODE, mRoomCode);
        TaskStackBuilder.create(ChatActivity.this).addNextIntentWithParentStack(intent).startActivities();
    }

    /**
     * Starts process of negotiating for a new Insider.
     *
     * @param invalidInsiderUid UID that should never become the next Insider.
     */
    private void negotiateNewInsider(final String invalidInsiderUid) {
        if (mIsInsider || mUid.equals(invalidInsiderUid)) return;
        if (mGameListener != null) {
            mDatabase.child("games/" + mRoomCode).removeEventListener(mGameListener);
            mGameListener = null;
        }
        if (mChatListener != null) {
            mDatabase.child("chats/" + mRoomCode).removeEventListener(mChatListener);
            mChatListener = null;
        }
        mDatabase.child("games/" + mRoomCode).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Game game = mutableData.getValue(Game.class);
                if (game == null) {
                    // Room has expired, do nothing and let resignation listener handle exiting the activity.
                    return Transaction.success(mutableData);
                }
                // If current player is not finished, do nothing.
                if (!game.insider_finished) {
                    return Transaction.success(mutableData);
                }
                // If current player is not a candidate, do nothing.
                if (game.insider_candidates != null && !game.insider_candidates.containsKey(mUid)) {
                    return Transaction.success(mutableData);
                }
                if (game.insider == null || game.insider.equals(invalidInsiderUid)) {
                    // Insider is still up for grabs.
                    if (game.insider_candidates == null) {
                        // End of round, so advance round counter.
                        if (game.round + 1 > game.round_max) {
                            // Reached end of game.
                            mutableData.child("game_over").setValue(true);
                            return Transaction.success(mutableData);
                        }
                        mutableData.child("round").setValue(game.round + 1);
                        for (Player player : mPlayers) {
                            if (!mUid.equals(player.uid)) {
                                mutableData.child("insider_candidates/" + player.uid).setValue(true);
                            }
                        }
                    } else {
                        mutableData.child("insider_candidates/" + mUid).setValue(null);
                    }
                    mutableData.child("insider").setValue(mUid);
                    mutableData.child("insider_finished").setValue(false);
                    mutableData.child("insider_timestamp").setValue(ServerValue.TIMESTAMP);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
                if (b) {
                    Game game = dataSnapshot.getValue(Game.class);
                    if (game == null) {
                        // Room has expired, do nothing and let resignation listener handle exiting the activity.
                        return;
                    }
                    mGame = game;
                    mIsInsider = mUid.equals(mGame.insider);
                    if (!mGame.game_over && mIsInsider) {
                        loadWord();
                        Map<String, Object> multipleUpdates = new HashMap<>();
                        multipleUpdates.put("/games/" + mRoomCode + "/insider_candidates/" + mUid, null);
                        Message message = new Message(mUid, -1, null, null, getString(R.string.message_insider, mPlayer.nickname));
                        addMessage(multipleUpdates, message);
                        mDatabase.updateChildren(multipleUpdates);
                    }
                    addGameListener();
                }
            }
        });
    }

    /**
     * Adds listener for player changes.
     */
    private void addPlayersListener() {
        if (mPlayersListener != null) {
            mDatabase.child("rooms/" + mRoomCode + "/players").removeEventListener(mPlayersListener);
            mPlayersListener = null;
        }
        mPlayersListener = mDatabase.child("rooms/" + mRoomCode + "/players").orderByChild("score").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PlayersFragmentItem[] items = new PlayersFragmentItem[(int)dataSnapshot.getChildrenCount()];
                List<Player> players = new ArrayList<>();
                boolean uidFound = false;
                int uidIndex = -1;
                int index = 0;
                for (DataSnapshot playerSnapshot : dataSnapshot.getChildren()) {
                    Player player = playerSnapshot.getValue(Player.class);
                    if (player == null) {
                        Log.e(TAG, "Unexpected null Player");
                        continue;
                    }
                    players.add(player);

                    CharSequence name;
                    if (player.uid.equals(mUid)) {
                        name = Html.fromHtml("<b>" + Html.escapeHtml(getString(R.string.you_format,
                                player.nickname)) + "</b>");
                        uidFound = true;
                        uidIndex = items.length - index - 1;
                    } else {
                        name = player.nickname;
                    }
                    int color = -1;
                    if (player.color != null) {
                        color = player.color;
                    }
                    int score = -1;
                    if (player.score != null) {
                        score = player.score;
                    }

                    items[items.length - index - 1] = new PlayersFragmentItem(player.uid, color, -1, name, "", score, false);
                    index += 1;
                }
                if (!uidFound) {
                    mIntentionalExit = true;
                    mDatabase.child("players/" + mUid + "/status").setValue(Player.STATUS_IDLE);
                    Intent intent = new Intent(ChatActivity.this, HomeActivity.class);
                    TaskStackBuilder.create(ChatActivity.this).addNextIntent(intent).startActivities();
                    Toast.makeText(ChatActivity.this, R.string.toast_resigned, Toast.LENGTH_SHORT).show();
                    return;
                }
                mPlayers = players;
                int rank = 1;
                int score = items[0].mScore;
                for (PlayersFragmentItem player : items) {
                    if (player.mScore < score) {
                        rank += 1;
                        score = player.mScore;
                    }
                    player.mRank = rank;
                }
                mScoreTextView.setText(String.valueOf(items[uidIndex].mScore));
                mRankTextView.setText(getString(R.string.rank_format, items[uidIndex].mRank));
                mPlayersFragment.changePlayers(items);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
            }
        });
    }

    /**
     * Adds listener for game state changes.
     */
    private void addGameListener() {
        mGameListener = mDatabase.child("games/" + mRoomCode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Game game = dataSnapshot.getValue(Game.class);
                if (game == null) {
                    // Room has expired, do nothing and let resignation listener handle exiting the activity.
                    return;
                }
                mGame = game;
                if (mGame.game_over) {
                    gameOver();
                    return;
                }
                if (mGame.insider_finished) {
                    negotiateNewInsider(mGame.insider);
                    return;
                }
                mIsInsider = mUid.equals(mGame.insider);
                if (mIsInsider) {
                    if (mChatListener == null) {
                        addChatListener(mGame.insider_timestamp);
                    }
                    mMessageEditText.setHint(R.string.give_a_hint);
                } else {
                    if (mChatListener != null) {
                        mDatabase.child("chats/" + mRoomCode).removeEventListener(mChatListener);
                        mChatListener = null;
                    }
                    mInsider.setVisibility(View.GONE);
                    if (mAdapter.getItemCount() > 0) {
                        mChatRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                    }
                    mMessageEditText.setHint(R.string.guess_the_word);
                }
                setTitle(getString(R.string.title_round, mGame.round, mGame.round_max));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
            }
        });
    }

    /**
     * Adds listener to detect secret word in chat.
     *
     * @param timestamp Starting timestamp of messages to scan.
     */
    private void addChatListener(long timestamp) {
        if (mChatListener != null) {
            mDatabase.child("chats/" + mRoomCode).removeEventListener(mChatListener);
            mChatListener = null;
        }
        mChatListener = mDatabase.child("chats/" + mRoomCode).orderByChild("timestamp").startAt(timestamp).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                if (message != null && message.color != -1 && message.color != mPlayer.color) {
                    Player correctPlayer = null;
                    for (Player player : mPlayers) {
                        if (player.color == message.color) {
                            correctPlayer = player;
                            break;
                        }
                    }
                    if (mGame.word != null && WordUtil.hasWord(message.message, mGame.word)) {
                        Map<String, Object> multipleUpdates = new HashMap<>();
                        multipleUpdates.put("/games/" + mRoomCode + "/insider_finished", true);
                        if (correctPlayer != null) {
                            multipleUpdates.put("/rooms/" + mRoomCode + "/players/" + message.uid + "/score", correctPlayer.score + 1);
                        }
                        Message systemMessage = new Message(mUid, -1, null, null, message.nickname + " guessed the secret word: " + mGame.word);
                        addMessage(multipleUpdates, systemMessage);
                        multipleUpdates.put("/games/" + mRoomCode + "/word", null);
                        multipleUpdates.put("/games/" + mRoomCode + "/definition", null);
                        mDatabase.updateChildren(multipleUpdates);
                        mGame.insider_finished = true;
                        mGame.word = null;
                        mGame.definition = null;
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
            }
        });
    }

    /**
     * Shows secret word and sets up chat listener. Loads new secret word if necessary.
     */
    private void loadWord() {
        if (mWordLoading) return;
        if (mGame.word == null) {
            mWordLoading = true;
            mWordTextView.setText(R.string.loading);
            mDefinitionTextView.setText(getString(R.string.loading));
            mInsider.setVisibility(View.VISIBLE);
            WordUtil.loadWord(new WordUtil.OnWordLoadedListener() {
                @Override
                public void onWordLoaded(String word, String definition) {
                    Map<String, Object> multipleUpdates = new HashMap<>();
                    multipleUpdates.put("/games/" + mRoomCode + "/word", word);
                    multipleUpdates.put("/games/" + mRoomCode + "/definition", definition);
                    Message message = new Message(mUid, -1, null, null, "The secret word has " + word.length() + " letters");
                    addMessage(multipleUpdates, message);
                    mDatabase.updateChildren(multipleUpdates);
                    mGame.word = word;
                    mGame.definition = definition;

                    mWordTextView.setText(word);
                    mDefinitionTextView.setText(definition);
                    if (mAdapter.getItemCount() > 0) {
                        mChatRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                    }
                    if (mChatListener == null) {
                        addChatListener(mGame.insider_timestamp);
                    }

                    mWordLoading = false;
                }
            });
        } else {
            mWordTextView.setText(mGame.word);
            mDefinitionTextView.setText(mGame.definition);
            mInsider.setVisibility(View.VISIBLE);
            if (mAdapter.getItemCount() > 0) {
                mChatRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
            }
            if (mChatListener == null) {
                addChatListener(mGame.insider_timestamp);
            }
        }
    }

    /**
     * {@link ViewHolder} for {@link RecyclerView} of chat messages.
     */
    private class ChatViewHolder extends ViewHolder {

        public final TextView mYou;

        public final TextView mOthers;

        public final TextView mSystem;

        public ChatViewHolder(View itemView) {
            super(itemView);
            mYou = itemView.findViewById(R.id.message_you);
            mOthers = itemView.findViewById(R.id.message_others);
            mSystem = itemView.findViewById(R.id.message_system);
        }
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
