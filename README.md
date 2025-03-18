# Media Worker

Service that manages media streams in the system. Internally media streams are ffmpeg, gstreamer processes and whatever 
has source and sink. Media Worker is supposed to be managed by external user. It receives command from rabbitMQ queue.

Key entities of the service:

- **MediaStream** - represents media stream in the system. Provide a way to run it.
- **MediaWorker** - serves as task dispatcher. Obtains tasks from a queue and runs them concurrently.
- **StreamingBackend** - runs stream processes, maintain their lifecycle. In particular plays role of adapter to ffmpeg
- **Broker** - provides a way to read messages from rabbitMQ queue and send them there
- **MediaWorkerCommand** - represents commands to be executed by worker, such as instantiating or stopping a media source
- **Clients**
  - **Client** - scala client for media worker
  - **JavaClient** - java client for media worker
- **Storage** - give an ability to work with external file storage

## Contribution

We use scalafmt to check code style. Use `scalafmt` to fix code style. Use `sbt scalafmtCheckAll`.