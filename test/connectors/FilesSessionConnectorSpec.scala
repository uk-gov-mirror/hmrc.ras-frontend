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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.{CallbackData, CreateFileSessionRequest, FileSession, ResultsFileMetaData}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, CREATED, NO_CONTENT, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import utils.RasTestHelper

class FilesSessionConnectorSpec extends AnyWordSpec with Matchers with RasTestHelper {
  val httpClient: HttpClientV2 = fakeApplication.injector.instanceOf[HttpClientV2]

  val connector: FilesSessionConnector = new FilesSessionConnector(httpClient, mockAppConfig)

  val callbackData: CallbackData       = CallbackData("reference-1234", None, "READY", None, None)
  val resultsFile: ResultsFileMetaData = ResultsFileMetaData("file-id-1", Some("fileName.csv"), Some(1234L), 123, 1234L)

  val fileSession: Option[FileSession] = Some(
    FileSession(Some(callbackData), Some(resultsFile), "A123456", Some(1765890906194L), None)
  )

  val fileSessionJson: String =
    """{"userFile":{"reference":"reference-1234","fileStatus":"READY"},"resultsFile":{"id":"file-id-1","filename":"fileName.csv","uploadDate":1234,"chunkSize":123,"length":1234},"userId":"A123456","uploadTimeStamp":1765890906194}""".stripMargin

  "fetchFileSession" should {

    "return correct file session response when file fetch call is successful" in {
      setupMockGet(OK, fileSessionJson, "/get-file-session/A123456")
      val result: Option[FileSession] = await(connector.fetchFileSession("A123456"))
      result shouldBe fileSession
      wireMockServer.verify(getRequestedFor(urlEqualTo("/get-file-session/A123456")))
    }

    "return None as the response when file fetch call is successful with bad request" in {
      setupMockGet(BAD_REQUEST, "{}", "/get-file-session/A123456")
      val result: Option[FileSession] = await(connector.fetchFileSession("A123456"))
      result shouldBe None
      wireMockServer.verify(getRequestedFor(urlEqualTo("/get-file-session/A123456")))
    }

    "return Ok for invalid json response in file fetch call" in {
      val invalidJson: String         = """{"test":"value"}""".stripMargin
      setupMockGet(OK, invalidJson, "/get-file-session/A123456")
      val result: Option[FileSession] = await(connector.fetchFileSession("A123456"))
      result shouldBe None
    }

  }

  "createFileSession" should {
    val fileSession: CreateFileSessionRequest = CreateFileSessionRequest("A123456", "reference-1234")

    "return the response as true once the file session is successful" in {
      setupMockPost(CREATED, fileSession.toString, "/create-file-session")
      val result: Boolean = await(connector.createFileSession(fileSession))
      result shouldBe true
      wireMockServer.verify(postRequestedFor(urlEqualTo("/create-file-session")))
    }

    "return the response as false if the file session is not successful" in {
      setupMockPost(BAD_REQUEST, "{}", "/create-file-session")
      val result: Boolean = await(connector.createFileSession(fileSession))
      result shouldBe false
      wireMockServer.verify(postRequestedFor(urlEqualTo("/create-file-session")))
    }

  }

  "deleteFileSession" should {

    "return the response as true once the file session is deleted successfully" in {
      setupMockDelete(NO_CONTENT, "A123456", "/delete-file-session/A123456")
      val result: Boolean = await(connector.deleteFileSession("A123456"))
      result shouldBe true
      wireMockServer.verify(deleteRequestedFor(urlEqualTo("/delete-file-session/A123456")))
    }

    "return the response as false if the deletion is failed" in {
      setupMockDelete(BAD_REQUEST, "{}", "/delete-file-session/A123456")
      val result: Boolean = await(connector.deleteFileSession("A123456"))
      result shouldBe false
      wireMockServer.verify(deleteRequestedFor(urlEqualTo("/delete-file-session/A123456")))
    }

  }

}
