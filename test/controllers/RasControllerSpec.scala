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

import config.ApplicationConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import utils.RasTestHelper

import scala.concurrent.{ExecutionContext, Future}

class RasControllerSpec extends AnyWordSpec with RasTestHelper {

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")

  val testController: RasController = new RasController {
    override val appConfig: ApplicationConfig = mockAppConfig
    override def authConnector: AuthConnector = mockAuthConnector
  }

  "Ras Controller" must {

    "successful retrieval of enrolment when authorising" when {

      "a users is authorised and valid HMRC-PSA-ORG enrolment is retrieved" in {

        val enrolmentIdentifier                     = EnrolmentIdentifier("PSAID", "Z123456")
        val enrolment                               =
          new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
        val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        val authorisedResult = testController.isAuthorised()
        val result           = authorisedResult.map { case Right(res) =>
          res
        }

        await(result) shouldBe "Z123456"
      }

      "a users is authorised and valid HMRC-PODS-ORG enrolment is retrieved" in {

        val enrolmentIdentifier                     = EnrolmentIdentifier("PSAID", "Z123456")
        val enrolment                               =
          new Enrolment(key = "HMRC-PODS-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
        val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        val authorisedResult = testController.isAuthorised()
        val result           = authorisedResult.map { case Right(res) =>
          res
        }

        await(result) shouldBe "Z123456"
      }

      "a users is authorised and valid HMRC-PODSPP-ORG enrolment is retrieved" in {

        val enrolmentIdentifier                     = EnrolmentIdentifier("PSPID", "Z123456")
        val enrolment                               =
          new Enrolment(key = "HMRC-PODSPP-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
        val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        val authorisedResult = testController.isAuthorised()
        val result           = authorisedResult.map { case Right(res) =>
          res
        }

        await(result) shouldBe "Z123456"
      }

      "a users is authorised and valid HMRC-PP-ORG enrolment is retrieved" in {

        val enrolmentIdentifier                     = EnrolmentIdentifier("PPID", "Z123456")
        val enrolment                               = new Enrolment(key = "HMRC-PP-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
        val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        val authorisedResult = testController.isAuthorised()
        val result           = authorisedResult.map { case Right(res) =>
          res
        }

        await(result) shouldBe "Z123456"
      }
    }

    "redirect when user is not logged in" when {

      "an non logged in response is returned from auth - expired bearer token" in {

        val unsuccessfulRetrieval: Future[NoActiveSession] = Future.failed(BearerTokenExpired(""))
        when(
          mockAuthConnector.authorise[NoActiveSession](any[Predicate], any[Retrieval[NoActiveSession]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(unsuccessfulRetrieval)

        val authorisedResult = testController.isAuthorised()
        val result           = authorisedResult.map { case Left(res) =>
          await(res)
        }
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(
          result
        )              shouldBe "http://localhost:9025/gg/sign-in?continue_url=%2Frelief-at-source%2F&origin=ras-frontend"
      }
    }

    "redirect when user is not authorised" when {

      "an unauthorised response is returned from auth - Missing Response Header" in {

        val unsuccessfulRetrieval: Future[AuthorisationException] =
          Future.failed(InternalError("MissingResponseHeader"))
        when(
          mockAuthConnector.authorise[AuthorisationException](any[Predicate], any[Retrieval[AuthorisationException]])(
            any[HeaderCarrier],
            any[ExecutionContext]
          )
        ).thenReturn(unsuccessfulRetrieval)

        val authorisedResult = testController.isAuthorised()
        val result           = authorisedResult.map { case Left(res) =>
          await(res)
        }
        status(result)           shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe "/relief-at-source/not-authorised"
      }
    }

    "an unauthorised response is returned from auth - Invalid Response header" in {

      val unsuccessfulRetrieval: Future[AuthorisationException] = Future.failed(InternalError("InvalidResponseHeader"))
      when(
        mockAuthConnector.authorise[AuthorisationException](any[Predicate], any[Retrieval[AuthorisationException]])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(unsuccessfulRetrieval)

      val authorisedResult = testController.isAuthorised()
      val result           = authorisedResult.map { case Left(res) =>
        await(res)
      }
      status(result)           shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe "/relief-at-source/not-authorised"
    }
  }

}
