package com.example.jiunwei.wordguess.model;

import java.util.Map;

/**
 * Represents a game in the Firebase Realtime Database.
 */
public class Game {

    /** Whether the game is over; required. */
    public boolean game_over;

    /** What the current secret word is; optional. */
    public String word;

    /** What the secret word's definition is; optional. */
    public String definition;

    /** The current round number; required. */
    public int round;

    /** The maximum number of rounds; required. */
    public int round_max;

    /** UID of the current insider; optional. */
    public String insider;

    /** Valid candidates for the next insider; optional. **/
    public Map<String, Boolean> insider_candidates;

    /** When the last insider was selected; optional. */
    public Long insider_timestamp;

    /** Whether the insider is finished with his/her turn; required. */
    public boolean insider_finished;

    /**
     * Default empty constructor.
     */
    public Game() { }

    /**
     * Creates a game for storage in the database.
     *
     * @param game_over Whether the game is over.
     * @param word What the current secret word is.
     * @param definition What the secret word's definition is.
     * @param round The current round number.
     * @param round_max The maximum number of rounds.
     * @param insider The UID of the current insider.
     * @param insider_candidates Valid candidates for the next insider.
     * @param insider_timestamp When the last insider was selected.
     * @param insider_finished Whether the insider is finished with his/her turn.
     */
    @SuppressWarnings("SameParameterValue")
    public Game(boolean game_over, String word, String definition, int round, int round_max,
                String insider, Map<String, Boolean> insider_candidates, Long insider_timestamp,
                boolean insider_finished) {
        this.game_over = game_over;
        this.word = word;
        this.definition = definition;
        this.round = round;
        this.round_max = round_max;
        this.insider = insider;
        this.insider_candidates = insider_candidates;
        this.insider_timestamp = insider_timestamp;
        this.insider_finished = insider_finished;
    }

}
