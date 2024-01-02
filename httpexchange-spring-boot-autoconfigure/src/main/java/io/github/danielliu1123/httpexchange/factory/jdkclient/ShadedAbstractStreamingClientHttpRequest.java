package io.github.danielliu1123.httpexchange.factory.jdkclient;

import java.io.IOException;
import java.io.OutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FastByteArrayOutputStream;

/**
 * @author Freeman
 */
abstract class ShadedAbstractStreamingClientHttpRequest extends AbstractClientHttpRequest
        implements StreamingHttpOutputMessage {

    @Nullable
    private Body body;

    @Nullable
    private FastByteArrayOutputStream bodyStream;

    @Override
    protected final OutputStream getBodyInternal(HttpHeaders headers) {
        Assert.state(this.body == null, "Invoke either getBody or setBody; not both");

        if (this.bodyStream == null) {
            this.bodyStream = new FastByteArrayOutputStream(1024);
        }
        return this.bodyStream;
    }

    @Override
    public final void setBody(Body body) {
        Assert.notNull(body, "Body must not be null");
        assertNotExecuted();
        Assert.state(this.bodyStream == null, "Invoke either getBody or setBody; not both");

        this.body = body;
    }

    @Override
    protected final ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
        if (this.body == null && this.bodyStream != null) {
            this.body = outputStream -> this.bodyStream.writeTo(outputStream);
        }
        return executeInternal(headers, this.body);
    }

    /**
     * Abstract template method that writes the given headers and content to the HTTP request.
     * @param headers the HTTP headers
     * @param body the HTTP body, may be {@code null} if no body was {@linkplain #setBody(Body) set}
     * @return the response object for the executed request
     * @since 6.1
     */
    protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException;
}
