import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

ThisBuild / scalaVersion := "3.3.8"
ThisBuild / majorVersion := 1

lazy val microservice = Project("ras-frontend", file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:msg=unused import&src=html/.*:s",
      "-Wconf:src=routes/.*:s",
      "-feature"
    ),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    ),
    PlayKeys.playDefaultPort := 9673,
    libraryDependencies ++= AppDependencies()
  )
  .settings(CodeCoverageSettings())

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
