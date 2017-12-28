package com.hypertino.services.apns

import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.{Accepted, BadRequest, EmptyBody, ErrorBody, ResponseBase}
import com.hypertino.hyperbus.serialization.SerializationOptions
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.service.control.api.Service
import com.hypertino.services.apns.clients.{ApnsClient, ApnsClientImpl, ApnsRejectionCause}
import com.hypertino.services.apns.api.{ApnsBadDeviceTokensFeedPost, ApnsPost, BadToken}
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler
import scaldi.{Injectable, Injector}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ApnsService(private val apnsClient: ApnsClient)(implicit val injector: Injector) extends Service with Injectable with Subscribable with StrictLogging {
  implicit val so = SerializationOptions.default
  import so._

  private implicit val scheduler: Scheduler = inject[Scheduler]
  private val hyperbus = inject[Hyperbus]

  private val handlers = hyperbus.subscribe(this, logger)
  logger.info(s"${getClass.getName} is STARTED")

  def onApnsPost(implicit r: ApnsPost): Task[ResponseBase] = {
    val deviceToken = r.body.deviceToken
    val bundleId = r.body.bundleId
    val payload = r.body.payload

    logger.info(s"Received APNS notification for device token $deviceToken and bundle $bundleId. ${r.body}")

    apnsClient.sendNotification(deviceToken, bundleId, payload)
      .flatMap { result =>
        if (result.isAccepted) {
          logger.info(s"APNS accepted notification for device token $deviceToken and bundle $bundleId. $result")
          Task.unit
        } else {
          logger.warn(s"APNS rejected notification for device token $deviceToken and bundle $bundleId. $result")

          if (result.getRejectionReason == ApnsRejectionCause.BadDeviceToken) {
            hyperbus.publish(ApnsBadDeviceTokensFeedPost(BadToken(deviceToken, bundleId))).map { _ => BadRequest(ErrorBody("bad-device-token")) }
          } else {
            Task.unit
          }
        }
      }.onErrorRestart(5)
      .onErrorHandleWith { error =>
        logger.error(s"APNS failed to send notification for device token $deviceToken and bundle $bundleId", error)

        Task.raiseError(error)
      }
      .runAsync

    Task.eval { Accepted(EmptyBody) }
  }


  override def stopService(controlBreak: Boolean, timeout: FiniteDuration): Future[Unit] = Future {
    handlers.foreach(_.cancel())
    logger.info(s"${getClass.getName} is STOPPED")
  }
}