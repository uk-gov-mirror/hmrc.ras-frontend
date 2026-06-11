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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.RasTestHelper

import scala.concurrent.{ExecutionContext, Future}

class ErrorControllerSpec extends AnyWordSpec with RasTestHelper {

  override val fakeRequest        = FakeRequest("GET", "/")
  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")

  private val enrolment =
    new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")

  val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))

  val TestErrorController: ErrorController = new ErrorController(
    mockAuthConnector,
    mockRasSessionCacheService,
    mockMCC,
    globalErrorView,
    problemUploadingFileView,
    fileNotAvailableView,
    unauthorisedView,
    startAtStartView
  )(using mockAppConfig) {
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
  }

  "ErrorController" must {

    "return error when global error endpoint is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return error when problem uploading file results is called" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 401 when not authorised file results is called" in {
      val result = TestErrorController.notAuthorised(fakeRequest)
      status(result) shouldBe Status.UNAUTHORIZED
    }

    "return error when file not available is called" in {
      val result = TestErrorController.fileNotAvailable(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return BadRequest when startAtStart is called" in {
      val result = TestErrorController.startAtStart(fakeRequest)
      status(result)      shouldBe Status.BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
    }

    "return HTML when global error is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }

    "return HTML when problem uploading file is called" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }

    val controller = new ErrorController(
      mockAuthConnector,
      mockRasSessionCacheService,
      mockMCC,
      globalErrorView,
      problemUploadingFileView,
      fileNotAvailableView,
      unauthorisedView,
      startAtStartView
    )(using mockAppConfig) {
      override def isAuthorised()(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[Either[Future[Result], String]] =
        Future.successful(Left(Future.successful(Forbidden("User not authorised"))))

    }

    "return the response from Left when user is not authorised for renderGlobalErrorPage" in {
      val result: Future[Result] = controller.renderGlobalErrorPage.apply(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return the response from Left when user is not authorised for renderProblemUploadingFilePage" in {
      val result: Future[Result] = controller.renderProblemUploadingFilePage.apply(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return the response from Left when user is not authorised for fileNotAvailable" in {
      val result: Future[Result] = controller.fileNotAvailable.apply(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

  }

}
