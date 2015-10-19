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
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * What we've got from Blobstore upload callback.
 *
 * @author Dzmitry Lazerka
 */
@Entity
public class Upload {
    @Id
    private Long id;

    private Map<String, List<BlobInfo>> blobInfos;

    @Index
    private Date savedAt;
    private Map<String, List<String>> headers;

    @Index
    private String ip;

    private Upload() {}

    public Upload(
            Map<String, List<BlobInfo>> blobInfos,
            Map<String, List<String>> headers,
            String ip) {
        this.blobInfos = blobInfos;
        this.headers = headers;
        this.ip = ip;
    }

    @OnSave
    private void onSave() {
        savedAt = new Date();
    }

    public Long getId() {
        return id;
    }

    public Map<String, List<BlobInfo>> getBlobInfos() {
        return blobInfos;
    }

    public Date getSavedAt() {
        return savedAt;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getIp() {
        return ip;
    }
}
