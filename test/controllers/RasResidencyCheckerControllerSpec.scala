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

package controllers

import config.ApplicationConfig
import connectors.ResidencyStatusAPIConnector
import models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.FakeRequest
import play.api.test.Helpers.{SEE_OTHER, defaultAwaitTimeout, status}
import services.SessionCacheService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import utils.RasTestHelper

import scala.concurrent.Future

class RasResidencyCheckerControllerSpec extends AnyWordSpec with RasTestHelper {

  def configureRasResidencyCheckerController(version: ApiVersion): RasResidencyCheckerController =
    new RasResidencyCheckerController {
      override val authConnector: AuthConnector                             = mockAuthConnector
      override val connector: DefaultAuditConnector                         = mockAuditConnector
      override val sessionService: SessionCacheService                      = mockRasSessionCacheService
      override val residencyStatusAPIConnector: ResidencyStatusAPIConnector = mockResidencyStatusAPIConnector
      override val apiVersion: ApiVersion                                   = version
      override val appConfig: ApplicationConfig                             = mockAppConfig
    }

  "RasResidencyCheckerController extractResidencyStatus" when {
    "version 1.0 of the API is used" must {
      "extract the result into the correct messages" in {
        val testRasResidencyCheckerController = configureRasResidencyCheckerController(ApiV1_0)

        testRasResidencyCheckerController.extractResidencyStatus(SCOTTISH) shouldBe "Scotland"
        testRasResidencyCheckerController.extractResidencyStatus(WELSH)    shouldBe ""
        testRasResidencyCheckerController.extractResidencyStatus(OTHER_UK) shouldBe "England, Northern Ireland or Wales"
        testRasResidencyCheckerController.extractResidencyStatus("")       shouldBe ""
      }
    }
    "version 2.0 of the API is used" must {
      "extract the result into the correct messages" in {
        val testRasResidencyCheckerController = configureRasResidencyCheckerController(ApiV2_0)

        testRasResidencyCheckerController.extractResidencyStatus(SCOTTISH) shouldBe "Scotland"
        testRasResidencyCheckerController.extractResidencyStatus(WELSH)    shouldBe "Wales"
        testRasResidencyCheckerController.extractResidencyStatus(OTHER_UK) shouldBe "England, Northern Ireland or Wales"
        testRasResidencyCheckerController.extractResidencyStatus("")       shouldBe ""
      }
    }
  }

  "RasResidencyCheckerController submitResidencyStatus" must {
    "redirect to startAtStart when the session has no name, nino or dob" in {
      val controller = configureRasResidencyCheckerController(ApiV2_0)

      when(mockRasSessionCacheService.resetRasSession()(any()))
        .thenReturn(Future.successful(Some(RasSession.cleanSession)))

      given request: FakeRequest[play.api.mvc.AnyContentAsEmpty.type] = FakeRequest()

      val result = controller.submitResidencyStatus(RasSession.cleanSession, "user-1")
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("service-start-at-start")
    }
  }

}
