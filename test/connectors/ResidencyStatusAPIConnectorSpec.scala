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

import models.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, InternalServerException, UpstreamErrorResponse}
import utils.RasTestHelper

import java.io.InputStream
import scala.io.Source

class ResidencyStatusAPIConnectorSpec extends AnyWordSpec with Matchers with RasTestHelper {
  val httpClient: HttpClientV2               = fakeApplication.injector.instanceOf[HttpClientV2]
  val connector: ResidencyStatusAPIConnector = new ResidencyStatusAPIConnector(httpClient, mockAppConfig)

  val memberName: MemberName = MemberName("John", "Smith")
  val rasDate: RasDate       = RasDate(Some("1"), Some("2"), Some("1990"))
  val memberDetails          = MemberDetails(memberName, "AB123456C", rasDate)
  val fileName               = "testFile.csv"

  val residencyStatusJson: String =
    """
      |{
      |  "currentYearResidencyStatus": "scotResident",
      |  "nextYearForecastResidencyStatus": "otherUKResident"
      |}
  """.stripMargin

  val expectedResponse = ResidencyStatus("scotResident", Some("otherUKResident"))

  "getResidencyStatus" should {

    "return the success response when API returns 200" in {
      setupMockPost(OK, residencyStatusJson, "/residency-status")
      val result: ResidencyStatus = await(connector.getResidencyStatus(memberDetails))
      result shouldBe expectedResponse
    }

    "throw internal server error when API returns 400" in {
      setupMockPost(BAD_REQUEST, "{}", "/residency-status")
      val result = intercept[InternalServerException] {
        await(connector.getResidencyStatus(memberDetails))
      }
      result.getMessage should include("Internal Server Error")
    }

    "throw UpstreamErrorResponse when API returns 403" in {
      setupMockPost(FORBIDDEN, "{}", "/residency-status")
      val result = intercept[UpstreamErrorResponse] {
        await(connector.getResidencyStatus(memberDetails))
      }
      result.getMessage should include("Member not found")
    }

    "throw InternalServerException when API returns 5XX" in {
      setupMockPost(INTERNAL_SERVER_ERROR, "{}", "/residency-status")
      val result = intercept[InternalServerException] {
        await(connector.getResidencyStatus(memberDetails))
      }
      result.getMessage should include("Internal Server Error")
    }

    "throw InternalServerException when API returns 200 but the body cannot be parsed as ResidencyStatus" in {
      setupMockPost(OK, "{}", "/residency-status")
      val result = intercept[InternalServerException] {
        await(connector.getResidencyStatus(memberDetails))
      }
      result.getMessage should include("could not be parsed")
    }
  }

  "getFile" should {

    val record = "John,Smith,AB123456C,1990-02-21"
    "return the success response when API returns 200" in {
      setupMockGet(OK, record, s"/ras-api/file/getFile/$fileName")
      val result: Option[InputStream] = await(connector.getFile(fileName, "A123456"))
      result.isDefined shouldBe true

      val value = result.get

      val content = Source.fromInputStream(value).mkString
      value.close()
      content should include("John")
    }

    "return empty InputStream when response body is empty" in {
      setupMockGet(OK, "", s"/ras-api/file/getFile/$fileName")
      val result: Option[InputStream] = await(connector.getFile(fileName, "A123456"))
      result.isDefined shouldBe true

      val content = Source.fromInputStream(result.get).mkString
      content shouldBe empty
    }

    "return InputStream even when response status is 500" in {
      setupMockGet(INTERNAL_SERVER_ERROR, "Internal server error", s"/ras-api/file/getFile/$fileName")
      val result: Option[InputStream] = await(connector.getFile(fileName, "A123456"))
      result.isDefined shouldBe true

      val content = Source.fromInputStream(result.get).mkString
      content shouldBe "Internal server error"
    }

  }

  "deleteFile" should {

    "return the response as OK once the file is deleted successfully" in {
      setupMockDelete(OK, "", s"/ras-api/file/remove/$fileName/A123456")
      val result: HttpResponse = await(connector.deleteFile(fileName, "A123456"))
      result.status shouldBe OK
    }

    "return the response as Bad Request if API returns 400" in {
      setupMockDelete(BAD_REQUEST, "", s"/ras-api/file/remove/$fileName/A123456")
      val result: HttpResponse = await(connector.deleteFile(fileName, "A123456"))
      result.status shouldBe BAD_REQUEST
    }
  }

}
