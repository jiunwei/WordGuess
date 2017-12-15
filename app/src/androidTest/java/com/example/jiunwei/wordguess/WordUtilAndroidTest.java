package com.example.jiunwei.wordguess;

import android.support.test.runner.AndroidJUnit4;

import com.example.jiunwei.wordguess.util.WordUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented tests for WordUtil class.
 */
@RunWith(AndroidJUnit4.class)
public class WordUtilAndroidTest {

    /**
     * Tests whether WordUtil.loadWord() works correctly.
     */
    @Test
    public void loadWord_notNull() {
        WordUtil.loadWord(new WordUtil.OnWordLoadedListener() {
            @Override
            public void onWordLoaded(String word, String definition) {
                assertNotNull(word);
                assertNotNull(definition);
            }
        });
    }

    /**
     * Tests whether WordUtil.underlineWords() works correctly.
     */
    @Test
    public void underlineWords_isCorrect() {
        String html = WordUtil.underlineWords("  they're only R&D", 4);
        assertEquals(html, "&nbsp; <u>they</u>'re <u>only</u> R&amp;D");
    }

}
