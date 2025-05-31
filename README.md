# PAK Media Worker

A distributed media streaming service that manages and processes media streams through command-driven architecture. The service handles media transformation tasks via FFmpeg processes and integrates with external storage and messaging systems.

## Overview

PAK Media Worker is designed as a scalable microservice for media processing workflows. It receives commands through RabbitMQ, processes media streams using FFmpeg, and provides both Scala and Java client libraries for integration.

### Key Features

- **Stream Management**: Start, stop, and monitor media streams
- **Multiple Backends**: FFmpeg and GStreamer support
- **Cloud Storage**: S3-compatible storage integration
- **Message Queue**: RabbitMQ-based command processing
- **Multi-language Clients**: Scala and Java client libraries
- **Docker Support**: Containerized deployment ready

## Architecture

The service is built using a modular architecture with the following core components:

### Core Components

- **MediaStream** - Represents a media stream with lifecycle management
- **MediaWorker** - Command dispatcher that processes tasks from the queue concurrently
- **StreamingBackend** - Process manager for media streaming tools (FFmpeg adapter)
- **Broker** - RabbitMQ message handling for commands and responses
- **MediaWorkerCommand** - Command interface for stream operations (start/stop)
- **Storage** - External file storage abstraction layer

### Client Libraries

- **Client** - Scala client for media worker integration
- **JavaClient** - Java client for media worker integration

### Project Structure

```
pak-media-worker/
├── domain/          # Core domain models and interfaces
├── server/          # Main server implementation
├── client/          # Client libraries (Scala/Java)
└── integration/     # Integration tests and examples
```

## Prerequisites

- **Java 17+**
- **FFmpeg** (for media processing)
- **RabbitMQ** (for message queuing)
- **MongoDB** (for persistence)
- **S3-compatible storage** (optional, for file storage)

## Installation

### Using Docker (Recommended)

```bash
# Build the Docker image
sbt server/docker

# Run with environment variables
docker run -e QUEUE_HOST=localhost \
           -e QUEUE_PORT=5672 \
           -e S3_ENDPOINT_URL=http://localhost:9000 \
           -e S3_ACCESS_KEY=your-key \
           -e S3_SECRET_KEY=your-secret \
           hirus/pak-media-worker:latest
```

### From Source

```bash
# Clone the repository
git clone <repository-url>
cd pak-media-worker

# Build the project
sbt compile

# Run the server
sbt server/run
```

## Configuration

The service is configured through environment variables:

| Variable | Description |
|----------|-------------|
| `QUEUE_HOST` | RabbitMQ host |
| `QUEUE_PORT` | RabbitMQ port |
| `S3_ENDPOINT_URL` | S3 endpoint URL |
| `S3_ACCESS_KEY` | S3 access key |
| `S3_SECRET_KEY` | S3 secret key |

## Usage

### Starting a Media Stream

Send a `StartMediaStream` command through RabbitMQ:

```json
{
  "type": "StartMediaStream",
  "name": "stream-name",
  "source": "rtsp://source-url",
  "sink": "rtmp://destination-url"
}
```

### Using the Scala Client

```scala
import client.Client

val client = Client.create()
client.startStream("stream-name", "rtsp://source", "rtmp://destination")
```

### Using the Java Client

```java
import client.JavaClient;

JavaClient client = new JavaClient();
client.startStream("stream-name", "rtsp://source", "rtmp://destination");
```

## Development

### Testing

```bash
# Run all tests
sbt test

# Run integration tests
sbt integration/test

# Run specific test suite
sbt server/test
```

### Code Style

We use Scalafmt for code formatting:

```bash
# Check code style
sbt scalafmtCheckAll

# Fix code style
sbt scalafmtAll
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests and ensure code style compliance
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## License

This project is licensed under the terms specified by the organization.