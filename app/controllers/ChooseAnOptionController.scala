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

import org.apache.pekko.stream.scaladsl.{Source, StreamConverters}
import org.apache.pekko.util.ByteString
import config.ApplicationConfig
import connectors.ResidencyStatusAPIConnector
import models.FileUploadStatus.*
import models.{FileSession, FileUploadStatus}

import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}
import play.api.Logging
import play.api.http.HttpEntity
import play.api.mvc.*
import services.{FilesSessionService, TaxYearResolver}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ChooseAnOptionController @Inject() (
  resultsFileConnector: ResidencyStatusAPIConnector,
  val authConnector: DefaultAuthConnector,
  val filesSessionService: FilesSessionService,
  val mcc: MessagesControllerComponents,
  val appConfig: ApplicationConfig,
  chooseAnOptionView: views.html.choose_an_option,
  fileReadyView: views.html.file_ready,
  uploadResultView: views.html.upload_result,
  resultsNotAvailableYetView: views.html.results_not_available_yet,
  noResultsAvailableView: views.html.no_results_available
) extends FrontendController(mcc) with PageFlowController with Logging {

  given ApplicationConfig    = appConfig
  given ec: ExecutionContext = mcc.executionContext
  private val _contentType   = "application/csv"

  def get: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(userId) =>
        filesSessionService.fetchFileSession(userId).flatMap { fileSession =>
          filesSessionService.determineFileStatus(userId).flatMap { fileStatus =>
            logger.info(s"[ChooseAnOptionController][get] determine file status returned $fileStatus")
            Future.successful(Ok(chooseAnOptionView(fileStatus, getHelpDate(fileStatus, fileSession))))
          }
        }
      case Left(resp)    =>
        logger.warn("[ChooseAnOptionController][get] user not authorised")
        resp
    }
  }

  private[controllers] def getHelpDate(
    fileStatus: FileUploadStatus.Value,
    fileSession: Option[FileSession]
  ): Option[String] =
    fileSession match {
      case Some(fileSession) =>
        fileStatus match {
          case Ready      =>
            Some(
              formattedExpiryDate(fileSession.resultsFile.get.uploadDate.get)
            ) // will always have a resultsfile and date if ready
          case InProgress =>
            Some(
              formattedUploadDate(fileSession.uploadTimeStamp.get)
            ) // will always have an upload date time if in progress
          case _          => None
        }
      case _                 => None
    }

  def formattedExpiryDate(timestamp: Long): String = {

    val expiryDate = Instant.ofEpochMilli(timestamp).plus(3, ChronoUnit.DAYS)

    val timeFormatter = DateTimeFormatter.ofPattern("H:mma")
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")

    s"${expiryDate.atZone(ZoneId.of("Europe/London")).format(timeFormatter).toLowerCase()} on ${expiryDate.atZone(ZoneId.of("Europe/London")).format(dateFormatter)}"
  }

  private def formattedUploadDate(timestamp: Long): String = {
    val uploadDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("Europe/London"))

    val todayOrYesterday = if (uploadDate.toLocalDate.isEqual(ZonedDateTime.now.toLocalDate)) {
      "today"
    } else {
      "yesterday"
    }

    s"$todayOrYesterday at ${uploadDate.format(DateTimeFormatter.ofPattern("h:mma").withLocale(Locale.UK)).toLowerCase()}"
  }

  def renderUploadResultsPage: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(userId) =>
        filesSessionService.fetchFileSession(userId).map {
          case Some(fileSession) =>
            fileSession.resultsFile match {
              case Some(resultFile) =>
                resultFile.uploadDate match {
                  case Some(timestamp) =>
                    fileSession.userFile match {
                      case Some(callbackData) =>
                        val currentTaxYear = TaxYearResolver.currentTaxYear
                        val filename       = filesSessionService.getDownloadFileName(fileSession)
                        Ok(
                          uploadResultView(
                            callbackData.reference,
                            formattedExpiryDate(timestamp),
                            isBeforeApr6(timestamp),
                            currentTaxYear,
                            filename
                          )
                        )
                      case _                  =>
                        logger.error(
                          "[ChooseAnOptionController][renderUploadResultsPage] failed to retrieve callback data"
                        )
                        Redirect(routes.ErrorController.renderGlobalErrorPage)
                    }
                  case _               =>
                    logger.error(
                      "[ChooseAnOptionController][renderUploadResultsPage] failed to retrieve upload timestamp"
                    )
                    Redirect(routes.ErrorController.renderGlobalErrorPage)
                }
              case _                =>
                logger.info("[ChooseAnOptionController][renderUploadResultsPage] file upload in progress")
                Redirect(routes.ChooseAnOptionController.renderNoResultsAvailableYetPage)
            }
          case _                 =>
            logger.info("[ChooseAnOptionController][renderUploadResultsPage] no results available")
            Redirect(routes.ChooseAnOptionController.renderNoResultAvailablePage)
        }
      case Left(resp)    =>
        logger.warn("[ChooseAnOptionController][renderUploadResultsPage] user not authorised")
        resp
    }
  }

  def renderNoResultAvailablePage: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(userId) =>
        filesSessionService.fetchFileSession(userId).flatMap {
          case Some(fileSession) =>
            fileSession.resultsFile match {
              case Some(_) =>
                logger.info(
                  "[ChooseAnOptionController][renderNotResultAvailablePage] there is a file ready, rendering results page"
                )
                Future.successful(Redirect(routes.ChooseAnOptionController.renderUploadResultsPage))
              case _       =>
                logger.info(
                  "[ChooseAnOptionController][renderNotResultAvailablePage] there is a file in progress, rendering results-not-available page"
                )
                Future.successful(Redirect(routes.ChooseAnOptionController.renderNoResultsAvailableYetPage))
            }
          case _                 =>
            logger.info("[ChooseAnOptionController][renderNotResultAvailablePage] rendering no result available page")
            Future.successful(Ok(noResultsAvailableView()))
        }
      case Left(resp)    =>
        logger.warn("[ChooseAnOptionController][renderNotResultAvailablePage] user not authorised")
        resp
    }
  }

  def renderNoResultsAvailableYetPage: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(userId) =>
        filesSessionService.fetchFileSession(userId).flatMap {
          case Some(fileSession) =>
            fileSession.resultsFile match {
              case Some(_) =>
                logger.info("[ChooseAnOptionController][renderNotResultAvailableYetPage] redirecting to results page")
                Future.successful(Redirect(routes.ChooseAnOptionController.renderUploadResultsPage))
              case _       =>
                logger.info(
                  "[ChooseAnOptionController][renderNotResultAvailableYetPage] rendering results not available page"
                )
                Future.successful(Ok(resultsNotAvailableYetView()))
            }
          case _                 =>
            logger.info(
              "[ChooseAnOptionController][renderNotResultAvailableYetPage] redirecting to results not available"
            )
            Future.successful(Redirect(routes.ChooseAnOptionController.renderNoResultAvailablePage))
        }
      case Left(resp)    =>
        logger.warn("[ChooseAnOptionController][renderNotResultAvailableYetPage] user not authorised")
        resp
    }
  }

  def renderFileReadyPage: Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(userId) =>
        filesSessionService.fetchFileSession(userId).flatMap {
          case Some(fileSession) =>
            fileSession.resultsFile match {
              case Some(_) =>
                Future.successful(Ok(fileReadyView()))
              case _       =>
                logger.error("[ChooseAnOptionController][renderFileReadyPage] session has no result file")
                Future.successful(Redirect(routes.UpscanController.uploadInProgress))
            }
          case _                 =>
            logger.error("[ChooseAnOptionController][renderFileReadyPage] failed to retrieve session")
            Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
        }
      case Left(resp)    =>
        logger.warn("[ChooseAnOptionController][renderFileReadyPage] user not authorised")
        resp
    }
  }

  def getResultsFile(fileName: String): Action[AnyContent] = Action.async { request =>
    given MessagesRequest[AnyContent] = request
    isAuthorised().flatMap {
      case Right(userId) =>
        filesSessionService.fetchFileSession(userId).flatMap {
          case Some(fileSession) =>
            fileSession.resultsFile match {
              case Some(resultFile) =>
                resultFile.filename match {
                  case Some(name) if name == fileName =>
                    filesSessionService.removeFileSessionFromCache(userId)
                    getFile(fileName, userId, filesSessionService.getDownloadFileName(fileSession))
                  case _                              =>
                    logger.error("[ChooseAnOptionController][getResultsFile] filename empty")
                    Future.successful(Redirect(routes.ErrorController.fileNotAvailable))
                }
              case _                =>
                logger.error("[ChooseAnOptionController][getResultsFile] no result file found")
                Future.successful(Redirect(routes.ErrorController.fileNotAvailable))
            }
          case _                 =>
            logger.error("[ChooseAnOptionController][getResultsFile] no file session for User Id")
            Future.successful(Redirect(routes.ErrorController.fileNotAvailable))
        }
      case Left(resp)    =>
        logger.warn("[ChooseAnOptionController][getResultsFile] user not authorised")
        resp
    }
  }

  private def getFile(fileName: String, userId: String, downloadFileName: String)(implicit
    hc: HeaderCarrier
  ): Future[play.api.mvc.Result] =
    resultsFileConnector
      .getFile(fileName, userId)
      .map { response =>
        val dataContent: Source[ByteString, _] = StreamConverters
          .fromInputStream(() => response.get)
          .watchTermination()((_, futDone) =>
            futDone.onComplete {
              case Success(_) =>
                logger.info(s"[ChooseAnOptionController][getFile] File with name $fileName has started to delete")
                resultsFileConnector.deleteFile(fileName, userId)
              case Failure(f) =>
                logger.warn(s"[ChooseAnOptionController][getFile] File with name $fileName was not deleted - $f")
            }
          )

        Ok.sendEntity(HttpEntity.Streamed(dataContent, None, Some(_contentType)))
          .withHeaders(
            CONTENT_DISPOSITION -> s"""attachment; filename="$downloadFileName-results.csv"""",
            // CONTENT_LENGTH -> s"${fileData.get.length}",
            CONTENT_TYPE        -> _contentType
          )

      }
      .recover { case ex: Throwable =>
        logger.error(
          "[ChooseAnOptionController][getFile] Request failed with Exception " + ex.getMessage + " for file -> " + fileName
        )
        Redirect(routes.ErrorController.renderGlobalErrorPage)
      }

  private def isBeforeApr6(timestamp: Long): Boolean = {
    val uploadDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("Europe/London"))
    uploadDate.isBefore(LocalDateTime.of(uploadDate.getYear, 4, 6, 0, 0, 0))
  }

}
