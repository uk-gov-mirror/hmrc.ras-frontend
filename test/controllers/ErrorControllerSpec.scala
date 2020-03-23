/*
 * Copyright 2020 HM Revenue & Customs
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

import helpers.{I18nHelper, RasTestHelper}
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ErrorControllerSpec extends UnitSpec with I18nHelper with RasTestHelper {

  override val fakeRequest = FakeRequest("GET", "/")
  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")
  private val enrolment = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
  val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))

  val TestErrorController: ErrorController = new ErrorController(mockAuthConnector, mockShortLivedCache, mockSessionService, mockAppConfig) {
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
    when(mockUserDetailsConnector.getUserDetails(any())(any(), any())).thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))
  }

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

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

  "global error page" should {

    "contain correct title and header" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("global.error.page.title")
      doc.getElementById("header").text shouldBe Messages("global.error.header")
      doc.getElementById("message").text shouldBe Messages("global.error.message")
    }
  }

  "problem uploading file page" should {

    "contain correct title and header when problem uploading file" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("problem.uploading.file.title")
      doc.getElementById("back").attr("href") should include("/upload-a-file")
      doc.getElementById("header").text shouldBe Messages("problem.uploading.file.header")
      doc.getElementById("try-again").text shouldBe Messages("upload.file.again")
      doc.getElementById("check-file").text shouldBe Messages("check.file")
      doc.getElementById("return-to-upload").text shouldBe Messages("return.to.upload")
    }

    "contain correct ga events when problem uploading file" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("back").attr("data-journey-click") shouldBe "navigation - link:There has been a problem uploading your file:Back"
      doc.getElementById("return-to-upload").attr("data-journey-click") shouldBe "button - click:There has been a problem uploading your file:Return to upload a file"
    }
  }

  "file not available page" should {

    "contain the correct page title" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).title shouldBe Messages("file.not.available.page.title")
    }

    "contain the correct page header" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("header").text shouldBe Messages("file.not.available.page.header")
    }

    "contain a back link pointing to /relief-at-source" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("back").attr("href") shouldBe "/relief-at-source"
    }

    "contain the correct content paragraph" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("sub-header").text shouldBe Messages("file.not.available.sub-header", Messages("file.not.available.link"))
    }

    "contain the correct link in the content paragraph" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("sub-header-link").attr("href") shouldBe "/relief-at-source"
    }

    "contain the correct ga events" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:File not available:Back"
      doc(result).getElementById("sub-header-link").attr("data-journey-click") shouldBe "link - click:File not available:Choose something else to do"
    }
  }

  "not authorised page" should {
    "contain the correct page title" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).title shouldBe Messages("unauthorised.error.page.title")
    }

    "contain the correct page header" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("header").text shouldBe Messages("unauthorised.page.header")
    }

    "contain the correct top paragraph" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("paragraph-top").text shouldBe Messages("unauthorised.paragraph.top")
    }

    "contain the correct bottom paragraph" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("paragraph-bottom").text shouldBe Messages("unauthorised.paragraph.bottom")
    }

    "contain a list with two items" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("action-list").children().size() shouldBe 2
    }

    "first list item should contain the correct text" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("action-list").children().first().text shouldBe Messages("unauthorised.list.first", Messages("unauthorised.list.first.link"))
    }

    "first list item link should have the correct href" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-sign-in").attr("href") shouldBe mockAppConfig.signOutAndContinueUrl
    }

    "second list item should contain the correct text" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("action-list").children().last().text shouldBe Messages("unauthorised.list.last")
    }

    "second list item link should have the correct href" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-register").attr("href") shouldBe "https://online.hmrc.gov.uk/registration/pensions"
    }

    "first list item link should have the correct ga event" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-sign-in").attr("data-journey-click") shouldBe "link - click:There is a problem:Sign in"
    }

    "second list item link should have the correct ga event" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-register").attr("data-journey-click") shouldBe "link - click:There is a problem:Register"
    }
  }
}
