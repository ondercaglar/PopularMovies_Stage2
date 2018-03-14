package com.example.android.popularmovies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.example.android.popularmovies.databinding.ActivityMainBinding;
import com.example.android.popularmovies.model.Movies;
import com.example.android.popularmovies.utilities.InternetConnectionDetector;
import com.example.android.popularmovies.utilities.JsonUtils;
import com.example.android.popularmovies.utilities.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class MainScreen extends AppCompatActivity  implements LoaderManager.LoaderCallbacks<String>,
        AdapterView.OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener
{
    private ActivityMainBinding mainBinding;
    private String SORT_PARAM;



    private static final String QUERY_URL = "query";

    /*
     * This number will uniquely identify our Loader and is chosen arbitrarily.
     */
    private static final int THE_MOVIE_DB_LOADER = 22;
    private List<Movies> moviesList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setupSharedPreferences();
        makeTheMovieDbSearchQuery();

         // Initialize the loader
        getSupportLoaderManager().initLoader(THE_MOVIE_DB_LOADER, null, this);
    }


    private void setupSharedPreferences()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SORT_PARAM = sharedPreferences.getString(getString(R.string.pref_sort_key), getString(R.string.pref_sort_most_popular_value));
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals(getString(R.string.pref_sort_key)))
        {
            SORT_PARAM = sharedPreferences.getString(getString(R.string.pref_sort_key), getString(R.string.pref_sort_most_popular_value));
            makeTheMovieDbSearchQuery();
        }
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // Unregister MainScreen activity as an OnPreferenceChangedListener to avoid any memory leaks.
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }


    private void makeTheMovieDbSearchQuery()
    {
        // creating connection detector class instance
        InternetConnectionDetector cd = new InternetConnectionDetector(getApplicationContext());
        Boolean isInternetPresent = cd.isConnectingToInternet();

        if(!isInternetPresent)
        {
           showErrorMessage();
           Toast.makeText(this, getResources().getString(R.string.no_connection), Toast.LENGTH_LONG).show();
           return;
        }

        URL TheMovieDbSearchUrl = NetworkUtils.buildUrl(SORT_PARAM);
        Bundle queryBundle = new Bundle();
        queryBundle.putString(QUERY_URL, TheMovieDbSearchUrl.toString());

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> theMovieDbLoader = loaderManager.getLoader(THE_MOVIE_DB_LOADER);
        if (theMovieDbLoader == null)
        {
            loaderManager.initLoader(THE_MOVIE_DB_LOADER, queryBundle, this);
        }
        else
        {
            loaderManager.restartLoader(THE_MOVIE_DB_LOADER, queryBundle, this);
        }
    }


    private void showJsonDataView()
    {
        mainBinding.tvErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mainBinding.moviesGrid.setVisibility(View.VISIBLE);
    }


    private void showErrorMessage()
    {
        mainBinding.moviesGrid.setVisibility(View.INVISIBLE);
        mainBinding.tvErrorMessageDisplay.setVisibility(View.VISIBLE);
    }


    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, final Bundle args)
    {
        return new AsyncTaskLoader<String>(this)
        {
            // Create a String member variable called mTheMovieDbJson that will store the raw JSON
            String mTheMovieDbJson;

            @Override
            protected void onStartLoading()
            {
                /* If no arguments were passed, we don't have a query to perform. Simply return. */
                if (args == null)
                {
                    return;
                }

                /*
                 * When we initially begin loading in the background, we want to display the
                 * loading indicator to the user
                 */
                mainBinding.pbLoadingIndicator.setVisibility(View.VISIBLE);

                // If mTheMovieDbJson is not null, deliver that result. Otherwise, force a load
                /*
                 * If we already have cached results, just deliver them now. If we don't have any
                 * cached results, force a load.
                 */
                if (mTheMovieDbJson != null)
                {
                    deliverResult(mTheMovieDbJson);
                }
                else
                {
                    forceLoad();
                }
            }

            @Override
            public String loadInBackground()
            {
                /* Extract the query from the args using our constant */
                String searchQueryUrlString = args.getString(QUERY_URL);

               /* Parse the URL from the passed in String and perform the search */
                try
                {
                    URL TheMovieDbUrl = new URL(searchQueryUrlString);
                    return NetworkUtils.getResponseFromHttpUrl(TheMovieDbUrl);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void deliverResult(String TheMovieDbJson)
            {
                mTheMovieDbJson = TheMovieDbJson;
                super.deliverResult(TheMovieDbJson);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data)
    {
        // When we finish loading, we want to hide the loading indicator from the user.
        mainBinding.pbLoadingIndicator.setVisibility(View.INVISIBLE);

        //If the results are null, we assume an error has occurred.
        if (null == data)
        {
            showErrorMessage();
        }
        else
        {
            showJsonDataView();
            moviesList= JsonUtils.parseMoviesJson(data);
            MoviesAdapter moviesAdapter = new MoviesAdapter(this, moviesList);
            mainBinding.moviesGrid.setAdapter(moviesAdapter);
            mainBinding.moviesGrid.setOnItemClickListener(this);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {
        /*
         * We aren't using this method in our example application, but we are required to Override
         * it to implement the LoaderCallbacks<String> interface
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.movies_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Movies movies = moviesList.get(position);
        Intent intent = new Intent(getBaseContext(), MovieDetailActivity.class);
        intent.putExtra("movies",movies);
        startActivity(intent);
    }

}