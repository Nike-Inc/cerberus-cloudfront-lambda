/*
 * Copyright 2019 Nike, Inc.
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

package com.nike.cerberus.lambda.waf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fieldju.commons.EnvUtils;
import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import okhttp3.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static okhttp3.ConnectionSpec.CLEARTEXT;
import static okhttp3.ConnectionSpec.MODERN_TLS;

public class CerberusMetadataLookup {

    private static final int DEFAULT_TIMEOUT = 60_000;
    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private static final String CERBERUS_TOKEN = "X-Cerberus-Token";
    private static final String EMAIL_SYMBOL = "@";

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Modify "MODERN_TLS" to remove TLS v1.0 and 1.1
     */
    private static final ConnectionSpec TLS_1_2_OR_NEWER = new ConnectionSpec.Builder(MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build();

    private OkHttpClient createHttpClient() {

        List<ConnectionSpec> connectionSpecs = new ArrayList<>();
        connectionSpecs.add(TLS_1_2_OR_NEWER);

        connectionSpecs.add(CLEARTEXT);

        return new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .connectionSpecs(connectionSpecs)
                .build();
    }

    /**
     * Obtains Cerberus Metadata which is a list of SDB summaries
     */
    public ArrayList<Map<String, String>> getCerberusMetadata(String environment) {

        String cerberusUrl = String.format("https://%s.cerberus.nikecloud.com", environment);
        OkHttpClient httpClient = createHttpClient();
        String region = EnvUtils.getRequiredEnv("REGION");
        DefaultCerberusCredentialsProviderChain chain = new DefaultCerberusCredentialsProviderChain(cerberusUrl, region);

        ArrayList<Map<String, String>> sdbMetadata = new ArrayList<>();
        String offset = "0";
        Boolean hasNext;

        do {
            HashMap result = executeRequest(httpClient, chain, cerberusUrl, offset);
            sdbMetadata.addAll((ArrayList<Map<String, String>>) result.get("safe_deposit_box_metadata"));

            offset = result.get("next_offset").toString();
            hasNext = Boolean.valueOf(result.get("has_next").toString());

        } while (hasNext);

        if (sdbMetadata.isEmpty()) {
            throw new NullPointerException("SDB Metadata is empty");
        }
        return sdbMetadata;
    }

    private ArrayList<String> getOwner(ArrayList<String> owner, Map<String, String> entry) {
        owner.add(entry.get("owner"));
        if (entry.get("created_by").contains(EMAIL_SYMBOL)) owner.add(entry.get("created_by"));
        if (entry.get("last_updated_by").contains(EMAIL_SYMBOL)
                && !entry.get("last_updated_by").equals(entry.get("created_by"))) {
            owner.add(entry.get("last_updated_by"));
        }
        return owner;
    }

    public ArrayList<String> searchCerberusMetadata(ArrayList<Map<String, String>> sdbMetadata, String sdbName, String principalName) {

        if (sdbMetadata == null) {
            throw new NullPointerException("SDB Metadata is empty");
        }

        ArrayList<String> owner = new ArrayList<>();

        for (Map<String, String> entry : sdbMetadata) {
            if (entry.get("path").contains(String.format("/%s/", sdbName)) && !sdbName.isEmpty()) {
                return getOwner(owner, entry);
            }
        }

        for (Map<String, String> entry : sdbMetadata) {
            if (entry.containsValue(principalName)) {
                return getOwner(owner, entry);
            }

        }

        owner.add("No owner found");
        return owner;
    }

    private HashMap executeRequest(OkHttpClient httpClient, DefaultCerberusCredentialsProviderChain chain,
                                   String cerberusUrl, String offset) {

        HashMap result;
        try {
            Request request = new Request.Builder()
                    .url(cerberusUrl + "/v1/metadata?limit=500&offset=" + offset)
                    .addHeader(CERBERUS_TOKEN, chain.getCredentials().getToken())
                    .get()
                    .build();
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body().string();
            result = new ObjectMapper().readValue(responseBody, HashMap.class);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while communicating with Cerberus", e);
        }

        return result;
    }
}
