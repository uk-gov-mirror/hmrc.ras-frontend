/*
 * Copyright 2017 HM Revenue & Customs
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

import java.io.File

import connectors.UserDetailsConnector
import helpers.RandomNino
import helpers.helpers.I18nHelper
import models._
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import play.api.{Configuration, Environment, Mode}
import services.SessionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SessionControllerSpec extends UnitSpec with WithFakeApplication with I18nHelper with MockitoSugar {

  implicit val headerCarrier = HeaderCarrier()

  val mockConfig: Configuration = mock[Configuration]
  val mockEnvironment: Environment = Environment(mock[File], mock[ClassLoader], Mode.Test)
  val mockAuthConnector = mock[AuthConnector]
  val mockUserDetailsConnector = mock[UserDetailsConnector]
  val mockSessionService = mock[SessionService]

  val nino = RandomNino.generate
  val dob = RasDate(Some("1"), Some("1"), Some("1999"))
  val memberDetails = MemberDetails("Jim", "McGill", nino, dob)
  val residencyStatusResult = ResidencyStatusResult("","","","","","","")
  val rasSession = RasSession(memberDetails,residencyStatusResult)

  object TestSessionController extends SessionController{
    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment
    override val sessionService = mockSessionService
  }

  "SessionController" should {
    "redirect to target" when {
      "cleanAndRedirect is called with member-details" in {
        when(mockSessionService.resetRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.cleanAndRedirect("member-details")(FakeRequest()))
        redirectLocation(result).get should include("member-details")
      }
    }

    "redirect to global error page" when {
      "no ras session is returned (target is irrelevant here)" in {
        when(mockSessionService.resetRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val result = await(TestSessionController.cleanAndRedirect("member-details")(FakeRequest()))
        redirectLocation(result).get should include("global-error")
      }

      "ras session is returned but target is not recognised" in {
        when(mockSessionService.resetRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.cleanAndRedirect("blah blah")(FakeRequest()))
        redirectLocation(result).get should include("global-error")
      }
    }
  }


}
