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
import services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ErrorController @Inject() (
  val authConnector: DefaultAuthConnector,
  val sessionService: SessionCacheService,
  val mcc: MessagesControllerComponents,
  globalErrorView: views.html.global_error,
  problemUploadingFileView: views.html.problem_uploading_file,
  fileNotAvailableView: views.html.file_not_available,
  unauthorisedView: views.html.unauthorised,
  startAtStartView: views.html.sorry_you_need_to_start_again
)(using val appConfig: ApplicationConfig)
    extends FrontendController(mcc) with RasController with Logging {

  given ec: ExecutionContext = mcc.executionContext

  def renderGlobalErrorPage: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)   =>
        logger.info("[ErrorController][renderGlobalErrorPage] rendering global error page")
        Future.successful(InternalServerError(globalErrorView()))
      case Left(resp) =>
        logger.warn("[ErrorController][renderGlobalErrorPage] user not authorised")
        resp
    }
  }

  def renderProblemUploadingFilePage: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)   =>
        logger.info("[ErrorController][renderProblemUploadingFilePage] rendering problem uploading file page")
        Future.successful(InternalServerError(problemUploadingFileView()))
      case Left(resp) =>
        logger.warn("[ErrorController][renderProblemUploadingFilePage] user not authorised")
        resp
    }
  }

  def fileNotAvailable: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)   =>
        logger.info("[ErrorController][fileNotAvailable] rendering file not available page")
        Future.successful(InternalServerError(fileNotAvailableView()))
      case Left(resp) =>
        logger.warn("[ErrorController][fileNotAvailable] user not authorised")
        resp
    }
  }

  def notAuthorised: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    Future.successful(Unauthorized(unauthorisedView()))
  }

  def startAtStart: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    Future.successful(BadRequest(startAtStartView()))
  }

}
