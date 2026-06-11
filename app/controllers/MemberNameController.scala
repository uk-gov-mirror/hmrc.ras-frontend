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
import connectors.ResidencyStatusAPIConnector
import forms.MemberNameForm.*
import models.ApiVersion
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest}
import services.SessionCacheService
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MemberNameController @Inject() (
  val authConnector: DefaultAuthConnector,
  val connector: DefaultAuditConnector,
  val residencyStatusAPIConnector: ResidencyStatusAPIConnector,
  val sessionService: SessionCacheService,
  val mcc: MessagesControllerComponents,
  val appConfig: ApplicationConfig,
  memberNameView: views.html.member_name
) extends FrontendController(mcc)
    with RasResidencyCheckerController
    with PageFlowController
    with Logging
    with WithUrlEncodedOnlyFormBinding {
  given ApplicationConfig    = appConfig
  given ec: ExecutionContext = mcc.executionContext
  val apiVersion: ApiVersion = appConfig.rasApiVersion

  def get(edit: Boolean = false): Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)   =>
        sessionService.fetchRasSession() map {
          case Some(session) => Ok(memberNameView(form.fill(session.name), edit))
          case _             => Ok(memberNameView(form, edit))
        }
      case Left(resp) =>
        logger.warn("[NameController][get] user Not authorised")
        resp
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(userId) =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              logger.warn("[NameController][post] Invalid form field passed")
              Future.successful(BadRequest(memberNameView(formWithErrors, edit)))
            },
            memberName =>
              sessionService.cacheName(memberName) flatMap {
                case Some(session) =>
                  if (edit) {
                    submitResidencyStatus(session, userId)
                  } else {
                    Future.successful(Redirect(routes.MemberNinoController.get()))
                  }
                case _             => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
              }
          )
      case Left(res)     =>
        logger.warn("[NameController][post] user Not authorised")
        res
    }
  }

  def back(edit: Boolean = false): Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(_)  => Future.successful(previousPage("MemberNameController", edit))
      case Left(res) =>
        logger.warn("[NameController][back] user Not authorised")
        res
    }
  }

}
