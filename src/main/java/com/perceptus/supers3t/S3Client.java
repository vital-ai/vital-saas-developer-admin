package com.perceptus.supers3t;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.streams.Pump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Client {
	
    public static final String DEFAULT_ENDPOINT = "s3-us-west-1.amazonaws.com";
    
    private static final Logger logger = LoggerFactory.getLogger(S3Client.class);


    private final String awsAccessKey;
    private final String awsSecretKey;
    private final String awsSessionToken;

    private final HttpClient client;

    public S3Client() {
        this(null, null);
    }

    public S3Client(String accessKey, String secretKey) {
        this(Vertx.vertx(), accessKey, secretKey, null, DEFAULT_ENDPOINT);
    }

    public S3Client(String accessKey, String secretKey, String sessionToken, String endpoint) {
        this(Vertx.vertx(), accessKey, secretKey, sessionToken, endpoint);
    }


    public S3Client(Vertx vertx, String accessKey, String secretKey, String sessionToken, String endpoint) {
        awsAccessKey = accessKey;
        awsSecretKey = secretKey;
        awsSessionToken = sessionToken;

        this.client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost(endpoint));

    }

    // Direct call (async)
    // -----------

    // GET (bucket, key) -> handler(Data)
    public void get(String bucket,
                    String key,
                    Handler<HttpClientResponse> handler) {
        S3ClientRequest request = createGetRequest(bucket, key, handler);
        request.end();
    }

    // PUT (bucket, key, data) -> handler(Response)
    public void put(String bucket,
                    String key,
                    Buffer data,
                    Handler<HttpClientResponse> handler) {
        S3ClientRequest request = createPutRequest(bucket, key, handler);
        request.end(data);
    }

    /*
     * uploads the file contents to S3.
     */
    public void put(String bucket,
                    String key,
                    HttpServerFileUpload upload,
                    Handler<HttpClientResponse> handler) {
        if (logger.isDebugEnabled()) {
            logger.debug("S3 request bucket: {}, key: {}", bucket, key);
        }

        
        S3ClientRequest request = createPutRequest(bucket, key, handler);
        
        if(upload.contentType() != null && upload.contentType().length() > 0) {
        	request.setContentType(upload.contentType());
        	request.putHeader(HttpHeaders.CONTENT_TYPE, upload.contentType());
        }
        
        Buffer buffer = Buffer.buffer();

        upload.endHandler(event -> {
            request.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(buffer.length()));
            request.end(buffer);
        });

        upload.handler(data -> {
            buffer.appendBuffer(data);
        });
    }

    /*
     * uploads the file contents to S3.
     */
    public void put(String bucket,
                    String key,
                    HttpServerFileUpload upload,
                    long fileSize,
                    Handler<HttpClientResponse> handler) {
        if (logger.isDebugEnabled()) {
            logger.debug("S3 request bucket: {}, key: {}", bucket, key);
        }

        S3ClientRequest request = createPutRequest(bucket, key, handler);
        request.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
        Buffer buffer = Buffer.buffer();

        upload.endHandler(event -> {
            request.end(buffer);
        });

        Pump pump = Pump.pump(upload, request);
        pump.start();
    }

    // DELETE (bucket, key) -> handler(Response)
    public void delete(String bucket,
                       String key,
                       Handler<HttpClientResponse> handler) {
        S3ClientRequest request = createDeleteRequest(bucket, key, handler);
        request.end();
    }

    // Create requests which can be customized
    // ---------------------------------------

    // create PUT -> requestObject (which you can do stuff with)
	public S3ClientRequest
    createPutRequest(String bucket,
                     String key,
                     Handler<HttpClientResponse> handler) {
		
        HttpClientRequest httpRequest = client.put("/" + bucket + "/" + key, handler);
        
        return new S3ClientRequest("PUT",
                bucket,
                key,
                httpRequest,
                awsAccessKey,
                awsSecretKey,
                awsSessionToken);
    }

    // create GET -> request Object
	public S3ClientRequest
    createGetRequest(String bucket,
                     String key,
                     Handler<HttpClientResponse> handler) {
		
        HttpClientRequest httpRequest = client.get("/" + bucket + "/" + key, handler);
        
        return new S3ClientRequest("GET",
                bucket,
                key,
                httpRequest,
                awsAccessKey,
                awsSecretKey,
                awsSessionToken);
    }

    // create DELETE -> request Object
	public S3ClientRequest
    createDeleteRequest(String bucket,
                        String key,
                        Handler<HttpClientResponse> handler) {
		
        HttpClientRequest httpRequest = client.delete("/" + bucket + "/" + key, handler);
        
        return new S3ClientRequest("DELETE",
                bucket,
                key,
                httpRequest,
                awsAccessKey,
                awsSecretKey,
                awsSessionToken);
    }

    public void close() {
        this.client.close();
    }

}
