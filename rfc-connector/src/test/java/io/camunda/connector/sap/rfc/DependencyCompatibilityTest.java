package io.camunda.connector.sap.rfc;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.StringValue;
import io.grpc.CallOptions;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class DependencyCompatibilityTest {

  @Test
  void jacksonGenerationsShareAnnotationsWithoutReplacingEachOther() throws Exception {
    var value = new Payload("RFC");
    var jackson2 = new com.fasterxml.jackson.databind.ObjectMapper();
    var jackson3 = tools.jackson.databind.json.JsonMapper.builder().build();

    assertThat(jackson2.writeValueAsString(value)).isEqualTo("{\"display_name\":\"RFC\"}");
    assertThat(jackson3.writeValueAsString(value)).isEqualTo("{\"display_name\":\"RFC\"}");
    assertThat(jackson2.readValue(jackson3.writeValueAsString(value), Payload.class))
        .isEqualTo(value);
    assertThat(jackson3.readValue(jackson2.writeValueAsString(value), Payload.class))
        .isEqualTo(value);
  }

  @Test
  void nettyHttpCodecDecodesRequestsAndEncodesResponses() {
    var channel = new EmbeddedChannel(new HttpServerCodec());
    try {
      assertThat(
              channel.writeInbound(
                  Unpooled.copiedBuffer(
                      "GET /rfc HTTP/1.1\r\nHost: localhost\r\n\r\n", CharsetUtil.US_ASCII)))
          .isTrue();
      HttpRequest request = channel.readInbound();
      assertThat(request.decoderResult().isSuccess()).isTrue();
      assertThat(request.method()).isEqualTo(HttpMethod.GET);
      assertThat(request.uri()).isEqualTo("/rfc");
      LastHttpContent content = channel.readInbound();
      ReferenceCountUtil.release(content);

      var response =
          new DefaultFullHttpResponse(
              HttpVersion.HTTP_1_1,
              HttpResponseStatus.OK,
              Unpooled.copiedBuffer("ok", CharsetUtil.UTF_8));
      HttpUtil.setContentLength(response, response.content().readableBytes());
      assertThat(channel.writeOutbound(response)).isTrue();
      var encoded = new StringBuilder();
      ByteBuf buffer;
      while ((buffer = channel.readOutbound()) != null) {
        try {
          encoded.append(buffer.toString(StandardCharsets.UTF_8));
        } finally {
          buffer.release();
        }
      }
      assertThat(encoded.toString()).startsWith("HTTP/1.1 200 OK\r\n").endsWith("\r\n\r\nok");
    } finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void grpcRoundTripsOverNettyHttp2() throws Exception {
    var method =
        MethodDescriptor.<StringValue, StringValue>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "echo"))
            .setRequestMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
            .setResponseMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
            .build();
    var service =
        ServerServiceDefinition.builder("test.Echo")
            .addMethod(
                method,
                ServerCalls.asyncUnaryCall(
                    (request, observer) -> {
                      observer.onNext(request);
                      observer.onCompleted();
                    }))
            .build();
    var server =
        NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1", 0))
            .addService(service)
            .build()
            .start();
    try {
      var channel =
          NettyChannelBuilder.forAddress("127.0.0.1", server.getPort()).usePlaintext().build();
      try {
        var request = StringValue.of("RFC");
        assertThat(
                ClientCalls.blockingUnaryCall(
                    channel,
                    method,
                    CallOptions.DEFAULT.withDeadlineAfter(10, TimeUnit.SECONDS),
                    request))
            .isEqualTo(request);
      } finally {
        channel.shutdownNow();
        assertThat(channel.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }
    } finally {
      server.shutdownNow();
      assertThat(server.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void logbackIntegratesWithProvidedSlf4jAndMdc() {
    var context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger logger = context.getLogger(DependencyCompatibilityTest.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.setContext(context);
    appender.start();
    logger.addAppender(appender);
    try (var ignored = MDC.putCloseable("correlationId", "rfc-test")) {
      LoggerFactory.getLogger(DependencyCompatibilityTest.class).info("RFC logging compatibility");
      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.getFirst().getFormattedMessage())
          .isEqualTo("RFC logging compatibility");
      assertThat(appender.list.getFirst().getMDCPropertyMap())
          .containsEntry("correlationId", "rfc-test");
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  record Payload(@JsonProperty("display_name") String name) {}
}
