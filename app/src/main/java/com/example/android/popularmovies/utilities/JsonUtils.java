package com.example.android.popularmovies.utilities;


import android.util.Log;

import com.example.android.popularmovies.model.Movies;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class JsonUtils
{

    private static final String JSON_RESULTS         = "results";
    private static final String JSON_ORIGINAL_TITLE  = "original_title";
    private static final String JSON_POSTER_IMAGE    = "poster_path";
    private static final String JSON_OVERWIEV        = "overview";
    private static final String JSON_VOTE_AVERAGE    = "vote_average";
    private static final String JSON_RELEASE_DATE    = "release_date";
    private static final String JSON_BACKDROP        = "backdrop_path";


    private final static String IMAGE_URL = "http://image.tmdb.org/t/p/w185/";


    public static   List<Movies> parseMoviesJson(String json)
    {

        List<Movies> mMovies = new ArrayList();

        try
        {
            JSONObject readerObject = new JSONObject(json);

            //Get the instance of JSONArray that contains JSONObjects
            JSONArray jsonArray = readerObject.optJSONArray(JSON_RESULTS);

            //Iterate the jsonArray and print the info of JSONObjects
            for(int i=0; i < jsonArray.length(); i++)
            {
                Movies mFilm = new Movies();
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String original_title   = jsonObject.optString(JSON_ORIGINAL_TITLE);
                mFilm.setOriginalTitle(original_title);

                String poster_path = jsonObject.optString(JSON_POSTER_IMAGE);
                mFilm.setPosterPath(IMAGE_URL + poster_path);
                Log.e("alom", IMAGE_URL + poster_path);

                String overview = jsonObject.optString(JSON_OVERWIEV);
                mFilm.setOverview(overview);

                String vote_average = jsonObject.optString(JSON_VOTE_AVERAGE);
                mFilm.setVoteAverage(vote_average);

                String release_date = jsonObject.optString(JSON_RELEASE_DATE);
                mFilm.setReleaseDate(release_date);

                String  backdrop_path = jsonObject.optString(JSON_BACKDROP);
                mFilm.setBackdropPath(IMAGE_URL + backdrop_path);
                Log.e("alom", IMAGE_URL + backdrop_path);

                mMovies.add(mFilm);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return mMovies;
    }

}
