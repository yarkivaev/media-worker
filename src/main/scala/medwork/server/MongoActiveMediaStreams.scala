package medwork.server

import mongo4cats.collection.MongoCollection
import cats.implicits._
import fs2.Stream
import cats.Monad
import cats.effect.Async
import mongo4cats.operations.Filter
import mongo4cats.models.collection.ReplaceOptions
import medwork.MediaStream
import cats.kernel.Hash
import cats.instances.HashInstances._
import cats.syntax._
import cats.effect.kernel.MonadCancel
import cats.Applicative


/**
  * Serves all active media streams
  *
  * @param mediaStreams
  */
class MongoActiveMediaStreams[F[_]](
    mediaStreams: MongoCollection[F, MediaStream],
    origin: ActiveMediaStreams[F]
    )(using
      me: MonadCancel[F, Throwable]
    ) extends ActiveMediaStreams[F] {

    val idFilter: MediaStream => Filter = mediaStream => Filter.eq("_id", mediaStream.hash)

    def manageMediaStream(mediaStream: MediaStream): F[Unit] = 
      mediaStreams.replaceOne(
        idFilter(mediaStream), 
        mediaStream,
        ReplaceOptions(upsert = true)
        ) *> origin.manageMediaStream(mediaStream)

    def contains(mediaStream: MediaStream): F[Boolean] =
      mediaStreams.find(idFilter(mediaStream)).first.map{
        case Some(mediaStream) => true
        case None => false
      }
  
    def stopMediaStream(mediaStream: MediaStream): F[Either[String, Unit]] =
      mediaStreams.findOneAndDelete(idFilter(mediaStream)).flatMap {
        case Some(mediaStream) => Applicative[F].unit
        case None => me.raiseError(new RuntimeException("No such media stream found"))
      } *> origin.stopMediaStream(mediaStream)
}

object MongoActiveMediaStreams {
  def apply[F[_]: Async](
    mediaStreams: MongoCollection[F, MediaStream],
    origin: MongoActiveMediaStreams[F]
    ): F[MongoActiveMediaStreams[F]] =
    mediaStreams.find.stream.flatMap(mediaStream => Stream.eval(origin.manageMediaStream(mediaStream))).compile.drain
      .map(_ => new MongoActiveMediaStreams(mediaStreams, origin))
}
