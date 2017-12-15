package com.example.jiunwei.wordguess.model;

/**
 * Represents a chat message in the Firebase Realtime Database.
 */
public class Message {

    /** UID of the sender; required. */
    public String uid;

    /** Color of message, where -1 represents a colorless system message; required. */
    public int color;

    /** Length to use when underlining words in this message; optional. */
    public Integer secret_length;

    /** Nickname of sender; optional. */
    public String nickname;

    /** Message text; required. */
    public String message;

    /** When the messsage was sent; required. */
    @SuppressWarnings("unused")
    public Long timestamp;

    /**
     * Default empty constructor.
     */
    public Message() { }

    /**
     * Creates a message for storage in database.
     *
     * @param uid UID of the sender.
     * @param color Color of message.
     * @param secret_length Length to use when underlining words.
     * @param nickname Nickname of sender.
     * @param message Message text.
     */
    public Message(String uid, int color, Integer secret_length, String nickname, String message) {
        this.uid = uid;
        this.color = color;
        this.secret_length = secret_length;
        this.nickname = nickname;
        this.message = message;
    }

}
