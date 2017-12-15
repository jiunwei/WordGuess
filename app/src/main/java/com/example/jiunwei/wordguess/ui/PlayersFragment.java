package com.example.jiunwei.wordguess.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.jiunwei.wordguess.R;

/**
 * {@link Fragment} subclass to display a list of {@link PlayersFragmentItem} objects. Activities
 * that contain this fragment must implement the {@link PlayersFragment.OnRemovePlayerListener}
 * interface to handle player removal (if enabled).
 */
public class PlayersFragment extends Fragment {

    /** Key for passing in player information as an argument. */
    private static final String ARG_PLAYERS = "players";

    /** Array of player information to display. */
    private PlayersFragmentItem[] mPlayers;

    /** Layout manager for {@link RecyclerView} of player information. */
    private LinearLayoutManager mManager;

    /** Adapter for {@link RecyclerView} of player information. */
    private PlayersAdapter mAdapter;

    /** Listener to call when removal button for a player is pressed. */
    private OnRemovePlayerListener mListener;

    /**
     * Default empty constructor.
     */
    public PlayersFragment() { }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @param players The players to display in the fragment.
     * @return A new instance of {@link PlayersFragment}.
     */
    @SuppressWarnings("unused")
    public static PlayersFragment newInstance(PlayersFragmentItem[] players) {
        PlayersFragment fragment = new PlayersFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(ARG_PLAYERS, players);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPlayers = (PlayersFragmentItem[]) getArguments().getParcelableArray(ARG_PLAYERS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_players, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            mManager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(mManager);
            if (mPlayers == null) {
                mAdapter = new PlayersAdapter(new PlayersFragmentItem[0], mListener);
            } else {
                mAdapter = new PlayersAdapter(mPlayers, mListener);
            }
            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnRemovePlayerListener) {
            mListener = (OnRemovePlayerListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnRemovePlayerListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Changes the player information being displayed.
     *
     * @param players The new player information to display.
     */
    public void changePlayers(PlayersFragmentItem[] players) {
        mPlayers = players;
        mAdapter.changePlayers(players);
    }

    /**
     * Returns the array of player information being displayed.
     *
     * @return Array of player information being displayed.
     */
    public PlayersFragmentItem[] getPlayers() {
        return mPlayers;
    }

    /**
     * Sets internal {@link RecyclerView} to use the given setStackfromEnd setting.
     *
     * @param b Whether the {@link RecyclerView} should be bottom aligned.
     */
    @SuppressWarnings("SameParameterValue")
    public void setStackFromEnd(boolean b) {
        mManager.setStackFromEnd(b);
    }

    /**
     * This interface must be implemented by activities that contain this fragment so player
     * removal requests from this fragment can be communicated to the activity and potentially
     * other fragments contained in that activity.
     */
    public interface OnRemovePlayerListener {

        /**
         * Called when a player is removed.
         *
         * @param index The index of the player in the list.
         * @param id The id attribute of the player.
         */
        @SuppressWarnings("unused")
        void onRemovePlayer(int index, String id);

    }

}
