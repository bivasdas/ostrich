package com.bazaarvoice.ostrich.perftest.pool.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class TransportData {
    private final String _data;
    private final String _function;
    private final String _output;
    private URI _uri;

    @JsonIgnore
    public TransportData(TransportData source, String output) {
        this._function = source.getFunction();
        this._data = source.getData();
        this._output = output;
    }

    @JsonIgnore
    public TransportData(String function, String data) {
        this._function = function;
        this._data = data;
        this._output = null;
    }

    @JsonCreator
    public TransportData(@JsonProperty ("function") String function, @JsonProperty ("data") String data, @JsonProperty ("output") String output) {
        this._function = function;
        this._data = data;
        this._output = output;
    }

    @JsonProperty ("data")
    public String getData() {
        return _data;
    }

    @JsonProperty ("function")
    public String getFunction() {
        return _function;
    }

    @JsonProperty ("output")
    public String getOutput() {
        return _output;
    }

    @JsonIgnore
    public URI getUri() {
        return _uri;
    }

    @JsonIgnore
    public TransportData withUri(URI uri) {
        this._uri = uri;
        return this;
    }
}
