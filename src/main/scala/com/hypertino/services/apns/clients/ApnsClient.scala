package com.hypertino.services.apns.clients

import java.util.Date

import com.turo.pushy.apns.{ApnsPushNotification, DeliveryPriority, PushNotificationResponse}
import monix.eval.Task

trait ApnsClient {
  def sendNotification(deviceToken: String,
                       topic: String,
                       payload: String,
                       invalidationTime: Option[Date] = None,
                       priority: DeliveryPriority = DeliveryPriority.IMMEDIATE,
                       collapseId: Option[String] = None): Task[PushNotificationResponse[ApnsPushNotification]]
}
