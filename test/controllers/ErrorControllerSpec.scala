/*
 * Copyright 2021 HM Revenue & Customs
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

import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

import scala.concurrent.Future

class ErrorControllerSpec extends UnitSpec with RasTestHelper {

  override val fakeRequest = FakeRequest("GET", "/")
  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")
  private val enrolment = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
  val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))

  val TestErrorController: ErrorController = new ErrorController(mockAuthConnector, mockShortLivedCache, mockSessionService, mockMCC, mockAppConfig, globalErrorView, problemUploadingFileView, fileNotAvailableView, unauthorisedView) {
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
    when(mockUserDetailsConnector.getUserDetails(any())(any(), any())).thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))
  }


  "ErrorController" should {
    "return error when global error endpoint is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return error when problem uploading file results is called" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 200 when not authorised file results is called" in {
      val result = TestErrorController.notAuthorised(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return error when file not available is called" in {
      val result = TestErrorController.fileNotAvailable(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return HTML when global error is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "return HTML when problem uploading file is called" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }
}
