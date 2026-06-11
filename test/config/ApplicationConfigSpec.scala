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

import models.{ApiV1_0, ApiV2_0}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.RasTestHelper

import scala.concurrent.duration.{Duration, HOURS}

class ApplicationConfigSpec extends AnyWordSpec with Matchers with RasTestHelper {

  private val baseConfig: Configuration = fakeApplication.injector.instanceOf[Configuration]

  private def configWith(rasApiVersion: String): ApplicationConfig = {
    val overridden = Configuration("ras-api-version" -> rasApiVersion).withFallback(baseConfig)
    new ApplicationConfig(new ServicesConfig(overridden))
  }

  private val config: ApplicationConfig = applicationConfig

  "ApplicationConfig" should {

    "expose contact-frontend related values" in {
      config.contactHost              shouldBe "http://localhost:9250"
      config.reportAProblemUrl        shouldBe "http://localhost:9250/contact/problem_reports"
      config.reportAProblemPartialUrl shouldBe "http://localhost:9250/contact/problem_reports_ajax?service=RAS"
      config.reportAProblemNonJSUrl   shouldBe "http://localhost:9250/contact/problem_reports_nonjs?service=RAS"
    }

    "throw when caFrontendHost is accessed (config key not defined in this service)" in {
      val ex = intercept[RuntimeException](config.caFrontendHost)
      ex.getMessage should include("ca-frontend.host")
    }

    "expose bas-gateway hosts and derived sign-in/sign-out URLs" in {
      config.basGatewayHost      shouldBe "http://localhost:9553"
      config.basAuthHost         shouldBe "http://localhost:9553"
      config.loginURL            shouldBe "http://localhost:9553/bas-gateway/sign-in"
      config.signOutAndContinueUrl should include("/bas-gateway/sign-out-without-state?continue=")
      config.signOutAndContinueUrl should include("/relief-at-source")
    }

    "expose re-upload wait time" in {
      config.hoursToWaitForReUpload shouldBe 24
    }

    "expose login and timed-out URLs" in {
      config.loginCallback shouldBe "/relief-at-source/"
      config.timedOutUrl   shouldBe "/relief-at-source/signed-out"
    }

    "expose RAS API URLs" in {
      config.fileDeletionUrl               shouldBe "/ras-api/file/remove/"
      config.rasApiResidencyStatusEndpoint shouldBe "residency-status"
      config.rasApiBaseUrl                 shouldBe "http://localhost:9669"
    }

    "expose RAS frontend values" in {
      config.rasFrontendBaseUrl   shouldBe "http://localhost:9673"
      config.rasFrontendUrlSuffix shouldBe "relief-at-source"
    }

    "expose session-timeout values" in {
      config.timeOutSeconds          shouldBe 900
      config.timeOutCountDownSeconds shouldBe 120
      config.refreshInterval         shouldBe 910
      config.enableRefresh           shouldBe true
    }

    "expose Upscan values" in {
      config.initiateUrl              shouldBe "http://localhost:9570/upscan/v2/initiate"
      config.uploadRedirectTargetBase shouldBe "http://localhost:9673"
      config.upscanCallbackEndpoint   shouldBe "/ras-api/file-processing/status"
      config.maxFileSize              shouldBe 2097152
    }

    "expose feedback URL" in {
      config.feedbackBaseUrl shouldBe "http://localhost:9514"
      config.feedbackUrl     shouldBe "http://localhost:9514/feedback/ras"
    }

    "expose user-sessions TTL" in {
      config.userSessionsTTL shouldBe Duration(1, HOURS)
    }

    "resolve rasApiVersion to ApiV2_0 when configured as '2.0'" in {
      configWith("2.0").rasApiVersion shouldBe ApiV2_0
    }

    "resolve rasApiVersion to ApiV1_0 when configured as '1.0'" in {
      configWith("1.0").rasApiVersion shouldBe ApiV1_0
    }

    "throw when rasApiVersion is configured to an invalid value" in {
      val invalid = configWith("invalid")
      val ex      = intercept[Exception](invalid.rasApiVersion)
      ex.getMessage should include("ras-api-version")
    }

    "fall back to the default 'true' for enableRefresh when sessionTimeout.enableRefresh is missing" in {
      val stripped = Configuration(
        baseConfig.underlying.withoutPath("microservice.services.sessionTimeout.enableRefresh")
      )
      val cfg      = new ApplicationConfig(new ServicesConfig(stripped))
      cfg.enableRefresh shouldBe true
    }
  }

}
