package com.example.jiunwei.wordguess.model;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Represents a player in the Firebase Realtime Database.
 */
@IgnoreExtraProperties
public class Player {

    /** Represents an idle player.*/
    public static final int STATUS_IDLE = 0;

    /** Represents player that is in a waiting room but not ready to play. */
    public static final int STATUS_NOT_READY = 1;

    /** Represents player that is in a waiting room and ready to play. */
    public static final int STATUS_READY = 2;

    /** Represents a player that is in the middle of a game. */
    public static final int STATUS_PLAYING = 3;

    /** Unique UID of the player; required. */
    public String uid;

    /** Nickname of the player; required. */
    public String nickname;

    /** Status of the player; required. */
    public int status;

    /** The player's current room code, if any; optional. */
    @SuppressWarnings("unused")
    public String room_code;

    /** The player's current color, if any; optional. */
    @SuppressWarnings("unused")
    public Integer color;

    /** The player's current score, if any; optional. */
    @SuppressWarnings("unused")
    public Integer score;

    /**
     * Default empty constructor.
     */
    public Player() { }

    /**
     * Creates a player for storage in database.
     *
     * @param uid Unique UID of the player.
     * @param nickname Player's nickname.
     * @param status Player's status.
     */
    public Player(String uid, String nickname, int status) {
        this.uid = uid;
        this.nickname = nickname;
        this.status = status;
    }

}
