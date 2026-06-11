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
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.domain.{NinoGenerator, PsaId}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.RasTestHelper

import scala.concurrent.Future

class MemberDOBControllerSpec extends AnyWordSpec with RasTestHelper with BeforeAndAfter {

  given headerCarrier: HeaderCarrier = HeaderCarrier()

  def randomPsaId: Gen[PsaId] =
    for {
      prefix <- Gen.alphaUpperChar
      number <- Gen.chooseNum(0, 9999999)
    } yield PsaId(f"$prefix$number%07d")

  private val memberName          = MemberName("Jackie", "Chan")
  private val memberNino: String  = NinoGenerator().nextNino.nino
  private val dob                 = RasDate(Some("12"), Some("12"), Some("2012"))
  private val memberDob           = MemberDateOfBirth(dob)
  private val rasSession          = RasSession(memberName, MemberNino(memberNino), memberDob, None, None)
  private val postData            = Seq("dateOfBirth.day" -> "12", "dateOfBirth.month" -> "12", "dateOfBirth.year" -> "2012")
  private val psaId: String       = randomPsaId.sample.get.id
  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", psaId)

  private val enrolment =
    new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")

  private val enrolments                      = Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)

  val TestMemberDobController: MemberDOBController = new MemberDOBController(
    mockAuthConnector,
    mockResidencyStatusAPIConnector,
    mockAuditConnector,
    mockRasSessionCacheService,
    mockMCC,
    mockAppConfig,
    memberDobView
  ) {
    override val apiVersion: ApiVersion = ApiV1_0
    when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
  }

  "MemberDobController get" must {

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

    when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
      .thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))

    "return ok" when {
      "called" in {
        val result = TestMemberDobController.get()(fakeRequest)
        status(result) shouldBe OK
      }

      "called without a ras session" in {
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
        val result = TestMemberDobController.get()(fakeRequest)
        status(result) shouldBe OK
      }
    }
  }

  "Member dob controller form submission" must {

    "return bad request with session name when form error present and session has a name" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      val postData = Seq("dateOfBirth.day" -> "0", "dateOfBirth.month" -> "1", "dateOfBirth.year" -> "1111")
      val result   =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result) should equal(BAD_REQUEST)
    }

    "return bad request with member as name when form error present and session has no name" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val postData = Seq("dateOfBirth.day" -> "0", "dateOfBirth.month" -> "1", "dateOfBirth.year" -> "1111")
      val result   =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result) should equal(BAD_REQUEST)
    }

    "redirect" in {
      when(mockRasSessionCacheService.cacheDob(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))

      val postData = Seq("dateOfBirth.day" -> "1", "dateOfBirth.month" -> "1", "dateOfBirth.year" -> "1999")
      val result   =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result) should equal(SEE_OTHER)
    }

    "redirect if current year residency status is empty" in {
      when(mockRasSessionCacheService.cacheDob(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.successful(ResidencyStatus("", Some(OTHER_UK))))
      val result =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)           should equal(SEE_OTHER)
      redirectLocation(result) should include("global-error")
    }

    "save details to cache" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))

      TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      when(mockRasSessionCacheService.cacheDob(any())(any())).thenReturn(Future.successful(None))
      verify(mockRasSessionCacheService, atLeastOnce).cacheDob(any())(any())
    }

    "redirect if unknown current year residency status is returned" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.successful(ResidencyStatus("blah", Some(OTHER_UK))))
      val result =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)         shouldBe 303
      redirectLocation(result) should include("global-error")
    }

    "redirect to technical error page if customer matching fails to return a response" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.failed(new Exception()))
      val result =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)         shouldBe 303
      redirectLocation(result) should include("global-error")
    }

    "redirect to technical error page if ras fails to return a response" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.failed(new Exception()))
      val result =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)         shouldBe 303
      redirectLocation(result) should include("global-error")
    }

    "return to member nino page when back link is clicked and edit is false" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestMemberDobController.back().apply(FakeRequest())
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/member-national-insurance-number")
    }

    "return to not found page when back link is clicked and edit is true" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestMemberDobController.back(true).apply(FakeRequest())
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/no-residency-status-displayed")
    }

    "redirect to global error page navigating back with no session" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val result = TestMemberDobController.back().apply(FakeRequest())
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("global-error")
    }

    "redirect to match found page" when {
      "a request is made which returns CY and NY results (i.e. before 6th April)" in {

        when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
          .thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))
        when(mockRasSessionCacheService.cacheDob(any())(any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockRasSessionCacheService.cacheResidencyStatusResult(any())(any()))
          .thenReturn(Future.successful(Some(rasSession)))

        val result =
          TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))

        status(result)           should equal(SEE_OTHER)
        redirectLocation(result) should include("/member-residency-status")

        verify(mockRasSessionCacheService, atLeastOnce).cacheDob(any())(any())
      }

      "a request is made which returns only a CY result (i.e. after 6th April)" in {

        when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
          .thenReturn(Future.successful(ResidencyStatus(SCOTTISH, None)))
        when(mockRasSessionCacheService.cacheDob(any())(any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockRasSessionCacheService.cacheResidencyStatusResult(any())(any()))
          .thenReturn(Future.successful(Some(rasSession)))

        val result =
          TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))

        status(result)           should equal(SEE_OTHER)
        redirectLocation(result) should include("/member-residency-status")

        verify(mockRasSessionCacheService, atLeastOnce).cacheDob(any())(any())
      }
    }

    "redirect to no match found page" when {
      "matching failed is returned from the connector" in {
        when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Member not found", 403, 403)))

        val result =
          TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
        status(result)           should equal(SEE_OTHER)
        redirectLocation(result) should include("/no-residency-status-displayed")

        verify(mockRasSessionCacheService, atLeastOnce).cacheDob(any())(any())
      }
    }

    "redirect to global error page" when {
      "an exception is thrown by submitResidencyStatus" in {
        when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Unknown Error", 500, 500)))

        val result =
          TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
        status(result)           should equal(SEE_OTHER)
        redirectLocation(result) should include("/global-error")

        verify(mockRasSessionCacheService, atLeastOnce).cacheDob(any())(any())
      }
    }
  }

  "MemberDOBController unauthenticated" must {
    "redirect get to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result = TestMemberDobController.get()(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect back to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result = TestMemberDobController.back()(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect post to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result =
        TestMemberDobController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }
  }

}
