package com.example.jiunwei.wordguess.ui;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Represents player information that can be displayed in a {@link PlayersFragment}.
 */
public class PlayersFragmentItem implements Parcelable {

    /** Player's unique UID. */
    public final String mId;

    /** Player's color. */
    public final int mColor;

    /** Player's rank in the game. */
    public int mRank;

    /** Player's nickname (or other information). */
    public final CharSequence mName;

    /** Player's status. */
    public final String mStatus;

    /** Player's score in the game. */
    public final int mScore;

    /** Whether player has a remove button enabled. */
    public final boolean mRemovable;

    /**
     * Creates representation of player information to display.
     *
     * @param id Player's unique UID.
     * @param color Player's color.
     * @param rank Player's rank in the game.
     * @param name Player's nickname.
     * @param status Player's status.
     * @param score Player's score in the game.
     * @param removable Whether player has a remove button enabled.
     */
    public PlayersFragmentItem(@NonNull String id, int color, int rank,
                               @NonNull CharSequence name, @NonNull String status,
                               int score, boolean removable) {
        mId = id;
        mColor = color;
        mRank = rank;
        mName = name;
        mStatus = status;
        mScore = score;
        mRemovable = removable;
    }

    private PlayersFragmentItem(Parcel in) {
        mId = in.readString();
        mColor = in.readInt();
        mRank = in.readInt();
        mName = in.readString();
        mStatus = in.readString();
        mScore = in.readInt();

        boolean[] booleanArray = new boolean[1];
        in.readBooleanArray(booleanArray);
        mRemovable = booleanArray[0];
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mId);
        out.writeInt(mColor);
        out.writeInt(mRank);
        out.writeString(mName.toString());
        out.writeString(mStatus);
        out.writeInt(mScore);

        boolean[] booleanArray = new boolean[] { mRemovable };
        out.writeBooleanArray(booleanArray);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<PlayersFragmentItem> CREATOR = new Parcelable.Creator<PlayersFragmentItem>() {
        public PlayersFragmentItem createFromParcel(Parcel in) {
            return new PlayersFragmentItem(in);
        }

        public PlayersFragmentItem[] newArray(int size) {
            return new PlayersFragmentItem[size];
        }
    };

}