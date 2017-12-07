package com.hypertino.services.apns

import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.{Accepted, BadRequest, EmptyBody, ErrorBody, ResponseBase}
import com.hypertino.hyperbus.serialization.SerializationOptions
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.service.control.api.Service
import com.hypertino.services.apns.clients.{ApnsClient, ApnsRejectionCause}
import com.hypertino.services.apns.api.{ApnsBadDeviceTokensFeedPost, ApnsPost, BadToken}
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler
import scaldi.{Injectable, Injector}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ApnsService(implicit val injector: Injector) extends Service with Injectable with Subscribable with StrictLogging {
  implicit val so = SerializationOptions.default
  import so._

  private val apnsClient = inject[ApnsClient]

  private implicit val scheduler: Scheduler = inject[Scheduler]
  private val hyperbus = inject[Hyperbus]

  private val handlers = hyperbus.subscribe(this, logger)
  logger.info(s"${getClass.getName} is STARTED")

  def onApnsPost(implicit r: ApnsPost): Task[ResponseBase] = {
    val deviceToken = r.body.deviceToken
    val bundleId = r.body.bundleId
    val payload = r.body.payload

    apnsClient.sendNotification(deviceToken, bundleId, payload)
      .flatMap { result =>
        if (result.isAccepted) {
          Task.eval { Accepted(EmptyBody) }
        } else {
          if (result.getRejectionReason == ApnsRejectionCause.BadDeviceToken) {
            hyperbus.publish(ApnsBadDeviceTokensFeedPost(BadToken(deviceToken, bundleId))).map { _ => BadRequest(ErrorBody("bad-device-token")) }
          } else {
            logger.warn(s"Apns rejected notification for device token $deviceToken and bundle $bundleId. " + result)
            Task.eval { BadRequest(ErrorBody("rejected", Some(s"Apns rejected the request: ${result.getRejectionReason}"))) }
          }
        }
      }
  }


  override def stopService(controlBreak: Boolean, timeout: FiniteDuration): Future[Unit] = Future {
    handlers.foreach(_.cancel())
    logger.info(s"${getClass.getName} is STOPPED")
  }
}