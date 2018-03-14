package com.example.android.popularmovies;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.example.android.popularmovies.databinding.ActivityDetailBinding;
import com.example.android.popularmovies.model.Movies;
import com.squareup.picasso.Picasso;

public class MovieDetailActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ActivityDetailBinding detailBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);

        ActionBar actionBar = this.getSupportActionBar();

        // Set the action bar back button to look like an up button
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        Movies movies = getIntent().getParcelableExtra("movies");

        if(movies!=null)
        {
            detailBinding.originalTitleTv.setText(movies.getOriginalTitle());
            detailBinding.voteAverageTv.setText(movies.getVoteAverage() + "/10");
            detailBinding.releaseDateTv.setText(movies.getReleaseDate());
            detailBinding.overviewTv.setText(movies.getOverview());

            Picasso.get()
                    .load(movies.getPosterPath())
                    .into(detailBinding.imageIv);
        }
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

}
