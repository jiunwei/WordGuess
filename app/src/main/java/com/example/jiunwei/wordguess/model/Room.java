package com.example.jiunwei.wordguess.model;

import java.util.Map;

/**
 * Represents a room for player matching in the Firebase Realtime Database.
 */
public class Room {

    /** Alphabetical room code; required. */
    @SuppressWarnings("unused")
    public String room_code;

    /** Whether the room is accepting new players; required. */
    public boolean open;

    /** When the room was created; required. */
    public long timestamp;

    /** Players in the room; required. */
    public Map<String, Player> players;

    /**
     * Default empty constructor.
     */
    public Room() { }

    /**
     * Creates a room for storage in the database.
     *
     * @param room_code Alphabetical room code.
     * @param open Whether the room is accepting new players.
     * @param timestamp When the room was created.
     * @param players Players in the room.
     */
    public Room(String room_code, boolean open, long timestamp, Map<String, Player> players) {
        this.room_code = room_code;
        this.open = open;
        this.timestamp = timestamp;
        this.players = players;
    }

}
