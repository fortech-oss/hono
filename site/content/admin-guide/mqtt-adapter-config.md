+++
title = "MQTT Adapter Configuration"
weight = 325
+++

The MQTT protocol adapter exposes an MQTT topic hierarchy for publishing messages and events to Eclipse Hono&trade;'s Telemetry and Event endpoints.
<!--more-->

The adapter is implemented as a Spring Boot application. It can be run either directly from the command line or by means of starting the corresponding [Docker image](https://hub.docker.com/r/eclipse/hono-adapter-mqtt-vertx/) created from it.

## Service Configuration

In addition to the following options, this component supports the options described in [Common Configuration]({{< relref "common-config.md" >}}).

The following table provides an overview of the configuration variables and corresponding command line options for configuring the MQTT adapter.

| Environment Variable<br>Command Line Option | Mandatory | Default Value | Description  |
| :------------------------------------------ | :-------: | :------------ | :------------|
| `HONO_APP_MAX_INSTANCES`<br>`--hono.app.maxInstances` | no | *#CPU cores* | The number of verticle instances to deploy. If not set, one verticle per processor core is deployed. |
| `HONO_APP_HEALTH_CHECK_PORT`<br>`--hono.app.healthCheckPort` | no | - | The port that the HTTP server, which exposes the service's health check resources, should bind to. If set, the adapter will expose a *readiness* probe at URI `/readiness` and a *liveness* probe at URI `/liveness`. |
| `HONO_APP_HEALTH_CHECK_BIND_ADDRESS`<br>`--hono.app.healthCheckBindAddress` | no | `127.0.0.1` | The IP address of the network interface that the HTTP server, which exposes the service's health check resources, should be bound to. The HTTP server will only be started if `HONO_APP_HEALTH_CHECK_BIND_ADDRESS` is set explicitly. |
| `HONO_CONNECTIONEVENTS_PRODUCER`<br>`--hono.connectionEvents.producer` | no | `logging` | The implementation of *connection events* producer which is to be used. This may be `logging` or `events`.<br>See [Connection Events]({{< relref "concepts/connection-events.md">}})|
| `HONO_MQTT_AUTHENTICATION_REQUIRED`<br>`--hono.mqtt.authenticationRequired` | no | `true` | If set to `true` the protocol adapter requires devices to authenticate when connecting to the adapter. The credentials provided by the device are verified using the configured [Credentials Service]({{< relref "#credentials-service-connection-configuration" >}}). Devices that have failed to authenticate are not allowed to publish any data. |
| `HONO_MQTT_BIND_ADDRESS`<br>`--hono.mqtt.bindAddress` | no | `127.0.0.1` | The IP address of the network interface that the secure port should be bound to.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_MQTT_CERT_PATH`<br>`--hono.mqtt.certPath` | no | - | The absolute path to the PEM file containing the certificate that the protocol adapter should use for authenticating to clients. This option must be used in conjunction with `HONO_MQTT_KEY_PATH`.<br>Alternatively, the `HONO_MQTT_KEY_STORE_PATH` option can be used to configure a key store containing both the key as well as the certificate. |
| `HONO_MQTT_COMMAND_ACK_TIMEOUT`<br>`--hono.mqtt.commandAckTimeout` | no | `100` | The amount of time (milliseconds) after which the sending of a command to a device using QoS 1 is considered to be failed. The value of this variable should be increased in cases where devices are connected over a network with high latency. |
| `HONO_MQTT_DEFAULTS_ENABLED`<br>`--hono.mqtt.defaultsEnabled` | no | `true` | If set to `true` the protocol adapter uses *default values* registered for a device to augment messages published by the device with missing information like a content type. In particular, the protocol adapter adds default values registered for the device as (application) properties with the same name to the AMQP 1.0 messages it sends downstream to the AMQP Messaging Network. |
| `HONO_MQTT_INSECURE_PORT_BIND_ADDRESS`<br>`--hono.mqtt.insecurePortBindAddress` | no | `127.0.0.1` | The IP address of the network interface that the insecure port should be bound to.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_MQTT_INSECURE_PORT_ENABLED`<br>`--hono.mqtt.insecurePortEnabled` | no | `false` | If set to `true` the protocol adapter will open an insecure port (not secured by TLS) using either the port number set via `HONO_MQTT_INSECURE_PORT` or the default MQTT port number (`1883`) if not set explicitly.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_MQTT_KEY_PATH`<br>`--hono.mqtt.keyPath` | no | - | The absolute path to the (PKCS8) PEM file containing the private key that the protocol adapter should use for authenticating to clients. This option must be used in conjunction with `HONO_MQTT_CERT_PATH`. Alternatively, the `HONO_MQTT_KEY_STORE_PATH` option can be used to configure a key store containing both the key as well as the certificate. |
| `HONO_MQTT_KEY_STORE_PASSWORD`<br>`--hono.mqtt.keyStorePassword` | no | - | The password required to read the contents of the key store. |
| `HONO_MQTT_KEY_STORE_PATH`<br>`--hono.mqtt.keyStorePath` | no | - | The absolute path to the Java key store containing the private key and certificate that the protocol adapter should use for authenticating to clients. Either this option or the `HONO_MQTT_KEY_PATH` and `HONO_MQTT_CERT_PATH` options need to be set in order to enable TLS secured connections with clients. The key store format can be either `JKS` or `PKCS12` indicated by a `.jks` or `.p12` file suffix respectively. |
| `HONO_MQTT_MAX_CONNECTIONS`<br>`--hono.mqtt.maxConnections` | no | `0` | The maximum number of concurrent connections that the protocol adapter should accept. If not set (or set to `0`), the protocol adapter determines a reasonable value based on the available resources like memory and CPU. |
| `HONO_MQTT_MAX_PAYLOAD_SIZE`<br>`--hono.mqtt.maxPayloadSize` | no | `2048` | The maximum allowed size of an incoming MQTT message's payload in bytes. When a client sends a message with a larger payload, the message is discarded and the connection to the client gets closed. |
| `HONO_MQTT_NATIVE_TLS_REQUIRED`<br>`--hono.mqtt.nativeTlsRequired` | no | `false` | The server will probe for OpenSLL on startup if a secure port is configured. By default, the server will fall back to the JVM's default SSL engine if not available. However, if set to `true`, the server will fail to start at all in this case. |
| `HONO_MQTT_PORT`<br>`--hono.mqtt.port` | no | `8883` | The secure port that the protocol adapter should listen on.<br>See [Port Configuration]({{< relref "#port-configuration" >}}) below for details. |
| `HONO_MQTT_SECURE_PROTOCOLS`<br>`--hono.mqtt.secureProtocols` | no | `TLSv1.2` | A (comma separated) list of secure protocols that are supported when negotiating TLS sessions. Please refer to the [vert.x documentation](https://vertx.io/docs/vertx-core/java/#ssl) for a list of supported protocol names. |
| `HONO_VERTX_DNS_QUERY_TIMEOUT`<br>`--hono.vertx.dnsQueryTimeout` | no | `5000` | The amount of time after which a DNS query is considered to be failed. Setting this variable to a smaller value may help to reduce the time required to establish connections to the services this adapter depends on. However, setting it to a value that is too small for any DNS query to succeed will effectively prevent any connections to be established at all. |

The variables only need to be set if the default values do not match your environment.


## Port Configuration

The MQTT protocol adapter can be configured to listen for connections on

* a secure port only (default) or
* an insecure port only or
* both a secure and an insecure port (dual port configuration)

The MQTT protocol adapter will fail to start if none of the ports is configured properly.

### Secure Port Only

The protocol adapter needs to be configured with a private key and certificate in order to open a TLS secured port.

There are two alternative ways for doing so:

1. either setting the `HONO_MQTT_KEY_STORE_PATH` and the `HONO_MQTT_KEY_STORE_PASSWORD` variables in order to load the key & certificate from a password protected key store, or
1. setting the `HONO_MQTT_KEY_PATH` and `HONO_MQTT_CERT_PATH` variables in order to load the key and certificate from two separate PEM files in PKCS8 format.

When starting up, the protocol adapter will bind a TLS secured socket to the default secure MQTT port 8883. The port number can also be set explicitly using the `HONO_MQTT_PORT` variable.

The `HONO_MQTT_BIND_ADDRESS` variable can be used to specify the network interface that the port should be exposed on. By default the port is bound to the *loopback device* only, i.e. the port will only be accessible from the local host. Setting this variable to `0.0.0.0` will let the port being bound to **all** network interfaces (be careful not to expose the port unintentionally to the outside world).

### Insecure Port Only

The secure port will mostly be required for production scenarios. However, it might be desirable to expose a non-TLS secured port instead, e.g. for testing purposes. In any case, the non-secure port needs to be explicitly enabled either by

- explicitly setting `HONO_MQTT_INSECURE_PORT` to a valid port number, or by
- implicitly configuring the default MQTT port (1883) by simply setting `HONO_MQTT_INSECURE_PORT_ENABLED` to `true`.

The protocol adapter issues a warning on the console if `HONO_MQTT_INSECURE_PORT` is set to the default secure MQTT port (8883).

The `HONO_MQTT_INSECURE_PORT_BIND_ADDRESS` variable can be used to specify the network interface that the port should be exposed on. By default the port is bound to the *loopback device* only, i.e. the port will only be accessible from the local host. This variable might be used to e.g. expose the non-TLS secured port on a local interface only, thus providing easy access from within the local network, while still requiring encrypted communication when accessed from the outside over public network infrastructure.

Setting this variable to `0.0.0.0` will let the port being bound to **all** network interfaces (be careful not to expose the port unintentionally to the outside world).

### Dual Port
 
The protocol adapter may be configured to open both a secure and a non-secure port at the same time simply by configuring both ports as described above. For this to work, both ports must be configured to use different port numbers, otherwise startup will fail.

### Ephemeral Ports

Both the secure as well as the insecure port numbers may be explicitly set to `0`. The protocol adapter will then use arbitrary (unused) port numbers determined by the operating system during startup.

## Tenant Service Connection Configuration

The adapter requires a connection to an implementation of Hono's [Tenant API]({{< ref "Tenant-API.md" >}}) in order to retrieve information for a tenant.

The connection to the Tenant Service is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
where the `${PREFIX}` is set to `HONO_TENANT` and the additional values for response caching apply.

The adapter caches the responses for the *get* operation until they expire. This greatly reduces load on the Tenant service.

## AMQP 1.0 Messaging Network Connection Configuration

The adapter requires a connection to the *AMQP 1.0 Messaging Network* in order to forward telemetry data and events received from devices to downstream consumers.

The connection to the messaging network is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
with `HONO_MESSAGING` being used as `${PREFIX}`. Since there are no responses being received, the properties for configuring response caching can be ignored.

## Device Registration Service Connection Configuration

The adapter requires a connection to an implementation of Hono's [Device Registration API]({{< ref "Device-Registration-API.md" >}}) in order to retrieve registration status assertions for connected devices.

The connection to the Device Registration Service is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
where the `${PREFIX}` is set to `HONO_REGISTRATION`.

The adapter caches responses for the *assert Device Registration* operation until the returned assertion tokens expire. This greatly reduces load on the Device Registration service.

## Credentials Service Connection Configuration

The adapter requires a connection to an implementation of Hono's [Credentials API]({{< ref "Credentials-API.md" >}}) in order to retrieve credentials stored for devices that need to be authenticated.

The connection to the Credentials Service is configured according to [Hono Client Configuration]({{< relref "hono-client-configuration.md" >}})
where the `${PREFIX}` is set to `HONO_CREDENTIALS`.

Responses of the Credentials Service are currently not cached, so the cache properties can be ignored.

## Metrics Configuration

See [Monitoring & Tracing Admin Guide]({{< ref "monitoring-tracing-config.md" >}}) for details on how to configure the reporting of metrics.

## Run as a Docker Swarm Service

The MQTT adapter can be run as a Docker container from the command line. The following commands create and start the MQTT adapter as a Docker Swarm service using the default keys  contained in the `demo-certs` module:

~~~sh
# in base directory of Hono repository:
docker secret create trusted-certs.pem demo-certs/certs/trusted-certs.pem
docker secret create mqtt-adapter-key.pem demo-certs/certs/mqtt-adapter-key.pem
docker secret create mqtt-adapter-cert.pem demo-certs/certs/mqtt-adapter-cert.pem
docker service create --detach --name hono-adapter-mqtt-vertx --network hono-net -p 1883:1883 -p 8883:8883 \
 --secret trusted-certs.pem \
 --secret mqtt-adapter-key.pem \
 --secret mqtt-adapter-cert.pem \
 -e 'HONO_MESSAGING_HOST=hono-service-messaging.hono' \
 -e 'HONO_MESSAGING_USERNAME=mqtt-adapter@HONO' \
 -e 'HONO_MESSAGING_PASSWORD=mqtt-secret' \
 -e 'HONO_MESSAGING_TRUST_STORE_PATH=/run/secrets/trusted-certs.pem' \
 -e 'HONO_TENANT_HOST=hono-service-device-registry.hono' \
 -e 'HONO_TENANT_USERNAME=mqtt-adapter@HONO' \
 -e 'HONO_TENANT_PASSWORD=mqtt-secret' \
 -e 'HONO_TENANT_TRUST_STORE_PATH=/run/secrets/trusted-certs.pem' \
 -e 'HONO_REGISTRATION_HOST=hono-service-device-registry.hono' \
 -e 'HONO_REGISTRATION_USERNAME=mqtt-adapter@HONO' \
 -e 'HONO_REGISTRATION_PASSWORD=mqtt-secret' \
 -e 'HONO_REGISTRATION_TRUST_STORE_PATH=/run/secrets/trusted-certs.pem' \
 -e 'HONO_CREDENTIALS_HOST=hono-service-device-registry.hono' \
 -e 'HONO_CREDENTIALS_USERNAME=mqtt-adapter@HONO' \
 -e 'HONO_CREDENTIALS_PASSWORD=mqtt-secret' \
 -e 'HONO_CREDENTIALS_TRUST_STORE_PATH=/run/secrets/trusted-certs.pem' \
 -e 'HONO_MQTT_BIND_ADDRESS=0.0.0.0' \
 -e 'HONO_MQTT_KEY_PATH=/run/secrets/mqtt-adapter-key.pem' \
 -e 'HONO_MQTT_CERT_PATH=/run/secrets/mqtt-adapter-cert.pem' \
 -e 'HONO_MQTT_INSECURE_PORT_ENABLED=true' \
 -e 'HONO_MQTT_INSECURE_PORT_BIND_ADDRESS=0.0.0.0' \
 eclipse/hono-adapter-mqtt-vertx:latest
~~~

{{% note %}}
There are several things noteworthy about the above command to start the service:

1. The *secrets* need to be created once only, i.e. they only need to be removed and re-created if they are changed.
1. The *--network* command line switch is used to specify the *user defined* Docker network that the MQTT adapter container should attach to. It is important that the MQTT adapter container is attached to the same network that the AMQP Messaging Network is attached to so that the MQTT adapter can use the AMQP Messaging Network's host name to connect to it via the Docker network. Please refer to the [Docker Networking Guide](https://docs.docker.com/engine/userguide/networking/#/user-defined-networks) for details regarding how to create a *user defined* network in Docker.
1. In cases where the MQTT adapter container requires a lot of configuration via environment variables (provided by means of *-e* switches), it is more convenient to add all environment variable definitions to a separate *env file* and refer to it using Docker's *--env-file* command line switch when starting the container. This way the command line to start the container is much shorter and can be copied and edited more easily.
{{% /note %}}

### Configuring the Java VM

The MQTT adapter Docker image by default does not pass any specific configuration options to the Java VM. The VM can be configured using the standard `-X` options by means of setting the `_JAVA_OPTIONS` environment variable which is evaluated by the Java VM during start up.

Using the example from above, the following environment variable definition needs to be added to limit the VM's heap size to 128MB:

~~~sh
...
 -e '_JAVA_OPTIONS=-Xmx128m' \
 eclipse/hono-adapter-mqtt-vertx:latest
~~~

## Run using the Docker Swarm Deployment Script

In most cases it is much easier to start all of Hono's components in one shot using the Docker Swarm deployment script provided in the `deploy/target/deploy/docker` folder.

## Run the Spring Boot Application

Sometimes it is helpful to run the adapter from its jar file, e.g. in order to attach a debugger more easily or to take advantage of code replacement.
In order to do so, the adapter can be started using the `spring-boot:run` maven goal from the `adapters/mqtt-vertx` folder.
The corresponding command to start up the adapter with the configuration used in the Docker example above looks like this:

~~~sh
# in directory: hono/adapters/mqtt-vertx/
mvn spring-boot:run -Dhono.app.healthCheckPort=8088 -Dhono.app.maxInstances=1 -Drun.arguments=\
--hono.messaging.host=hono-service-messaging.hono,\
--hono.messaging.username=mqtt-adapter@HONO,\
--hono.messaging.password=mqtt-secret,\
--hono.messaging.trustStorePath=../../demo-certs/certs/trusted-certs.pem,\
--hono.tenant.host=hono-service-device-registry.hono,\
--hono.tenant.username=mqtt-adapter@HONO,\
--hono.tenant.password=mqtt-secret,\
--hono.tenant.trustStorePath=../../demo-certs/certs/trusted-certs.pem,\
--hono.registration.host=hono-service-device-registry.hono,\
--hono.registration.username=mqtt-adapter@HONO,\
--hono.registration.password=mqtt-secret,\
--hono.registration.trustStorePath=../../demo-certs/certs/trusted-certs.pem,\
--hono.credentials.host=hono-service-device-registry.hono,\
--hono.credentials.username=mqtt-adapter@HONO,\
--hono.credentials.password=mqtt-secret,\
--hono.credentials.trustStorePath=../../demo-certs/certs/trusted-certs.pem,\
--hono.mqtt.bindAddress=0.0.0.0,\
--hono.mqtt.insecurePortEnabled=true,\
--hono.mqtt.insecurePortBindAddress=0.0.0.0
~~~

{{% note %}}
In the example above the *--hono.messaging.host=hono-service-messaging.hono* command line option indicates that the AMQP Messaging Network is running on a host
with name *hono-service-messaging.hono*. However, if the AMQP Messaging Network has been started as a Docker container then the *hono-service-messaging.hono* host name will most likely only be resolvable on the network that Docker has created for running the container on, i.e. when you run the MQTT adapter
from the Spring Boot application and want it to connect to the AMQP Messaging Network run as a Docker container then you need to set the
value of the *--hono.messaging.host* option to the IP address (or name) of the Docker host running the AMQP Messaging Network.
The same holds true analogously for the *hono-service-device-registry.hono* address.
{{% /note %}}
