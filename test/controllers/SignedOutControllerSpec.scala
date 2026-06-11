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

package controllers

import org.scalatest.OptionValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.RasTestHelper

import scala.concurrent.{ExecutionContext, Future}

class SignedOutControllerSpec extends AnyWordSpec with Matchers with RasTestHelper {

  "SignedOut Controller" should {

    "when User is not authenticated" in {

      val request = FakeRequest(GET, routes.SignedOutController.signedOut.url)

      val result = route(fakeApplication, request).value

      status(result) shouldBe SEE_OTHER

    }

    class TestController extends SignedOutController(mockAuthConnector, mockMCC, mockAppConfig, signedOutView) {
      override def isAuthorised()(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[Either[Future[Result], String]] =
        Future.successful(Right("testUserId"))
    }

    "when User is authenticated" in {

      val controller = new TestController

      val request = FakeRequest(GET, routes.SignedOutController.signedOut.url)

      val result = controller.signedOut.apply(request)

      status(result) shouldBe OK

    }

  }

}
