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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.LocalDate

class ModelCoverageSpec extends AnyWordSpec with Matchers {

  "FileUploadStatus" should {
    "expose the five enum values" in {
      val all = Set(
        FileUploadStatus.NoFileSession,
        FileUploadStatus.Ready,
        FileUploadStatus.InProgress,
        FileUploadStatus.UploadError,
        FileUploadStatus.TimeExpiryError
      )
      all                     should have size 5
      FileUploadStatus.values should contain allElementsOf all
    }
  }

  "Questionnaire form" should {
    "round-trip a filled form (exercises the reverse mapping)" in {
      val q      = Questionnaire(easyToUse = 1, satisfactionLevel = 2, whyGiveThisRating = Some("ok"), referer = None)
      val filled = Questionnaire.form.fill(q)
      filled.value shouldBe Some(q)
    }
  }

  "RasDate" should {
    "format toString as year-month-day" in {
      val date = RasDate(Some("1"), Some("2"), Some("1999"))
      date.toString shouldBe "Some(1999)-Some(2)-Some(1)"
    }

    "report isInFuture = false for a past date" in {
      val past = RasDate(Some("1"), Some("1"), Some("1900"))
      past.isInFuture shouldBe false
    }

    "report isInFuture = true for a far-future date" in {
      val futureYear = (LocalDate.now.getYear + 5).toString
      val future     = RasDate(Some("1"), Some("1"), Some(futureYear))
      future.isInFuture shouldBe true
    }

    "default missing day/month/year fields to 1 in asLocalDate" in {
      RasDate(None, None, None).asLocalDate shouldBe LocalDate.of(1, 1, 1)
    }
  }

  "ResidencyStatus" should {
    "expose the SCOTTISH and NON_SCOTTISH constants" in {
      val rs = ResidencyStatus("scotResident", Some("otherUKResident"))
      rs.SCOTTISH     shouldBe "scotResident"
      rs.NON_SCOTTISH shouldBe "otherUKResident"
    }

    "round-trip through JSON" in {
      val rs = ResidencyStatus("scotResident", Some("otherUKResident"))
      Json.toJson(rs).as[ResidencyStatus] shouldBe rs
    }
  }

  "RasSession.selectKeysToCache" should {
    val baseName = MemberName("Jim", "McGill")
    val baseNino = MemberNino("AB123456C")
    val baseDob  = MemberDateOfBirth(RasDate(Some("1"), Some("1"), Some("1990")))
    val session  = RasSession(baseName, baseNino, baseDob)

    "replace name when CacheKey.Name is supplied with Some" in {
      val newName = MemberName("New", "Name")
      session.selectKeysToCache(session, CacheKey.Name, Some(newName)).name shouldBe newName
    }

    "clear name when CacheKey.Name is supplied with None" in {
      session.selectKeysToCache(session, CacheKey.Name, None).name shouldBe RasSession.cleanMemberName
    }

    "leave session unchanged when CacheKey.Name is supplied with a value of the wrong type" in {
      session.selectKeysToCache(session, CacheKey.Name.asInstanceOf[CacheKey[Any]], Some("not a name")) shouldBe session
    }

    "replace nino when CacheKey.Nino is supplied with Some" in {
      val newNino = MemberNino("CD654321D")
      session.selectKeysToCache(session, CacheKey.Nino, Some(newNino)).nino shouldBe newNino
    }

    "clear nino when CacheKey.Nino is supplied with None" in {
      session.selectKeysToCache(session, CacheKey.Nino, None).nino shouldBe RasSession.cleanMemberNino
    }

    "leave session unchanged when CacheKey.Nino is supplied with a value of the wrong type" in {
      session.selectKeysToCache(session, CacheKey.Nino.asInstanceOf[CacheKey[Any]], Some(123)) shouldBe session
    }

    "replace dob when CacheKey.Dob is supplied with Some" in {
      val newDob = MemberDateOfBirth(RasDate(Some("2"), Some("2"), Some("2000")))
      session.selectKeysToCache(session, CacheKey.Dob, Some(newDob)).dateOfBirth shouldBe newDob
    }

    "clear dob when CacheKey.Dob is supplied with None" in {
      session.selectKeysToCache(session, CacheKey.Dob, None).dateOfBirth shouldBe RasSession.cleanMemberDateOfBirth
    }

    "leave session unchanged when CacheKey.Dob is supplied with a value of the wrong type" in {
      session.selectKeysToCache(session, CacheKey.Dob.asInstanceOf[CacheKey[Any]], Some("nope")) shouldBe session
    }

    "replace residencyStatusResult when CacheKey.StatusResult is supplied with Some" in {
      val rsr = ResidencyStatusResult("scotResident", None, "2024", "2025", "Jim McGill", "1990-01-01", "AB123456C")
      session.selectKeysToCache(session, CacheKey.StatusResult, Some(rsr)).residencyStatusResult shouldBe Some(rsr)
    }

    "clear residencyStatusResult when CacheKey.StatusResult is supplied with None" in {
      session.selectKeysToCache(session, CacheKey.StatusResult, None).residencyStatusResult shouldBe None
    }

    "leave session unchanged when CacheKey.StatusResult is supplied with a value of the wrong type" in {
      session.selectKeysToCache(session, CacheKey.StatusResult.asInstanceOf[CacheKey[Any]], Some(42)) shouldBe session
    }

    "replace uploadResponse when CacheKey.UploadResponse is supplied with Some" in {
      val ur = UploadResponse("200", None)
      session.selectKeysToCache(session, CacheKey.UploadResponse, Some(ur)).uploadResponse shouldBe Some(ur)
    }

    "clear uploadResponse when CacheKey.UploadResponse is supplied with None" in {
      session.selectKeysToCache(session, CacheKey.UploadResponse, None).uploadResponse shouldBe None
    }

    "leave session unchanged when CacheKey.UploadResponse is supplied with a value of the wrong type" in {
      session.selectKeysToCache(
        session,
        CacheKey.UploadResponse.asInstanceOf[CacheKey[Any]],
        Some("wrong")
      ) shouldBe session
    }

    "replace file when CacheKey.File is supplied with Some" in {
      val file = File("ref-123")
      session.selectKeysToCache(session, CacheKey.File, Some(file)).file shouldBe Some(file)
    }

    "clear file when CacheKey.File is supplied with None" in {
      session.selectKeysToCache(session, CacheKey.File, None).file shouldBe None
    }

    "leave session unchanged when CacheKey.File is supplied with a value of the wrong type" in {
      session.selectKeysToCache(session, CacheKey.File.asInstanceOf[CacheKey[Any]], Some(7)) shouldBe session
    }

    "return a clean session when CacheKey.All is supplied" in {
      session.selectKeysToCache(session, CacheKey.All, None) shouldBe RasSession.cleanSession
    }

    "throw IllegalArgumentException when an unknown CacheKey is supplied" in {
      object UnknownKey extends CacheKey[Any]
      val ex = intercept[IllegalArgumentException] {
        session.selectKeysToCache(session, UnknownKey, None)
      }
      ex.getMessage should include("Mismatched key and value types")
    }
  }

  "UpscanInitiateRequest" should {
    "fill in default values for optional fields when only the callback URL is provided" in {
      val req = models.upscan.UpscanInitiateRequest("https://callback.example/url")
      req.callbackUrl     shouldBe "https://callback.example/url"
      req.successRedirect shouldBe None
      req.errorRedirect   shouldBe None
      req.minimumFileSize shouldBe None
      req.maximumFileSize shouldBe Some(2097152)
    }

    "round-trip a populated request through JSON" in {
      val req = models.upscan.UpscanInitiateRequest(
        callbackUrl = "https://callback.example/url",
        successRedirect = Some("https://success"),
        errorRedirect = Some("https://err"),
        minimumFileSize = Some(0),
        maximumFileSize = Some(1024)
      )
      Json.toJson(req).as[models.upscan.UpscanInitiateRequest] shouldBe req
    }
  }

  "PreparedUpload" should {
    "parse from a JSON payload (exercises the Reads)" in {
      val json   = Json.parse("""{
        "reference": "ref-123",
        "uploadRequest": {
          "href": "https://upload.example",
          "fields": { "key": "value" }
        }
      }""")
      val parsed = json.as[models.upscan.PreparedUpload]
      parsed.reference.value             shouldBe "ref-123"
      parsed.uploadRequest.href          shouldBe "https://upload.example"
      parsed.uploadRequest.fields("key") shouldBe "value"
    }
  }

  "Upscan response types" should {
    "construct UpscanFileReference, UpscanInitiateResponse, UploadForm and UploadDetails" in {
      import models.upscan.*
      val ref      = UpscanFileReference("ref-1")
      val form     = UploadForm("https://target", Map("a" -> "b"))
      val response = UpscanInitiateResponse(ref, "https://target", Map("a" -> "b"))
      val details  = UploadDetails("id-1", UploadId("upload-1"), Reference("ref-1"), NotStarted)

      ref.reference           shouldBe "ref-1"
      form.href               shouldBe "https://target"
      response.fileReference  shouldBe ref
      response.postTarget     shouldBe "https://target"
      response.formFields     shouldBe Map("a" -> "b")
      details.status          shouldBe NotStarted
      details.uploadId.value  shouldBe "upload-1"
      details.reference.value shouldBe "ref-1"
    }
  }

  "UploadStatus reads" should {
    "exercise the UploadedSuccessfully arm of the JsString matcher" in {
      import models.upscan.*
      import play.api.libs.json.JsString
      // The reads matches on JsString and routes "UploadedSuccessfully" through
      // `Json.fromJson[UploadedSuccessfully](json)`, which fails because the same
      // JsString can't be reparsed as an UploadedSuccessfully object — the line is
      // executed (covering the arm) and the validation returns JsError.
      JsString("UploadedSuccessfully").validate[UploadStatus].isError shouldBe true
    }

    "round-trip a written UploadedSuccessfully payload as JsObject" in {
      import models.upscan.*
      val status = UploadedSuccessfully("file.csv", "text/csv", "https://download.example", Some(1234L))
      val asJson = Json.toJson(status: UploadStatus)
      (asJson \ "_type").as[String] shouldBe "UploadedSuccessfully"
      (asJson \ "name").as[String]  shouldBe "file.csv"
    }

    "parse the simple JsString variants" in {
      import models.upscan.*
      import play.api.libs.json.{JsObject, JsString}
      JsString("NotStarted").as[UploadStatus]            shouldBe NotStarted
      JsString("InProgress").as[UploadStatus]            shouldBe InProgress
      JsString("Failed").as[UploadStatus]                shouldBe Failed
      JsString("Unknown").validate[UploadStatus].isError shouldBe true
      JsObject(Seq.empty).validate[UploadStatus].isError shouldBe true
    }

    "write NotStarted / InProgress / Failed as plain JsString" in {
      import models.upscan.*
      Json.toJson(NotStarted: UploadStatus) shouldBe play.api.libs.json.JsString("NotStarted")
      Json.toJson(InProgress: UploadStatus) shouldBe play.api.libs.json.JsString("InProgress")
      Json.toJson(Failed: UploadStatus)     shouldBe play.api.libs.json.JsString("Failed")
    }
  }

  "RasSession JSON formatter" should {
    "round-trip a populated session" in {
      val rsr     = ResidencyStatusResult("scotResident", None, "2024", "2025", "Jim McGill", "1990-01-01", "AB123456C")
      val session = RasSession(
        MemberName("Jim", "McGill"),
        MemberNino("AB123456C"),
        MemberDateOfBirth(RasDate(Some("1"), Some("1"), Some("1990"))),
        Some(rsr),
        Some(UploadResponse("200", None)),
        Some(File("ref-1")),
        Some(true)
      )
      Json.toJson(session).as[RasSession] shouldBe session
    }
  }

}
