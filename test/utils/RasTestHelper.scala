/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import config.{ApplicationConfig, RasSessionCache, RasShortLivedHttpCaching}
import connectors.{FileUploadConnector, ResidencyStatusAPIConnector, UserDetailsConnector}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{AuditService, SessionService, ShortLivedCache}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.test.WithFakeApplication
import views.html._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait RasTestHelper extends MockitoSugar with WithFakeApplication {  this: Suite =>

	def doc(result: Html): Document = Jsoup.parse(contentAsString(result))

	private val messagesActionBuilder: MessagesActionBuilder = new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
	private val cc: ControllerComponents = stubControllerComponents()
	val fakeRequest = FakeRequest()

	val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
		messagesActionBuilder,
		DefaultActionBuilder(stubBodyParser[AnyContent]()),
		cc.parsers,
		fakeApplication.injector.instanceOf[MessagesApi],
		cc.langs,
		cc.fileMimeTypes,
		ExecutionContext.global
	)

	implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)
	implicit val actorSystem: ActorSystem = ActorSystem()
	implicit val materializer: ActorMaterializer = ActorMaterializer()
	implicit val hc: HeaderCarrier = HeaderCarrier()

	val SCOTTISH = "scotResident"
	val WELSH = "welshResident"
	val OTHER_UK = "otherUKResident"

	val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
	val mockAppCrypto: ApplicationCrypto = mock[ApplicationCrypto]

	val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
	val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
	val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
	val mockAuditService: AuditService = mock[AuditService]

	val mockFileUploadConnector: FileUploadConnector = mock[FileUploadConnector]
	val mockResidencyStatusAPIConnector: ResidencyStatusAPIConnector = mock[ResidencyStatusAPIConnector]
	val mockUserDetailsConnector: UserDetailsConnector = mock[UserDetailsConnector]
	val mockSessionService: SessionService = mock[SessionService]
	val mockRasSessionCache: RasSessionCache = mock[RasSessionCache]
	val mockRasShortLivedHttpCache : RasShortLivedHttpCaching = mock[RasShortLivedHttpCaching]
	val mockShortLivedCache: ShortLivedCache = mock[ShortLivedCache]

	when(mockAppConfig.hoursToWaitForReUpload).thenReturn(24)
	when(mockAppConfig.reportAProblemPartialUrl).thenReturn("reportAProblemPartialUrl")
	when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("reportAProblemNonJSUrl")
	when(mockAppConfig.timeOutSeconds).thenReturn(780)
	when(mockAppConfig.timeOutCountDownSeconds).thenReturn(120)
	when(mockAppConfig.signOutUrl).thenReturn("signOutUrl")
	when(mockAppConfig.signOutAndContinueUrl).thenReturn("signOutAndContinueUrl")
	when(mockAppConfig.urBannerEnabled).thenReturn(false)
	when(mockAppConfig.urBannerLinkUrl).thenReturn("urBannerLinkUrl")
	when(mockAppConfig.rasFrontendBaseUrl).thenReturn("http://localhost:9673")
	when(mockAppConfig.rasFrontendUrlSuffix).thenReturn("relief-at-source")
	when(mockAppConfig.fileUploadFrontendBaseUrl).thenReturn("http://localhost:8899")
	when(mockAppConfig.fileUploadFrontendSuffix).thenReturn("file-upload/upload/envelopes")
	when(mockAppConfig.loginCallback).thenReturn("/relief-at-source/")
	when(mockAppConfig.loginURL).thenReturn("http://localhost:9025/gg/sign-in")

	val cannotUploadAnotherFileView: cannot_upload_another_file = fakeApplication.injector.instanceOf[cannot_upload_another_file]
	val chooseAnOptionView: choose_an_option = fakeApplication.injector.instanceOf[choose_an_option]
	val fileNotAvailableView: file_not_available = fakeApplication.injector.instanceOf[file_not_available]
	val fileReadyView: file_ready = fakeApplication.injector.instanceOf[file_ready]
	val fileUploadView: file_upload = fakeApplication.injector.instanceOf[file_upload]
	val fileUploadSuccessfulView: file_upload_successful = fakeApplication.injector.instanceOf[file_upload_successful]
	val globalErrorView: global_error = fakeApplication.injector.instanceOf[global_error]
	val globalPageNotFoundView: global_page_not_found = fakeApplication.injector.instanceOf[global_page_not_found]
	val matchFoundView: match_found = fakeApplication.injector.instanceOf[match_found]
	val matchNotFoundView: match_not_found = fakeApplication.injector.instanceOf[match_not_found]
	val memberDobView: member_dob = fakeApplication.injector.instanceOf[member_dob]
	val memberNameView: member_name = fakeApplication.injector.instanceOf[member_name]
	val memberNinoView: member_nino = fakeApplication.injector.instanceOf[member_nino]
	val noResultsAvailableView: no_results_available = fakeApplication.injector.instanceOf[no_results_available]
	val problemUploadingFileView: problem_uploading_file = fakeApplication.injector.instanceOf[problem_uploading_file]
	val resultsNotAvailableYetView: results_not_available_yet = fakeApplication.injector.instanceOf[results_not_available_yet]
	val unauthorisedView: unauthorised = fakeApplication.injector.instanceOf[unauthorised]
	val uploadResultView: upload_result = fakeApplication.injector.instanceOf[upload_result]
	val feedbackQuestionnaireView: feedback.feedbackQuestionaire = fakeApplication.injector.instanceOf[feedback.feedbackQuestionaire]
	val thanksFeedbackQuestionnaireView: feedback.thanks = fakeApplication.injector.instanceOf[feedback.thanks]

}
