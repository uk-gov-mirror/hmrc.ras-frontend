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

package views

import forms.{MemberDateOfBirthForm, MemberNameForm, MemberNinoForm}
import models.FileUploadStatus.*
import models.upscan.{UpscanFileReference, UpscanInitiateResponse}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.twirl.api.Html
import utils.RasTestHelper

/** Twirl generates three public entry points for every template — `apply`, `render` (the Java-friendly flattened
  * signature) and `f` (the curried function value) — plus a `ref` self-reference. Production code and the other view
  * specs only ever call `apply`, so this spec pins the invariant that the generated delegates render identically.
  */
class ViewEntryPointsSpec extends AnyWordSpec with RasTestHelper {

  private val upscanResponse: UpscanInitiateResponse =
    UpscanInitiateResponse(UpscanFileReference(""), "", Map("" -> ""))

  private val memberNameForm = MemberNameForm.form.bind(Map("firstName" -> "Jackie", "lastName" -> "Chan"))
  private val memberNinoForm = MemberNinoForm(Some("Jackie Chan")).bind(Map("nino" -> "AA123456A"))

  private val memberDobForm = MemberDateOfBirthForm(Some("Jackie Chan"))
    .bind(Map("dateOfBirth.day" -> "1", "dateOfBirth.month" -> "1", "dateOfBirth.year" -> "1990"))

  private def agree(applied: Html, rendered: Html, viaF: Html, view: AnyRef, ref: AnyRef): Assertion = {
    applied.body    should not be empty
    rendered.body shouldBe applied.body
    viaF.body     shouldBe applied.body
    ref             should be theSameInstanceAs view
  }

  "every view's generated render/f/ref entry points" should {

    "agree with apply for cannot_upload_another_file" in agree(
      cannotUploadAnotherFileView()(fakeRequest, testMessages, mockAppConfig),
      cannotUploadAnotherFileView.render(fakeRequest, testMessages, mockAppConfig),
      cannotUploadAnotherFileView.f()(fakeRequest, testMessages, mockAppConfig),
      cannotUploadAnotherFileView,
      cannotUploadAnotherFileView.ref
    )

    "agree with apply for choose_an_option" in agree(
      chooseAnOptionView(Ready, Some("1 January 2024"))(fakeRequest, testMessages, mockAppConfig),
      chooseAnOptionView.render(Ready, Some("1 January 2024"), fakeRequest, testMessages, mockAppConfig),
      chooseAnOptionView.f(Ready, Some("1 January 2024"))(fakeRequest, testMessages, mockAppConfig),
      chooseAnOptionView,
      chooseAnOptionView.ref
    )

    "agree with apply for error" in agree(
      errorView("title", "heading", "message")(fakeRequest, testMessages, mockAppConfig),
      errorView.render("title", "heading", "message", fakeRequest, testMessages, mockAppConfig),
      errorView.f("title", "heading", "message")(fakeRequest, testMessages, mockAppConfig),
      errorView,
      errorView.ref
    )

    "agree with apply for file_not_available" in agree(
      fileNotAvailableView()(fakeRequest, testMessages, mockAppConfig),
      fileNotAvailableView.render(fakeRequest, testMessages, mockAppConfig),
      fileNotAvailableView.f()(fakeRequest, testMessages, mockAppConfig),
      fileNotAvailableView,
      fileNotAvailableView.ref
    )

    "agree with apply for file_ready" in agree(
      fileReadyView()(fakeRequest, testMessages, mockAppConfig),
      fileReadyView.render(fakeRequest, testMessages, mockAppConfig),
      fileReadyView.f()(fakeRequest, testMessages, mockAppConfig),
      fileReadyView,
      fileReadyView.ref
    )

    "agree with apply for file_upload" in agree(
      fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig),
      fileUploadView.render(upscanResponse, "errorMessage", fakeRequest, testMessages, mockAppConfig),
      fileUploadView.f(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig),
      fileUploadView,
      fileUploadView.ref
    )

    "agree with apply for file_upload_successful" in agree(
      fileUploadSuccessfulView()(fakeRequest, testMessages, mockAppConfig),
      fileUploadSuccessfulView.render(fakeRequest, testMessages, mockAppConfig),
      fileUploadSuccessfulView.f()(fakeRequest, testMessages, mockAppConfig),
      fileUploadSuccessfulView,
      fileUploadSuccessfulView.ref
    )

    "agree with apply for global_error" in agree(
      globalErrorView()(fakeRequest, testMessages, mockAppConfig),
      globalErrorView.render(fakeRequest, testMessages, mockAppConfig),
      globalErrorView.f()(fakeRequest, testMessages, mockAppConfig),
      globalErrorView,
      globalErrorView.ref
    )

    "agree with apply for global_page_not_found" in agree(
      globalPageNotFoundView()(testMessages, fakeRequest, mockAppConfig),
      globalPageNotFoundView.render(testMessages, fakeRequest, mockAppConfig),
      globalPageNotFoundView.f()(testMessages, fakeRequest, mockAppConfig),
      globalPageNotFoundView,
      globalPageNotFoundView.ref
    )

    "agree with apply for govuk_wrapper" in agree(
      govukWrapperView("Test page")(Html("<p>c</p>"))(fakeRequest, testMessages, applicationConfig),
      govukWrapperView
        .render("Test page", None, None, true, Html("<p>c</p>"), fakeRequest, testMessages, applicationConfig),
      govukWrapperView
        .f("Test page", None, None, true)(Html("<p>c</p>"))(fakeRequest, testMessages, applicationConfig),
      govukWrapperView,
      govukWrapperView.ref
    )

    "agree with apply for match_found" in agree(
      matchFoundView("Jim", "1 January 1990", "AA123456A", SCOTTISH, Some(SCOTTISH), 1000, 1001)(
        fakeRequest,
        testMessages,
        mockAppConfig
      ),
      matchFoundView.render(
        "Jim",
        "1 January 1990",
        "AA123456A",
        SCOTTISH,
        Some(SCOTTISH),
        1000,
        1001,
        fakeRequest,
        testMessages,
        mockAppConfig
      ),
      matchFoundView.f("Jim", "1 January 1990", "AA123456A", SCOTTISH, Some(SCOTTISH), 1000, 1001)(
        fakeRequest,
        testMessages,
        mockAppConfig
      ),
      matchFoundView,
      matchFoundView.ref
    )

    "agree with apply for match_not_found" in agree(
      matchNotFoundView("Jim", "1 January 1990", "AA123456A")(fakeRequest, testMessages, mockAppConfig),
      matchNotFoundView.render("Jim", "1 January 1990", "AA123456A", fakeRequest, testMessages, mockAppConfig),
      matchNotFoundView.f("Jim", "1 January 1990", "AA123456A")(fakeRequest, testMessages, mockAppConfig),
      matchNotFoundView,
      matchNotFoundView.ref
    )

    "agree with apply for member_dob" in agree(
      memberDobView(memberDobForm, "Jackie Chan", edit = false)(fakeRequest, testMessages, mockAppConfig),
      memberDobView.render(memberDobForm, "Jackie Chan", false, fakeRequest, testMessages, mockAppConfig),
      memberDobView.f(memberDobForm, "Jackie Chan", false)(fakeRequest, testMessages, mockAppConfig),
      memberDobView,
      memberDobView.ref
    )

    "agree with apply for member_name" in agree(
      memberNameView(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig),
      memberNameView.render(memberNameForm, false, fakeRequest, testMessages, mockAppConfig),
      memberNameView.f(memberNameForm, false)(fakeRequest, testMessages, mockAppConfig),
      memberNameView,
      memberNameView.ref
    )

    "agree with apply for member_nino" in agree(
      memberNinoView(memberNinoForm, "Jackie Chan", edit = false)(fakeRequest, testMessages, mockAppConfig),
      memberNinoView.render(memberNinoForm, "Jackie Chan", false, fakeRequest, testMessages, mockAppConfig),
      memberNinoView.f(memberNinoForm, "Jackie Chan", false)(fakeRequest, testMessages, mockAppConfig),
      memberNinoView,
      memberNinoView.ref
    )

    "agree with apply for no_results_available" in agree(
      noResultsAvailableView()(fakeRequest, testMessages, mockAppConfig),
      noResultsAvailableView.render(fakeRequest, testMessages, mockAppConfig),
      noResultsAvailableView.f()(fakeRequest, testMessages, mockAppConfig),
      noResultsAvailableView,
      noResultsAvailableView.ref
    )

    "agree with apply for problem_uploading_file" in agree(
      problemUploadingFileView()(fakeRequest, testMessages, mockAppConfig),
      problemUploadingFileView.render(fakeRequest, testMessages, mockAppConfig),
      problemUploadingFileView.f()(fakeRequest, testMessages, mockAppConfig),
      problemUploadingFileView,
      problemUploadingFileView.ref
    )

    "agree with apply for results_not_available_yet" in agree(
      resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig),
      resultsNotAvailableYetView.render(fakeRequest, testMessages, mockAppConfig),
      resultsNotAvailableYetView.f()(fakeRequest, testMessages, mockAppConfig),
      resultsNotAvailableYetView,
      resultsNotAvailableYetView.ref
    )

    "agree with apply for signed_out" in agree(
      signedOutView()(fakeRequest, testMessages, mockAppConfig),
      signedOutView.render(fakeRequest, testMessages, mockAppConfig),
      signedOutView.f()(fakeRequest, testMessages, mockAppConfig),
      signedOutView,
      signedOutView.ref
    )

    "agree with apply for sorry_you_need_to_start_again" in agree(
      startAtStartView()(fakeRequest, testMessages, mockAppConfig),
      startAtStartView.render(fakeRequest, testMessages, mockAppConfig),
      startAtStartView.f()(fakeRequest, testMessages, mockAppConfig),
      startAtStartView,
      startAtStartView.ref
    )

    "agree with apply for unauthorised" in agree(
      unauthorisedView()(fakeRequest, testMessages, mockAppConfig),
      unauthorisedView.render(fakeRequest, testMessages, mockAppConfig),
      unauthorisedView.f()(fakeRequest, testMessages, mockAppConfig),
      unauthorisedView,
      unauthorisedView.ref
    )

    "agree with apply for upload_result" in agree(
      uploadResultView("fileId", "1 January 2024", isBeforeApr6 = true, currentTaxYear = 1000, "filename")(
        fakeRequest,
        testMessages,
        mockAppConfig
      ),
      uploadResultView
        .render("fileId", "1 January 2024", true, 1000, "filename", fakeRequest, testMessages, mockAppConfig),
      uploadResultView
        .f("fileId", "1 January 2024", true, 1000, "filename")(fakeRequest, testMessages, mockAppConfig),
      uploadResultView,
      uploadResultView.ref
    )
  }

}
