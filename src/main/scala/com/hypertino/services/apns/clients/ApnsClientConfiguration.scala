package com.hypertino.services.apns.clients

case class ApnsClientConfiguration(host: String,
                                   port: Int,
                                   pkcs8FileResourceName: String,
                                   teamId: String,
                                   keyId: String)
