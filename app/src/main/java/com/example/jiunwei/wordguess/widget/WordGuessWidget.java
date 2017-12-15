package com.example.jiunwei.wordguess.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.jiunwei.wordguess.R;
import com.example.jiunwei.wordguess.model.Player;
import com.example.jiunwei.wordguess.ui.ChatActivity;
import com.example.jiunwei.wordguess.ui.HomeActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Implementation of App Widget functionality.
 */
public class WordGuessWidget extends AppWidgetProvider {

    /** Tag for logging. */
    private static final String TAG = WordGuessWidget.class.getSimpleName();

    /** Reference to database root. */
    private DatabaseReference mDatabase;

    /** Unique UID of player. */
    private String mUid;

    /**
     * Updates the widget with the given appWidgetId.
     *
     * @param context Application context to work with.
     * @param appWidgetManager Global widget manager.
     * @param appWidgetId ID of the widget to update.
     * @param player Player information.
     */
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, Player player) {

        // Construct the RemoteViews object.
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.word_guess_widget);

        // Show and hide views depending on status.
        switch (player.status) {

            case Player.STATUS_IDLE:
                views.setViewVisibility(R.id.home, View.VISIBLE);
                views.setViewVisibility(R.id.home_disabled, View.GONE);
                views.setViewVisibility(R.id.chat, View.GONE);
                views.setViewVisibility(R.id.progress, View.GONE);

                Intent createIntent = new Intent(context, HomeActivity.class);
                createIntent.setAction(HomeActivity.ACTION_CREATE);
                PendingIntent create = PendingIntent.getActivity(context, 0, createIntent, 0);
                views.setOnClickPendingIntent(R.id.create, create);

                Intent joinIntent = new Intent(context, HomeActivity.class);
                joinIntent.setAction(HomeActivity.ACTION_JOIN);
                PendingIntent join = PendingIntent.getActivity(context, 0, joinIntent, 0);
                views.setOnClickPendingIntent(R.id.join, join);
                break;

            case Player.STATUS_NOT_READY:
            case Player.STATUS_READY:
                views.setViewVisibility(R.id.home, View.GONE);
                views.setViewVisibility(R.id.home_disabled, View.VISIBLE);
                views.setViewVisibility(R.id.chat, View.GONE);
                views.setViewVisibility(R.id.progress, View.GONE);
                break;

            case Player.STATUS_PLAYING:
                views.setViewVisibility(R.id.home, View.GONE);
                views.setViewVisibility(R.id.home_disabled, View.GONE);
                views.setViewVisibility(R.id.chat, View.VISIBLE);
                views.setViewVisibility(R.id.progress, View.GONE);

                Intent intent = new Intent(context, WordGuessWidgetService.class);
                intent.putExtra(WordGuessWidgetService.EXTRA_ROOM_CODE, player.room_code);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                views.setRemoteAdapter(R.id.messages, intent);
                views.setEmptyView(R.id.messages, R.id.progress);

                Intent chatIntent = new Intent(context, ChatActivity.class);
                chatIntent.putExtra(ChatActivity.EXTRA_ROOM_CODE, player.room_code);
                chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent chat = PendingIntent.getActivity(context, 0, chatIntent, FLAG_UPDATE_CURRENT);
                views.setPendingIntentTemplate(R.id.messages, chat);
                break;

        }

        // Instruct the widget manager to update the widget.
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (auth.getCurrentUser() != null) {
                        mUid = auth.getCurrentUser().getUid();
                        onUpdateAfterAuth(context, appWidgetManager, appWidgetIds);
                    }
                }
            });
        } else {
            mUid = auth.getCurrentUser().getUid();
            onUpdateAfterAuth(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onEnabled(Context context) { }

    @Override
    public void onDisabled(Context context) { }

    /**
     * Called to complete onUpdate() after authorization is complete.
     *
     * @param context Application context to work with.
     * @param appWidgetManager Global widget manager.
     * @param appWidgetIds Array of widget IDs to update.
     */
    private void onUpdateAfterAuth(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        mDatabase.child("players/" + mUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Player player = dataSnapshot.getValue(Player.class);

                // There may be multiple widgets active, so update all of them
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, player);
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

}

