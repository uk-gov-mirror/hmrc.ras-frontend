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

import java.time.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Result
import play.api.test.Helpers.{OK, contentAsString, *}
import play.api.test.{FakeRequest, Helpers}
import services.TaxYearResolver
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.RasTestHelper

import java.io.ByteArrayInputStream
import scala.concurrent.Future

class ChooseAnOptionControllerSpec extends AnyWordSpec with RasTestHelper {

  given headerCarrier: HeaderCarrier = HeaderCarrier()
  val currentTaxYear: Int            = TaxYearResolver.currentTaxYear

  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")

  private val enrolment =
    new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")

  val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))
  val mockUploadTimeStamp: Long               = Instant.now().minus(Duration.ofDays(10)).toEpochMilli
  val mockExpiryTimeStamp: Long               = Instant.now().minus(Duration.ofDays(7)).toEpochMilli

  val mockResultsFileMetadata: ResultsFileMetaData =
    ResultsFileMetaData("", Some("testFile.csv"), Some(mockUploadTimeStamp), 1, 1L)

  val fileSession: FileSession = FileSession(
    Some(CallbackData("", None, "", None, None)),
    Some(mockResultsFileMetadata),
    "1234",
    Some(Instant.now().toEpochMilli),
    None
  )

  val row1        = "John,Smith,AB123456C,1990-02-21"
  val inputStream = new ByteArrayInputStream(row1.getBytes)

  val TestChooseAnOptionController: ChooseAnOptionController = new ChooseAnOptionController(
    mockResidencyStatusAPIConnector,
    mockAuthConnector,
    mockFilesSessionService,
    mockMCC,
    mockAppConfig,
    chooseAnOptionView,
    fileReadyView,
    uploadResultView,
    resultsNotAvailableYetView,
    noResultsAvailableView
  ) {

    when(mockFilesSessionService.determineFileStatus(any())(any(), any()))
      .thenReturn(Future.successful(FileUploadStatus.NoFileSession))
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
    when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
    when(mockResidencyStatusAPIConnector.getFile(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(inputStream)))
  }

  "getHelpDate" must {
    import models.FileUploadStatus.*
    val testTimeStamp                   = LocalDateTime.of(2013, 4, 5, 0, 0, 0, 0).atZone(ZoneId.of("Europe/London"))
    val currentTime                     = LocalDateTime.of(2014, 4, 6, 0, 0, 0, 0).atZone(ZoneId.of("Europe/London"))
    val timestampInMillis: Option[Long] = Some(testTimeStamp.toInstant.toEpochMilli)
    val mockResultsFileMetadata         = ResultsFileMetaData("", Some("testFile.csv"), timestampInMillis, 1, 1L)
    val optionalFileSession             = Some(
      FileSession(
        Some(CallbackData("", None, "", None, None)),
        Some(mockResultsFileMetadata),
        "1234",
        Some(currentTime.toInstant.toEpochMilli),
        None
      )
    )

    "return the expiry date format message" in {
      val result = TestChooseAnOptionController.getHelpDate(Ready, optionalFileSession)
      result shouldBe Some("0:00am on Monday 8 April 2013")
    }

    "return the upload date format message" in {
      val result = TestChooseAnOptionController.getHelpDate(InProgress, optionalFileSession)
      result shouldBe Some("yesterday at 12:00am")
    }

    "return None when there fileStatus is not Ready or InProgress" in {
      val result = TestChooseAnOptionController.getHelpDate(NoFileSession, optionalFileSession)
      result shouldBe None
    }

    "return None when there is no File session" in {
      val result = TestChooseAnOptionController.getHelpDate(Ready, None)
      result shouldBe None
    }
  }

  "get" when {

    "for any status" must {
      "respond to GET /choose-an-option-to-get-residency-status" in {
        val result: Future[Result] = TestChooseAnOptionController.get(fakeRequest)
        status(result) shouldBe OK
      }
    }
  }

  "renderUploadResultsPage" must {
    "return ok when called" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result: Result = await(TestChooseAnOptionController.renderUploadResultsPage(fakeRequest))
      result.header.status shouldBe OK
    }

    "return global error" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession.copy(userFile = None))))
      val result: Future[Result] = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
      redirectLocation(result) should include("/global-error")
    }

    "download a file containing the results" in {
      val mockUploadTimeStamp     = LocalDate.ofEpochDay(2018 - 12 - 31).toEpochDay
      val mockResultsFileMetadata = ResultsFileMetaData("", Some("testFile.csv"), Some(mockUploadTimeStamp), 1, 1L)
      val fileSession             =
        FileSession(Some(CallbackData("", None, "", None, None)), Some(mockResultsFileMetadata), "1234", None, None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result                  = TestChooseAnOptionController
        .getResultsFile("testFile.csv")
        .apply(FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      contentAsString(result) shouldBe row1
    }

    "not be able to download a file containing the results when file name is incorrect" in {
      val mockUploadTimeStamp     = LocalDateTime.of(2018, 12, 31, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli
      val mockResultsFileMetadata = ResultsFileMetaData("", Some("wrongName.csv"), Some(mockUploadTimeStamp), 1, 1L)
      val fileSession             =
        FileSession(Some(CallbackData("", None, "", None, None)), Some(mockResultsFileMetadata), "1234", None, None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result                  = TestChooseAnOptionController
        .getResultsFile("testFile.csv")
        .apply(FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/file-not-available")
    }

    "not be able to download a file containing the results when there is no results file" in {
      val fileSession = FileSession(Some(CallbackData("", None, "", None, None)), None, "1234", None, None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result      = TestChooseAnOptionController
        .getResultsFile("testFile.csv")
        .apply(FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/file-not-available")
    }

    "not be able to download a file containing the results when there is no file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController
        .getResultsFile("testFile.csv")
        .apply(FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/file-not-available")
    }

    "redirect to error page" when {

      "render upload result page is called but there is no callback data in the retrieved file session" in {
        val fileSession = FileSession(
          None,
          Some(ResultsFileMetaData("", None, None, 1, 1L)),
          "1234",
          Some(LocalDateTime.now().plusDays(10).toInstant(ZoneOffset.UTC).toEpochMilli),
          None
        )
        when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
          .thenReturn(Future.successful(Some(fileSession)))
        val result      = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
        status(result)         shouldBe SEE_OTHER
        redirectLocation(result) should include("/global-error")
      }
    }
  }

  "renderFileReadyPage" must {
    "return ok when called" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderFileReadyPage(fakeRequest)
      status(result) shouldBe OK
    }

    "return global error page when there is no file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.renderFileReadyPage(fakeRequest)
      redirectLocation(result) should include("/global-error")
    }

    "redirect to cannot upload another file there is no result file ready" in {
      val fileSession = FileSession(None, None, "1234", None, None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result      = TestChooseAnOptionController.renderFileReadyPage(fakeRequest)
      redirectLocation(result) should include("/cannot-upload-another-file")
    }
  }

  "renderResultsNotAvailableYetPage" must {
    "return ok when called" in {
      val fileSession = FileSession(None, None, "1234", None, None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result      = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/results-not-available")
    }

    "return error when there is a result file in file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderNoResultsAvailableYetPage(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/residency-status-added")
    }

    "return error when there is no file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.renderNoResultsAvailableYetPage(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/no-results-available")
    }
  }

  "renderNoResultAvailablePage" must {
    "return ok when called" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.renderNoResultAvailablePage(fakeRequest)
      status(result)                                 shouldBe OK
      await(await(result).body.consumeData).utf8String should include("You have not uploaded a file")
    }

    "redirect to results-not-avilable when there is a file session with a file in progress" in {
      val session = fileSession.copy(resultsFile = None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(session)))
      val result  = TestChooseAnOptionController.renderNoResultAvailablePage(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/results-not-available")
    }

    "redirect to results page when there is a file session with a file ready" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderNoResultAvailablePage(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/residency-status-added")
    }
  }

  "fomattedExpiryDate method" must {
    "return correctly formatted date and time" in {
      val date = LocalDateTime.of(2020, 3, 20, 10, 30, 0, 0)
      assert(
        TestChooseAnOptionController.formattedExpiryDate(
          date.toInstant(ZoneOffset.UTC).toEpochMilli
        ) == "10:30am on Monday 23 March 2020"
      )
    }
  }

  "getResultsFile when the file connector fails" must {
    "redirect to global error page" in {
      val resultsFileMetadata = ResultsFileMetaData("", Some("testFile.csv"), Some(mockUploadTimeStamp), 1, 1L)
      val sessionWithResults  =
        FileSession(Some(CallbackData("", None, "", None, None)), Some(resultsFileMetadata), "1234", None, None)

      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(sessionWithResults)))
      when(mockResidencyStatusAPIConnector.getFile(any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("download failed")))

      val result = TestChooseAnOptionController
        .getResultsFile("testFile.csv")
        .apply(FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))

      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/global-error")
    }

    "Failure when the streamed download terminates with an exception" in {
      val resultsFileMetadata = ResultsFileMetaData("", Some("testFile.csv"), Some(mockUploadTimeStamp), 1, 1L)
      val sessionWithResults  =
        FileSession(Some(CallbackData("", None, "", None, None)), Some(resultsFileMetadata), "1234", None, None)
      val failingStream       = new java.io.InputStream {
        override def read(): Int = throw new java.io.IOException("read failed")
      }

      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(sessionWithResults)))
      when(mockResidencyStatusAPIConnector.getFile(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(failingStream)))

      val result = TestChooseAnOptionController
        .getResultsFile("testFile.csv")
        .apply(FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      // Consume the response body so the stream is materialized and watchTermination's Failure branch executes
      scala.util.Try(contentAsString(result))
      status(result) shouldBe OK
    }
  }

  "getHelpDate with today's upload" must {
    "return the upload date message with 'today' prefix when the upload timestamp falls on today" in {
      import models.FileUploadStatus.InProgress
      val nowMillis        = Instant.now().toEpochMilli
      val fileSessionToday = FileSession(
        Some(CallbackData("", None, "", None, None)),
        None,
        "1234",
        Some(nowMillis),
        None
      )
      val result           = TestChooseAnOptionController.getHelpDate(InProgress, Some(fileSessionToday))
      result.get should startWith("today at ")
    }
  }

  "renderUploadResultsPage when no file session" must {
    "redirect to no-results-available page" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/no-results-available")
    }
  }

  "renderNoResultsAvailableYetPage when results file is None but file session exists" must {
    "return OK with the results-not-available-yet view" in {
      val fileSessionNoResults = fileSession.copy(resultsFile = None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSessionNoResults)))
      val result               = TestChooseAnOptionController.renderNoResultsAvailableYetPage(fakeRequest)
      status(result) shouldBe OK
    }
  }

  "ChooseAnOptionController unauthenticated" must {
    def stubUnauth(): Unit =
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))

    "redirect get to GG sign-in" in {
      stubUnauth()
      val result = TestChooseAnOptionController.get(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect renderUploadResultsPage to GG sign-in" in {
      stubUnauth()
      val result = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect renderNoResultAvailablePage to GG sign-in" in {
      stubUnauth()
      val result = TestChooseAnOptionController.renderNoResultAvailablePage(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect renderNoResultsAvailableYetPage to GG sign-in" in {
      stubUnauth()
      val result = TestChooseAnOptionController.renderNoResultsAvailableYetPage(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect renderFileReadyPage to GG sign-in" in {
      stubUnauth()
      val result = TestChooseAnOptionController.renderFileReadyPage(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect getResultsFile to GG sign-in" in {
      stubUnauth()
      val result = TestChooseAnOptionController.getResultsFile("testFile.csv").apply(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }
  }

}
