package com.example.jiunwei.wordguess.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Message;
import com.example.jiunwei.wordguess.ui.ChatActivity;
import com.example.jiunwei.wordguess.util.ChatUtil;
import com.example.jiunwei.wordguess.widget.WordGuessWidget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Background {@link Service} for keeping track of new chat messages and updating any widgets that
 * may be displaying the chat transcript.
 */
public class BackgroundChatService extends Service {

    /** Tag for logging. */
    private static final String TAG = BackgroundChatService.class.getSimpleName();

    /** Key for room code intent extra. */
    public static final String EXTRA_ROOM_CODE = "room_code";

    /** Key for nickname intent extra. */
    public static final String EXTRA_NICKNAME = "nickname";

    /** Intent used to start this service. */
    public static Intent sIntent;

    /** Reference to database root. */
    private DatabaseReference mDatabase;

    /** Unique UID of player. */
    private String mUid;

    /** Current room code. */
    private String mRoomCode;

    /** Current player's nickname. */
    private String mNickname;

    /** Notification style for building notifications. */
    private NotificationCompat.MessagingStyle mStyle;

    /** Listener of new message events. */
    private ChildEventListener mChatListener;

    /** Listener of room change events. */
    private ValueEventListener mRoomListener;

    /** Whether this is the first time Service is being run. */
    private boolean mFirstRun = true;

    /**
     * Default empty constructor.
     */
    public BackgroundChatService() { }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIntent = intent;
        mRoomCode = intent.getStringExtra(EXTRA_ROOM_CODE);
        mNickname = intent.getStringExtra(EXTRA_NICKNAME);

        // Set up listener for new chats if needed.
        mDatabase.child("chats/" + mRoomCode).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String lastKey = null;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    lastKey = child.getKey();
                }
                if (mChatListener == null) {
                    mChatListener = mDatabase.child("chats/" + mRoomCode).orderByKey().startAt(lastKey).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            if (mFirstRun) {
                                mFirstRun = false;
                                return;
                            }
                            Message message = dataSnapshot.getValue(Message.class);
                            if (message == null) {
                                Log.e(TAG, "Unexpected null Message");
                                return;
                            }
                            String nickname = message.nickname;
                            if (message.color == -1) {
                                nickname = "System";
                            }
                            mStyle.addMessage(message.message, System.currentTimeMillis(), nickname);
                            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            if (manager != null) {
                                manager.notify(ChatUtil.CHAT_NOTIFICATION_ID, createNotification(mStyle));
                            }

                            // Update widget as well.
                            AppWidgetManager widgetManager = AppWidgetManager.getInstance(BackgroundChatService.this);
                            int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(BackgroundChatService.this, WordGuessWidget.class));
                            widgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.messages);
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.toString());
                }
            }
        });

        // Set up listener for whether player is still in room.
        if (mRoomListener == null) {
            mRoomListener = mDatabase.child("rooms/" + mRoomCode + "/players/" + mUid + "/uid").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String uid = dataSnapshot.getValue(String.class);
                    if (uid == null) {
                        // Abort service if player leaves room.
                        stopSelf();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (databaseError != null) {
                        Log.e(TAG, databaseError.toString());
                    }
                }
            });
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase globals.
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Abort service if not authenticated.
            stopSelf();
            return;
        }
        mUid = user.getUid();

        // Set up notification channel if on Oreo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence name = "WordGuess";
            String description = "Notification channel for WordGuess messages";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(ChatUtil.CHAT_CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            mChannel.setShowBadge(false);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
        }

        // Request foreground status.
        mStyle = new NotificationCompat.MessagingStyle(mNickname);
        startForeground(ChatUtil.CHAT_NOTIFICATION_ID, createNotification(mStyle));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification(NotificationCompat.MessagingStyle style) {
        Intent notificationIntent = new Intent(this, ChatActivity.class);
        notificationIntent.putExtra(ChatActivity.EXTRA_ROOM_CODE, mRoomCode);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChatUtil.CHAT_CHANNEL_ID)
                        .setContentTitle("WordGuess")
                        .setContentText("Game is in progress.")
                        .setSmallIcon(R.drawable.ic_stat_wg)
                        .setContentIntent(pendingIntent);

        if (style.getMessages().size() > 0) {
            builder.setStyle(style);
        }

        return builder.build();
    }

    @Override
    public void onDestroy() {
        if (mChatListener != null) {
            mDatabase.child("chats/" + mRoomCode).removeEventListener(mChatListener);
        }
        if (mRoomListener != null) {
            mDatabase.child("rooms/" + mRoomCode + "/players/" + mUid + "/uid").removeEventListener(mRoomListener);
        }
        super.onDestroy();
    }
}
