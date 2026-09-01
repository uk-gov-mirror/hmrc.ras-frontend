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

package views.helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.Html
import utils.RasTestHelper

class ViewSpecHelper extends AnyWordSpec with RasTestHelper {

  def pageWithFeedbackLink(page: Html): Assertion = {
    val body: Document = Jsoup.parse(contentAsString(page))

    assert(
      body
        .getElementById("feedback-link")
        .html() == """<a href="http://localhost:9514/feedback/ras?useServiceNavigation" class="govuk-link">What did you think of this service?</a> (takes 30 seconds)""",
      "the feedback link was not found or was invalid"
    )
  }

}
