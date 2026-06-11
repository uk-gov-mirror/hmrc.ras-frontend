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
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.RasTestHelper

import scala.concurrent.Future

class MemberNameControllerSpec extends AnyWordSpec with RasTestHelper {

  given headerCarrier: HeaderCarrier = HeaderCarrier()
  private val enrolmentIdentifier    = EnrolmentIdentifier("PSAID", "Z123456")

  private val enrolment =
    new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")

  private val enrolments                      = Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)
  val memberName: MemberName                  = MemberName("Jackie", "Chan")
  val memberNino: MemberNino                  = MemberNino("AB123456C")
  val memberDob: MemberDateOfBirth            = MemberDateOfBirth(RasDate(Some("12"), Some("12"), Some("2012")))
  val rasSession: RasSession                  = RasSession(memberName, memberNino, memberDob, None, None)
  val postData: Seq[(String, String)]         = Seq("firstName" -> "Jim", "lastName" -> "McGill")

  val TestMemberNameController: MemberNameController = new MemberNameController(
    mockAuthConnector,
    mockAuditConnector,
    mockResidencyStatusAPIConnector,
    mockRasSessionCacheService,
    mockMCC,
    mockAppConfig,
    memberNameView
  ) {
    override val apiVersion: ApiVersion = ApiV1_0

    when(mockRasSessionCacheService.cacheName(any())(any())).thenReturn(Future.successful(Some(rasSession)))
    when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
  }

  "MemberNameController" must {

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

    "return 200" in {
      val result = TestMemberNameController.get()(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = TestMemberNameController.get()(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }

    "return HTML when there is no ras session" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val result = TestMemberNameController.get()(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }
  }

  "Member name controller form submission" must {

    "return bad request when form error present" in {
      val postData = Seq("firstName" -> "", "lastName" -> "Esfandiari")
      val result   =
        TestMemberNameController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result) should equal(BAD_REQUEST)
    }

    "save details to cache" in {
      await(TestMemberNameController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*)))
      verify(mockRasSessionCacheService, atLeastOnce).cacheName(any())(any())
    }

    "redirect to nino page when name cached and edit mode is false" in {
      val session = RasSession(memberName, MemberNino(""), MemberDateOfBirth(RasDate(None, None, None)), None, None)
      when(mockRasSessionCacheService.cacheName(any())(any())).thenReturn(Future.successful(Some(session)))
      val result  =
        TestMemberNameController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)         shouldBe 303
      redirectLocation(result) should include("/member-national-insurance-number")
    }

    "redirect to match found page when edit mode is true and matching successful" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))
      when(mockRasSessionCacheService.cacheResidencyStatusResult(any())(any()))
        .thenReturn(Future.successful(Some(rasSession)))
      when(mockRasSessionCacheService.cacheName(any())(any())).thenReturn(Future.successful(Some(rasSession)))

      val result =
        TestMemberNameController.post(true).apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))

      status(result)           should equal(SEE_OTHER)
      redirectLocation(result) should include("/member-residency-status")

      verify(mockRasSessionCacheService, atLeastOnce).cacheName(any())(any())
    }

    "redirect to no match found page when edit mode is true and matching failed" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Member not found", 403, 403)))

      val result =
        TestMemberNameController.post(true).apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)           should equal(SEE_OTHER)
      redirectLocation(result) should include("/no-residency-status-displayed")

      verify(mockRasSessionCacheService, atLeastOnce).cacheName(any())(any())
    }

    "redirect to technical error page if name is not cached" in {
      when(mockRasSessionCacheService.cacheName(any())(any())).thenReturn(Future.successful(None))
      val result =
        TestMemberNameController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)         shouldBe 303
      redirectLocation(result) should include("global-error")
    }

  }

  "Member name controller back" must {

    "return to chooseAnOption page when back link is clicked and edit mode is false" in {
      val result = TestMemberNameController.back().apply(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/")
    }

    "return to match not found page when back link is clicked and edit mode is true" in {
      val result = TestMemberNameController.back(true).apply(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/no-residency-status-displayed")
    }
  }

  "MemberNameController unauthenticated" must {
    "redirect get to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result = TestMemberNameController.get()(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect post to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result =
        TestMemberNameController.post().apply(fakeRequest.withMethod("POST").withFormUrlEncodedBody(postData*))
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect back to GG sign-in when user has no active session" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))
      val result = TestMemberNameController.back()(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("gg/sign-in")
    }
  }

}
