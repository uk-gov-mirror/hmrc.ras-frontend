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

package forms

import forms.MemberNameForm.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import play.api.libs.json.Json
import utils.RasTestHelper

class MemberNameFormSpec extends AnyWordSpec with RasTestHelper {

  val MAX_NAME_LENGTH        = 35
  val fromJsonMaxChars: Long = 102400

  "Find member details form" must {

    "return no error when valid data is entered" in {
      val formData      = Json.obj("firstName" -> "Ramin", "lastName" -> "Esfandiari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "return an error when first name field is empty" in {
      val formData      = Json.obj("firstName" -> "", "lastName" -> "Esfandiari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("firstName", List("error.mandatory.firstName"))))
    }

    "return an error when last name field is empty" in {
      val formData      = Json.obj("firstName" -> "Ramin", "lastName" -> "")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("lastName", List("error.mandatory.lastName"))))
    }

    "return error when first name is longer max allowed length" in {
      val formData      = Json.obj("firstName" -> "r" * (MAX_NAME_LENGTH + 1), "lastName" -> "Esfandiari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("firstName", List("error.length.firstName"))))
    }

    "return error when last name is longer max allowed length" in {
      val formData      = Json.obj("firstName" -> "Ramin", "lastName" -> "e" * (MAX_NAME_LENGTH + 1))
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("lastName", List("error.length.lastName"))))
    }

    "return no error when first name is of minimum allowed length" in {
      val formData      = Json.obj("firstName" -> "r", "lastName" -> "Esfandiari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "return no error when last name is of minimum allowed length" in {
      val formData      = Json.obj("firstName" -> "Ramin", "lastName" -> "E")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "return no error when first name max allowed length" in {
      val formData      = Json.obj("firstName" -> "r" * MAX_NAME_LENGTH, "lastName" -> "Esfandiari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "return no error when last name max allowed length" in {
      val formData      = Json.obj("firstName" -> "Ramin", "lastName" -> "E" * MAX_NAME_LENGTH)
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "return an error when name contains a digit" in {
      val formData1      = Json.obj("firstName" -> "Ramin1", "lastName" -> "Esfandiari")
      val validatedForm1 = form.bind(formData1, fromJsonMaxChars)
      val formData2      = Json.obj("firstName" -> "Ramin", "lastName" -> "Esfandiar3i")
      val validatedForm2 = form.bind(formData2, fromJsonMaxChars)
      assert(validatedForm1.errors.contains(FormError("firstName", List("error.firstName.invalid"))))
      assert(validatedForm2.errors.contains(FormError("lastName", List("error.lastName.invalid"))))
    }

    "allow apostrophes" in {
      val formData1      = Json.obj("firstName" -> "R'n", "lastName" -> "Esfandiari")
      val validatedForm1 = form.bind(formData1, fromJsonMaxChars)
      val formData2      = Json.obj("firstName" -> "Ramin", "lastName" -> "Esfa'ndiari")
      val validatedForm2 = form.bind(formData2, fromJsonMaxChars)
      assert(validatedForm1.errors.isEmpty)
      assert(validatedForm2.errors.isEmpty)
    }

    "allow hyphens" in {
      val formData      = Json.obj("firstName" -> "Ram-in", "lastName" -> "Esfa-ndiari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "disallow other special characters" in {
      val formData1      = Json.obj("firstName" -> "Ra$min", "lastName" -> "Esfandiari")
      val validatedForm1 = form.bind(formData1, fromJsonMaxChars)
      val formData2      = Json.obj("firstName" -> "Ramin", "lastName" -> "Esfan@diari")
      val validatedForm2 = form.bind(formData2, fromJsonMaxChars)
      assert(validatedForm1.errors.contains(FormError("firstName", List("error.firstName.invalid"))))
      assert(validatedForm2.errors.contains(FormError("lastName", List("error.lastName.invalid"))))
    }

    "allow whitespace" in {
      val formData      = Json.obj("firstName" -> "Ra min", "lastName" -> "Esfand iari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "trim leading whitespace" in {
      val formData      = Json.obj("firstName" -> " Ramin", "lastName" -> "   Esfandiari")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
      assert(validatedForm.get.firstName == "Ramin")
      assert(validatedForm.get.lastName == "Esfandiari")
    }

    "trim trailing whitespace" in {
      val formData      = Json.obj("firstName" -> "Ramin   ", "lastName" -> "Esfandiari   ")
      val validatedForm = form.bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
      assert(validatedForm.get.firstName == "Ramin")
      assert(validatedForm.get.lastName == "Esfandiari")
    }
  }

}
