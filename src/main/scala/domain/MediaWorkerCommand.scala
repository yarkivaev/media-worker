package domain

import domain.Action.*
import io.circe.Decoder
import io.circe.generic.semiauto._

enum Action:
  case RecordVideoSourceAction, RouteCameraToMiddlewareAction, SupplyWebRtcServerAction

sealed trait MediaWorkerCommand {
  val actionType: Action
}

case class RecordVideoSource(source: MediaSource) extends MediaWorkerCommand {
  val actionType: Action = RecordVideoSourceAction
  implicit val decoder: Decoder[RecordVideoSource] = deriveDecoder[RecordVideoSource]
}

case class RouteCameraToMiddleware(
                                    camera: MediaSource,
                                    middleware: MediaSink
                                  ) extends MediaWorkerCommand {
  val actionType: Action = RouteCameraToMiddlewareAction
}

case class SupplyWebRtcServer(
                               source: MediaSource,
                               webRtc: MediaSink
                             ) extends MediaWorkerCommand {
  val actionType: Action = SupplyWebRtcServerAction
}
