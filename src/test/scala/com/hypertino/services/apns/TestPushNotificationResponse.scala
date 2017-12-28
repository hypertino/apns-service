package com.hypertino.services.apns

import java.util.Date

import com.turo.pushy.apns.{ApnsPushNotification, PushNotificationResponse}

case class TestPushNotificationResponse(apnsPushNotification: ApnsPushNotification,
                                        success: Boolean,
                                        rejectionReason: String,
                                        tokenInvalidationTimestamp: Date) extends PushNotificationResponse[ApnsPushNotification] {

  override def getRejectionReason: String = rejectionReason

  override def getPushNotification: ApnsPushNotification = apnsPushNotification

  override def isAccepted: Boolean = success

  override def getTokenInvalidationTimestamp: Date = tokenInvalidationTimestamp
}
