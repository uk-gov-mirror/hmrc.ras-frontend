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

package utils

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.ApplicationConfig
import connectors.{FilesSessionConnector, ResidencyStatusAPIConnector, UpscanInitiateConnector}
import org.apache.pekko.actor.ActorSystem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, i18n}
import play.twirl.api.Html
import repository.RasSessionCacheRepository
import services.{AuditService, FilesSessionService, SessionCacheService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import views.html.*

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

trait RasTestHelper extends MongoSupport with MockitoSugar with WireMockSupport { this: Suite =>

  def fakeApplicationCreation: Application = {
    wireMockServer.start()

    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> "false")
      .build()
  }

  val fakeApplication: Application = fakeApplicationCreation

  def await[A](future: Future[A], timeout: Duration = 20.seconds): A = Await.result(future, timeout)

  def redirectLocation(result: Result): String = result.header.headers("Location")

  def redirectLocation(result: Future[Result], timeout: Duration = 20.seconds): String =
    Await.result(result, timeout).header.headers("Location")

  def doc(result: Html): Document = Jsoup.parse(contentAsString(result))

  private val messagesActionBuilder: MessagesActionBuilder =
    new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())

  private val cc: ControllerComponents = stubControllerComponents()
  val fakeRequest                      = FakeRequest()

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  given testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)
  given actorSystem: ActorSystem   = ActorSystem()
  given hc: HeaderCarrier          = HeaderCarrier()
  given ec: ExecutionContext       = ExecutionContext.global

  val SCOTTISH = "scotResident"
  val WELSH    = "welshResident"
  val OTHER_UK = "otherUKResident"

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  val mockAuthConnector: DefaultAuthConnector   = mock[DefaultAuthConnector]
  val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
  val mockAuditService: AuditService            = mock[AuditService]

  val mockUpscanInitiateConnector: UpscanInitiateConnector         = mock[UpscanInitiateConnector]
  val mockResidencyStatusAPIConnector: ResidencyStatusAPIConnector = mock[ResidencyStatusAPIConnector]

  val applicationConfig: ApplicationConfig = fakeApplication.injector.instanceOf[ApplicationConfig]

  // user sessions
  val mockRasSessionCacheService: SessionCacheService          = mock[SessionCacheService]
  val mockRasSessionCacheRepository: RasSessionCacheRepository = mock[RasSessionCacheRepository]

  // file sessions
  val mockFilesSessionService: FilesSessionService     = mock[FilesSessionService]
  val mockFilesSessionConnector: FilesSessionConnector = mock[FilesSessionConnector]

  when(mockAppConfig.hoursToWaitForReUpload).thenReturn(24)
  when(mockAppConfig.reportAProblemPartialUrl).thenReturn("reportAProblemPartialUrl")
  when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("reportAProblemNonJSUrl")
  when(mockAppConfig.timeOutSeconds).thenReturn(780)
  when(mockAppConfig.timeOutCountDownSeconds).thenReturn(120)
  when(mockAppConfig.signOutAndContinueUrl).thenReturn("signOutAndContinueUrl")
  when(mockAppConfig.rasFrontendBaseUrl).thenReturn("http://localhost:9673")
  when(mockAppConfig.rasFrontendUrlSuffix).thenReturn("relief-at-source")
  when(mockAppConfig.loginCallback).thenReturn("/relief-at-source/")
  when(mockAppConfig.loginURL).thenReturn("http://localhost:9025/gg/sign-in")
  when(mockAppConfig.feedbackUrl).thenReturn("http://localhost:9514/feedback/ras?useServiceNavigation")

  when(mockAppConfig.rasApiBaseUrl).thenReturn(wireMockUrl)
  when(mockAppConfig.rasApiResidencyStatusEndpoint).thenReturn("residency-status")
  when(mockAppConfig.fileDeletionUrl).thenReturn("/ras-api/file/remove/")
  when(mockAppConfig.initiateUrl).thenReturn(wireMockUrl + "/upscan/v2/initiate")
  when(mockAppConfig.upscanCallbackEndpoint).thenReturn("/ras-api/file-processing/status")

  val cannotUploadAnotherFileView: cannot_upload_another_file =
    fakeApplication.injector.instanceOf[cannot_upload_another_file]

  val chooseAnOptionView: choose_an_option             = fakeApplication.injector.instanceOf[choose_an_option]
  val errorView: error                                 = fakeApplication.injector.instanceOf[error]
  val fileNotAvailableView: file_not_available         = fakeApplication.injector.instanceOf[file_not_available]
  val fileReadyView: file_ready                        = fakeApplication.injector.instanceOf[file_ready]
  val fileUploadView: file_upload                      = fakeApplication.injector.instanceOf[file_upload]
  val fileUploadSuccessfulView: file_upload_successful = fakeApplication.injector.instanceOf[file_upload_successful]
  val globalErrorView: global_error                    = fakeApplication.injector.instanceOf[global_error]
  val globalPageNotFoundView: global_page_not_found    = fakeApplication.injector.instanceOf[global_page_not_found]
  val govukWrapperView: govuk_wrapper                  = fakeApplication.injector.instanceOf[govuk_wrapper]
  val matchFoundView: match_found                      = fakeApplication.injector.instanceOf[match_found]
  val matchNotFoundView: match_not_found               = fakeApplication.injector.instanceOf[match_not_found]
  val memberDobView: member_dob                        = fakeApplication.injector.instanceOf[member_dob]
  val memberNameView: member_name                      = fakeApplication.injector.instanceOf[member_name]
  val memberNinoView: member_nino                      = fakeApplication.injector.instanceOf[member_nino]
  val noResultsAvailableView: no_results_available     = fakeApplication.injector.instanceOf[no_results_available]
  val problemUploadingFileView: problem_uploading_file = fakeApplication.injector.instanceOf[problem_uploading_file]

  val resultsNotAvailableYetView: results_not_available_yet =
    fakeApplication.injector.instanceOf[results_not_available_yet]

  val unauthorisedView: unauthorised  = fakeApplication.injector.instanceOf[unauthorised]
  val uploadResultView: upload_result = fakeApplication.injector.instanceOf[upload_result]

  val startAtStartView: sorry_you_need_to_start_again =
    fakeApplication.injector.instanceOf[sorry_you_need_to_start_again]

  val signedOutView: signed_out = fakeApplication.injector.instanceOf[signed_out]

  def setupMockGet(statusCode: Int, body: String, url: String): StubMapping =
    wireMockServer.stubFor(
      get(urlPathEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
            .withBody(body)
        )
    )

  def setupMockPost(statusCode: Int, body: String, url: String): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
            .withBody(body)
        )
    )

  def setupMockDelete(statusCode: Int, body: String, url: String): StubMapping =
    wireMockServer.stubFor(
      delete(urlPathEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
            .withBody(body)
        )
    )

}
