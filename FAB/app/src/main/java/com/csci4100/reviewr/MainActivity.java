package com.csci4100.reviewr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.csci4100.reviewr.helper.ImageHelper;
import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_PHOTO = 0;
    private static  final int LOADING_REQUEST_CODE = 0;

    // The URI of photo taken with camera
    private Uri mUriPhotoTaken;
    private Bitmap mBitmap;
    private VisionServiceClient client;

    EditText bookToSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set app name font to custom font
        Typeface face = Typeface.createFromAsset(getAssets(),"fonts/Pacifico.ttf");
        TextView appName = (TextView) findViewById(R.id.appName);
        appName.setTypeface(face);


        if (client==null){
            client = new VisionServiceRestClient(getString(R.string.subscription_key));
        }

        Button takePhoto = (Button) findViewById(R.id.takePhotoButton);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        Button search = (Button) findViewById(R.id.searchButton);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookSearch();
            }
        });

        bookToSearch = (EditText) findViewById(R.id.bookInput);
        bookToSearch.setHint(R.string.input_hint);
        bookToSearch.setHintTextColor(getResources().getColor(R.color.icons));

    }


    // Deal with the result of selection of the photos and faces.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri imageUri;
            if (data == null || data.getData() == null) {
                imageUri = mUriPhotoTaken;
            } else {
                imageUri = data.getData();
            }

            mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, getContentResolver());
            if (mBitmap != null) {
                doRecognize();
            }
        }
    }

    public void bookSearch(){
        Intent startResultIntent = new Intent(MainActivity.this, Result.class);
        startResultIntent.putExtra("result", bookToSearch.getText().toString());
        startActivity(startResultIntent);
    }

    // Launch the camera to allow the user to take a photo
    public void takePhoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null) {
            // Save the photo taken to a temporary file.
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                mUriPhotoTaken = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                Log.d("ERROR", e.getMessage());
            }
        }
    }

    public void doRecognize() {
        try {
            new doRequest().execute();
        } catch (Exception e)
        {
            Log.d("Error encountered", " Exception is: " + e.toString());
        }
    }

    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();
        OCR ocr;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        ocr = this.client.recognizeText(inputStream, LanguageCodes.AutoDetect, true);

        String result = gson.toJson(ocr);

        return result;
    }

    private class doRequest extends AsyncTask<String, String, String> {
        private Exception e = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Start loading screen activity
            Intent startLoadingScreenActivity = new Intent(MainActivity.this, LoadingScreen.class);
            startLoadingScreenActivity.putExtra("load_message", "Detecting Photo");
            startActivityForResult(startLoadingScreenActivity, LOADING_REQUEST_CODE);
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);

            // Close loading screen activity
            finishActivity(LOADING_REQUEST_CODE);

            if (e != null) {
                this.e = null;
            } else {
                Gson gson = new Gson();
                OCR r = gson.fromJson(data, OCR.class);

                String result = "";
                for (Region reg : r.regions) {
                    for (Line line : reg.lines) {
                        for (Word word : line.words) {
                            result += word.text + " ";
                        }
                        result += "\n";
                    }
                    result += "\n\n";
                }
                Log.d("Result", result);

                if(result != "") {
                    Intent startResultIntent = new Intent(MainActivity.this, Result.class);
                    startResultIntent.putExtra("result", result);
                    startActivity(startResultIntent);
                }else {
                    Intent startBookEntryIntent = new Intent(MainActivity.this, BookEntry.class);
                    startActivity(startBookEntryIntent);
                }
            }
        }

    }
}