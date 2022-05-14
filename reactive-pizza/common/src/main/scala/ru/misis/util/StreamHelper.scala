package ru.misis.util

import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Sink}

trait StreamHelper {

  protected def broadcastSink2[T, M](sink1: Sink[T, M], sink2: Sink[T, M]): Sink[T, (M, M)] = {
    Sink.fromGraph(GraphDSL.create(sink1, sink2)((_, _)) {
      implicit builder =>
        (sink1, sink2) =>
          import GraphDSL.Implicits._
          val broadcast = builder add Broadcast[T](2)
          broadcast ~> sink1.in
          broadcast ~> sink2.in

          SinkShape(broadcast.in)
    })
  }
}
