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

import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.twirl.api.Html
import utils.RasTestHelper

import scala.jdk.CollectionConverters.*

class GovukWrapperViewSpec extends AnyWordSpec with RasTestHelper {

  private def renderedPage: Document =
    doc(govukWrapperView("Test page")(Html("<p>content</p>"))(fakeRequest, testMessages, applicationConfig))

  "govuk_wrapper" should {

    "render the service navigation component" in {
      renderedPage.select(".govuk-service-navigation").size should be > 0
    }

    "request the service navigation component on every link to a shared PlatUI page" in {
      val document = renderedPage

      val sharedPagePaths = Seq(
        "/accessibility-statement/",
        "/contact/report-technical-problem",
        "/help/cookies",
        "/help/privacy",
        "/help/terms-and-conditions"
      )

      sharedPagePaths.foreach { path =>
        withClue(s"links to $path: ") {
          val hrefs = document.select(s"""a[href*="$path"]""").eachAttr("href").asScala.toSeq
          hrefs should not be empty
          hrefs.foreach(_ should include("useServiceNavigation"))
        }
      }
    }
  }

}
