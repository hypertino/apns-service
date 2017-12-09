scalaVersion := "2.11.12"

lazy val `apns-service` = project in file(".") enablePlugins Raml2Hyperbus settings (
  name := "apns-service",
  version := "0.1-SNAPSHOT",
  organization := "com.hypertino",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("public")
  ),
  libraryDependencies ++= Seq(
    "com.hypertino" %% "hyperbus" % "0.5-SNAPSHOT",
    "com.hypertino" %% "hyperbus-t-inproc" % "0.5-SNAPSHOT" % "test",
    "com.hypertino" %% "service-control" % "0.3.1",
    "com.hypertino" %% "service-config" % "0.2.3" % "test",

    "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test",

    "com.turo" % "pushy" % "0.11.3",

    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  ),
  ramlHyperbusSources := Seq(
    ramlSource(
      path = "api/apns-service-api/apns-api.raml",
      packageName = "com.hypertino.services.apns.api",
      isResource = false
    )
  )
)
