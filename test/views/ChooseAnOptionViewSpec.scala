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

package views

import models.FileUploadStatus.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import utils.RasTestHelper

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.Locale

class ChooseAnOptionViewSpec extends AnyWordSpec with RasTestHelper {

  val mockExpiryTimeStamp: Long = Instant.now().toEpochMilli
  val zoneID: ZoneId            = ZoneId.of("Europe/London")

  def formattedExpiryDate(timestamp: Long): Option[String] = {
    val expiryDate    = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneID)
    val timeFormatter = DateTimeFormatter.ofPattern("H:mma").withLocale(Locale.UK).withZone(zoneID)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy").withLocale(Locale.UK).withZone(zoneID)

    val formattedDate = s"${expiryDate.format(timeFormatter).toLowerCase()} on ${expiryDate.format(dateFormatter)}"
    Some(formattedDate)
  }

  private def formattedUploadDate(timestamp: Long): Option[String] = {
    val uploadDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneID)

    val todayOrYesterday = if (uploadDate.toLocalDate.isEqual(ZonedDateTime.now.toLocalDate)) {
      "today"
    } else {
      "yesterday"
    }
    Some(
      s"$todayOrYesterday at ${uploadDate.format(DateTimeFormatter.ofPattern("h:mma").withLocale(Locale.UK)).toLowerCase()}"
    )
  }

  "choose an option page" when {
    "the file status is NoFileSession" must {
      "contain an Upload a file link" in {
        val result = chooseAnOptionView(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementById("upload-link").text()     shouldBe Messages("Upload a file")
        doc(result).getElementById("upload-link").attr("href") should include("/upload-a-file")
        doc(result)
          .getElementById("upload-link")
          .attr("data-journey-click")                        shouldBe "link - click:Choose option to get residency status:Upload a file"
      }

      "contain the correct title and header" in {
        val result = chooseAnOptionView(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
        doc(result).title                                       shouldBe Messages("chooseAnOption.page.title", Messages("filestatus.NoFileSession"))
        doc(result).getElementsByClass("govuk-heading-xl").text shouldBe Messages("chooseAnOption.page.header")
      }

      "contain the single member h2" in {
        val result = chooseAnOptionView(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("app-task-list__section").get(0).html() shouldBe Messages(
          "single.member.subheading"
        )
      }

      "contain the enter a members detail link" in {
        val result = chooseAnOptionView(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementById("single-member-link").text       shouldBe Messages("enter.members.details")
        doc(result).getElementById("single-member-link").attr("href") should include("/member-name")
        doc(result)
          .getElementById("single-member-link")
          .attr(
            "data-journey-click"
          )                                                         shouldBe "link - click:Choose option to get residency status:Enter a members details"
      }

      "contain the Multiple members h2" in {
        val result = chooseAnOptionView(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("app-task-list__section").get(1).html() shouldBe Messages(
          "multiple.members.subheading"
        )
      }
    }

    "the file status is Ready" must {
      "contain a download your results link" in {
        val result = chooseAnOptionView(Ready, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("task-name").get(1).html()     shouldBe Messages("download.results")
        doc(result).getElementById("download-result-link").attr("href") should include("/residency-status-added")
        doc(result)
          .getElementById("download-result-link")
          .attr(
            "data-journey-click"
          )                                                           shouldBe "link - click:Choose option to get residency status:Download your results"

      }

      "contain a File ready Icon" in {
        val result = chooseAnOptionView(Ready, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("task-completed").text shouldBe Messages("file.ready")
      }

      "contain File ready paragraph" in {
        val date          = LocalDateTime.ofInstant(Instant.ofEpochMilli(mockExpiryTimeStamp), zoneID)
        val result        =
          chooseAnOptionView(Ready, formattedExpiryDate(mockExpiryTimeStamp))(fakeRequest, testMessages, mockAppConfig)
        val timeFormatter = DateTimeFormatter.ofPattern("H:mma").withLocale(Locale.UK).withZone(zoneID)
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy").withLocale(Locale.UK).withZone(zoneID)

        doc(result).getElementsByClass("paragraph-info").text shouldBe Messages(
          "result.timescale",
          s"${date.format(timeFormatter).toLowerCase()} on ${date.format(dateFormatter)}"
        )
      }
    }

    "for Processing Only" must {
      "contain a Processing icon" in {
        val result = chooseAnOptionView(InProgress, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("task-completed").text shouldBe Messages("file.processing")

      }

      "contain File processing paragraphs with todays date" in {
        val date   = Instant.now().minus(1, ChronoUnit.DAYS)
        val result = chooseAnOptionView(InProgress, formattedUploadDate(mockExpiryTimeStamp))(
          fakeRequest,
          testMessages,
          mockAppConfig
        )
        doc(result).getElementsByClass("paragraph-info").get(0).text() shouldBe Messages("file.processing") + Messages(
          "file.upload.time",
          s"${Messages("today")} at ${date.atZone(zoneID).format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase()}"
        )
        doc(result).getElementsByClass("paragraph-info").get(1).text() shouldBe Messages("file.size.info")
        doc(result).getElementsByClass("paragraph-info").get(2).text() shouldBe Messages("processing.file")

      }

      "contain File processing paragraphs with yesterday date" in {
        val date   = Instant.now().minus(1, ChronoUnit.DAYS)
        val result = chooseAnOptionView(InProgress, formattedUploadDate(date.toEpochMilli))(
          fakeRequest,
          testMessages,
          mockAppConfig
        )
        doc(result).getElementsByClass("paragraph-info").get(0).text() shouldBe Messages("file.processing") + Messages(
          "file.upload.time",
          s"${Messages("yesterday")} at ${date.atZone(zoneID).format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase()}"
        )
        doc(result).getElementsByClass("paragraph-info").get(1).text() shouldBe Messages("file.size.info")
        doc(result).getElementsByClass("paragraph-info").get(2).text() shouldBe Messages("processing.file")
      }
    }

    "for UploadError Only" must {
      "contain an upload your file again link" in {
        val result = chooseAnOptionView(UploadError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementById("file-problem-link").text()     shouldBe Messages("upload.file.again")
        doc(result).getElementById("file-problem-link").attr("href") should include("/upload-a-file")
        doc(result)
          .getElementById("file-problem-link")
          .attr("data-journey-click")                              shouldBe "link - click:Choose option to get residency status:Upload a file"
      }

      "contain a File problem icon" in {
        val result = chooseAnOptionView(UploadError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("task-completed").text shouldBe Messages("file.problem")
      }

      "contain File problem paragraphs" in {
        val result = chooseAnOptionView(UploadError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("paragraph-info").get(0).text shouldBe Messages("file.problem") + Messages(
          "problem.getting.result"
        )
        doc(result).getElementsByClass("paragraph-info").get(1).text shouldBe Messages(
          "file.problem.paragraph.start"
        ) + " " + Messages("upload.file.again") + " " + Messages("file.problem.paragraph.end")
      }
    }

    "for TimeExpiryError Only" must {
      "contain an upload your file again link" in {
        val result = chooseAnOptionView(TimeExpiryError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("file-problem-link").text()     shouldBe Messages("upload.file.again")
        doc(result).getElementsByClass("file-problem-link").attr("href") should include("/upload-a-file")
        doc(result)
          .getElementsByClass("file-problem-link")
          .attr("data-journey-click")                                  shouldBe "link - click:Choose option to get residency status:Upload a file"
      }

      "contain a File problem icon" in {
        val result = chooseAnOptionView(TimeExpiryError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("task-completed").text shouldBe Messages("file.problem")

      }

      "contain File problem paragraphs" in {
        val result = chooseAnOptionView(TimeExpiryError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
        doc(result).getElementsByClass("paragraph-info").get(0).text shouldBe Messages("file.problem") + Messages(
          "problem.getting.result"
        )
        doc(result).getElementsByClass("paragraph-info").get(1).text shouldBe Messages(
          "file.problem.paragraph.start"
        ) + " " + Messages("upload.file.again") + " " + Messages("file.problem.paragraph.end")
      }
    }
  }

}
