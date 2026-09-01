/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import models.{ApiV1_0, ApiV2_0, ApiVersion}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.duration.Duration

class ApplicationConfig @Inject() (config: ServicesConfig) {
  private def loadConfig(key: String) = config.getString(key)

  lazy val contactHost: String                          = loadConfig("contact-frontend.host")
  private lazy val contactFormServiceIdentifier: String = loadConfig("contact-frontend.serviceId")
  lazy val caFrontendHost: String                       = loadConfig("ca-frontend.host")
  lazy val basGatewayHost: String                       = loadConfig("bas-gateway.host")

  lazy val hoursToWaitForReUpload: Int = config.getConfInt(s"re-upload.wait.time.hours", 24)

  private lazy val signOutBaseUrl: String = s"$basGatewayHost/bas-gateway/sign-out-without-state?continue="
  private lazy val continueCallback       = config.getConfString("gg-urls.continue-callback.url", "/relief-at-source/")

  lazy val basAuthHost: String = s"${config.getConfString("auth.bas-gateway.host", "")}"
  lazy val loginURL: String    = s"$basAuthHost/bas-gateway/sign-in"

  lazy val reportAProblemUrl: String = s"$contactHost/contact/problem_reports"

  lazy val reportAProblemPartialUrl: String =
    s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"

  lazy val reportAProblemNonJSUrl: String =
    s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val signOutAndContinueUrl: String         = s"$signOutBaseUrl$continueCallback"
  lazy val timedOutUrl: String                   = "/relief-at-source/signed-out"
  lazy val loginCallback: String                 = config.getConfString("gg-urls.login-callback.url", "/relief-at-source/")
  lazy val fileDeletionUrl: String               = config.getConfString("file-deletion-url", "/ras-api/file/remove/")
  lazy val rasApiResidencyStatusEndpoint: String = loadConfig("residency-status-url")

  lazy val rasApiVersion: ApiVersion = loadConfig("ras-api-version") match {
    case "1.0" => ApiV1_0
    case "2.0" => ApiV2_0
    case _     => throw new Exception(s"Invalid value for configuration key: ras-api-version")
  }

  lazy val timeOutSeconds: Int          = config.getConfInt("sessionTimeout.timeoutSeconds", 780)
  lazy val timeOutCountDownSeconds: Int = config.getConfInt("sessionTimeout.time-out-countdown-seconds", 120)
  lazy val refreshInterval: Int         = timeOutSeconds + 10
  lazy val enableRefresh: Boolean       = config.getConfBool("sessionTimeout.enableRefresh", defBool = true)

  lazy val rasApiBaseUrl: String        = config.baseUrl("relief-at-source")
  lazy val rasFrontendBaseUrl: String   = loadConfig("ras-frontend.host")
  lazy val rasFrontendUrlSuffix: String = loadConfig("ras-frontend-url-suffix")

  // Upscan
  lazy val initiateUrl: String            = config.baseUrl("upscan-initiate") + "/upscan/v2/initiate"
  lazy val uploadRedirectTargetBase       = loadConfig("upload-redirect-target-base")
  lazy val upscanCallbackEndpoint: String = loadConfig("upscan.callback-endpoint")
  lazy val maxFileSize: Int               = config.getInt("upscan.maxFileSize")

  lazy val feedbackBaseUrl: String = config.getString("feedback-link-base")
  val feedbackUrl: String          = s"$feedbackBaseUrl/feedback/ras?useServiceNavigation"

  lazy val userSessionsTTL: Duration = config.getDuration("mongodb.userSessionsCacheTTLHours")
}
