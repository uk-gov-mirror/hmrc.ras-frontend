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
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.TaxYearResolver
import uk.gov.hmrc.auth.core.*
import utils.{RandomNino, RasTestHelper}

import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.Future

class ResultsControllerSpec extends AnyWordSpec with RasTestHelper {

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  val currentTaxYear: Int                                       = TaxYearResolver.currentTaxYear

  override val SCOTTISH = "Scotland"
  val NON_SCOTTISH      = "England, Northern Ireland or Wales"

  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")

  private val enrolment =
    new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")

  private val enrolments                           = Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments]      = Future.successful(enrolments)
  val name: MemberName                             = MemberName("Jim", "McGill")
  val nino: MemberNino                             = MemberNino(RandomNino.generate)
  val dob: RasDate                                 = RasDate(Some("1"), Some("1"), Some("1999"))
  val memberDob: MemberDateOfBirth                 = MemberDateOfBirth(dob)
  val residencyStatusResult: ResidencyStatusResult = ResidencyStatusResult("", None, "", "", "", "", "")
  val postData: JsObject                           = Json.obj("firstName" -> "Jim", "lastName" -> "McGill", "nino" -> nino, "dateOfBirth" -> dob)
  val rasSession: RasSession                       = RasSession(name, nino, memberDob, Some(residencyStatusResult), None)

  val TestResultsController: ResultsController = new ResultsController(
    mockAuthConnector,
    mockRasSessionCacheService,
    mockMCC,
    mockAppConfig,
    matchFoundView,
    matchNotFoundView
  ) {
    when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
  }

  "Results Controller" must {
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

    "return 200 when match found" in {
      val result = TestResultsController.matchFound(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 303 when match not found" in {
      val result = TestResultsController.noMatchFound(fakeRequest)
      status(result) shouldBe 303
    }

    "return HTML when match found" in {
      val result = TestResultsController.matchFound(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }

    "return HTML when match not found" in {
      when(mockRasSessionCacheService.fetchRasSession()(any()))
        .thenReturn(Future.successful(Some(rasSession.copy(residencyStatusResult = None))))
      val result = TestResultsController.noMatchFound(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }

    "redirect to global error page when no session data is returned on match found" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val result = TestResultsController.matchFound.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("global-error")
    }

    "redirect to homepage when session data is returned with no result for match found" in {
      when(mockRasSessionCacheService.fetchRasSession()(any()))
        .thenReturn(Future.successful(Some(rasSession.copy(residencyStatusResult = None))))
      val result = TestResultsController.matchFound.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe "/relief-at-source"
    }

    "redirect to global error page when no session data is returned on match not found" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val result = TestResultsController.noMatchFound.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("global-error")
    }

    "redirect to homepage when session data is returned with no result for match not found" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val result = TestResultsController.noMatchFound.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result)         shouldBe 303
      redirectLocation(result) should include("/relief-at-source")
    }

    "return to member dob page when back link is clicked" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(
        Future.successful(
          Some(
            RasSession(
              name,
              nino,
              memberDob,
              Some(
                ResidencyStatusResult(
                  NON_SCOTTISH,
                  Some(NON_SCOTTISH),
                  currentTaxYear.toString,
                  (currentTaxYear + 1).toString,
                  name.firstName + " " + name.lastName,
                  memberDob.dateOfBirth.asLocalDate
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(Locale.UK)),
                  ""
                )
              ),
              None
            )
          )
        )
      )
      val result = TestResultsController.back.apply(FakeRequest())
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/member-date-of-birth")
    }

    "redirect to global error when no session and back link is clicked" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val result = TestResultsController.back.apply(FakeRequest())
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/global-error")
    }

    "redirect to choose-an-option for noMatchFound when session is incomplete" in {
      val incompleteSession =
        rasSession.copy(name = MemberName("", ""), residencyStatusResult = None)
      when(mockRasSessionCacheService.fetchRasSession()(any()))
        .thenReturn(Future.successful(Some(incompleteSession)))
      val result            = TestResultsController.noMatchFound(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/relief-at-source")
    }
  }

  "ResultsController unauthenticated" must {
    "redirect matchFound to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result = TestResultsController.matchFound(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect noMatchFound to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result = TestResultsController.noMatchFound(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect back to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result = TestResultsController.back(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }
  }

}
