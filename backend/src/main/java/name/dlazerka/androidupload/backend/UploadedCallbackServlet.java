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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Just saves the received Blob.
 * This servlet must be as simple as possible, because if we get an exception here, we'll lose the callback.
 *
 * @author Dzmitry Lazerka
 */
@Singleton
public class UploadedCallbackServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(UploadedCallbackServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Usually:
        // Content-Type
        // X-Zoo
        // User-Agent
        // Host
        // X-AppEngine-BlobUpload
        // X-AppEngine-BlobChunkSize
        // X-AppEngine-BlobSize
        // X-Google-Apps-Metadata
        // X-AppEngine-Default-Namespace
        // X-AppEngine-Country
        @SuppressWarnings("unchecked")
        List<String> headerNames = Collections.list(req.getHeaderNames());

        //
        if (!headerNames.contains("X-AppEngine-BlobUpload")) {
            logger.warn("No header X-AppEngine-BlobUpload, ip: {}, headerNames: {}", req.getRemoteAddr(), headerNames);
            resp.sendError(403, "Forbidden");
            return;
        }

        // Save all those headers just in case.
        Map<String, List<String>> headers = new HashMap<>();
        for (String headerName : headerNames) {
            @SuppressWarnings("unchecked")
            ArrayList<String> value = Collections.list(req.getHeaders(headerName));
            headers.put(headerName, value);
        }

        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        Map<String, List<BlobInfo>> blobs = blobstoreService.getBlobInfos(req);

        Upload upload = new Upload(blobs, headers, req.getRemoteAddr());
        ofy().save().entity(upload).now();

        logger.info("Saved " + upload.getId());

        resp.setStatus(200);
    }
}
