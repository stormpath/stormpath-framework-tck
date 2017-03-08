package com.stormpath.tck.util

import com.fasterxml.jackson.annotation.JsonProperty

class GuerillaEmail {
    private String alias
    private String email
    private long timestamp
    private String token

    String getAlias() {
        return alias
    }

    void setAlias(String alias) {
        this.alias = alias
    }

    String getEmail() {
        return email
    }

    @JsonProperty("email_addr")
    void setEmail(String email) {
        this.email = email
    }

    long getTimestamp() {
        return timestamp
    }

    @JsonProperty("email_timestamp")
    void setTimestamp(long timestamp) {
        this.timestamp = timestamp
    }

    String getToken() {
        return token
    }

    @JsonProperty("sid_token")
    void setToken(String token) {
        this.token = token
    }
}
