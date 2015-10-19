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

package name.dlazerka.androidupload.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import name.dlazerka.androidupload.R;
import name.dlazerka.androidupload.ThanksActivity;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * See {@link #createIntent} to start this activity.
 * Make sure to call {@link UploadStatusObservables#createSubject} before starting this activity.
 *
 * Public only for Android, should be package-private.
 *
 * @author Dzmitry Lazerka
 */
public class UploadActivity extends Activity {
    private static final Logger logger = LoggerFactory.getLogger(UploadActivity.class);

    public static final String EXTRA_CONTENT_TYPE = "content_type";
    public static final String EXTRA_FINISHED_INTENT = "finished_intent";
    /** Whether it should start service. Default: true.*/
    public static final String EXTRA_START_SERVICE = "start_service";

    private final UploadStatusObserver statusObserver = new UploadStatusObserver();

    private ProgressBar progressBar;
    private TextView progressPercent;
    private TextView uploadRate;

    private ChoiceFormat choiceFormat;
    private DecimalFormat rateNumberFormat;

    /**
     * Creates intent to launch this activity.
     * @param context for Intent() constructor.
     * @param filePath what to upload.
     * @param contentType what Content-Type header to set for upload to server.
     * @param finishedIntent Where to send user when upload finished.
     * @return Fully-functional intent to startActivity() with.
     */
    public static Intent createIntent(
            @Nonnull Context context,
            @Nonnull Uri filePath,
            @Nonnull String contentType,
            @Nonnull Intent finishedIntent
    ) {
        Intent intent = new Intent(context, UploadActivity.class);
        intent.setData(checkNotNull(filePath));
        intent.putExtra(EXTRA_CONTENT_TYPE, checkNotNull(contentType));
        intent.putExtra(EXTRA_FINISHED_INTENT, checkNotNull(finishedIntent));

        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); // Upload is one-off action, don't need history.

        return intent;
    }

    private Uri getFilePath() {
        return checkNotNull(getIntent().getData());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_status);

        choiceFormat = new ChoiceFormat(getString(R.string.rate_choice));
        rateNumberFormat = new DecimalFormat(getString(R.string.rate_number_format));

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressPercent = (TextView) findViewById(R.id.progress_percent);
        uploadRate = (TextView) findViewById(R.id.upload_rate);

        Uri filePath = getFilePath();

        setTitle(getString(R.string.uploading_s, filePath.getLastPathSegment()));

        // First-time start.
        if (UploadStatusObservables.getObservable(filePath) == null && savedInstanceState == null) {
            logger.info("Uploading {}", filePath);

            progressBar.setIndeterminate(true);

            UploadStatusObservables.createSubject(filePath);

            Bundle extras = checkNotNull(getIntent().getExtras());
            if (extras.getBoolean(EXTRA_START_SERVICE, true)) {
                String contentType = checkNotNull(extras.getString(EXTRA_CONTENT_TYPE),
                        EXTRA_CONTENT_TYPE + " extra is null");
                Intent finishedIntent = checkNotNull((Intent) extras.getParcelable(EXTRA_FINISHED_INTENT));

                Intent serviceIntent =
                        UploadService.createMyIntent(this, filePath, contentType, getIntent(), finishedIntent);
                startService(serviceIntent);
            }
        } else {
            logger.info("There's already upload of the same file in progress: {}", filePath);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Uri filePath = getFilePath();

        Observable<UploadStatus> uploadStatusObservable = UploadStatusObservables.getObservable(filePath);
        if (uploadStatusObservable == null) {
            // Upload have finished.
            onUploadSuccess();
        } else {
            uploadStatusObservable
                    // Upload progress events come in big chunks, throttle them a little.
                    .debounce(50, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(statusObserver);
        }
    }

    @Override
    protected void onStop() {
        statusObserver.unsubscribe();
        super.onStop();
    }

    private void onUploadSuccess() {
        logger.info("Uploaded {}", getFilePath());

        Uri filePath = getFilePath();
        BehaviorSubject<UploadStatus> old = UploadStatusObservables.removeSubject(filePath);
        if (old == null) {
            logger.warn("Strange, old status observable is null, while shouldn't ever be.");
        }

        finish();
        startActivity(ThanksActivity.createIntent(this));
    }

    private void onUploadFailure(int code, String message) {
        String text = "Error uploading: " + code + " " + message;
        logger.warn(text);
        Toast.makeText(UploadActivity.this, text, Toast.LENGTH_LONG)
                .show();

        progressPercent.setText(getString(R.string.error));
        uploadRate.setText(null);

        // Leave user staring at error message. Let them press Back once they get it.
    }

    private void onUploadProgress(UploadProgress progress) {
        float fraction = progress.getFraction();
        int percent = Math.round(fraction * 100);

        if (percent >= 100) {
            progressPercent.setText(getString(R.string.uploading));
            progressBar.setIndeterminate(true);
        } else {
            String humanReadablePerSecond = getHumanReadablePerSecond(progress.getBytesPerSecond());
            progressBar.setIndeterminate(false);
            progressPercent.setText(getString(R.string.percent, percent));
            uploadRate.setText(humanReadablePerSecond);
            progressBar.setProgress(percent);
        }
    }

    private String getHumanReadablePerSecond(float bytesPerSecond) {
        // Example: "%s MB/s"
        String format1 = choiceFormat.format(Math.round(bytesPerSecond));

        if (bytesPerSecond >= 1000_000) {
            bytesPerSecond /= 1000_000;
        } else if (bytesPerSecond >= 1000) {
            bytesPerSecond /= 1000;
        }
        // Example: "12.3"
        String number = rateNumberFormat.format(bytesPerSecond);

        return String.format(format1, number);
    }

    private class UploadStatusObserver extends Subscriber<UploadStatus> {
        @Override
        public void onCompleted() {
            // See onNext() instead.
            logger.error("onCompleted() aren't to be called, because we want to know Response.");
        }

        @Override
        public void onError(Throwable e) {
            logger.warn("onError {}", e.getMessage());
            onUploadFailure(0, e.getMessage());
        }

        @Override
        public void onNext(UploadStatus status) {
            if (status.progress != null) {
                onUploadProgress(status.progress);
            }
            else if (status.response != null) {
                if (status.response.isSuccessful()) {
                    onUploadSuccess();
                } else {
                    onUploadFailure(status.response.code(), status.response.message());
                }
            }
        }
    }
}
