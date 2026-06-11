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

package config

import org.scalatest.matchers.must.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.{Messages, MessagesApi}
import utils.RasTestHelper
import views.html.{error, global_page_not_found}

import scala.concurrent.ExecutionContext

class ErrorHandlerSpec extends AnyWordSpec with RasTestHelper {

  private val messageApi: MessagesApi = fakeApplicationCreation.injector.instanceOf[MessagesApi]
  private val errorTemplate: error    = fakeApplicationCreation.injector.instanceOf[error]

  private val errorNotFoundTemplate: global_page_not_found =
    fakeApplicationCreation.injector.instanceOf[global_page_not_found]

  private val errorHandler: ErrorHandler =
    new ErrorHandler(messageApi, mockAppConfig, ExecutionContext.global, errorTemplate, errorNotFoundTemplate)

  "ErrorHandler" must {

    "return an error page" in {
      val result = await(
        errorHandler.standardErrorTemplate(
          pageTitle = "pageTitle",
          heading = "heading",
          message = "message"
        )(using fakeRequest)
      )

      result.body must include("pageTitle")
      result.body must include("heading")
      result.body must include("message")
    }

    "return a not found template" in {
      val result = await(errorHandler.notFoundTemplate(fakeRequest))

      val pageNotFoundTitle    = Messages("error.page_not_found.tabtitle")
      val pageNotFoundHeading  = Messages("error.page_not_found.heading")
      val pageNotFoundMessage1 = Messages("error.page.not.found.error.check.web.address.correct")
      val pageNotFoundMessage2 = Messages("error.page.not.found.error.check.web.address.full")
      val pageNotFoundMessage3 = Messages("error.page.not.found.error.contact")

      result.body must include(pageNotFoundTitle)
      result.body must include(pageNotFoundHeading)
      result.body must include(pageNotFoundMessage1)
      result.body must include(pageNotFoundMessage2)
      result.body must include(pageNotFoundMessage3)
    }

  }

}
