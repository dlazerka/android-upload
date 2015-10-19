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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.test.ActivityUnitTestCase;
import android.widget.ProgressBar;

import name.dlazerka.androidupload.R;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * @author Dzmitry Lazerka
 */
public class UploadActivityTest extends ActivityUnitTestCase<UploadActivity> {

    private static final Uri FILE_PATH = Uri.parse("content:///some/path.jpg");

    public UploadActivityTest() {
        super(UploadActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getContext();
        Intent intent = UploadActivity.createIntent(context, FILE_PATH, "image/jpeg", new Intent("dummy"));
        intent.putExtra(UploadActivity.EXTRA_START_SERVICE, false);
        startActivity(intent, null, null);
    }

    @Override
    protected void tearDown() throws Exception {
        UploadStatusObservables.removeSubject(FILE_PATH);

        super.tearDown();
    }

    /**
     * Tests progress wheel stops spinning upon receiving a progress event.
     */
    public void testProgressBar() throws Exception {
        getActivity().onStart();

        final ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progress_bar);

        waitFor(new Condition() {
            @Override
            public boolean check() {
                return !progressBar.isIndeterminate();
            }
        }, 10000);

        UploadProgress progress = new UploadProgress(10, 1000, SystemClock.uptimeMillis() - 1000);
        UploadStatusObservables.getObserver(FILE_PATH).onNext(new UploadStatus(progress));

        assertThat(UploadStatusObservables.getSubject(FILE_PATH).hasCompleted(), is(false));

        waitFor(new Condition() {
            @Override
            public boolean check() {
                return progressBar.isIndeterminate();
            }
        }, 10000);
        assertThat(progressBar.getProgress(), is(not(0)));
    }

    private void waitFor(Condition function, int millis) throws InterruptedException {
        long started = System.currentTimeMillis();
        while (function.check()) {
            Thread.sleep(100);
            if (System.currentTimeMillis() - started > millis) {
                fail("Waited for " + millis + "ms, but condition is still not reached.");
            }
        }
    }

    interface Condition {
        boolean check();
    }
}
