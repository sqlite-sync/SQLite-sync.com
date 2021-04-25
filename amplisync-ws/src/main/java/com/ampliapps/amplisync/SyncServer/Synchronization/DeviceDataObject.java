package com.ampliapps.amplisync.SyncServer.Synchronization;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

/*************************************************************************
 *
 * CONFIDENTIAL
 * __________________
 *
 *  AMPLIFIER sp. z o.o.
 *  www.ampliapps.com
 *  support@ampliapps.com
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AMPLIFIER sp. z o.o. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to AMPLIFIER sp. z o.o.
 * and its suppliers and may be covered by U.S., European and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AMPLIFIER sp. z o.o..
 **************************************************************************/

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "subscriber",
        "content",
        "version"
})
public class DeviceDataObject {

    @JsonProperty("subscriber")
    private String subscriber;
    @JsonProperty("content")
    private String content;
    @JsonProperty("version")
    private String version;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("subscriber")
    public String getSubscriber() {
        return subscriber;
    }

    @JsonProperty("subscriber")
    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(String content) {
        this.content = content;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
