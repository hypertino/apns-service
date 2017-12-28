package com.hypertino.services.apns

import com.hypertino.binders.config.ConfigBinders._
import com.hypertino.services.apns.clients.{ApnsClient, ApnsClientConfiguration, ApnsClientImpl}
import com.typesafe.config.Config
import scaldi.Module

class ApnsServiceModule(configSection: String) extends Module {
  bind[ApnsClientConfiguration] to inject[Config].read[ApnsClientConfiguration](configSection)
  bind[ApnsClient] to new ApnsClientImpl(inject[ApnsClientConfiguration])
}
