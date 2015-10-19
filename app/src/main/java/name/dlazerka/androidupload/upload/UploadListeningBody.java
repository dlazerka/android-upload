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

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.FileInputStream;
import java.io.IOException;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Extends regular RequestBOdy by notifying listener of UploadProgress.
 *
 * @author Dzmitry Lazerka
 */
class UploadListeningBody extends RequestBody {
    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final ParcelFileDescriptor fd;
    private final MediaType contentType;
    private final PublishSubject<UploadProgress> progressSubject;

    /**
     * @param fd opened file descriptor. It's caller responsibility to close it.
     */
    public UploadListeningBody(ParcelFileDescriptor fd, String contentType) {
        this.fd = fd;
        this.contentType = MediaType.parse(contentType);
        this.progressSubject = PublishSubject.create();
    }

    /** Returns an observable that you can subscribe to to get updates. */
    public Observable<UploadProgress> getProgressObservable() {
        return progressSubject;
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = Okio.source(new FileInputStream(fd.getFileDescriptor()));
        //noinspection TryFinallyCanBeTryWithResources, API 16 doesn't support it
        try {
            long transferred = 0;
            long length = fd.getStatSize();
            long startedAt = SystemClock.uptimeMillis();

            long read;
            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                transferred += read;
                sink.flush(); // important

                progressSubject.onNext(new UploadProgress(transferred, length, startedAt));
            }
            sink.writeAll(source);
            sink.flush();
            progressSubject.onCompleted();
        } catch (Exception e) {
            progressSubject.onError(e);
            throw e;
        } finally {
            source.close();
        }
    }

    @Override
    public long contentLength() throws IOException {
        return fd.getStatSize();
    }
}
