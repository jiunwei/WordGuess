package com.example.jiunwei.wordguess.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jiunwei.wordguess.R;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlayersFragmentItem} and makes a call to the
 * specified {@link PlayersFragment.OnRemovePlayerListener}.
 */
public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.ViewHolder> {

    /** Background drawables for each color. */
    private static final int[] CIRCLES = {
            R.drawable.circle_red,
            R.drawable.circle_blue,
            R.drawable.circle_green,
            R.drawable.circle_orange,
            R.drawable.circle_brown,
            R.drawable.circle_purple
    };

    /** Array of player information to display. */
    private PlayersFragmentItem[] mPlayers;

    /** Listener to call when removal button for a player is pressed. */
    private final PlayersFragment.OnRemovePlayerListener mListener;

    /**
     * Creates a PlayersAdapter with the given player information and listener to store.
     *
     * @param players Player information to display.
     * @param listener Listener to store for calling later.
     */
    public PlayersAdapter(PlayersFragmentItem[] players, PlayersFragment.OnRemovePlayerListener listener) {
        mPlayers = players;
        mListener = listener;
    }

    /**
     * Changes the player information being displayed.
     *
     * @param players The new player information to display.
     */
    public void changePlayers(PlayersFragmentItem[] players) {
        mPlayers = players;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        PlayersFragmentItem player = mPlayers[position];
        holder.mCircle.setBackgroundResource(CIRCLES[player.mColor]);
        holder.mCircle.setText(player.mRank < 1 ? "" : String.valueOf(player.mRank));
        holder.mName.setText(player.mName);
        holder.mStatus.setText(player.mStatus);
        holder.mStatus.setVisibility(player.mStatus.trim().equals("") ? View.GONE : View.VISIBLE);
        holder.mScore.setText(player.mScore < 0 ? "" : String.valueOf(player.mScore));
        holder.mScore.setVisibility(player.mScore < 0 ? View.GONE : View.VISIBLE);
        holder.mRemove.setVisibility(player.mRemovable ? View.VISIBLE : View.GONE);

        holder.mRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onRemovePlayer(holder.getAdapterPosition(),
                            mPlayers[holder.getAdapterPosition()].mId);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPlayers.length;
    }

    /**
     * ViewHolder for {@link RecyclerView} of player information in a {@link PlayersFragment}.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView mCircle;

        public final TextView mName;

        public final TextView mStatus;

        public final TextView mScore;

        public final ImageView mRemove;

        public ViewHolder(View view) {
            super(view);
            mCircle = view.findViewById(R.id.circle);
            mName = view.findViewById(R.id.name);
            mStatus = view.findViewById(R.id.status);
            mScore = view.findViewById(R.id.score);
            mRemove = view.findViewById(R.id.remove);
        }

    }
}