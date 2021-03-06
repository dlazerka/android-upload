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

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Dzmitry Lazerka
 */
@Singleton
public class GetUploadUrlServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();

        URI uri;
        try {
            uri = new URI(req.getRequestURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Make callback URL relative to
        String callbackUrl = uri.resolve(ServletContextListener.BLOB_UPLOADED_CALLBACK).toString();

        String uploadUrl = blobstoreService.createUploadUrl(callbackUrl);
        resp.addHeader("Content-Type", "text/plain");
        resp.getWriter().write(uploadUrl);
    }
}
