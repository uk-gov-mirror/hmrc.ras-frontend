/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper
import play.api.test.Helpers._


class MatchFoundViewSpec extends UnitSpec with RasTestHelper {

	override val SCOTTISH = "Scotland"
	val NON_SCOTTISH = "England, Northern Ireland or Wales"

	val dob: LocalDate = new LocalDate(1999, 1, 1)
	"match found page" should {
		"contain correct title when match found" in {
			val result = matchFoundView("Jim Mcgill", dob.toString("d MMMM yyyy"), "AA123456A", NON_SCOTTISH, Some(SCOTTISH), 1000, 1001)(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))
			doc.title shouldBe Messages("match.found.page.title")
		}

		"contain customer details and residency status when match found and CY and CY+1 is present" in {
			val result = matchFoundView("Jim Mcgill", dob.toString("d MMMM yyyy"), "AA123456A", SCOTTISH, Some(NON_SCOTTISH), 1000, 1001)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("what-happens-next-content").text shouldBe Messages("match.found.top")
			doc(result).getElementById("what-happens-next-sub-header").text shouldBe Messages("match.found.what.happens.next")
			doc(result).getElementById("cy-residency-status").text shouldBe SCOTTISH
			doc(result).getElementById("ny-residency-status").text shouldBe NON_SCOTTISH
			doc(result).getElementById("choose-something-else-link").text shouldBe Messages("choose.something.else")
		}

		"contain correct ga events when match found and CY and CY+1 is present" in {
			val result = matchFoundView("Jim Mcgill", dob.toString("d MMMM yyyy"), "AA123456A", NON_SCOTTISH, Some(SCOTTISH), 1000, 1001)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else-link").attr("data-journey-click") shouldBe "button - click:Residency status added CY & CY + 1:Choose something else to do"
			doc(result).getElementById("look-up-another-member-link").attr("data-journey-click") shouldBe "link - click:Residency status added CY & CY + 1:Look up another member"
		}

		"contain customer details and residency status when match found and only CY is present" in {
			val result = matchFoundView("Jim Mcgill", dob.toString("d MMMM yyyy"), "AA123456A", SCOTTISH, None, 1000, 1001)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("what-happens-next-content").text shouldBe Messages("match.found.top")
			doc(result).getElementById("what-happens-next-sub-header").text shouldBe Messages("match.found.what.happens.next")
			doc(result).getElementById("bottom-content-cy").text shouldBe Messages("match.found.bottom.current-year.bottom", (1000 + 1).toString, "Jim Mcgill", (1000 + 1).toString, (1000 + 2).toString)
			doc(result).getElementById("cy-residency-status").text shouldBe SCOTTISH
			doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
		}

		"contain correct ga event when match found and only CY is present" in {
			val result = matchFoundView("Jim Mcgill", dob.toString("d MMMM yyyy"), "AA123456A", NON_SCOTTISH, None, 1000, 1001)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else-link").attr("data-journey-click") shouldBe "button - click:Residency status added CY:Choose something else to do"
			doc(result).getElementById("look-up-another-member-link").attr("data-journey-click") shouldBe "link - click:Residency status added CY:Look up another member"
		}

		"display correct residency status for UK UK" in {
			val result = matchFoundView("Jim Mcgill", dob.toString("d MMMM yyyy"), "AA123456A", NON_SCOTTISH, Some(NON_SCOTTISH), 1000, 1001)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("cy-residency-status").text shouldBe NON_SCOTTISH
			doc(result).getElementById("ny-residency-status").text shouldBe NON_SCOTTISH
		}

		"contain a look up another member link when match found" in {
			val result = matchFoundView("Jim Mcgill", dob.toString("d MMMM yyyy"), "AA123456A", SCOTTISH, None, 1000, 1001)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("look-up-another-member-link").attr("href") shouldBe "/relief-at-source/check-another-member/member-name?cleanSession=true"
		}
	}
}
