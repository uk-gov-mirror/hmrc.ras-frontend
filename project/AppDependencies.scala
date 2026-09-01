import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.8.0"
  private val hmrcMongoVersion = "2.13.0"
  private val domainVersion    = "13.0.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "13.11.0",
    "uk.gov.hmrc"       %% "domain-play-30"             % domainVersion,
    "uk.gov.hmrc"       %% "tax-year"                   % "6.0.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain-test-play-30"     % domainVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus" %% "scalacheck-1-19"         % "3.2.20.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
