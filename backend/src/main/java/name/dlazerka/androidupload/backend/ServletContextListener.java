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

package name.dlazerka.androidupload.backend;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.ObjectifyService;

import javax.inject.Singleton;

/**
 * @author Dzmitry Lazerka
 */
public class ServletContextListener extends GuiceServletContextListener {

    public static final String BLOB_UPLOADED_CALLBACK = "/admin/uploaded-callback";

    @Override
    protected Injector getInjector() {

        ObjectifyService.register(Upload.class);

        return Guice.createInjector(new GuiceModule());
    }

    private static class GuiceModule  extends ServletModule {
        @Override
        protected void configureServlets() {

            bind(ObjectifyFilter.class).in(Singleton.class);
            filter("/*").through(ObjectifyFilter.class);

            serve("/status").with(StatusServlet.class);

            serve("/get-upload-url").with(GetUploadUrlServlet.class);
            serve(BLOB_UPLOADED_CALLBACK)
                    .with(UploadedCallbackServlet.class);
        }
    }
}
