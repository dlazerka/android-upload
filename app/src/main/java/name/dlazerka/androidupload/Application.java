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

import com.squareup.okhttp.OkHttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dzmitry Lazerka
 */
public class Application extends android.app.Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static final String UPLOAD_FORM_PARAM = "photo";

    public static final int NOTIFICATION_ID_UPLOAD = 1;

    public static OkHttpClient okHttpClient;

    @Override
    public void onCreate() {
        // Not visible by default, set "adb shell setprop log.tag.MyAppTag VERBOSE" to configure.
        logger.trace("onCreate");

        super.onCreate();

        okHttpClient = new OkHttpClient();
    }
}
