package com.example.android.popularmovies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.android.popularmovies.databinding.ActivityMainBinding;
import com.example.android.popularmovies.model.Movies;
import com.example.android.popularmovies.model.MoviesContract;
import com.example.android.popularmovies.utilities.InternetConnectionDetector;
import com.example.android.popularmovies.utilities.JsonUtils;
import com.example.android.popularmovies.utilities.NetworkUtils;
import com.example.android.popularmovies.utilities.Utility;
import com.facebook.stetho.Stetho;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class MainScreen extends AppCompatActivity  implements LoaderManager.LoaderCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener,
        MoviesAdapter.MoviesAdapterOnClickHandler,
        FavoritesAdapter.FavoritesAdapterOnClickHandler
{

    private static final String[] MAIN_MOVIES_PROJECTION =
    {
            MoviesContract.MovieEntry.COLUMN_MOVIE_ID,
            MoviesContract.MovieEntry.COLUMN_ORIGINAL_TITLE,
            MoviesContract.MovieEntry.COLUMN_POSTER_PATH,
            MoviesContract.MovieEntry.COLUMN_OVERVIEW,
            MoviesContract.MovieEntry.COLUMN_VOTE_AVERAGE,
            MoviesContract.MovieEntry.COLUMN_RELEASE_DATE,
            MoviesContract.MovieEntry.COLUMN_BACKDROP_PATH
    };

    /*
     * We store the indices of the values in the array of Strings above to more quickly be able to
     * access the data from our query. If the order of the Strings above changes, these indices
     * must be adjusted to match the order of the Strings.
     */
    private static final int INDEX_MOVIE_ID       = 0;
    private static final int INDEX_ORIGINAL_TITLE = 1;
    public static final int INDEX_POSTER_PATH    = 2;
    private static final int INDEX_OVERVIEW       = 3;
    private static final int INDEX_VOTE_AVERAGE   = 4;
    private static final int INDEX_RELEASE_DATE   = 5;
    private static final int INDEX_BACKDROP_PATH  = 6;


    private ActivityMainBinding mainBinding;
    private String SORT_PARAM;

    private MoviesAdapter moviesAdapter;
    private FavoritesAdapter favoritesAdapter;
    private static final String QUERY_URL = "query";


    /*
     * This number will uniquely identify our Loader and is chosen arbitrarily.
     */
    private static final int THE_MOVIE_DB_LOADER = 22;
    private static final int ID_FAVORITES_LOADER = 44;
    private ArrayList<Movies> moviesList;
    private Cursor mCursor;

    //private Parcelable savedRecyclerLayoutState;
    //private static final String RECYCLER_MOVIES = "recycler_movies";
    private static final String STATE_KEY = "state_key";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        Stetho.initializeWithDefaults(this);

        int mNoOfColumns = Utility.calculateNoOfColumns(getApplicationContext());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, mNoOfColumns);
        mainBinding.recyclerviewMovies.setLayoutManager(gridLayoutManager);
        mainBinding.recyclerviewMovies.setHasFixedSize(true);

        setupSharedPreferences();

        if (savedInstanceState != null &&
                !SORT_PARAM.equals(getString(R.string.pref_sort_favorites_value)))
        {
            moviesAdapter = new MoviesAdapter(this, this);
            mainBinding.recyclerviewMovies.setAdapter(moviesAdapter);
            moviesList = savedInstanceState.getParcelableArrayList(STATE_KEY);
            moviesAdapter.swapMovie(moviesList);
            /*
            savedRecyclerLayoutState = savedInstanceState.getParcelable(RECYCLER_MOVIES);
            if(savedRecyclerLayoutState!=null)
            {
                mainBinding.recyclerviewMovies.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
            }*/
        }
        else
        {
            makeTheMovieDbSearchQuery();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        //outState.putParcelable(RECYCLER_MOVIES, mainBinding.recyclerviewMovies.getLayoutManager().onSaveInstanceState());
        outState.putParcelableArrayList(STATE_KEY, moviesList);
    }


    /*
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null)
        {
            savedRecyclerLayoutState = savedInstanceState.getParcelable(RECYCLER_MOVIES);
        }
    }*/



    private void setupSharedPreferences()
    {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        SORT_PARAM = sharedPreferences.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_sort_most_popular_value));
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals(getString(R.string.pref_sort_key)))
        {
            SORT_PARAM = sharedPreferences.getString(getString(R.string.pref_sort_key),
                    getString(R.string.pref_sort_most_popular_value));
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
        if (SORT_PARAM.equals(getString(R.string.pref_sort_favorites_value)))
        {
            favoritesAdapter = new FavoritesAdapter(this, this);
            mainBinding.recyclerviewMovies.setAdapter(favoritesAdapter);
            getSupportLoaderManager().initLoader(ID_FAVORITES_LOADER, null, this);
        }
        else
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

            moviesAdapter = new MoviesAdapter(this, this);
            mainBinding.recyclerviewMovies.setAdapter(moviesAdapter);

            // Initialize the loader
            getSupportLoaderManager().initLoader(THE_MOVIE_DB_LOADER, null, this);

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
    }


    private void showJsonDataView()
    {
        mainBinding.tvErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mainBinding.recyclerviewMovies.setVisibility(View.VISIBLE);
    }


    private void showErrorMessage()
    {
        mainBinding.recyclerviewMovies.setVisibility(View.INVISIBLE);
        mainBinding.tvErrorMessageDisplay.setVisibility(View.VISIBLE);
    }


    @NonNull
    @Override
    public Loader onCreateLoader(int loaderId, final Bundle args)
    {
        switch (loaderId) {

            case THE_MOVIE_DB_LOADER:

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

            case ID_FAVORITES_LOADER:
                /* URI for all rows of movie data in our movies table */
                Uri moviesQueryUri = MoviesContract.MovieEntry.CONTENT_URI;

                String sortOrder =  MoviesContract.MovieEntry.COLUMN_VOTE_AVERAGE + " DESC";
                /*
                 * A SELECTION in SQL declares which rows you'd like to return. In our case, we
                 * want all movie data. We created a handy method to do that in our MoviesEntry class.
                 */

                return new CursorLoader(this,
                        moviesQueryUri,
                        MAIN_MOVIES_PROJECTION,
                        null,
                        null,
                        sortOrder);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }


    @Override
    public void onLoadFinished(@NonNull Loader loader, Object data)
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
            switch (loader.getId())
            {
                case THE_MOVIE_DB_LOADER:
                    showJsonDataView();
                    moviesList = JsonUtils.parseMoviesJson(data.toString());
                    moviesAdapter.swapMovie(moviesList);
                    break;

                case ID_FAVORITES_LOADER:
                    favoritesAdapter.swapMovie((Cursor) data);
                    if (((Cursor) data).getCount() != 0) showJsonDataView();
                    mCursor = (Cursor) data;
                    break;

                default:
                    throw new RuntimeException("Loader Not Implemented: " + loader.getId());
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader)
    {
        switch (loader.getId())
        {
            case THE_MOVIE_DB_LOADER:
                moviesAdapter.swapMovie(null);
                break;

            case ID_FAVORITES_LOADER:
                favoritesAdapter.swapMovie(null);
                break;

            default:
                throw new RuntimeException("Loader Not Implemented: " + loader.getId());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.sort_menu, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (SORT_PARAM.equals(getString(R.string.pref_sort_most_popular_value)))
        {
            menu.findItem(R.id.most_popular).setChecked(true);
        }
        else  if (SORT_PARAM.equals(getString(R.string.pref_sort_top_rated_value)))
        {
            menu.findItem(R.id.top_rated).setChecked(true);
        }
        else
        {
            menu.findItem(R.id.favorites).setChecked(true);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor mPrefsEditor = sharedPreferences.edit();

        switch (item.getItemId())
        {
            case R.id.most_popular:
                SORT_PARAM = getString(R.string.pref_sort_most_popular_value);
                break;

            case R.id.top_rated:
                SORT_PARAM = getString(R.string.pref_sort_top_rated_value);
                break;

            case R.id.favorites:
                SORT_PARAM =  getString(R.string.pref_sort_favorites_value);
                break;
        }

        mPrefsEditor.putString(getString(R.string.pref_sort_key), SORT_PARAM);
        mPrefsEditor.apply();

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClickMovieAdapter(int clickedItemIndex)
    {
        Movies movies = moviesList.get(clickedItemIndex);
        Intent intent = new Intent(getBaseContext(), MovieDetailActivity.class);
        intent.putExtra("movies",movies);
        startActivity(intent);
    }

    @Override
    public void onClickFavoritesAdapter(int clickedItemIndex)
    {
        mCursor.moveToPosition(clickedItemIndex);

        Movies mFilm = new Movies();
        mFilm.setMovieID(mCursor.getString(MainScreen.INDEX_MOVIE_ID));
        mFilm.setOriginalTitle(mCursor.getString(MainScreen.INDEX_ORIGINAL_TITLE));
        mFilm.setPosterPath(mCursor.getString(MainScreen.INDEX_POSTER_PATH));
        mFilm.setOverview(mCursor.getString(MainScreen.INDEX_OVERVIEW));
        mFilm.setVoteAverage(mCursor.getString(MainScreen.INDEX_VOTE_AVERAGE));
        mFilm.setReleaseDate(mCursor.getString(MainScreen.INDEX_RELEASE_DATE));
        mFilm.setBackdropPath(mCursor.getString(MainScreen.INDEX_BACKDROP_PATH));

        Intent intent = new Intent(getBaseContext(), MovieDetailActivity.class);
        intent.putExtra("movies",mFilm);
        startActivity(intent);
    }
}