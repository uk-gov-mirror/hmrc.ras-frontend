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

package validators

import forms.MemberDateOfBirthForm
import models.{MemberDateOfBirth, RasDate}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}
import play.api.data.validation.{Invalid, Valid, ValidationError}
import validators.DateValidator.rasDateConstraint

import java.time.LocalDate

class DateValidatorSpec extends AnyWordSpec with Matchers {

  private def formWith(errors: FormError*): Form[MemberDateOfBirth] =
    MemberDateOfBirthForm().copy(errors = errors.toList)

  "checkDayRange" should {
    "return false when day is non digit" in {
      DateValidator.checkDayRange(RasDate(Some("a"), Some("1"), Some("1999"))) shouldBe false
    }

    "return false when year is a non digit" in {
      DateValidator.checkDayRange(RasDate(Some("1"), Some("1"), Some("C"))) shouldBe false
    }

    "return true for 29th of February in a leap year" in {
      DateValidator.checkDayRange(RasDate(Some("29"), Some("2"), Some("2020"))) shouldBe true
    }

    "return false for 30th of February in a leap year" in {
      DateValidator.checkDayRange(RasDate(Some("30"), Some("2"), Some("2020"))) shouldBe false
    }

    "return true for 28th of February in a non leap year" in {
      DateValidator.checkDayRange(RasDate(Some("28"), Some("2"), Some("2021"))) shouldBe true
    }

    "return false for 29th of February in a non leap year" in {
      DateValidator.checkDayRange(RasDate(Some("29"), Some("2"), Some("2021"))) shouldBe false
    }

    "return true for 30th of April (a 30-day month)" in {
      DateValidator.checkDayRange(RasDate(Some("30"), Some("4"), Some("2021"))) shouldBe true
    }

    "return false for 31st of April (a 30-day month)" in {
      DateValidator.checkDayRange(RasDate(Some("31"), Some("4"), Some("2021"))) shouldBe false
    }

    "return true for 31st of January (a 31-day month)" in {
      DateValidator.checkDayRange(RasDate(Some("31"), Some("1"), Some("2021"))) shouldBe true
    }

    "return false for day 0" in {
      DateValidator.checkDayRange(RasDate(Some("0"), Some("1"), Some("2021"))) shouldBe false
    }
  }

  "checkMonthRange" should {
    "return false when month is non digit" in {
      DateValidator.checkMonthRange("a") shouldBe false
    }

    "return true for a valid month" in {
      DateValidator.checkMonthRange("6") shouldBe true
    }

    "return false for month 0" in {
      DateValidator.checkMonthRange("0") shouldBe false
    }

    "return false for month 13" in {
      DateValidator.checkMonthRange("13") shouldBe false
    }
  }

  "checkYearLength" should {
    "return false when year is non digit" in {
      DateValidator.checkYearLength("a") shouldBe false
    }

    "return true when year has 4 digits" in {
      DateValidator.checkYearLength("1999") shouldBe true
    }

    "return false when year has fewer than 4 digits" in {
      DateValidator.checkYearLength("99") shouldBe false
    }
  }

  "isAfter1900" should {
    "return false when year is less than 1900" in {
      DateValidator.isAfter1900("1899") shouldBe false
    }

    "return true when year is 1900 or after" in {
      DateValidator.isAfter1900("1900") shouldBe true
    }

    "return false when year contains a character" in {
      DateValidator.isAfter1900("19C") shouldBe false
    }
  }

  "rasDateConstraint" should {
    "return Invalid with error.day.invalid.feb.leap when day is out of range in leap February" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("30"), Some("2"), Some("2020"))))
      result shouldBe Invalid(Seq(ValidationError("error.day.invalid.feb.leap", "day")))
    }

    "return Invalid with error.day.invalid.feb when day is out of range in non-leap February" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("29"), Some("2"), Some("2021"))))
      result shouldBe Invalid(Seq(ValidationError("error.day.invalid.feb", "day")))
    }

    "return Invalid with error.day.invalid.thirty when day is out of range in a 30-day month" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("31"), Some("4"), Some("2021"))))
      result shouldBe Invalid(Seq(ValidationError("error.day.invalid.thirty", "day")))
    }

    "return Invalid with error.day.invalid when day is out of range in a 31-day month" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("32"), Some("1"), Some("2021"))))
      result shouldBe Invalid(Seq(ValidationError("error.day.invalid", "day")))
    }

    "return Invalid with error.month.invalid when month is out of range" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("1"), Some("13"), Some("2021"))))
      result shouldBe Invalid(Seq(ValidationError("error.month.invalid", "month")))
    }

    "return Invalid with error.year.invalid.format when year is the wrong length" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("1"), Some("1"), Some("21"))))
      result shouldBe Invalid(Seq(ValidationError("error.year.invalid.format", "year")))
    }

    "return Invalid with error.dob.invalid.future when date is in the future" in {
      val futureYear = (LocalDate.now.getYear + 1).toString
      val result     = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("1"), Some("1"), Some(futureYear))))
      result shouldBe Invalid(Seq(ValidationError("error.dob.invalid.future")))
    }

    "return Invalid with error.dob.before.1900 when year is before 1900" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("1"), Some("1"), Some("1899"))))
      result shouldBe Invalid(Seq(ValidationError("error.dob.before.1900")))
    }

    "return Valid for a valid date" in {
      val result = rasDateConstraint("dateOfBirth")(MemberDateOfBirth(RasDate(Some("15"), Some("6"), Some("1990"))))
      result shouldBe Valid
    }

    "handle NumberFormatException in the leapYear computation" in {
      val invalidYearMemberDOB = MemberDateOfBirth(RasDate(Some("1"), Some("12"), Some("non-integer")))

      val constraint = rasDateConstraint("dateOfBirth")
      val result     = constraint.apply(invalidYearMemberDOB)

      result shouldBe an[Invalid]
    }
  }

  "updatedErrors" should {
    "collapse to a single error.dob.missing when day, month and year are all missing" in {
      val result = DateValidator.updatedErrors(
        formWith(
          FormError("dateOfBirth.day", "error.day.missing"),
          FormError("dateOfBirth.month", "error.month.missing"),
          FormError("dateOfBirth.year", "error.year.missing")
        )
      )
      result.errors shouldBe Seq(FormError("dateOfBirth", "error.dob.missing"))
    }

    "produce error.dob.missing.day.month when day and month are missing" in {
      val result = DateValidator.updatedErrors(
        formWith(
          FormError("dateOfBirth.day", "error.day.missing"),
          FormError("dateOfBirth.month", "error.month.missing")
        )
      )
      result.errors shouldBe Seq(
        FormError("dateOfBirth.day", "error.dob.missing.day.month"),
        FormError("dateOfBirth.month", "error.dob.missing.day.month")
      )
    }

    "produce error.dob.missing.month.year when month and year are missing" in {
      val result = DateValidator.updatedErrors(
        formWith(
          FormError("dateOfBirth.month", "error.month.missing"),
          FormError("dateOfBirth.year", "error.year.missing")
        )
      )
      result.errors shouldBe Seq(
        FormError("dateOfBirth.month", "error.dob.missing.month.year"),
        FormError("dateOfBirth.year", "error.dob.missing.month.year")
      )
    }

    "produce error.dob.missing.day.year when day and year are missing" in {
      val result = DateValidator.updatedErrors(
        formWith(
          FormError("dateOfBirth.day", "error.day.missing"),
          FormError("dateOfBirth.year", "error.year.missing")
        )
      )
      result.errors shouldBe Seq(
        FormError("dateOfBirth.day", "error.dob.missing.day.year"),
        FormError("dateOfBirth.year", "error.dob.missing.day.year")
      )
    }

    "leave errors untouched for any other combination" in {
      val originalErrors = Seq(
        FormError("dateOfBirth.day", "error.day.missing")
      )
      val result         = DateValidator.updatedErrors(formWith(originalErrors*))
      result.errors shouldBe originalErrors
    }
  }

}
