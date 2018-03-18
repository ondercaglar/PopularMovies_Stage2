package com.example.android.popularmovies;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.example.android.popularmovies.databinding.ActivityDetailBinding;
import com.example.android.popularmovies.model.Movies;
import com.example.android.popularmovies.model.MoviesContract;
import com.example.android.popularmovies.model.Reviews;
import com.example.android.popularmovies.model.Trailers;
import com.example.android.popularmovies.utilities.InternetConnectionDetector;
import com.example.android.popularmovies.utilities.JsonUtils;
import com.example.android.popularmovies.utilities.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks,
        TrailerAdapter.TrailerAdapterOnClickHandler, ReviewsAdapter.ReviewsAdapterOnClickHandler
{
    private ActivityDetailBinding detailBinding;
    private TrailerAdapter trailerAdapter;
    private ReviewsAdapter reviewsAdapter;
    private int mPosition = RecyclerView.NO_POSITION;


    private static final String QUERY_URL_VIDEOS  = "query_videos";
    private static final String QUERY_URL_REVIEWS = "query_reviews";
    private static final String YOUTUBE = "https://www.youtube.com/watch?v=";

    /*
     * This number will uniquely identify our Loader and is chosen arbitrarily.
     */
    private static final int THE_TRAILER_LOADER = 22;
    private static final int THE_REVIEW_LOADER = 20;
    private static final int ID_FAVORITES_LOADER = 67;
    private List<Trailers> trailersList;
    private List<Reviews>  reviewsList;

    private  Movies movies;
    private Cursor mCursor;
    private Uri uriForRowClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        detailBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);

        ActionBar actionBar = this.getSupportActionBar();

        // Set the action bar back button to look like an up button
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        detailBinding.imgbtnFavorite.setOnClickListener(favoritesButtonListener);
        detailBinding.imgbtnShare.setOnClickListener(shareButtonListener);

        movies = getIntent().getParcelableExtra("movies");

        if(movies!=null)
        {
            detailBinding.originalTitleTv.setText(movies.getOriginalTitle());
            detailBinding.voteAverageTv.setText(movies.getVoteAverage() + "/10");
            detailBinding.releaseDateTv.setText(movies.getReleaseDate());
            detailBinding.overviewTv.setText(movies.getOverview());

            Picasso.get()
                    .load(movies.getPosterPath())
                    .placeholder(R.drawable.user_placeholder)
                    .error(R.drawable.user_placeholder_error)
                    .into(detailBinding.imageIv);
        }


        makeTheMovieDbSearchQuery();

        LinearLayoutManager layoutManager    = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        LinearLayoutManager newLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        detailBinding.trailersInfo.recyclerviewTrailers.setLayoutManager(layoutManager);
        detailBinding.trailersInfo.recyclerviewTrailers.setHasFixedSize(true);
        trailerAdapter = new TrailerAdapter(this, this);
        detailBinding.trailersInfo.recyclerviewTrailers.setAdapter(trailerAdapter);


        detailBinding.reviewsInfo.recyclerviewRevies.setLayoutManager(newLayoutManager);
        detailBinding.reviewsInfo.recyclerviewRevies.setHasFixedSize(true);
        reviewsAdapter = new ReviewsAdapter(this, this);
        detailBinding.reviewsInfo.recyclerviewRevies.setAdapter(reviewsAdapter);


        uriForRowClicked =  MoviesContract.MovieEntry.buildMovieUriWithID(Long.parseLong(movies.getMovieID()));


        // Initialize the loader
        getSupportLoaderManager().initLoader(THE_TRAILER_LOADER,  null, this);
        getSupportLoaderManager().initLoader(THE_REVIEW_LOADER,   null, this);
        getSupportLoaderManager().initLoader(ID_FAVORITES_LOADER, null, this);

    }


    private View.OnClickListener favoritesButtonListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (mCursor.getCount() != 0)
            {
                ContentResolver reminderContentResolver = getContentResolver();
                reminderContentResolver.delete(uriForRowClicked, null, null);

                detailBinding.imgbtnFavorite.setImageResource(R.drawable.ic_favorite_border_black_48dp);

            }
            else
            {
                // Insert new favorite movie data via a ContentResolver
                // Create new empty ContentValues object
                ContentValues contentValues = new ContentValues();
                contentValues.put(MoviesContract.MovieEntry.COLUMN_MOVIE_ID,       movies.getMovieID());
                contentValues.put(MoviesContract.MovieEntry.COLUMN_ORIGINAL_TITLE, movies.getOriginalTitle());
                contentValues.put(MoviesContract.MovieEntry.COLUMN_POSTER_PATH,    movies.getPosterPath());
                contentValues.put(MoviesContract.MovieEntry.COLUMN_OVERVIEW,       movies.getOverview());
                contentValues.put(MoviesContract.MovieEntry.COLUMN_VOTE_AVERAGE,   movies.getVoteAverage());
                contentValues.put(MoviesContract.MovieEntry.COLUMN_RELEASE_DATE,   movies.getReleaseDate());
                contentValues.put(MoviesContract.MovieEntry.COLUMN_BACKDROP_PATH,  movies.getBackdropPath());
                // Insert the content values via a ContentResolver
               getContentResolver().insert(MoviesContract.MovieEntry.CONTENT_URI, contentValues);

                detailBinding.imgbtnFavorite.setImageResource(R.drawable.ic_favorite_red_500_48dp);
            }
        }
    };



    private void configureFavoriteButton()
    {
        if (mCursor.getCount() != 0)
        {
            detailBinding.imgbtnFavorite.setImageResource(R.drawable.ic_favorite_red_500_48dp);
        }
        else
        {
            detailBinding.imgbtnFavorite.setImageResource(R.drawable.ic_favorite_border_black_48dp);
        }
    }



    private View.OnClickListener shareButtonListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent shareIntent = createShareMovieIntent();
            startActivity(shareIntent);
        }
    };



    /**
     * Uses the ShareCompat Intent builder to create our Forecast intent for sharing.  All we need
     * to do is set the type, text and the NEW_DOCUMENT flag so it treats our share as a new task.
     */
    private Intent createShareMovieIntent()
    {
        Trailers trailers = trailersList.get(0);
        String mUrl = YOUTUBE + trailers.getKey();

        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setSubject(getResources().getString(R.string.txt_trailers_label))
                .setText(mUrl)
                .getIntent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        return shareIntent;
    }



    private void makeTheMovieDbSearchQuery()
    {
        // creating connection detector class instance
        InternetConnectionDetector cd = new InternetConnectionDetector(getApplicationContext());
        Boolean isInternetPresent = cd.isConnectingToInternet();

        if(!isInternetPresent)
        {
            //Toast.makeText(this, getResources().getString(R.string.no_connection), Toast.LENGTH_LONG).show();
            detailBinding.trailersInfo.txtTrailerLabel.setVisibility(View.GONE);
            detailBinding.trailersInfo.recyclerviewTrailers.setVisibility(View.GONE);
            detailBinding.reviewsInfo.txtReviewsLabel.setVisibility(View.GONE);
            detailBinding.reviewsInfo.recyclerviewRevies.setVisibility(View.GONE);
            return;
        }


        String PARAM_VIDEOS = "videos";
        URL TrailersUrl = NetworkUtils.buildUrlVideosReviews(movies.getMovieID(), PARAM_VIDEOS);
        Bundle queryVideosBundle = new Bundle();
        queryVideosBundle.putString(QUERY_URL_VIDEOS, TrailersUrl.toString());

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> trailerLoader = loaderManager.getLoader(THE_TRAILER_LOADER);
        if (trailerLoader == null)
        {
            loaderManager.initLoader(THE_TRAILER_LOADER, queryVideosBundle, this);
        }
        else
        {
            loaderManager.restartLoader(THE_TRAILER_LOADER, queryVideosBundle, this);
        }


        String PARAM_REVIEWS = "reviews";
        URL ReviewsUrl = NetworkUtils.buildUrlVideosReviews(movies.getMovieID(), PARAM_REVIEWS);
        Bundle queryReviewsBundle = new Bundle();
        queryReviewsBundle.putString(QUERY_URL_REVIEWS, ReviewsUrl.toString());

        Loader<String> reviewsLoader = loaderManager.getLoader(THE_REVIEW_LOADER);
        if (reviewsLoader == null)
        {
            loaderManager.initLoader(THE_REVIEW_LOADER, queryReviewsBundle, this);
        }
        else
        {
            loaderManager.restartLoader(THE_REVIEW_LOADER, queryReviewsBundle, this);
        }
    }


    @NonNull
    @Override
    public Loader onCreateLoader(int id, final Bundle args)
    {
        switch (id)
        {
            case ID_FAVORITES_LOADER:
                return new CursorLoader(this,
                                        uriForRowClicked,
                                        null,
                                        null,
                                        null,
                                        null);

            case THE_TRAILER_LOADER:

                return new AsyncTaskLoader<String>(this)
                {
                    String mTrailersJson;

                    @Override
                    protected void onStartLoading()
                    {
                        if (args == null)
                        {
                            return;
                        }

                        if (mTrailersJson != null)
                        {
                            deliverResult(mTrailersJson);
                        }
                        else
                        {
                            forceLoad();
                        }
                    }

                    @Override
                    public String loadInBackground()
                    {
                        String searchQueryUrlString = args.getString(QUERY_URL_VIDEOS);
                        try
                        {
                            URL TrailersUrl = new URL(searchQueryUrlString);
                            return NetworkUtils.getResponseFromHttpUrl(TrailersUrl);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    public void deliverResult(String TrailersJson)
                    {
                        mTrailersJson = TrailersJson;
                        super.deliverResult(TrailersJson);
                    }
                };

            case THE_REVIEW_LOADER:

                return new AsyncTaskLoader<String>(this)
                {
                    String mReviewsJson;

                    @Override
                    protected void onStartLoading()
                    {
                        if (args == null) {
                            return;
                        }

                        if (mReviewsJson != null)
                        {
                            deliverResult(mReviewsJson);
                        }
                        else
                        {
                            forceLoad();
                        }
                    }

                    @Override
                    public String loadInBackground()
                    {
                        String searchQueryUrlString = args.getString(QUERY_URL_REVIEWS);

                        try
                        {
                            URL ReviewsUrl = new URL(searchQueryUrlString);
                            return NetworkUtils.getResponseFromHttpUrl(ReviewsUrl);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    public void deliverResult(String ReviewsJson)
                    {
                        mReviewsJson = ReviewsJson;
                        super.deliverResult(ReviewsJson);
                    }
                };

            default:
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader loader, Object data)
    {
        if (null == data)
        {
            //Toast.makeText(this, getResources().getString(R.string.error_message), Toast.LENGTH_LONG).show();
        }
        else
        {
            switch (loader.getId())
            {
                case ID_FAVORITES_LOADER:
                    mCursor = (Cursor) data;
                    configureFavoriteButton();
                    break;

                case THE_TRAILER_LOADER:
                trailersList = JsonUtils.parseTrailersJson(data.toString());
                if (trailersList.size() == 0)
                {
                    detailBinding.trailersInfo.txtTrailerLabel.setVisibility(View.GONE);
                    detailBinding.trailersInfo.recyclerviewTrailers.setVisibility(View.GONE);
                }
                else
                {
                    detailBinding.imgbtnShare.setVisibility(View.VISIBLE);
                    trailerAdapter.swapMovie(trailersList);
                    if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;
                    detailBinding.trailersInfo.recyclerviewTrailers.smoothScrollToPosition(mPosition);
                }
                break;

                case THE_REVIEW_LOADER:
                    reviewsList = JsonUtils.parseReviewsJson(data.toString());
                    if (reviewsList.size() == 0)
                    {
                        detailBinding.reviewsInfo.txtReviewsLabel.setVisibility(View.GONE);
                        detailBinding.reviewsInfo.recyclerviewRevies.setVisibility(View.GONE);
                    }
                    else
                    {
                        reviewsAdapter.swapMovie(reviewsList);
                        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;
                        detailBinding.reviewsInfo.recyclerviewRevies.smoothScrollToPosition(mPosition);
                    }
                    break;

                default:
                    throw new RuntimeException("Loader Not Implemented: " + loader.getId());
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader)
    {
        trailerAdapter.swapMovie(null);
        reviewsAdapter.swapMovie(null);
        mCursor = null;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClickTrailer(int clickedItemIndex)
    {
        Trailers trailers = trailersList.get(clickedItemIndex);
        String url = YOUTUBE + trailers.getKey();
        openWebPage(url);
    }

    @Override
    public void onClickReview(int clickedItemIndex)
    {
        Reviews reviews = reviewsList.get(clickedItemIndex);
        String url = reviews.getUrl();
        openWebPage(url);
    }


    /**
     * This method fires off an implicit Intent to open a webpage.
     *
     * @param url Url of webpage to open.
     */
    private void openWebPage(String url)
    {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);

        /*
         * This is a check we perform with every implicit Intent that we launch. In some cases,
         * the device where this code is running might not have an Activity to perform the action
         * with the data we've specified. Without this check, in those cases your app would crash.
         */
        if (intent.resolveActivity(getPackageManager()) != null)
        {
            startActivity(intent);
        }
    }


}
