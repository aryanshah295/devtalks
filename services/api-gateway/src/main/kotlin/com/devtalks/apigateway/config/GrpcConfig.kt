package com.devtalks.apigateway.config

import com.devtalks.v1.TalkCatalogServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Wires a blocking gRPC stub bound to the same in-process channel name that
// `net.devh:grpc-server-spring-boot-starter` listens on (configured in application.yml
// under `grpc.server.in-process-name`). REST controllers dial gRPC through this stub
// — same process, no TCP socket, no serialization across the wire.
@Configuration
class GrpcConfig {

    @Bean(destroyMethod = "shutdown")
    fun inProcessChannel(
        @Value("\${grpc.server.in-process-name:devtalks-internal}") name: String,
    ): ManagedChannel =
        InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build()

    @Bean
    fun talkCatalogStub(channel: ManagedChannel): TalkCatalogServiceGrpc.TalkCatalogServiceBlockingStub =
        TalkCatalogServiceGrpc.newBlockingStub(channel)
}
