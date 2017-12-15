package com.example.jiunwei.wordguess.util;

/**
 * Contains convenience methods for dealing with room codes.
 */
public class RoomUtil {

    /** Starting number of letters for a room code. */
    public static final int STARTING_CODE_LENGTH = 4;

    /** Amount of time in milliseconds before a room expires. */
    public static final long MAX_AGE = 7200000;

    /** Maximum number of players in a room. */
    public static final int MAX_PLAYERS = 6;

    /**
     * Generate a random sequence of capital letters with the given length.
     *
     * @param length Length of result.
     * @return Random sequence of capital letters with given length.
     */
    public static String generateRoomCode(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            result.append((char)('A' + Math.floor(Math.random() * 26)));
        }
        return result.toString();
    }

}
