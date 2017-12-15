package com.example.jiunwei.wordguess.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Player;
import com.example.jiunwei.wordguess.service.BackgroundChatService;
import com.example.jiunwei.wordguess.widget.WordGuessWidget;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Provides user interface for player to review and share results.
 */
public class GameOverActivity extends AppCompatActivity implements PlayersFragment.OnRemovePlayerListener {

    /** Tag for logging. */
    private static final String TAG = GameOverActivity.class.getSimpleName();

    /** Key for room code intent extra. */
    public static final String EXTRA_ROOM_CODE = "room_code";

    /** View for displaying player's score. */
    private TextView mScoreTextView;

    /** View for displaying player's rank. */
    private TextView mRankTextView;

    /** Fragment to display scores. */
    private PlayersFragment mPlayersFragment;

    /** Reference to database root. */
    private DatabaseReference mDatabase;

    /** {@link ActionProvider} for share actions. */
    private ShareActionProvider mShareActionProvider;

    /** Summary of game results. */
    private String mSummary;

    /** Unique UID of player. */
    private String mUid;

    /** Current room code. */
    private String mRoomCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameover);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize view bindings.
        mScoreTextView = findViewById(R.id.score);
        mRankTextView = findViewById(R.id.rank);
        mPlayersFragment = (PlayersFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment);

        // Initialize test ad.
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
        AdView ad = findViewById(R.id.ad);
        AdRequest adRequest = new AdRequest.Builder().build();
        ad.loadAd(adRequest);

        // Initialize Firebase globals.
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            Toast.makeText(this, getString(R.string.toast_not_authenticated), Toast.LENGTH_SHORT).show();
            return;
        }
        mUid = user.getUid();

        // Initialize room code from Intent.
        mRoomCode = getIntent().getStringExtra(EXTRA_ROOM_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Remove background service if running.
        if (BackgroundChatService.sIntent != null) {
            stopService(BackgroundChatService.sIntent);
            BackgroundChatService.sIntent = null;
        }

        // Keep track of players and their states.
        mDatabase.child("rooms/" + mRoomCode + "/players").orderByChild("score").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PlayersFragmentItem[] items = new PlayersFragmentItem[(int)dataSnapshot.getChildrenCount()];
                int uidIndex = -1;
                int index = 0;
                String nickname = null;
                ArrayList<String> others = new ArrayList<>();
                for (DataSnapshot playerSnapshot : dataSnapshot.getChildren()) {
                    Player player = playerSnapshot.getValue(Player.class);
                    if (player == null) {
                        Log.e(TAG, "Unexpected null Player");
                        continue;
                    }

                    CharSequence name;
                    if (player.uid.equals(mUid)) {
                        name = Html.fromHtml("<b>" + Html.escapeHtml(getString(R.string.you_format,
                                player.nickname)) + "</b>");
                        uidIndex = items.length - index - 1;
                        nickname = player.nickname;
                    } else {
                        name = player.nickname;
                        others.add(player.nickname);
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
                int rank = 1;
                int score = items[0].mScore;
                for (PlayersFragmentItem player : items) {
                    if (player.mScore < score) {
                        rank += 1;
                        score = player.mScore;
                    }
                    player.mRank = rank;
                }
                if (uidIndex == -1) {
                    finish();
                    Toast.makeText(GameOverActivity.this, getString(R.string.toast_room_expired), Toast.LENGTH_SHORT).show();
                    return;
                }
                mScoreTextView.setText(String.valueOf(items[uidIndex].mScore));
                mRankTextView.setText(getString(R.string.rank_format, items[uidIndex].mRank));
                mPlayersFragment.changePlayers(items);

                // Create summary for sharing.
                if (nickname != null && others.size() > 0) {
                    String delimiter = "";
                    StringBuilder othersBuilder = new StringBuilder("");
                    for (String other : others) {
                        othersBuilder.append(delimiter).append(other);
                        delimiter = getString(R.string.delimiter);
                    }
                    mSummary = getString(R.string.summary, nickname, items[uidIndex].mScore, items[uidIndex].mRank, othersBuilder.toString());
                }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_over, menu);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_share));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mSummary != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, mSummary);
            intent.setType("text/plain");
            mShareActionProvider.setShareIntent(intent);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRemovePlayer(int index, String id) { }

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
