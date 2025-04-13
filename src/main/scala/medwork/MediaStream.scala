package medwork;

import io.circe.generic.semiauto.*

/** Represents media data flow in the hospital system.
  *
  * @param source
  *   Source of the flow.
  * @param sink
  *   Sink of the flow.
  */
case class MediaStream(source: MediaSource, sink: MediaSink)
