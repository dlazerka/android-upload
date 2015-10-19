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

import android.os.SystemClock;

/**
 * @author Dzmitry Lazerka
 */
class UploadProgress {
    private final long transferred;
    private final long total;
    private final long startedAtMs;

    public UploadProgress(long transferred, long total, long startedAtMs) {
        this.transferred = transferred;
        this.total = total;
        this.startedAtMs = startedAtMs;
    }

    public float getFraction() {
        return transferred / (float) total;
    }

    public float getBytesPerSecond() {
        long current = SystemClock.uptimeMillis();
        long elapsedMs = current - startedAtMs;
        return (float) transferred * 1000 / elapsedMs;
    }
}
