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

package services

import models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import utils.RasTestHelper

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class FilesSessionServiceSpec extends PlaySpec with RasTestHelper with PrivateMethodTester {

  val callbackData: CallbackData       = CallbackData("reference-1234", None, "READY", None, None)
  val resultsFile: ResultsFileMetaData = ResultsFileMetaData("file-id-1", Some("fileName.csv"), Some(1234L), 123, 1234L)
  val fileMetaData: FileMetadata       = FileMetadata("file-id-1", Some("fileName.csv"), None)
  val newFileSession: FileSession      = FileSession(None, None, "A123456", Some(Instant.now().toEpochMilli), None)

  val fileSession: FileSession =
    FileSession(Some(callbackData), Some(resultsFile), "A123456", Some(Instant.now().toEpochMilli), None)

  val failedFileSession: FileSession = FileSession(
    Some(callbackData.copy(fileStatus = "ERROR")),
    Some(resultsFile),
    "A123456",
    Some(Instant.now().toEpochMilli),
    None
  )

  val filesSessionService: FilesSessionService = new FilesSessionService(mockFilesSessionConnector, mockAppConfig)

  "createFileSession" must {
    "return true if session created successfully" in {
      when(mockFilesSessionConnector.createFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = filesSessionService.createFileSession("A123456", "reference-1234").futureValue

      result shouldBe true
    }

    "return false if session not created" in {
      when(mockFilesSessionConnector.createFileSession(any())(any(), any()))
        .thenReturn(Future.successful(false))

      val result: Boolean = filesSessionService.createFileSession("A123456", "reference-1234").futureValue

      result shouldBe false
    }
  }

  "fetchFileSession" must {
    "return fileSession" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(newFileSession)))

      val result: FileSession = filesSessionService.fetchFileSession("A123456").futureValue.value

      result.copy(uploadTimeStamp = None) shouldBe newFileSession.copy(uploadTimeStamp = None)
    }

    "return None if file session was not found" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result: Option[FileSession] = filesSessionService.fetchFileSession("A123456").futureValue

      result shouldBe None
    }
  }

  "removeFileSessionFromCache" must {
    "return true if file session was removed" in {
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = filesSessionService.removeFileSessionFromCache("A123456").futureValue

      result shouldBe true
    }

    "return false if file session remove failed" in {
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(false))

      val result: Boolean = filesSessionService.removeFileSessionFromCache("A123456").futureValue

      result shouldBe false
    }
  }

  "hasBeen24HoursSinceTheUpload" must {
    "return true if more than 24 hours have passed since the upload" in {
      val hasBeen24HoursSinceTheUpload = PrivateMethod[Boolean](Symbol("hasBeen24HoursSinceTheUpload"))

      val fileUploadTime = System.currentTimeMillis() - 25 * 60 * 60 * 1000 // 25 hours ago

      val result: Boolean = filesSessionService.invokePrivate(hasBeen24HoursSinceTheUpload(fileUploadTime))

      result shouldBe true
    }

    "return false if less than 24 hours have passed since the upload" in {
      val hasBeen24HoursSinceTheUpload = PrivateMethod[Boolean](Symbol("hasBeen24HoursSinceTheUpload"))

      val fileUploadTime = System.currentTimeMillis() - 23 * 60 * 60 * 1000 // 23 hours ago

      val result: Boolean = filesSessionService.invokePrivate(hasBeen24HoursSinceTheUpload(fileUploadTime))

      result shouldBe false
    }
  }

  "failedProcessingUploadedFile" must {
    "return true if processing of uploaded file failed" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(failedFileSession)))
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = filesSessionService.failedProcessingUploadedFile("A123456").futureValue

      result shouldBe true
    }

    "return false if processing of uploaded file was successful" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))

      val result: Boolean = filesSessionService.failedProcessingUploadedFile("A123456").futureValue

      result shouldBe false
    }

    "return true when the file session exists but results file is empty" in {
      val fileSessionWithoutResultsFile: FileSession = FileSession(
        Some(callbackData),
        None,
        "A123456",
        Some(Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli),
        None
      )

      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSessionWithoutResultsFile)))

      val result: Boolean = filesSessionService.failedProcessingUploadedFile("A123456").futureValue

      result shouldBe true
    }

    "return false when no upload timestamp is found" in {
      val fileSessionWithoutTimestamp: FileSession =
        FileSession(Some(callbackData), Some(resultsFile), "A123456", None, None)

      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSessionWithoutTimestamp)))

      val result: Boolean = filesSessionService.failedProcessingUploadedFile("A123456").futureValue

      result shouldBe false
    }

    "return false when no file session is found" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result: Boolean = filesSessionService.failedProcessingUploadedFile("A123456").futureValue

      result shouldBe false
    }
  }

  "errorInFileUpload" must {
    "return true if there was error during upload" in {
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = filesSessionService.errorInFileUpload(failedFileSession)

      result shouldBe true
    }

    "return false if there was no error during upload" in {
      val result: Boolean = filesSessionService.errorInFileUpload(fileSession)

      result shouldBe false
    }

    "return false if userFile is not defined" in {
      val result: Boolean = filesSessionService.errorInFileUpload(fileSession.copy(userFile = None))

      result shouldBe false
    }
  }

  "getDownloadFileName" must {
    "return file name" in {
      val result: String = filesSessionService.getDownloadFileName(fileSession.copy(fileMetadata = Some(fileMetaData)))

      result shouldBe "fileName"
    }

    "return default file name" in {
      val result: String = filesSessionService.getDownloadFileName(fileSession)

      result shouldBe "Residency-status"
    }
  }

  "isFileInProgress" must {
    "return true if file is in progress" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession)))

      val result: Boolean = filesSessionService.isFileInProgress("A123456").futureValue

      result shouldBe true
    }

    "return false if file session is not defined" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result: Boolean = filesSessionService.isFileInProgress("B123456").futureValue

      result shouldBe false
    }
  }

  "determineFileStatus" must {
    "return Ready if results file available" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession.copy(resultsFile = Some(resultsFile)))))
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: FileUploadStatus.Value = filesSessionService.determineFileStatus("A123456").futureValue

      result shouldBe FileUploadStatus.Ready
    }

    "return InProgress if file processing is not completed" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(fileSession.copy(resultsFile = None))))
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: FileUploadStatus.Value = filesSessionService.determineFileStatus("A123456").futureValue

      result shouldBe FileUploadStatus.InProgress
    }

    "return UploadError if failedProcessingUploadedFile returns true" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(Some(failedFileSession.copy(resultsFile = None))))
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: FileUploadStatus.Value = filesSessionService.determineFileStatus("A123456").futureValue

      result shouldBe FileUploadStatus.UploadError
    }

    "return NoFileSession if no entry in db for userId" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.successful(None))
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: FileUploadStatus.Value = filesSessionService.determineFileStatus("A123456").futureValue

      result shouldBe FileUploadStatus.NoFileSession
    }

    "return TimeExpiryError if no entry in db for userId " in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              failedFileSession.copy(
                resultsFile = None,
                uploadTimeStamp = Some(Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli)
              )
            )
          )
        )
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result: FileUploadStatus.Value = filesSessionService.determineFileStatus("A123456").futureValue

      result shouldBe FileUploadStatus.TimeExpiryError
    }
  }

  "createFileSession recover" must {
    "return false when the connector future fails" in {
      when(mockFilesSessionConnector.createFileSession(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("connector boom")))

      val result: Boolean = filesSessionService.createFileSession("A123456", "reference-1234").futureValue

      result shouldBe false
    }
  }

  "fetchFileSession recover" must {
    "return None when the connector future fails" in {
      when(mockFilesSessionConnector.fetchFileSession(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("connector boom")))

      val result: Option[FileSession] = filesSessionService.fetchFileSession("A123456").futureValue

      result shouldBe None
    }
  }

  "removeFileSessionFromCache recover" must {
    "return false when the connector future fails" in {
      when(mockFilesSessionConnector.deleteFileSession(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("connector boom")))

      val result: Boolean = filesSessionService.removeFileSessionFromCache("A123456").futureValue

      result shouldBe false
    }
  }

  "isFileInProgress recover" must {
    "return false when the fetched session future fails" in {
      val failingService: FilesSessionService =
        new FilesSessionService(mockFilesSessionConnector, mockAppConfig) {
          override def fetchFileSession(userId: String)(implicit
            hc: HeaderCarrier,
            ec: ExecutionContext
          ): Future[Option[FileSession]] =
            Future.failed(new RuntimeException("fetch boom"))
        }

      val result: Boolean = failingService.isFileInProgress("A123456").futureValue

      result shouldBe false
    }
  }

}
