package com.hypertino.services.apns.clients

import java.util.Date

import com.turo.pushy.apns.auth.ApnsSigningKey
import com.turo.pushy.apns.util.{SimpleApnsPushNotification, TokenUtil}
import com.turo.pushy.apns.{ApnsClientBuilder, ApnsPushNotification, DeliveryPriority, PushNotificationResponse}
import com.typesafe.scalalogging.StrictLogging
import io.netty.util
import io.netty.util.concurrent.GenericFutureListener
import monix.eval.Task
import monix.execution.Cancelable

class ApnsClient(configuration: ApnsClientConfiguration) extends StrictLogging {

  private val apnsClient = new ApnsClientBuilder()
      .setApnsServer(configuration.host, configuration.port)
      .setSigningKey(ApnsSigningKey.loadFromInputStream(getClass.getResourceAsStream(configuration.pkcs8FileResourceName),
        configuration.teamId,
        configuration.keyId))
      .build()

  def sendNotification(deviceToken: String,
                       topic: String,
                       payload: String,
                       invalidationTime: Option[Date] = None,
                       priority: DeliveryPriority = DeliveryPriority.IMMEDIATE,
                       collapseId: Option[String] = None): Task[PushNotificationResponse[SimpleApnsPushNotification]] = {
    val token = TokenUtil.sanitizeTokenString(deviceToken)

    val pushNotification = new SimpleApnsPushNotification(token,
      topic,
      payload,
      invalidationTime.orNull,
      priority,
      collapseId.orNull)

    sendNotification(pushNotification)
  }

  protected def sendNotification[T <: ApnsPushNotification](notification: T): Task[PushNotificationResponse[T]] = Task.create { (_, callback) =>
    val request = apnsClient.sendNotification(notification)

    request.addListener(new GenericFutureListener[util.concurrent.Future[PushNotificationResponse[T]]] {
      override def operationComplete(future: util.concurrent.Future[PushNotificationResponse[T]]): Unit = {
        if (future.isSuccess){
          val pushNotificationResponse = future.get()

          if(!pushNotificationResponse.isAccepted) {
            logger.error("Apns notification rejected: " + pushNotificationResponse)
          }

          callback.onSuccess(future.get())
        }else{
          callback.onError(future.cause())
        }
      }
    })

    Cancelable(() => request.cancel(true))
  }
}
