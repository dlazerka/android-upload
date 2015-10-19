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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import name.dlazerka.androidupload.Application;
import name.dlazerka.androidupload.R;
import rx.Observer;
import rx.functions.Action1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uploads file. Since it's IntentService, runs in background, one instance per upload.
 *
 * See {@link #createMyIntent} to start this service.
 * Make sure to call {@link UploadStatusObservables#createSubject} before starting this service.
 *
 * Public only for Android, should be package-private.
 *
 * @author Dzmitry Lazerka
 */
public class UploadService extends IntentService {
    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    // Public to be able to create custom intent, not using {@link #createMyIntent}.
    public static final String EXTRA_CONTENT_TYPE = "content_type";
    public static final String EXTRA_PROGRESS_INTENT = "progress_intent";
    public static final String EXTRA_FINISHED_INTENT = "finished_intent";

    private Uri filePath;
    private String contentType;
    private Intent progressIntent;
    private Intent finishedIntent;

    private final OkHttpClient uploadClient;

    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private Observer<UploadStatus> statusObserver;

    public UploadService() {
        super(UploadService.class.getSimpleName());
        setIntentRedelivery(true);

        uploadClient = Application.okHttpClient.clone();
        uploadClient.setWriteTimeout(0, TimeUnit.DAYS);// never
        uploadClient.setReadTimeout(0, TimeUnit.DAYS);// never
    }

    /**
     * Creates intent to launch this service.
     * @param context for Intent() constructor.
     * @param filePath what to upload.
     * @param contentType what Content-Type header to set for upload to server.
     * @param progressIntent Where to send user that taps Notification when upload is in progress.
     * @param finishedIntent Where to send user that taps Notification when upload finished.
     * @return Fully-functional intent to startService() with.
     */
    public static Intent createMyIntent(
            @Nonnull Context context,
            @Nonnull Uri filePath,
            @Nonnull String contentType,
            @Nonnull Intent progressIntent,
            @Nonnull Intent finishedIntent
    ) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setData(checkNotNull(filePath));
        intent.putExtra(EXTRA_CONTENT_TYPE, checkNotNull(contentType));
        intent.putExtra(EXTRA_PROGRESS_INTENT, checkNotNull(progressIntent));
        intent.putExtra(EXTRA_FINISHED_INTENT, checkNotNull(finishedIntent));
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.upload);
        notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.upload_gray)
                .setLargeIcon(bitmap)
                .setContentTitle(getString(R.string.app_name_long));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        filePath = checkNotNull(intent.getData());
        Bundle extras = checkNotNull(intent.getExtras());
        contentType = checkNotNull(extras.getString(EXTRA_CONTENT_TYPE));
        progressIntent = (Intent) checkNotNull(extras.getParcelable(EXTRA_PROGRESS_INTENT));
        finishedIntent = (Intent) checkNotNull(extras.getParcelable(EXTRA_FINISHED_INTENT));

        logger.info("Uploading {} {}", contentType, filePath);

        statusObserver = UploadStatusObservables.getObserver(filePath);

        try {
            uploadFile();

            // Had we call onCompleted(), then UploadActivity might never had a chance to get Response.
            // statusObserver.onCompleted();
        } catch (IOException e) {
            logger.warn("IOException: {}", e.getMessage(), e);
            statusObserver.onError(e);
        }
    }

    private String loadUploadUrl() throws IOException {
        String url = getResources().getString(R.string.get_upload_url);
        Request request = new Request.Builder().url(url).build();
        Response response = Application.okHttpClient.newCall(request).execute();
        return response.body().string();
    }

    private void uploadFile() throws IOException {
        showProgressNotification();

        String uploadUrl = loadUploadUrl();

        ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(filePath, "r");
        if (fd == null) {
            logger.error("fd is null for {}", filePath);
            return;
        }

        try {
            List<String> pathSegments = filePath.getPathSegments();
            String fileName = pathSegments.get(pathSegments.size() - 1);

            UploadListeningBody body = new UploadListeningBody(fd, contentType);
            body.getProgressObservable().subscribe(new Action1<UploadProgress>() {
                @Override
                public void call(UploadProgress progress) {
                    statusObserver.onNext(new UploadStatus(progress));
                    updateNotification(progress);
                }
            });

            RequestBody requestBody = new MultipartBuilder()
                    .addFormDataPart(Application.UPLOAD_FORM_PARAM, fileName, body)
                    .build();

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();

            Response response = uploadClient.newCall(request).execute();

            statusObserver.onNext(new UploadStatus(response));

            showSuccessNotification();
        } finally {
            fd.close();
        }
    }

    private void showProgressNotification() {
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, progressIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder
                .setContentText(getString(R.string.uploading_s, filePath.getLastPathSegment()))
                .setProgress(100, 0, true)
                .setContentIntent(pendingIntent);
        notificationManager.notify(Application.NOTIFICATION_ID_UPLOAD, notificationBuilder.build());
    }

    private void updateNotification(UploadProgress progress) {
        notificationBuilder.setProgress(100, Math.round(progress.getFraction() * 100), false);
        notificationManager.notify(Application.NOTIFICATION_ID_UPLOAD, notificationBuilder.build());
    }

    private void showSuccessNotification() {
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, finishedIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = notificationBuilder
                .setSubText(getString(R.string.uploaded_s, filePath.getLastPathSegment()))
                .setContentText(getString(R.string.thanks))
                .setProgress(0, 0, false)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(Application.NOTIFICATION_ID_UPLOAD, notification);
    }
}
