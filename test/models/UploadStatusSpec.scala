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

package models

import models.upscan.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class UploadStatusSpec extends AnyWordSpec with Matchers {

  val expectedJson = UploadedSuccessfully("test", "csv", "file.csv", Some(10))

  "UploadStatus reads" should {

    "convert from string to object" in {
      Json.fromJson[UploadStatus](JsString("NotStarted")) shouldBe JsSuccess(NotStarted)
      Json.fromJson[UploadStatus](JsString("InProgress")) shouldBe JsSuccess(InProgress)
      Json.fromJson[UploadStatus](JsString("Failed"))     shouldBe JsSuccess(Failed)
    }

    "convert UploadedSuccessfully from full object" in {
      val json = Json.obj(
        "_type"       -> "UploadedSuccessfully",
        "name"        -> "test",
        "mimeType"    -> "csv",
        "downloadUrl" -> "file.csv",
        "size"        -> 10
      )

      Json.fromJson[UploadedSuccessfully](json) shouldBe JsSuccess(expectedJson)
    }

    "fail to convert when _type is missing" in {
      val json = Json.obj("fileName" -> "file.csv")
      Json.fromJson[UploadStatus](json) shouldBe JsError("Missing _type field")
    }

    "value is an unexpected string" in {
      val json   = JsString("unexpected")
      val result = Json.fromJson[UploadStatus](json)
      result shouldBe JsError("Unexpected value of _type: unexpected")
    }

  }

  "UploadStatus writes" should {

    "serialize simple statuses to string" in {
      Json.toJson(NotStarted: UploadStatus) shouldBe JsString("NotStarted")
      Json.toJson(InProgress: UploadStatus) shouldBe JsString("InProgress")
      Json.toJson(Failed: UploadStatus)     shouldBe JsString("Failed")
    }

    "serialize UploadedSuccessfully to JSON with _type" in {

      val json = Json.toJson(expectedJson: UploadStatus)
      (json \ "_type").as[String]       shouldBe "UploadedSuccessfully"
      (json \ "name").as[String]        shouldBe "test"
      (json \ "mimeType").as[String]    shouldBe "csv"
      (json \ "downloadUrl").as[String] shouldBe "file.csv"
      (json \ "size").as[Int]           shouldBe 10
    }

  }

}
