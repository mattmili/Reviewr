package com.csci4100.reviewr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.csci4100.reviewr.helper.BookTitleHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Result extends AppCompatActivity {

    private String textResult, query;
    private static  final int LOADING_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Intent caller = getIntent();
        textResult = caller.getStringExtra("result");

        String ISBNPattern = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$";
        Pattern r = Pattern.compile(ISBNPattern);

        Matcher m = r.matcher(textResult);
        if (m.find( )) {
            query = m.group(0);
            getReviews(query);
        }else{
            query = textResult.replace(" ", "+");
            getReviews(query);
        }

    }

    public void getReviews(String key){
        new getJSONData().execute(key);
    }

    private class getJSONData extends AsyncTask<String, Void, String>{

        private Exception exception = null;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Start loading screen activity
            Intent startLoadingScreenActivity = new Intent(Result.this, LoadingScreen.class);
            startLoadingScreenActivity.putExtra("load_message", "Retrieving book reviews");
            startActivityForResult(startLoadingScreenActivity, LOADING_REQUEST_CODE);
        }

        @Override
        protected String doInBackground(String... args) {
            try{
                String JSONResult ="";
                String key = args[0];
                String url_api = "https://damp-wildwood-1388.herokuapp.com/getbook?items="+key;
                Log.d("URL", url_api);
                URL url = new URL(url_api);
                HttpURLConnection conn;
                conn = (HttpURLConnection)url.openConnection();
                int result = conn.getResponseCode();
                if(result == HttpURLConnection.HTTP_OK){
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder out = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line);
                    }
                    JSONResult = out.toString();
                }
                return JSONResult;
            }catch (Exception e){
                this.exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String json) {
            // Close loading screen activity
            finishActivity(LOADING_REQUEST_CODE);

            try {
                populateView(json);
            } catch (org.json.JSONException e) {
                Log.d("JSON Error", e.toString());
            }
            Log.d("Result", json);
        }
    }

    public void populateView(String jsonString) throws org.json.JSONException {
        final ParseDisplay display = new ParseDisplay(jsonString);

        if (!display.getNoBookResult().equals("No Books")) {
            final BookTitleHelper helperTitle = new BookTitleHelper(this, display.getTitle().replace(" ", "+"));

            ImageView bufferCover = (ImageView) findViewById(R.id.book_cover);
            bufferCover.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.logo_sample));

            final ImageView cover = (ImageView) findViewById((R.id.book_cover));

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Bitmap testTwo = helperTitle.getCoverBitmap();
                    if (testTwo == null) {

                    } else {
                        cover.setImageBitmap(testTwo);
                    }
                }
            }, 3000);

            TextView title = (TextView) findViewById(R.id.book_title);
            TextView author = (TextView) findViewById(R.id.name_of_author);
            TextView genre = (TextView) findViewById(R.id.genre_label);
            TextView rating = (TextView) findViewById(R.id.rating_label);
            TextView numRating = (TextView) findViewById(R.id.num_rating_label);

            TextView reviewLabel = (TextView) findViewById(R.id.review_label);
            ListView reviews = (ListView) findViewById(R.id.review_list);

            title.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            title.setText(display.getTitle());
            author.setText(this.getResources().getString(R.string.by) + " " + display.getAuthor());
            genre.setText(this.getResources().getString(R.string.genre) + " " + display.getGenre());
            rating.setText(this.getResources().getString(R.string.rating_start) + " " + display.getRating() + "%");
            numRating.setText(this.getResources().getString(R.string.num_rating_start) + " " + display.getNumReviews());

            if (!display.getNoReviewResult().equals("No Reviews")) {
                reviewLabel.setText("Displaying " + display.getReviews().size() + " of " + display.getNumReviews());

                class MySimpleArrayAdapter extends ArrayAdapter<String> {
                    private final Context context;
                    private final ArrayList<String> reviews;
                    private final ArrayList<String> reviewers;
                    private final ArrayList<Double> rating;

                    public MySimpleArrayAdapter(Context context, ArrayList<String> reviews, ArrayList<String> reviewers, ArrayList<Double> ratings) {
                        super(context, R.layout.row_layout, reviews);
                        this.context = context;
                        this.reviews = reviews;
                        this.rating = ratings;
                        this.reviewers = reviewers;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        LayoutInflater inflater = (LayoutInflater) context
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View rowView = inflater.inflate(R.layout.row_layout, parent, false);
                        TextView contentView = (TextView) rowView.findViewById(R.id.contentTextView);
                        TextView reviewerView = (TextView) rowView.findViewById(R.id.reviewerTextView);
                        TextView ratingView = (TextView) rowView.findViewById(R.id.reviewRatingTextView);

                        contentView.setText(reviews.get(position));
                        reviewerView.setText(getResources().getString(R.string.source).toString() + " " + reviewers.get(position));
                        ratingView.setText(getResources().getString(R.string.score).toString() + " " + Double.toString(rating.get(position)));

                        return rowView;
                    }
                }

                final ArrayList<String> tempReview = new ArrayList<>();
                final ArrayList<String> tempReviewer = new ArrayList<>();
                final ArrayList<Double> tempRatings = new ArrayList<>();
                for (int i = 0; i < display.getReviews().size(); i++) {
                    tempReview.add(display.getReviews().get(i).get(0));
                    tempReviewer.add(display.getReviews().get(i).get(1));
                    tempRatings.add(Double.parseDouble(display.getReviews().get(i).get(3)));
                }

                MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, tempReview, tempReviewer, tempRatings);
                reviews.setAdapter(adapter);

                //ListAdapter listAdapter = new ArrayAdapter<>(this, R.layout.row_layout, tempReview);
                //reviews.setAdapter(listAdapter);
                reviews.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView parent, View view, int position, long id) {
                        try {
                            String link = display.getReviews().get(position).get(2);
                            Log.d("ListClick", link);
                            if (link != null) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                                startActivity(browserIntent);
                            }
                        } catch (Exception ex) {
                            Log.println(1, "item-click-event", ex.getMessage());
                        }
                    }
                });
            } else {
                reviewLabel.setText(this.getResources().getString(R.string.no_reviews));
            }

        } else  {

            Intent noBook = new Intent(Result.this, BookEntry.class);
            startActivity(noBook);
            //TextView title = (TextView) findViewById(R.id.book_title);
            //title.setText(this.getResources().getString(R.string.no_book));
        }
    }
}