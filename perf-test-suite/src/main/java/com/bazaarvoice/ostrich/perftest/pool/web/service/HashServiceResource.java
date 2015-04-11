package com.bazaarvoice.ostrich.perftest.pool.web.service;

import com.bazaarvoice.ostrich.perftest.core.utils.HashFunction;
import com.bazaarvoice.ostrich.perftest.pool.web.TransportData;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path ("/service")
public class HashServiceResource {

    @POST
    @Path ("/hash")
    public TransportData hash(@Valid TransportData transportData) {
        String output = HashFunction.valueOf(transportData.getFunction()).process(transportData.getData());
        return new TransportData(transportData, output);
    }
}
