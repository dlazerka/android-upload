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

import android.net.Uri;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import rx.Observable;
import rx.Observer;
import rx.subjects.BehaviorSubject;

/**
 * Holds observables for {@link UploadStatus}, one observable per {@link Uri}.
 * All observables must be well-behaved (i.e. call onComplete() or onError() exactly once).
 * All methods are pure, so static for simplicity.
 *
 * @author Dzmitry Lazerka
 */
class UploadStatusObservables {
    private static final Logger logger = LoggerFactory.getLogger(UploadStatusObservables.class);

    /**
     * Map from file we're uploading, to subject that reports status.
     * Subjects are used as event buses to publish upload status.
     *
     * Concurrent, because getObserver/getObservable are called from different threads.
     */
    private static final ConcurrentHashMap<Uri, BehaviorSubject<UploadStatus>> statusPublishers
            = new ConcurrentHashMap<>();

    @Nonnull
    public static BehaviorSubject<UploadStatus> createSubject(@Nonnull Uri filePath) {
        return create(filePath);
    }

    @Nonnull
    private static BehaviorSubject<UploadStatus> create(@Nonnull Uri filePath) {
        BehaviorSubject<UploadStatus> subject = BehaviorSubject.create();
        return statusPublishers.putIfAbsent(filePath, subject);
    }

    /** @return Old status observable. */
    @Nullable
    public static BehaviorSubject<UploadStatus> removeSubject(@Nonnull Uri filePath) {
        return statusPublishers.remove(filePath);
    }

    /**
     * @return Status observer for this uri. If one wasn't already created, will create a new one (and record an error).
     */
    @Nonnull
    public static Observer<UploadStatus> getObserver(@Nonnull Uri filePath) {
        BehaviorSubject<UploadStatus> result = statusPublishers.get(filePath);
        if (result == null) {
            // Activity interaction got screwed up, definitely an error. Let's try to save face here.
            logger.error("Current statusObserver is null for {}," +
                    " should have been created by someone before calling this.", filePath);
            result = create(filePath);
        }

        return result;
    }

    /**
     * @return Status observable for this uri, or null if no.
     */
    @Nullable
    public static Observable<UploadStatus> getObservable(@Nonnull Uri filePath) {
        return statusPublishers.get(filePath);
    }


    /**
     * @return Status subject for this uri, or null if no.
     */
    @VisibleForTesting
    static BehaviorSubject<UploadStatus> getSubject(@Nonnull Uri filePath) {
        return statusPublishers.get(filePath);
    }
}
