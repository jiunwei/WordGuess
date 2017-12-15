package com.example.jiunwei.wordguess.util;

import android.os.AsyncTask;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Utility class for tasks related to words and the Wordnik API.
 */
public class WordUtil {

    /** Wordnik API key; replace with actual API key if needed. */
    private static final String API_KEY = null;

    /** Demonstration API key taken from http://developer.wordnik.com/. */
    private static final String DEMO_KEY = "a2a73e7b926c924fad7001ca3111acd55af2ffabf50eb4ae5";

    /** API endpoint for getting a random word. */
    private static final String URL_RANDOM_WORD = "http://api.wordnik.com/v4/words.json/randomWord?hasDictionaryDef=true&minCorpusCount=1000000&minLength=5&api_key=";

    /** First part of API endpoint for getting a definition. */
    private static final String URL_DEFINITION_1 = "http://api.wordnik.com/v4/word.json/";

    /** Second part of API endpoint for getting a definition. */
    private static final String URL_DEFINITION_2 = "/definitions?limit=1&useCanonical=true&api_key=";

    /** {@link okhttp3.OkHttpClient} to use when accessing Wordnik API. */
    private static final OkHttpClient client = new OkHttpClient();

    /** Listener top call when word and definition are loaded. */
    private static OnWordLoadedListener mListener;

    /**
     * {@link AsyncTask} subclass to fetch word and definition using Wordnik API.
     */
    private static class LoadWordTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... voids) {
            @SuppressWarnings("ConstantConditions") String key = API_KEY == null ? DEMO_KEY : API_KEY;

            String word = null;
            String definition = null;

            do {

                do {
                    Request request = new Request.Builder().url(URL_RANDOM_WORD + key).build();
                    try {
                        Response response = client.newCall(request).execute();
                        ResponseBody body = response.body();
                        if (body != null) {
                            JSONObject json = new JSONObject(body.string());
                            word = json.getString("word").toLowerCase();
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } while (word == null || !word.matches("[\\w]+"));


                do {
                    Request request = new Request.Builder().url(URL_DEFINITION_1 + word + URL_DEFINITION_2 + key).build();
                    try {
                        Response response = client.newCall(request).execute();
                        ResponseBody body = response.body();
                        if (body != null) {
                            JSONArray array = new JSONArray(body.string());
                            definition = array.getJSONObject(0).getString("text");
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } while (definition == null);

            } while (definition.length() > 100);

            return new String[] { word, definition };
        }

        @Override
        protected void onPostExecute(String[] strings) {
            mListener.onWordLoaded(strings[0], strings[1]);
            super.onPostExecute(strings);
        }
    }

    /**
     * Loads a random word and definition, then calls the given listener.
     *
     * @param listener Listener to call when word and definition are loaded.
     */
    public static void loadWord(OnWordLoadedListener listener) {
        mListener = listener;
        new LoadWordTask().execute();
    }

    /**
     * Returns HTML for underlining all words of a given length in a given message.
     *
     * @param message The message to format in HTML.
     * @param length The length of words to underline.
     * @return The formatted HTML.
     */
    public static String underlineWords(String message, int length) {
        String[] pieces = message.split("\\b");
        StringBuilder html = new StringBuilder("");
        for (String piece : pieces) {
            if (piece.length() == length && piece.matches("\\w+")) {
                html.append("<u>");
                html.append(Html.escapeHtml(piece));
                html.append("</u>");
            } else {
                html.append(Html.escapeHtml(piece));
            }
        }
        return html.toString();
    }

    /**
     * Returns whether the given message contains the given word.
     *
     * @param message The message to search.
     * @param word The word to look for.
     * @return Whether word is found in message.
     */
    public static boolean hasWord(String message, String word) {
        word = word.toLowerCase();
        String[] pieces = message.toLowerCase().split("\\b");
        for (String piece : pieces) {
            if (piece.equals(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Interface for listener to call when word and definition are loaded.
     */
    public interface OnWordLoadedListener {

        /**
         * Called when word and definition are loaded.
         *
         * @param word The loaded word.
         * @param definition The loaded definition.
         */
        void onWordLoaded(String word, String definition);

    }

}
