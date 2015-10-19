/*
 * Copyright (c) 2015 Dzmitry Lazerka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.dlazerka.androidupload;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import name.dlazerka.androidupload.upload.UploadActivity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sample activity to show how to use Upload.
 * Allows to upload an already taken picture
 *
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_SELECT_FILE = 2;
    private Uri photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton takePicture = (ImageButton) findViewById(R.id.take_picture);
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        ImageButton selectFile = (ImageButton) findViewById(R.id.select_file);
        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent filePickerIntent = new Intent(Intent.ACTION_PICK);
                filePickerIntent.setType("image/*");
                startActivityForResult(filePickerIntent, REQUEST_SELECT_FILE);
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            String msg = "Cannot resolve activity " + takePictureIntent.getAction() +
                    "\nCheck Camera permissions.";
            logger.warn(msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // Create the File where the photo should go
        try {
            createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            String msg = "Cannot create temporary file to store photo.";
            logger.warn(msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // Continue only if the File was successfully created
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile);
        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
    }

    /** Creates a file: path for use with ACTION_VIEW intents */
    private void createImageFile() throws IOException {
        // Create an image file name
        File storageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                getPackageName() + "_",
                ".jpg",
                storageDir
        );

        photoFile = Uri.fromFile(image);

        logger.info("Created file for image: {}", photoFile);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    logger.info("Photo taken");

                    addPhotoToGallery();
                    startUpload();

                } else if (resultCode != RESULT_CANCELED) {
                    String msg = "Error taking picture: " + resultCode;
                    logger.warn(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case REQUEST_SELECT_FILE:
                if (resultCode == RESULT_OK) {
                    logger.info("File selected");

                    photoFile = checkNotNull(data.getData());
                    startUpload();

                } else {
                    String msg = "Error choosing a file: " + resultCode;
                    logger.warn(msg);
                }
                break;
            default:
                logger.warn("Not ours");
                break;
        }
    }

    private void addPhotoToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(photoFile);
        sendBroadcast(mediaScanIntent);
    }

    private void startUpload() {
        Intent finishedIntent = ThanksActivity.createIntent(this);
        Intent intent = UploadActivity.createIntent(this, photoFile, "image/jpeg", finishedIntent);
        startActivity(intent);
    }
}
