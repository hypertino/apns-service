package com.hypertino.services.apns

import java.util.Date

import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.Accepted
import com.hypertino.hyperbus.transport.api.ServiceRegistrator
import com.hypertino.hyperbus.transport.registrators.DummyRegistrator
import com.hypertino.service.config.ConfigLoader
import com.hypertino.services.apns.api.{ApnsBadDeviceTokensFeedPost, ApnsNotification, ApnsPost, BadToken}
import com.hypertino.services.apns.clients.ApnsClient
import com.turo.pushy.apns.DeliveryPriority
import com.turo.pushy.apns.util.SimpleApnsPushNotification
import com.typesafe.config.Config
import monix.eval.Task
import monix.execution.Ack.Continue
import monix.execution.Scheduler
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpec}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ApnsServiceTest extends FlatSpec
  with BeforeAndAfterEach
  with TestServiceBase
  with MockFactory {

  implicit val scheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  bind [Config] to ConfigLoader()
  bind [Scheduler] to scheduler
  bind [Hyperbus] to injected[Hyperbus]
  bind [ServiceRegistrator] to DummyRegistrator

  "ApnsService" should "send notification" in {
    val apnsClient = mock[ApnsClient]

    val mockResponse = TestPushNotificationResponse(new SimpleApnsPushNotification("token", "topic", "payload"), success = true, "", new Date())

    (apnsClient.sendNotification _).expects("token", "topic", "payload", None, DeliveryPriority.IMMEDIATE, None).returns(Task(mockResponse)).once

    val apnsService = new ApnsService(apnsClient)(this)

    _hyperbus.ask(ApnsPost(new ApnsNotification("token", "topic", "payload")))
      .runAsync
      .futureValue shouldBe a[Accepted[_]]

    Await.ready(apnsService.stopService(false, 1.second), Duration.Inf)
  }

  it should "send feed-event on BadDeviceToken response" in {
    val apnsClient = mock[ApnsClient]

    val mockResponse = TestPushNotificationResponse(new SimpleApnsPushNotification("token", "topic", "payload"), success = false, "BadDeviceToken", new Date())
    (apnsClient.sendNotification _).expects("token", "topic", "payload", None, DeliveryPriority.IMMEDIATE, None).returns(Task(mockResponse)).once

    val apnsService = new ApnsService(apnsClient)(this)
    val serviceMock = new ServiceMock(_hyperbus)

    val mockedMethods = mock[MockedMethods]
    (mockedMethods.onApnsBadDeviceTokensFeedPost _).expects(where { rq: ApnsBadDeviceTokensFeedPost => rq.body.deviceToken == "token" && rq.body.bundleId == "topic" }).returns(Future { Continue }).once
    serviceMock.mockHandler = Some(mockedMethods)

    _hyperbus.ask(ApnsPost(new ApnsNotification("token", "topic", "payload")))
      .runAsync
      .futureValue shouldBe a[Accepted[_]]

    // waiting at least 1 second for feed put
    Thread.sleep(1000)
    Await.ready(Future.sequence(Seq(apnsService.stopService(false, 1.second),
      serviceMock.stopService(false, 1.second))), Duration.Inf)
  }
}
