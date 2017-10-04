/**
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial creation
 */
package org.eclipse.hono.deviceregistry;

import static org.eclipse.hono.service.http.HttpEndpointUtils.CONTENT_TYPE_JSON;
import static org.eclipse.hono.util.CredentialsConstants.*;
import static org.eclipse.hono.util.RequestResponseApiConstants.FIELD_DEVICE_ID;

import io.vertx.core.json.JsonArray;
import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.service.credentials.CredentialsHttpEndpoint;
import org.eclipse.hono.util.CredentialsConstants;
import org.junit.*;
import org.junit.runner.RunWith;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests the Credentials REST Interface of the {@link DeviceRegistryRestServer}.
 * Currently limited to the POST method only.
 */
@RunWith(VertxUnitRunner.class)
public class CredentialsRestServerTest {

    private static final String HOST = InetAddress.getLoopbackAddress().getHostAddress();
    private static final String TENANT = "testTenant";
    private static final String TEST_DEVICE_ID = "4711";
    private static final String TEST_AUTH_ID = "sensor20";
    private static final long TEST_TIMEOUT_MILLIS = 500;
    private static final Vertx vertx = Vertx.vertx();
    private static FileBasedCredentialsService credentialsService;
    private static DeviceRegistryRestServer deviceRegistryRestServer;

    /**
     * Deploys the server to vert.x.
     * 
     * @param context The vert.x test context.
     */
    @BeforeClass
    public static void setUp(final TestContext context) {

        final ServiceConfigProperties restServerProps = new ServiceConfigProperties();
        restServerProps.setInsecurePortEnabled(true);
        restServerProps.setInsecurePort(0);
        restServerProps.setInsecurePortBindAddress(HOST);

        final CredentialsHttpEndpoint credentialsHttpEndpoint = new CredentialsHttpEndpoint(vertx);
        deviceRegistryRestServer = new DeviceRegistryRestServer();
        deviceRegistryRestServer.addEndpoint(credentialsHttpEndpoint);
        deviceRegistryRestServer.setConfig(restServerProps);

        final FileBasedCredentialsConfigProperties credentialsServiceProps = new FileBasedCredentialsConfigProperties();
        credentialsService = new FileBasedCredentialsService();
        credentialsService.setConfig(credentialsServiceProps);

        final Future<String> restServerDeploymentTracker = Future.future();
        vertx.deployVerticle(deviceRegistryRestServer, restServerDeploymentTracker.completer());
        final Future<String> credentialsServiceDeploymentTracker = Future.future();
        vertx.deployVerticle(credentialsService, credentialsServiceDeploymentTracker.completer());

        CompositeFuture.all(restServerDeploymentTracker, credentialsServiceDeploymentTracker)
                .setHandler(context.asyncAssertSuccess());

    }

    /**
     * Shuts down the server.
     * 
     * @param context The vert.x test context.
     */
    @AfterClass
    public static void tearDown(final TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    /**
     * Removes all entries from the Credentials service.
     */
    @After
    public void clearRegistry() {
        credentialsService.clear();
    }

    private int getPort() {
        return deviceRegistryRestServer.getInsecurePort();
    }

    /**
     * Verify that a correctly filled json payload to add credentials is responded with {@link HttpURLConnection#HTTP_CREATED}
     * and an empty response message.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testAddCredentials(final TestContext context)  {
        final String requestUri = buildCredentialsPostUri();

        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);

        final Async async = context.async();
        vertx.createHttpClient().post(getPort(), HOST, requestUri).putHeader("Content-Type", CONTENT_TYPE_JSON)
                .handler(response -> {
                    context.assertEquals(HttpURLConnection.HTTP_CREATED, response.statusCode());
                    response.bodyHandler(totalBuffer -> {
                        context.assertTrue(totalBuffer.toString().isEmpty());
                        async.complete();
                    });
                }).exceptionHandler(context::fail).end(requestBodyAddCredentials.encodePrettily());
    }

    /**
     * Verify that a correctly filled json payload to add credentials for an already existing record is
     * responded with {@link HttpURLConnection#HTTP_CONFLICT} and a non empty error response message.
     .
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testAddCredentialsConflictReported(final TestContext context)  {
        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);
        final Future<Integer> addCredentialsFuture = Future.future();
        addCredentials(requestBodyAddCredentials, addCredentialsFuture);

        final String requestUri = buildCredentialsPostUri();

        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());

        addCredentialsFuture.compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            // now try to add credentials again
            vertx.createHttpClient().post(getPort(), HOST, requestUri).putHeader("content-type", CONTENT_TYPE_JSON)
                    .handler(response -> {
                        context.assertEquals(HttpURLConnection.HTTP_CONFLICT, response.statusCode());
                        done.complete();
                    }).exceptionHandler(done::fail).end(requestBodyAddCredentials.encodePrettily());
        }, done);
    }

    /**
     * Verify that a Content-Type for form-urlencoded data is not accepted and responded with {@link HttpURLConnection#HTTP_BAD_REQUEST}
     * and a non empty error response message.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testAddCredentialsWrongContentType(final TestContext context)  {
        final String requestUri = buildCredentialsPostUri();
        final String contentType = "application/x-www-form-urlencoded";

        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);

        final Async async = context.async();
        final int expectedStatus = HttpURLConnection.HTTP_BAD_REQUEST;

        postPayloadAndExpectErrorResponse(context, async, requestUri, contentType, requestBodyAddCredentials, expectedStatus);
    }

    /**
     * Verify that an empty json payload to add credentials is not accepted and responded with {@link HttpURLConnection#HTTP_BAD_REQUEST}
     * and a non empty error response message.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testAddCredentialsWrongJsonPayloadEmpty(final TestContext context) {
        final String requestUri = buildCredentialsPostUri();

        final JsonObject requestBodyAddCredentials = new JsonObject();

        final Async async = context.async();
        final int expectedStatus = HttpURLConnection.HTTP_BAD_REQUEST;

        postPayloadAndExpectErrorResponse(context, async, requestUri, CONTENT_TYPE_JSON, requestBodyAddCredentials, expectedStatus);
    }

    /**
     * Verify that a json payload to add credentials that does not contain a {@link CredentialsConstants#FIELD_DEVICE_ID}
     * is not accepted and responded with {@link HttpURLConnection#HTTP_BAD_REQUEST}
     * and a non empty error response message.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testAddCredentialsWrongJsonPayloadPartsMissingDeviceId(final TestContext context) {
        testPostWithMissingPayloadParts(context, FIELD_DEVICE_ID);
    }

    /**
     * Verify that a json payload to add credentials that does not contain a {@link CredentialsConstants#FIELD_TYPE}
     * is not accepted and responded with {@link HttpURLConnection#HTTP_BAD_REQUEST}
     * and a non empty error response message.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testAddCredentialsWrongJsonPayloadPartsMissingType(final TestContext context) {
        testPostWithMissingPayloadParts(context, FIELD_TYPE);
    }

    /**
     * Verify that a json payload to add credentials that does not contain a {@link CredentialsConstants#FIELD_AUTH_ID}
     * is not accepted and responded with {@link HttpURLConnection#HTTP_BAD_REQUEST}
     * and a non empty error response message.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testAddCredentialsWrongJsonPayloadPartsMissingAuthId(final TestContext context) {
        testPostWithMissingPayloadParts(context, FIELD_AUTH_ID);
    }

    /**
     * Verify that a correctly added credentials record can be successfully deleted again by using the device-id.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testRemoveAddedCredentialsByDeviceId(final TestContext context) {
        final String requestUri = String.format("/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT, TEST_DEVICE_ID);

        addAndRemoveCredentialsAgain(context, requestUri, HttpURLConnection.HTTP_NO_CONTENT);
    }

    /**
     * Verify that a correctly added credentials record can be successfully deleted again by using the type and authId.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testRemoveAddedCredentialsByTypeAndAuthId(final TestContext context) {
        final String requestUri = String.format("/%s/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                TEST_AUTH_ID, SECRETS_TYPE_HASHED_PASSWORD);

        addAndRemoveCredentialsAgain(context, requestUri, HttpURLConnection.HTTP_NO_CONTENT);
    }

    /**
     * Verify that a correctly added credentials record can not be deleted by using the correct authId but a non matching type.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testRemoveAddedCredentialsByNonExistingTypeButWithAuthId(final TestContext context) {
        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);

        final Future<Integer> addCredentialsFuture = Future.future();
        addCredentials(requestBodyAddCredentials, addCredentialsFuture);

        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());
        final String deleteRequestUri = String.format("/%s/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                TEST_AUTH_ID, "notExistingType");

        addCredentialsFuture.compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            // now try to remove credentials again
            vertx.createHttpClient().delete(getPort(), HOST, deleteRequestUri)
                    .handler(response -> {
                        context.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
                        done.complete();
                    }).exceptionHandler(done::fail).end();
        }, done);
    }

    /**
     * Verify that a non existing credentials record cannot be successfully deleted by using the device-id.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testRemoveNonExistingCredentialsByDeviceId(final TestContext context) {
        final String requestUri = String.format("/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT, TEST_DEVICE_ID);
        final Async async = context.async();

        vertx.createHttpClient().delete(getPort(), HOST, requestUri)
                .handler(response -> {
                    context.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
                    async.complete();
                }).exceptionHandler(context::fail).end();
    }

    /**
     * Verify that a correctly added credentials record can be successfully looked up again by using the type and authId.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testGetAddedCredentials(final TestContext context)  {
        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);
        final Future<Integer> addCredentialsFuture = Future.future();
        addCredentials(requestBodyAddCredentials, addCredentialsFuture);

        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());

        final String requestUri = String.format("/%s/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                TEST_AUTH_ID, SECRETS_TYPE_HASHED_PASSWORD);

        addCredentialsFuture.compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            // now try to get credentials again
            validateCredentialsGetRequest(context, requestUri, requestBodyAddCredentials, done);
        }, done);
    }

    /**
     * Verify that multiple (2) correctly added credentials records of the same authId can be successfully looked up by single
     * requests using their type and authId again.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testGetAddedCredentialsMultipleTypesSingleRequests(final TestContext context) throws InterruptedException {
        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());

        final JsonObject requestBodyAddCredentialsHashedPassword = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);

        final JsonObject requestBodyAddCredentialsPresharedKey = buildCredentialsPayloadPresharedKey(TEST_DEVICE_ID,
                TEST_AUTH_ID);

        final ArrayList<JsonObject> credentialsListToAdd = new ArrayList();
        credentialsListToAdd.add(requestBodyAddCredentialsHashedPassword);
        credentialsListToAdd.add(requestBodyAddCredentialsPresharedKey);
        addMultipleCredentials(context, credentialsListToAdd
        ).compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            Future<Void> getDone = Future.future();
            // now try to get credentials again
            final String requestUriHashedPassword = String.format("/%s/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                    TEST_AUTH_ID, SECRETS_TYPE_HASHED_PASSWORD);
            validateCredentialsGetRequest(context, requestUriHashedPassword, requestBodyAddCredentialsHashedPassword, getDone);
            return getDone;
        }).compose(ar -> {
            // now try to get the other credentials again
            final String requestUriPresharedKey = String.format("/%s/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                    TEST_AUTH_ID, SECRETS_TYPE_PRESHARED_KEY);
            validateCredentialsGetRequest(context, requestUriPresharedKey, requestBodyAddCredentialsPresharedKey, done);
        }, done);
    }

    /**
     * Verifies that the service returns all credentials registered for a given device regardless of authentication identifier.
     * <p>
     * The returned JsonObject must consist of the total number of entries and contain all previously added credentials
     * in the provided JsonArray that is found under the key of the endpoint {@link CredentialsConstants#CREDENTIALS_ENDPOINT}.
     * 
     * @param context The vert.x test context.
     * @throws InterruptedException if registration of credentials is interrupted.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testGetCredentialsForDeviceRegardlessOfAuthId(final TestContext context) throws InterruptedException {

        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());

        final List<JsonObject> credentialsListToAdd = new ArrayList<>();
        credentialsListToAdd.add(buildCredentialsPayloadPresharedKey(TEST_DEVICE_ID, "auth"));
        credentialsListToAdd.add(buildCredentialsPayloadPresharedKey(TEST_DEVICE_ID, "other-auth"));

        addMultipleCredentials(context, credentialsListToAdd).compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            // now try to get all these credentials again
            final String requestUriAuthId = String.format("/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                    TEST_DEVICE_ID);
            validateCredentialsGetMultipleRequest(context, requestUriAuthId, credentialsListToAdd, done);
        }, done);
    }

    /**
     * Verifies that the service returns all credentials registered for a given device regardless of type.
     * <p>
     * The returned JsonObject must consist of the total number of entries and contain all previously added credentials
     * in the provided JsonArray that is found under the key of the endpoint {@link CredentialsConstants#CREDENTIALS_ENDPOINT}.
     * 
     * @param context The vert.x test context.
     * @throws InterruptedException if registration of credentials is interrupted.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testGetCredentialsForDeviceRegardlessOfType(final TestContext context) throws InterruptedException {

        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());

        final List<JsonObject> credentialsToAdd = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            final JsonObject requestBody = buildCredentialsPayloadPresharedKey(TEST_DEVICE_ID, TEST_AUTH_ID);
            requestBody.put(FIELD_TYPE, "type" + i);
            credentialsToAdd.add(requestBody);
        }
        addMultipleCredentials(context, credentialsToAdd).compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            // now try to get all these credentials again
            final String requestUriAuthId = String.format("/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                    TEST_DEVICE_ID);
            validateCredentialsGetMultipleRequest(context, requestUriAuthId, credentialsToAdd, done);
        }, done);
    }

    /**
     * Verify that a correctly added credentials record is not found when looking it up again with a wrong type.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testGetAddedCredentialsButWithWrongType(final TestContext context)  {
        final String requestUri = String.format("/%s/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                "notExistingType", TEST_AUTH_ID);
        addAndGetCredentialsAgain(context, requestUri, HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Verify that a correctly added credentials record is not found when looking it up again with a wrong authId.
     */
    @Test(timeout = TEST_TIMEOUT_MILLIS)
    public void testGetAddedCredentialsButWithWrongAuthId(final TestContext context)  {
        final String requestUri = String.format("/%s/%s/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT,
                SECRETS_TYPE_HASHED_PASSWORD, "wrongAuthId");
        addAndGetCredentialsAgain(context, requestUri, HttpURLConnection.HTTP_NOT_FOUND);
    }

    private void addAndGetCredentialsAgain(final TestContext context, final String requestUri, final int expectedStatusCode) {
        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);
        final Future<Integer> addCredentialsFuture = Future.future();
        addCredentials(requestBodyAddCredentials, addCredentialsFuture);

        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());

        addCredentialsFuture.compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            // now try to get credentials again
            vertx.createHttpClient().get(getPort(), HOST, requestUri)
                    .handler(response -> {
                        context.assertEquals(expectedStatusCode, response.statusCode());
                        done.complete();
                    }).exceptionHandler(done::fail).end();
        }, done);
    }

    private void addAndRemoveCredentialsAgain(final TestContext context, final String requestUri, final int expectedStatusCode) {
        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);
        final Future<Integer> addCredentialsFuture = Future.future();
        addCredentials(requestBodyAddCredentials, addCredentialsFuture);

        final Future<Void> done = Future.future();
        done.setHandler(context.asyncAssertSuccess());

        addCredentialsFuture.compose(ar -> {
            context.assertTrue(ar == HttpURLConnection.HTTP_CREATED);
            // now try to remove credentials again
            vertx.createHttpClient().delete(getPort(), HOST, requestUri)
                    .handler(response -> {
                        context.assertEquals(expectedStatusCode, response.statusCode());
                        done.complete();
                    }).exceptionHandler(done::fail).end();
        }, done);
    }

    private Future<Integer> addMultipleCredentials(final TestContext context,
                                                   final List<JsonObject> credentialsList) throws InterruptedException {
        int elemCounter = 0;

        while (elemCounter < credentialsList.size()) {
            final Async batchComplete = context.async();

            final Future<Integer> addCredentialsFuture = Future.future();
            addCredentialsFuture.setHandler(handler -> {
                batchComplete.complete();
            });

            final JsonObject currentCredentialsElem = credentialsList.get(elemCounter);
            addCredentials(currentCredentialsElem, addCredentialsFuture);

            batchComplete.await(2000);
            elemCounter++;
        }

        return Future.succeededFuture(HttpURLConnection.HTTP_CREATED);
    }

    private void validateCredentialsGetRequest(final TestContext context, final String getRequestUri,
                                               final JsonObject requestBodyAddCredentialsHashedPassword, final Future<Void> validationFuture) {
        vertx.createHttpClient().get(getPort(), HOST, getRequestUri)
                .handler(response -> {
                    context.assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
                    response.bodyHandler(totalBuffer -> {
                        context.assertFalse(totalBuffer.toString().isEmpty()); // credentials object expected
                        // the answer must contain all of the payload of the add request, so test that now
                        context.assertTrue(testJsonObjectToBeContained(
                                new JsonObject(totalBuffer.toString()), requestBodyAddCredentialsHashedPassword));
                        validationFuture.complete();
                    });
                }).exceptionHandler(validationFuture::fail).end();
    }

    private void validateCredentialsGetMultipleRequest(final TestContext context, final String getRequestUri,
                                                       final List<JsonObject> credentialsList,
                                                       final Future<Void> validationFuture) {
        vertx.createHttpClient().get(getPort(), HOST, getRequestUri)
                .handler(response -> {
                    context.assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
                    response.bodyHandler(totalBuffer -> {
                        context.assertFalse(totalBuffer.toString().isEmpty()); // credentials object expected
                        // the answer must contain all of the payload of the add request, so test that now
                        JsonObject multipleCredentialsResponse = new JsonObject(totalBuffer.toString());
                        context.assertTrue(multipleCredentialsResponse.containsKey(FIELD_CREDENTIALS_TOTAL));
                        Integer totalCredentialsFound = multipleCredentialsResponse.getInteger(FIELD_CREDENTIALS_TOTAL);
                        context.assertEquals(totalCredentialsFound, credentialsList.size());
                        context.assertTrue(multipleCredentialsResponse.containsKey(CREDENTIALS_ENDPOINT));
                        final JsonArray credentials = multipleCredentialsResponse.getJsonArray(CREDENTIALS_ENDPOINT);
                        context.assertNotNull(credentials);
                        context.assertEquals(credentials.size(), totalCredentialsFound);
                        // TODO: add full test if the lists are 'identical' (contain the same JsonObjects by using the
                        //       contained helper method)
                        validationFuture.complete();
                    });
                }).exceptionHandler(validationFuture::fail).end();
    }

    private String buildCredentialsPostUri() {
        return String.format("/%s/%s", CredentialsConstants.CREDENTIALS_ENDPOINT, TENANT);
    }

    private void postPayloadAndExpectErrorResponse(final TestContext context, final Async async, final String requestUri,
                                                   final String contentType, final JsonObject requestBody, final int expectedStatus) {
        vertx.createHttpClient().post(getPort(), HOST, requestUri).putHeader("Content-Type", contentType)
                .handler(response -> {
                    context.assertEquals(expectedStatus, response.statusCode());
                    response.bodyHandler(totalBuffer -> {
                        context.assertFalse(totalBuffer.toString().isEmpty()); // error message expected
                        async.complete();
                    });
                }).exceptionHandler(context::fail).end(requestBody.encodePrettily());
    }

    private JsonObject buildCredentialsPayloadHashedPassword(final String deviceId, final String authId) {
        final JsonObject secret = new JsonObject().
                put(FIELD_SECRETS_NOT_BEFORE, "2017-05-01T14:00:00+01:00").
                put(FIELD_SECRETS_NOT_AFTER, "2037-06-01T14:00:00+01:00").
                put(FIELD_SECRETS_HASH_FUNCTION, "sha-512").
                put(FIELD_SECRETS_SALT, "aG9ubw==").
                put(FIELD_SECRETS_PWD_HASH, "C9/T62m1tT4ZxxqyIiyN9fvoEqmL0qnM4/+M+GHHDzr0QzzkAUdGYyJBfxRSe4upDzb6TSC4k5cpZG17p4QCvA==");
        final JsonObject credPayload = new JsonObject().
                put(FIELD_DEVICE_ID, deviceId).
                put(FIELD_TYPE, SECRETS_TYPE_HASHED_PASSWORD).
                put(FIELD_AUTH_ID, authId).
                put(FIELD_SECRETS, new JsonArray().add(secret));
        return credPayload;
    }

    private JsonObject buildCredentialsPayloadPresharedKey(final String deviceId, final String authId) {
        final JsonObject secret = new JsonObject().
                put(FIELD_SECRETS_NOT_BEFORE, "2017-05-01T14:00:00+01:00").
                put(FIELD_SECRETS_NOT_AFTER, "2037-06-01T14:00:00+01:00").
                put(FIELD_SECRETS_KEY, "aG9uby1zZWNyZXQ="); // base64 "hono-secret"
        final JsonObject credPayload = new JsonObject().
                put(FIELD_DEVICE_ID, deviceId).
                put(FIELD_TYPE, SECRETS_TYPE_PRESHARED_KEY).
                put(FIELD_AUTH_ID, authId).
                put(FIELD_SECRETS, new JsonArray().add(secret));
        return credPayload;
    }

    /**
     * A simple implementation of subtree containment: all entries of the JsonObject that is tested to be contained
     * must be contained in the other JsonObject as well. Nested JsonObjects are treated the same by recursively callong
     * this method to test the containment.
     * Note that currently JsonArrays need to be equal and are not tested for containment (not necessary for our purposes
     * here).
     * @param jsonObject The JsonObject that must fully contain the other JsonObject (but may contain more entries as well).
     * @param jsonObjectToBeContained The JsonObject that needs to be fully contained inside the other JsonObject.
     * @return The result of the containment test.
     */
    private boolean testJsonObjectToBeContained(final JsonObject jsonObject, final JsonObject jsonObjectToBeContained) {
        if (jsonObjectToBeContained == null) {
            return true;
        }
        if (jsonObject == null) {
            return false;
        }
        AtomicBoolean containResult = new AtomicBoolean(true);

        jsonObjectToBeContained.forEach(entry -> {
            if (!jsonObject.containsKey(entry.getKey())) {
                containResult.set(false);
            } else {
                if (entry.getValue() == null) {
                    if (jsonObject.getValue(entry.getKey()) != null) {
                        containResult.set(false);
                    }
                } else if (entry.getValue() instanceof JsonObject) {
                    if (!(jsonObject.getValue(entry.getKey()) instanceof JsonObject)) {
                        containResult.set(false);
                    } else {
                        if (!testJsonObjectToBeContained((JsonObject)entry.getValue(),
                                (JsonObject)jsonObject.getValue(entry.getKey()))) {
                            containResult.set(false);
                        }
                    }
                } else {
                    if (!(entry.getValue().equals(jsonObject.getValue(entry.getKey())))) {
                        containResult.set(false);
                    }
                }
            }
        });
        return containResult.get();
    }

    private void addCredentials(final JsonObject requestPayload, final Future<Integer> resultFuture) {
        final String requestUri = buildCredentialsPostUri();

        vertx.createHttpClient().post(getPort(), HOST, requestUri).putHeader("Content-Type", CONTENT_TYPE_JSON)
                .handler(response -> {
                    if (response.statusCode() == HttpURLConnection.HTTP_CREATED) {
                        resultFuture.complete(response.statusCode());
                    } else {
                        resultFuture.fail("add credentials failed; response status code: " + response.statusCode());
                    }
                }).exceptionHandler(resultFuture::fail).end(requestPayload.encodePrettily());
    }

    private void testPostWithMissingPayloadParts(final TestContext context, final String fieldMissing) {
        final String requestUri = buildCredentialsPostUri();

        final JsonObject requestBodyAddCredentials = buildCredentialsPayloadHashedPassword(TEST_DEVICE_ID, TEST_AUTH_ID);
        requestBodyAddCredentials.remove(fieldMissing);

        final Async async = context.async();
        final int expectedStatus = HttpURLConnection.HTTP_BAD_REQUEST;

        postPayloadAndExpectErrorResponse(context, async, requestUri, CONTENT_TYPE_JSON, requestBodyAddCredentials, expectedStatus);
    }
}
