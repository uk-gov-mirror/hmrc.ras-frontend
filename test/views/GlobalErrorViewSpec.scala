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

import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.api.test.Helpers.*
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import utils.RasTestHelper

class GlobalErrorViewSpec extends AnyWordSpec with RasTestHelper {

  "global error page" must {

    "contain correct title and header" in {
      val result = globalErrorView()(fakeRequest, testMessages, mockAppConfig)
      val doc    = Jsoup.parse(contentAsString(result))
      doc.title                                       shouldBe Messages("global.error.page.title")
      doc.getElementsByClass("govuk-heading-xl").text shouldBe Messages("global.error.header")
      doc.getElementsByClass("govuk-body").text       shouldBe Messages("you.can.either")
    }

    "request the service navigation component on the report-a-problem link" in {
      val result = globalErrorView()(fakeRequest, testMessages, mockAppConfig)
      val doc    = Jsoup.parse(contentAsString(result))
      val href   = doc.select("a.hmrc-report-technical-issue").attr("href")

      href should startWith("http://localhost:9250/contact/report-technical-problem")
      href should include("useServiceNavigation")
    }

    "still request the service navigation component when there is no referrer URL" in {
      val noReferrerConfig =
        new ContactFrontendConfig(Configuration("contact-frontend.host" -> "http://localhost:9250")) {
          override def referrerUrl(implicit request: RequestHeader): Option[String] = None
        }

      val view = new views.html.global_error(govukWrapperView, noReferrerConfig)
      val doc  = Jsoup.parse(contentAsString(view()(fakeRequest, testMessages, mockAppConfig)))
      val href = doc.select("a.hmrc-report-technical-issue").attr("href")

      href should not include "referrerUrl"
      href should endWith("useServiceNavigation")
    }
  }

}
