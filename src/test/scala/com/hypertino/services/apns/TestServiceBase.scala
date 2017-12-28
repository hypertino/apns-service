package com.hypertino.services.apns

import com.hypertino.hyperbus.model.MessagingContext
import com.hypertino.hyperbus.subscribe.Subscribable
import org.scalamock.MockHelper
import org.scalamock.context.MockContext
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, TestSuite}

trait TestServiceBase extends TestSuite
  with Subscribable
  with ScalaFutures
  with Eventually
  with MockFactory
  with Matchers {

  implicit val patience = PatienceConfig(scaled(Span(20, Seconds)))

  protected implicit val mcx = MessagingContext.empty

  protected implicit val _scheduler = monix.execution.Scheduler.Implicits.global

  def verifyExpectations(): Unit ={
    MockHelper.verify(this)
  }
}
