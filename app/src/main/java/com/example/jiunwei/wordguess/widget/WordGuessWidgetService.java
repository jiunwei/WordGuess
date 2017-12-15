package com.example.jiunwei.wordguess.widget;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Log;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * {@link RemoteViewsService} to provide a {@link RemoteViewsFactory} for the app's widget.
 */
public class WordGuessWidgetService extends RemoteViewsService {

    /** Key for room code intent extra. */
    public static final String EXTRA_ROOM_CODE = "room_code";

    /**
     * {@link RemoteViewsFactory} for widget to populate {@link ListView} of messages.
     */
    private static class ChatViewsFactory implements RemoteViewsFactory {

        /** Tag for logging. */
        private static final String TAG = ChatViewsFactory.class.getSimpleName();

        /** Number of most recent messages to display. */
        private static final int SIZE = 10;

        /** Layouts to use based on color. */
        private static final int[] ITEMS = {
                R.layout.item_widget_player0,
                R.layout.item_widget_player1,
                R.layout.item_widget_player2,
                R.layout.item_widget_player3,
                R.layout.item_widget_player4,
                R.layout.item_widget_player5
        };

        /** Reference to root of database. */
        private DatabaseReference mDatabase;

        /** List of messages to display. */
        private List<Message> mMessages;

        /** Unique UID of player. */
        private String mUid;

        /** Application context. */
        private final Context mContext;

        /** Current room code. */
        private final String mRoomCode;

        /**
         * Creates a factory with the given context and intent.
         *
         * @param context Application context.
         * @param intent Intent used to start the surrounding service.
         */
        public ChatViewsFactory(Context context, Intent intent) {
            mContext = context;
            mRoomCode = intent.getStringExtra(EXTRA_ROOM_CODE);
        }

        @Override
        public void onCreate() {
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }

        @Override
        public void onDataSetChanged() {
            // Use blocking queue to make async calls synchronous.
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);

            final FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (auth.getCurrentUser() != null) {
                            mUid = auth.getCurrentUser().getUid();
                            queue.add(0);
                        }
                    }
                });
            } else {
                mUid = auth.getCurrentUser().getUid();
                queue.add(0);
            }

            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mDatabase.child("chats/" + mRoomCode).limitToLast(SIZE).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mMessages = new ArrayList<>();
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Message message = child.getValue(Message.class);
                        mMessages.add(message);
                    }
                    queue.add(0);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (databaseError != null) {
                        Log.e(TAG, databaseError.toString());
                    }
                }
            });

            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            if (mMessages == null) return 0;
            return Math.min(SIZE, mMessages.size());
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (mUid == null || mMessages == null) onDataSetChanged();
            Message message = mMessages.get(position);
            RemoteViews views;
            if (message.color == -1) {
                views = new RemoteViews(mContext.getPackageName(), R.layout.item_widget_system);
                views.setTextViewText(R.id.message, message.message);
            } else {
                views = new RemoteViews(mContext.getPackageName(), ITEMS[message.color]);
                String name = message.nickname;
                if (message.uid.equals(mUid)) name = mContext.getResources().getString(R.string.you);
                views.setTextViewText(R.id.message, Html.fromHtml(mContext.getResources().getString(R.string.message,
                        Html.escapeHtml(name), Html.escapeHtml(message.message))));
            }
            views.setOnClickFillInIntent(R.id.message, new Intent());
            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return ITEMS.length + 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ChatViewsFactory(getApplicationContext(), intent);
    }

}
