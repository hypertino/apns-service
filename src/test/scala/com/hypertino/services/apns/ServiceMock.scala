package com.hypertino.services.apns

import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.hyperbus.subscribe.annotations.groupName
import com.hypertino.service.control.api.Service
import com.hypertino.services.apns.api.ApnsBadDeviceTokensFeedPost
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Ack.Stop
import monix.execution.{Ack, Scheduler}
import scaldi.Injectable

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ServiceMock(hyperbus: Hyperbus) extends Service
  with Subscribable
  with Injectable
  with StrictLogging
  with MockedMethods {
  implicit val scheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  val handlers = hyperbus.subscribe(this, logger)

  var mockHandler: Option[MockedMethods] = None

  @groupName("service-mock")
  def onApnsBadDeviceTokensFeedPost(r: ApnsBadDeviceTokensFeedPost): Future[Ack] = {
    mockHandler map { it => it.onApnsBadDeviceTokensFeedPost(r) } getOrElse Future {
      new IllegalStateException("Method handler is not provided")
      Stop
    }
  }

  def reset(): Unit = {
    mockHandler = None
  }

  override def stopService(controlBreak: Boolean, timeout: FiniteDuration): Future[Unit] = Future {
    handlers.foreach(_.cancel())
  }
}

trait MockedMethods {
  def onApnsBadDeviceTokensFeedPost(r: ApnsBadDeviceTokensFeedPost): Future[Ack]
}
