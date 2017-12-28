package com.hypertino.services.apns

import java.util.Date

import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.Accepted
import com.hypertino.hyperbus.transport.api.ServiceRegistrator
import com.hypertino.hyperbus.transport.registrators.DummyRegistrator
import com.hypertino.service.config.ConfigLoader
import com.hypertino.services.apns.api.{ApnsBadDeviceTokensFeedPost, ApnsNotification, ApnsPost}
import com.hypertino.services.apns.clients.ApnsClient
import com.turo.pushy.apns.DeliveryPriority
import com.turo.pushy.apns.util.SimpleApnsPushNotification
import com.typesafe.config.Config
import monix.eval.Task
import monix.execution.Ack.Continue
import monix.execution.Scheduler
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import scaldi.{DynamicModule, Injectable, Injector}

import scala.concurrent.Future

class ApnsServiceTest extends FlatSpec
  with BeforeAndAfterEach
  with TestServiceBase
  with MockFactory
  with Injectable {

  "ApnsService" should "send notification" in {
    implicit val module: Injector = getModule

    val hyperbus = inject[Hyperbus]
    val apnsClient = mock[ApnsClient]

    val mockResponse = TestPushNotificationResponse(new SimpleApnsPushNotification("token", "topic", "payload"), success = true, "", new Date())

    (apnsClient.sendNotification _).expects("token", "topic", "payload", None, DeliveryPriority.IMMEDIATE, None).returns(Task(mockResponse)).once

    val apnsService = new ApnsService(apnsClient)(module)

    hyperbus.ask(ApnsPost(new ApnsNotification("token", "topic", "payload")))
      .runAsync
      .futureValue shouldBe a[Accepted[_]]
  }

  it should "send feed-event on BadDeviceToken response" in {
    implicit val module: Injector = getModule

    val hyperbus = inject[Hyperbus]
    val apnsClient = mock[ApnsClient]

    val mockResponse = TestPushNotificationResponse(new SimpleApnsPushNotification("token", "topic", "payload"), success = false, "BadDeviceToken", new Date())
    (apnsClient.sendNotification _).expects("token", "topic", "payload", None, DeliveryPriority.IMMEDIATE, None).returns(Task(mockResponse)).once

    val apnsService = new ApnsService(apnsClient)
    val serviceMock = new ServiceMock(hyperbus)

    val mockedMethods = mock[MockedMethods]
    (mockedMethods.onApnsBadDeviceTokensFeedPost _).expects(where { rq: ApnsBadDeviceTokensFeedPost => rq.body.deviceToken == "token" && rq.body.bundleId == "topic" }).returns(Future { Continue }).once
    serviceMock.mockHandler = Some(mockedMethods)

    hyperbus.ask(ApnsPost(new ApnsNotification("token", "topic", "payload")))
      .runAsync
      .futureValue shouldBe a[Accepted[_]]

    Thread.sleep(1000)
  }

  def getModule: Injector = DynamicModule { implicit module =>
    val configLoader = ConfigLoader()
    module.bind[Config] to configLoader
    module.bind[Scheduler] to _scheduler
    module.bind[Hyperbus] to new Hyperbus(configLoader)
    module.bind[ServiceRegistrator] to DummyRegistrator
  }
}
