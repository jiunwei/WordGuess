package com.example.jiunwei.wordguess;

import com.example.jiunwei.wordguess.util.RoomUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Local unit tests for RoomUtil class.
 */
public class RoomUtilTest {

    /**
     * Tests whether RoomUtil.generateRoomCode() works correctly.
     */
    @Test
    public void generateRoomCode_isCorrect() {
        assertTrue(RoomUtil.generateRoomCode(3).matches("[A-Z]{3}"));
        assertTrue(RoomUtil.generateRoomCode(4).matches("[A-Z]{4}"));
        assertTrue(RoomUtil.generateRoomCode(5).matches("[A-Z]{5}"));
    }

}