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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest}
import services.{SessionCacheService, TaxYearResolver}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ResultsController @Inject() (
  val authConnector: AuthConnector,
  val sessionService: SessionCacheService,
  val mcc: MessagesControllerComponents,
  val appConfig: ApplicationConfig,
  matchFoundView: views.html.match_found,
  matchNotFoundView: views.html.match_not_found
) extends FrontendController(mcc) with PageFlowController with Logging {

  given ApplicationConfig    = appConfig
  given ec: ExecutionContext = mcc.executionContext

  def matchFound: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)  =>
        sessionService.fetchRasSession() map {
          case Some(session) =>
            session.residencyStatusResult match {
              case Some(residencyStatusResult) =>
                val name                       = s"${session.name.firstName.capitalize} ${session.name.lastName.capitalize}"
                val dateOfBirth                = session.dateOfBirth.dateOfBirth.toString
                val nino                       = session.nino.nino
                val currentTaxYear             = TaxYearResolver.currentTaxYear
                val nextTaxYear                = TaxYearResolver.nextTaxYear
                val currentYearResidencyStatus = residencyStatusResult.currentYearResidencyStatus
                val nextYearResidencyStatus    = residencyStatusResult.nextYearResidencyStatus

                sessionService.resetRasSession()

                logger.info("[ResultsController][matchFound] Successfully retrieved ras session")
                Ok(
                  matchFoundView(
                    name,
                    dateOfBirth,
                    nino,
                    currentYearResidencyStatus,
                    nextYearResidencyStatus,
                    currentTaxYear,
                    nextTaxYear
                  )
                )

              case _ =>
                logger.info(
                  "[ResultsController][matchFound] Session does not contain residency status result - wrong result"
                )
                Redirect(routes.ChooseAnOptionController.get)
            }
          case _             =>
            logger.error("[ResultsController][matchFound] failed to retrieve ras session")
            Redirect(routes.ErrorController.renderGlobalErrorPage)
        }
      case Left(res) =>
        logger.warn("[ResultsController][matchFound] user Not authorised")
        res
    }
  }

  def noMatchFound: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)  =>
        sessionService.fetchRasSession() map {
          case Some(session) =>
            if (session.name.hasAValue && session.nino.hasAValue && session.dateOfBirth.hasAValue) {
              session.residencyStatusResult match {
                case None =>

                  val name        = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
                  val nino        = session.nino.nino
                  val dateOfBirth = session.dateOfBirth.dateOfBirth.asLocalDate
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(Locale.UK))
                  logger.info("[ResultsController][noMatchFound] Successfully retrieved ras session")
                  Ok(matchNotFoundView(name, dateOfBirth, nino))

                case Some(_) =>
                  logger.info("[ResultsController][noMatchFound] Session contains residency result - wrong result")
                  Redirect(routes.ChooseAnOptionController.get)
              }
            } else {
              logger.info("[ResultsController][noMatchFound] Session does not contain residency status result")
              Redirect(routes.ChooseAnOptionController.get)
            }
          case _             =>
            logger.error("[ResultsController][noMatchFound] failed to retrieve ras session")
            Redirect(routes.ErrorController.renderGlobalErrorPage)
        }
      case Left(res) =>
        logger.warn("[ResultsController][matchFound] user Not authorised")
        res
    }
  }

  def back: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)  =>
        sessionService.fetchRasSession() map {
          case Some(_) => previousPage("ResultsController")
          case _       => Redirect(routes.ErrorController.renderGlobalErrorPage)
        }
      case Left(res) =>
        logger.warn("[ResultsController][back] user Not authorised")
        res
    }
  }

}
