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

import models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments, SessionRecordNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{RandomNino, RasTestHelper}

import scala.concurrent.Future

class SessionControllerSpec extends AnyWordSpec with RasTestHelper {

  given headerCarrier: HeaderCarrier = HeaderCarrier()

  val nino: MemberNino             = MemberNino(RandomNino.generate)
  val dob: RasDate                 = RasDate(Some("1"), Some("1"), Some("1999"))
  val memberDob: MemberDateOfBirth = MemberDateOfBirth(dob)
  val rasSession: RasSession       = RasSession(MemberName("Jim", "McGill"), nino, memberDob, None, None)
  private val enrolmentIdentifier  = EnrolmentIdentifier("PSAID", "Z123456")

  private val enrolment =
    new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")

  private val enrolments                      = Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)

  val TestSessionController =
    new SessionController(mockAuthConnector, mockRasSessionCacheService, mockMCC, mockAppConfig)

  "SessionController" must {

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

    "redirect to target" when {

      "redirect is called with choose-an-option" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("choose-an-option", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("relief-at-source")
      }

      "redirect is called with member-name in edit mode" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result =
          await(TestSessionController.redirect("member-name", cleanSession = false, edit = true)(FakeRequest()))
        redirectLocation(result) should include("member-name")
      }

      "redirect is called with member-nino in edit mode" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result =
          await(TestSessionController.redirect("member-nino", cleanSession = false, edit = true)(FakeRequest()))
        redirectLocation(result) should include("member-national-insurance-number")
      }

      "redirect is called with member-dob in edit mode" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result =
          await(TestSessionController.redirect("member-dob", cleanSession = false, edit = true)(FakeRequest()))
        redirectLocation(result) should include("member-date-of-birth")
      }

      "redirect is called with member-name and clean" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("member-name", cleanSession = true)(FakeRequest()))
        redirectLocation(result) should include("member-name")
      }

      "redirect is called with choose-an-option in a clean session" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result =
          await(TestSessionController.redirect("choose-an-option", cleanSession = true, edit = true)(FakeRequest()))
        redirectLocation(result) should include("relief-at-source")
      }

      "redirect is called with member-nino and clean" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("member-nino", cleanSession = true)(FakeRequest()))
        redirectLocation(result) should include("member-national-insurance-number")
      }

      "redirect is called with member-dob and clean" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("member-dob", cleanSession = true)(FakeRequest()))
        redirectLocation(result) should include("member-date-of-birth")
      }

      "redirect is called with member-name" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("member-name", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("member-name")
      }

      "redirect is called with member-nino" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("member-nino", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("member-national-insurance-number")
      }

      "redirect is called with member-dob" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("member-dob", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("member-date-of-birth")
      }

      "redirect is called with start" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("start", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("relief-at-source")
      }

      "redirect is called with chooseAnOption" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("chooseAnOption", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("relief-at-source")
      }

      "redirect is called with chooseAnOption and clean" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("chooseAnOption", cleanSession = true)(FakeRequest()))
        redirectLocation(result) should include("relief-at-source")
      }
    }

    "redirect to global error page" when {
      "no ras session is returned (target is irrelevant here)" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(None))
        val result = await(TestSessionController.redirect("member-details", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("global-error")
      }

      "no ras session is returned (target is irrelevant here) and clean is true" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(None))
        val result = await(TestSessionController.redirect("member-details", cleanSession = true)(FakeRequest()))
        redirectLocation(result) should include("global-error")
      }

      "ras session is returned but target is not recognised" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("blah blah", cleanSession = false)(FakeRequest()))
        redirectLocation(result) should include("global-error")
      }

      "ras session is returned but target is not recognised and clean is true" in {
        when(mockRasSessionCacheService.resetRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestSessionController.redirect("blah blah", cleanSession = true)(FakeRequest()))
        redirectLocation(result) should include("global-error")
      }
    }

    "keep a session alive" when {
      "keep alive action is called" in {
        val result = TestSessionController.keepAlive()(FakeRequest())
        status(result) shouldBe OK
      }
    }

    "redirect to GG sign-in when user is unauthenticated" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session found")))
      val result = TestSessionController.redirect("choose-an-option", cleanSession = false)(FakeRequest())
      redirectLocation(result) should include("gg/sign-in")
    }
  }

}
