# Zookeeper

Zookeeper server compiled to native using Quarkus and GraalVM.

Based on work from:

* https://github.com/ozangunalp/kafka-native
* https://github.com/solsson/dockerfiles/blob/master/native/substitutions/zookeeper-server-start/


## Project Structure

- `quarkus-zookeeper-server-extension`: Quarkus extension including for compiling Zookeeper Server to native using GraalVM.
- `zookeeper-server`: Quarkus application starting a Zookeeper server in Kraft-mode using the zookeeper-server-extension. Compiles to JVM and native executable.
- `zookeeper-native-test-container`: Test container starting a single-node Zookeeper broker using the native-compiled zookeeper-server. Includes integration tests.

## Building the project

```shell script
mvn install
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
cd zookeeper-server
mvn compile quarkus:dev
```

Starts a single-node Zookeeper broker listening on :2181
Uses `./target/log-dir` as log directory.

## Packaging and running the application

The application can be packaged using the following on `zookeeper-server` directory:
```shell script
mvn package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## Creating a native executable

You can create a native executable using the following on `zookeeper-server` directory:
```shell script
mvn package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:
```shell script
mvn package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/zookeeper-server-1.0.0-SNAPSHOT-runner`

## Creating a container from native executable

You can create a container from the native executable using: 
```shell script
mvn package -Dnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true
```

The container image will be built with tag `quay.io/ogunalp/zookeeper-native:1.0.0-SNAPSHOT`.

If you want to reuse the existing native executable:

```shell script
mvn package -Dnative -Dquarkus.native.reuse-existing=true -Dquarkus.container-image.build=true
```

In case your container runtime is not running locally, use the parameter `-Dquarkus.native.remote-container-build=true` instead of `-Dquarkus.native.container-build=true`.

Then you can run the docker image using:

```shell script
docker run -p 2181:2181 -it --rm -e quay.io/k_wall/zookeeper-native:1.0.0-SNAPSHOT
```

## Configuring the Zookeeper broker

By default, the `zookeeper-server` application configures the embedded Zookeeper Kraft server for a single node cluster.

Following configuration options are available:

| Key                           | Description                                               | Default            |
|-------------------------------|-----------------------------------------------------------|--------------------|
| `server.zookeeper-port`       | External listener port                                    | 9092               |


You can set configuration options using Java system properties, e.g.

```shell script
java -Dserver.zookeeper-port=2181 \
  ./target/quarkus-app/quarkus-run.jar
```

Or environment variables, e.g.

```shell script
docker run -it --rm -p 19092:9092 \
  -e SERVER_ZOOKEEPER_PORT=2181 \
  quay.io/k_wall/zookeeper-native:1.0.0-SNAPSHOT
```
