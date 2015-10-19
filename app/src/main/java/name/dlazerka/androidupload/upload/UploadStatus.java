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


import com.squareup.okhttp.Response;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exactly one field is not null.
 *
 * @author Dzmitry Lazerka
 */
class UploadStatus {
    public final UploadProgress progress;
    public final Response response;

    public UploadStatus(@Nonnull UploadProgress progress) {
        this(checkNotNull(progress), null);
    }

    public UploadStatus(@Nonnull Response response) {
        this(null, checkNotNull(response));
    }

    private UploadStatus(UploadProgress progress, Response response) {
        this.response = response;
        this.progress = progress;
    }
}
