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
import models.upscan.{UpscanFileReference, UpscanInitiateResponse}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.RasTestHelper

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class UpscanControllerSpec extends AnyWordSpec with RasTestHelper {

  given headerCarrier: HeaderCarrier = HeaderCarrier()
  private val enrolmentIdentifier    = EnrolmentIdentifier("PSAID", "Z123456")

  private val enrolment =
    new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")

  val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))
  val memberName: MemberName                  = MemberName("Jackie", "Chan")
  val memberNino: MemberNino                  = MemberNino("AB123456C")
  val memberDob: MemberDateOfBirth            = MemberDateOfBirth(RasDate(Some("12"), Some("12"), Some("2012")))
  val rasSession: RasSession                  = RasSession(memberName, memberNino, memberDob, None)

  val upscanResponse: models.upscan.UpscanInitiateResponse =
    UpscanInitiateResponse(UpscanFileReference(""), "upscan/upload-proxy", Map("" -> ""))

  val mockUploadTimeStamp: Long = Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli

  val fileSession: FileSession =
    FileSession(Some(CallbackData("", None, "", None, None)), None, "1234", Some(Instant.now().toEpochMilli), None)

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  val TestUpscanController: UpscanController = new UpscanController(
    mockUpscanInitiateConnector,
    mockAuthConnector,
    mockFilesSessionService,
    mockRasSessionCacheService,
    mockMCC,
    mockAppConfig,
    fileUploadView,
    fileUploadSuccessfulView,
    cannotUploadAnotherFileView
  ) {
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
  }

  "UpscanController" must {

    "render file upload page" when {
      "a url is successfully created from a file stored in the session" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, None, None, Some(File("existingReference123")))
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
        when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any))
          .thenReturn(Future.successful(upscanResponse))
        when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
        val result     = TestUpscanController.get.apply(fakeRequest)
        status(result) shouldBe OK
        val expectedUrlPart = "upscan/upload-proxy"
        doc(result).getElementById("upload-form").attr("action") should include(expectedUrlPart)
      }

      "a url is successfully created using a new file reference where session does not exist" in {
        val rasSession =
          RasSession(memberName, memberNino, memberDob, None, None, Some(File("0b215e97-11d4-4006-91db-c067e74fc653")))
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
        when(mockUpscanInitiateConnector.initiateUpscan(any(), any(), any())(any()))
          .thenReturn(Future.successful(upscanResponse))
        when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
        val result     = TestUpscanController.get.apply(fakeRequest)
        status(result) shouldBe OK
        val expectedUrlPart = "upscan/upload-proxy"
        doc(result).getElementById("upload-form").attr("action") should include(expectedUrlPart)
      }
    }

    "redirect to cannot upload another file page" when {
      "a file is already in process" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, None, None, Some(File("existingReference123")))
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(true))
        val result     = TestUpscanController.get.apply(fakeRequest)
        status(result)         shouldBe SEE_OTHER
        redirectLocation(result) should include("/cannot-upload-another-file")
      }
    }

    "display file upload page when a file is not in processing" in {
      val rasSession =
        RasSession(memberName, memberNino, memberDob, None, None, Some(File("existingReference123")), Some(false))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      val result     = TestUpscanController.get.apply(fakeRequest)
      status(result) shouldBe OK
    }

    "redirect to global error page" when {
      "the upload error endpoint in called by the file upload but caching fails" in {
        when(mockRasSessionCacheService.cacheUploadResponse(any())(any())).thenReturn(Future.successful(None))
        val uploadRequest = FakeRequest(
          GET,
          "/relief-at-source/upload-error?errorCode=400&reason={%22error%22:{%22msg%22:%22Envelope%20does%20not%20allow%20zero%20length%20files,%20and%20submitted%20file%20has%20length%200%22}}"
        )
        val result        = await(TestUpscanController.uploadError.apply(uploadRequest))
        redirectLocation(result) should include("/file-upload-problem")
      }

      "a url is not successfully created from an existing file stored in the session" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, None, None, None)
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockUpscanInitiateConnector.initiateUpscan(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException))
        val result     = TestUpscanController.get.apply(fakeRequest)
        status(result)         shouldBe SEE_OTHER
        redirectLocation(result) should include("/global-error")
      }

      "a new url is not successfully created" in {
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
        when(mockUpscanInitiateConnector.initiateUpscan(any(), any(), any())(any()))
          .thenReturn(Future.successful(upscanResponse))
        when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(None))
        val result = TestUpscanController.get.apply(fakeRequest)
        status(result)         shouldBe SEE_OTHER
        redirectLocation(result) should include("/global-error")
      }

      "session retrieval fails" in {
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.failed(new RuntimeException))
        val result = TestUpscanController.get.apply(fakeRequest)
        status(result)         shouldBe SEE_OTHER
        redirectLocation(result) should include("/global-error")
      }

      "upload success endpoint has been called but we fail to create a file session" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, None, None, Some(File("existingReference123")))
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockFilesSessionService.createFileSession(any(), any())(any(), any())).thenReturn(Future.successful(false))
        val result     = TestUpscanController.uploadSuccess.apply(fakeRequest)
        redirectLocation(result) should include("/global-error")
      }

      "upload success endpoint has been called but no file exists in the session" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, None, None, None)
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        val result     = TestUpscanController.uploadSuccess.apply(fakeRequest)
        redirectLocation(result) should include("/global-error")
      }
    }

    "redirect to chooseAnOption page when back link is clicked" in {
      val result = TestUpscanController.back.apply(FakeRequest())
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("/")
    }

    "display file upload successful page" when {
      "file has been uploaded successfully" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, None, None, Some(File("existingReference123")))
        when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockFilesSessionService.createFileSession(any(), any())(any(), any())).thenReturn(Future.successful(true))
        val result     = TestUpscanController.uploadSuccess.apply(fakeRequest)
        status(result) shouldBe OK
      }
    }

    "create a file session when a file has been successfully uploaded" in {
      when(mockFilesSessionService.createFileSession(any(), any())(any(), any())).thenReturn(Future.successful(true))
      await(TestUpscanController.uploadSuccess.apply(fakeRequest))
      verify(mockFilesSessionService, atLeastOnce).createFileSession(any(), any())(any(), any())
    }

    "redirect to file upload page" when {
      "the upload error endpoint in called by the file upload" in {
        when(mockRasSessionCacheService.cacheUploadResponse(any())(any()))
          .thenReturn(Future.successful(Some(rasSession)))
        val uploadRequest = FakeRequest(
          GET,
          "/relief-at-source/upload-error?errorCode=400&reason={%22error%22:{%22msg%22:%22Envelope%20does%20not%20allow%20zero%20length%20files,%20and%20submitted%20file%20has%20length%200%22}}"
        )
        val result        = TestUpscanController.uploadError.apply(uploadRequest)
        redirectLocation(result) should include("/upload-a-file")
      }
    }
  }

  "rendered file upload page" must {
    "redirect to problem uploading file if a bad request has been submitted" in {
      val uploadResponse = UploadResponse("400", None)
      val rasSession     = RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse), Some(File("123456")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      val result         = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("/relief-at-source/global-error")
    }

    "contain file too large error if present in session cache" in {
      val uploadResponse = UploadResponse("EntityTooLarge", Some(""))
      val rasSession     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any)).thenReturn(Future.successful(upscanResponse))
      when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      val result         = TestUpscanController.get.apply(fakeRequest)
      doc(result).getElementById("upload-error").text shouldBe "file.large.error"
    }

    "contain file empty error if EntityTooSmall is present in session cache" in {
      val uploadResponse = UploadResponse("EntityTooSmall", Some(""))
      val rasSession     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any)).thenReturn(Future.successful(upscanResponse))
      when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      val result         = TestUpscanController.get.apply(fakeRequest)
      doc(result).getElementById("upload-error").text shouldBe "file.empty.error"
    }

    "redirect to problem uploading file if file not found in session cache" in {
      val uploadResponse = UploadResponse("404", Some(""))
      val rasSession     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any)).thenReturn(Future.successful(upscanResponse))
      when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(None))
      val result         = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("/relief-at-source/global-error")
    }

    "redirect to problem uploading file if file type is wrong" in {
      val uploadResponse = UploadResponse("415", Some(""))
      val rasSession     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any)).thenReturn(Future.successful(upscanResponse))
      when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      val result         = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("/file-upload-problem")
    }

    "redirect to problem uploading file if locked" in {
      val uploadResponse = UploadResponse("423", Some(""))
      val rasSession     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      val result         = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("/file-upload-problem")
    }

    "redirect to problem uploading file if server error" in {
      val uploadResponse = UploadResponse("500", None)
      val rasSession     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any)).thenReturn(Future.successful(upscanResponse))
      when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      val result         = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("/file-upload-problem")
    }

    "redirect to problem uploading file if any unknown errors" in {
      val uploadResponse1 = UploadResponse("500", None)
      val rasSession1     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse1), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession1)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any)).thenReturn(Future.successful(upscanResponse))
      when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockRasSessionCacheService.cacheUploadResponse(any())(any())).thenReturn(Future.successful(None))
      val result          = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("/relief-at-source/global-error")
    }

    "render file upload page if upload response contains no errors" in {
      val uploadResponse1 = UploadResponse("", None)
      val rasSession1     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse1), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession1)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      when(mockUpscanInitiateConnector.initiateUpscan(any, any, any)(any)).thenReturn(Future.successful(upscanResponse))
      when(mockRasSessionCacheService.cacheFile(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockRasSessionCacheService.cacheUploadResponse(any())(any())).thenReturn(Future.successful(Some(rasSession)))
      val result          = TestUpscanController.get.apply(fakeRequest)
      status(result) shouldBe OK
    }

    "redirect to global error page when upload response has not been cleared properly (no session returned form cache upload response)" in {
      val uploadResponse1 = UploadResponse("500", None)
      val rasSession1     =
        RasSession(memberName, memberNino, memberDob, None, Some(uploadResponse1), Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession1)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      when(mockRasSessionCacheService.cacheUploadResponse(any())(any())).thenReturn(Future.successful(None))
      val result          = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("/global-error")
    }
  }

  "cannot upload another file page" must {
    "return global error if there is no file session" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, None, None, Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(true))
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result     = TestUpscanController.uploadInProgress.apply(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("global-error")
    }

    "redirect to file ready if there is a file session and the file is ready" in {
      val rasSession              = RasSession(memberName, memberNino, memberDob, None, None, Some(File("existingReference123")))
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFilesSessionService.isFileInProgress(any())(any(), any())).thenReturn(Future.successful(true))
      val mockResultsFileMetadata = ResultsFileMetaData("", Some("testFile.csv"), Some(mockUploadTimeStamp), 1, 1L)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession.copy(resultsFile = Some(mockResultsFileMetadata)))))
      val result                  = TestUpscanController.uploadInProgress.apply(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("file-ready")
    }

    "return Ok with cannot-upload-another-file view when file session exists but no results file" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession.copy(resultsFile = None))))
      val result = TestUpscanController.uploadInProgress.apply(fakeRequest)
      status(result) shouldBe OK
    }
  }

  "uploadSuccess when ras session cannot be retrieved" must {
    "redirect to global error page" in {
      when(mockRasSessionCacheService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val result = TestUpscanController.uploadSuccess.apply(fakeRequest)
      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should include("global-error")
    }
  }

  "UpscanController unauthenticated" must {
    def stubUnauth(): Unit =
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session")))

    "redirect get to GG sign-in" in {
      stubUnauth()
      val result = TestUpscanController.get.apply(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect back to GG sign-in" in {
      stubUnauth()
      val result = TestUpscanController.back.apply(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect uploadSuccess to GG sign-in" in {
      stubUnauth()
      val result = TestUpscanController.uploadSuccess.apply(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect uploadError to GG sign-in" in {
      stubUnauth()
      val result = TestUpscanController.uploadError.apply(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }

    "redirect uploadInProgress to GG sign-in" in {
      stubUnauth()
      val result = TestUpscanController.uploadInProgress.apply(fakeRequest)
      redirectLocation(result) should include("gg/sign-in")
    }
  }

}
