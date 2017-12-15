package com.example.jiunwei.wordguess;

import com.example.jiunwei.wordguess.util.WordUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Local unit tests for WordUtil class.
 */
public class WordUtilTest {

    /**
     * Tests whether WordUtil.hasWord() works correctly.
     */
    @Test
    public void hasWord_isCorrect() {
        assertFalse(WordUtil.hasWord("fend, fiend, find", "friend"));
        assertFalse(WordUtil.hasWord("befriend, friendly, friends", "friend"));
        assertTrue(WordUtil.hasWord("fend, friend's, find", "friend"));
        assertTrue(WordUtil.hasWord("fend, friend, FRIEND, find", "friend"));
        assertTrue(WordUtil.hasWord("fend, FRIEND, find", "friend"));
    }

}