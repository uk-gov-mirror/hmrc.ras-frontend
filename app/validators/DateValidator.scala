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

import models.{MemberDateOfBirth, RasDate}

import java.time.Year
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormError}

trait DateValidator {

  val YEAR_FIELD_LENGTH: Int = 4
  val YEAR                   = "year"
  val MONTH                  = "month"
  val DAY                    = "day"

  def updatedErrors(formWithErrors: Form[MemberDateOfBirth]): Form[MemberDateOfBirth] = {
    val isDayMissing   = formWithErrors.errors.exists(_.message.matches("error.day.missing"))
    val isMonthMissing = formWithErrors.errors.exists(_.message.matches("error.month.missing"))
    val isYearMissing  = formWithErrors.errors.exists(_.message.matches("error.year.missing"))
    val formErrors     = (isDayMissing, isMonthMissing, isYearMissing) match {
      case (true, true, true) => Seq(FormError("dateOfBirth", "error.dob.missing"))

      case (true, true, false) =>
        Seq(
          FormError("dateOfBirth.day", "error.dob.missing.day.month"),
          FormError("dateOfBirth.month", "error.dob.missing.day.month")
        )

      case (false, true, true) =>
        Seq(
          FormError("dateOfBirth.month", "error.dob.missing.month.year"),
          FormError("dateOfBirth.year", "error.dob.missing.month.year")
        )

      case (true, false, true) =>
        Seq(
          FormError("dateOfBirth.day", "error.dob.missing.day.year"),
          FormError("dateOfBirth.year", "error.dob.missing.day.year")
        )

      case _ => formWithErrors.errors
    }

    formWithErrors.copy(errors = formErrors)
  }

  def rasDateConstraint(name: String): Constraint[MemberDateOfBirth] = Constraint(name) { memDob =>
    val date = memDob.dateOfBirth

    val leapYear: Boolean =
      try
        Year.isLeap(date.year.getOrElse("0").toInt)
      catch {
        case _: NumberFormatException => false
      }

    if (!DateValidator.checkDayRange(date)) {
      if (date.month.getOrElse("0").toInt == 2 && leapYear)
        Invalid(Seq(ValidationError("error.day.invalid.feb.leap", DAY)))
      else if (date.month.getOrElse("0").toInt == 2)
        Invalid(Seq(ValidationError("error.day.invalid.feb", DAY)))
      else if (List(4, 6, 9, 11).contains(date.month.getOrElse("0").toInt))
        Invalid(Seq(ValidationError("error.day.invalid.thirty", DAY)))
      else
        Invalid(Seq(ValidationError("error.day.invalid", DAY)))
    } else if (!DateValidator.checkMonthRange(date.month.getOrElse("0")))
      Invalid(Seq(ValidationError("error.month.invalid", MONTH)))
    else if (!DateValidator.checkYearLength(date.year.getOrElse("0")))
      Invalid(Seq(ValidationError("error.year.invalid.format", YEAR)))
    else if (date.isInFuture)
      Invalid(Seq(ValidationError("error.dob.invalid.future")))
    else if (!DateValidator.isAfter1900(date.year.getOrElse("0")))
      Invalid(Seq(ValidationError("error.dob.before.1900")))
    else
      Valid
  }

  def checkDayRange(date: RasDate): Boolean =

    try {

      val day               = date.day.getOrElse("0")
      val month             = date.month.getOrElse("0")
      val year              = date.year.getOrElse("0").toInt
      val leapYear: Boolean = Year.isLeap(year)

      if (day forall Character.isDigit) {
        if (month.toInt == 2 && leapYear)
          day.toInt > 0 && day.toInt < 30
        else if (month.toInt == 2)
          day.toInt > 0 && day.toInt < 29
        else if (List(4, 6, 9, 11).contains(month.toInt))
          day.toInt > 0 && day.toInt < 31
        else
          day.toInt > 0 && day.toInt < 32
      } else
        false
    } catch {
      case _: NumberFormatException => false
    }

  def checkMonthRange(month: String): Boolean =
    if (month forall Character.isDigit)
      month.toInt > 0 && month.toInt < 13
    else
      false

  def checkYearLength(year: String): Boolean =
    if (year forall Character.isDigit)
      year.length == YEAR_FIELD_LENGTH
    else
      false

  def isAfter1900(year: String): Boolean =
    if (year forall Character.isDigit)
      year.toInt >= 1900
    else
      false

}

object DateValidator extends DateValidator
