package com.hypertino.services.apns

import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.{MessagingContext, NotFound}
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.hyperbus.transport.api.ServiceRegistrator
import com.hypertino.hyperbus.transport.registrators.DummyRegistrator
import com.hypertino.service.config.ConfigLoader
import com.hypertino.service.control.api.Service
import com.typesafe.config.Config
import monix.execution.{Cancelable, Scheduler}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, TestSuite}
import scaldi.Module

import scala.collection.mutable
import scala.concurrent.duration._

trait TestServiceBase extends TestSuite
  with Subscribable
  with ScalaFutures
  with Eventually
  with Matchers {

  implicit val patience = PatienceConfig(scaled(Span(20, Seconds)))

  protected implicit val mcx = MessagingContext.empty

  protected implicit val _scheduler = monix.execution.Scheduler.Implicits.global
}
