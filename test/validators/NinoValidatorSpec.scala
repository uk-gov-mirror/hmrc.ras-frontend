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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.validation.{Invalid, Valid, ValidationError}
import utils.RandomNino

class NinoValidatorSpec extends AnyWordSpec with Matchers {

  "The validation of a nino" must {

    "pass with valid NINO" in {
      validateNino(RandomNino.generate) should equal(true)
    }
    "fail with empty string" in {
      validateNino("") should equal(false)
    }
    "fail with only space" in {
      validateNino("    ") should equal(false)
    }
    "fail with total garbage" in {
      validateNino("XXX")             should equal(false)
      validateNino("werionownadefwe") should equal(false)
      validateNino("@£%!)(*&^")       should equal(false)
      validateNino("123456")          should equal(false)
    }
    "fail with only one starting letter" in {
      validateNino("A123456C")  should equal(false)
      validateNino("A1234567C") should equal(false)
    }
    "fail with three starting letters" in {
      validateNino("ABC12345C")  should equal(false)
      validateNino("ABC123456C") should equal(false)
    }
    "fail with less than 6 middle digits" in {
      validateNino("AB12345C") should equal(false)
    }
    "fail with more than 6 middle digits" in {
      validateNino("AB1234567C") should equal(false)
    }

    "fail if we start with invalid characters" in {
      val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
      for (v <- invalidPrefixes)
        validateNino(v + "123456C") should equal(false)
    }

    "pass if we have spaces" in {
      validateNino("C E0 00 00 0A") shouldBe true
    }

    "fail if the second letter O" in {
      validateNino("AO123456C") should equal(false)
    }

    "fail if the suffix is E" in {
      validateNino("AB123456E") should equal(false)
    }

    "pass with 'KC' prefixed NINO" in {
      validateNino("KC000000A") should equal(true)
    }

    "should pass for lower case ninos" in {
      validateNino("gy000002a") should equal(true)
    }

  }

  "containsNoSpecialCharacters" should {
    "return true when the input is alphanumeric" in {
      NinoValidator.containsNoSpecialCharacters("AB123456C") shouldBe true
    }

    "return true when whitespace is present (whitespace is stripped before regex check)" in {
      NinoValidator.containsNoSpecialCharacters("AB 12 34 56 C") shouldBe true
    }

    "return false when special characters are present" in {
      NinoValidator.containsNoSpecialCharacters("AB-12-34-56C") shouldBe false
    }
  }

  "ninoConstraint" should {
    val constraint = NinoValidator.ninoConstraint("nino")

    "return Invalid with error.withName.mandatory when nino is empty" in {
      constraint("") shouldBe Invalid(
        Seq(ValidationError("error.withName.mandatory", "National Insurance number"))
      )
    }

    "return Invalid with error.withName.mandatory when nino is only whitespace" in {
      constraint("    ") shouldBe Invalid(
        Seq(ValidationError("error.withName.mandatory", "National Insurance number"))
      )
    }

    "return Invalid with error.nino.special.character when nino contains special characters" in {
      constraint("AB-12-34-56C") shouldBe Invalid(Seq(ValidationError("error.nino.special.character")))
    }

    "return Invalid with error.nino.length when nino is shorter than 8 characters" in {
      constraint("AB12345") shouldBe Invalid(Seq(ValidationError("error.nino.length")))
    }

    "return Invalid with error.nino.length when nino is longer than 9 characters" in {
      constraint("AB12345678C") shouldBe Invalid(Seq(ValidationError("error.nino.length")))
    }

    "return Invalid with error.nino.invalid when nino has a valid shape but an invalid prefix" in {
      constraint("BG123456C") shouldBe Invalid(Seq(ValidationError("error.nino.invalid")))
    }

    "return Valid for a well-formed nino" in {
      constraint("KC000000A") shouldBe Valid
    }

    "return Valid for a well-formed nino with spaces" in {
      constraint("C E0 00 00 0A") shouldBe Valid
    }
  }

  def validateNino(nino: String): Boolean = NinoValidator.isValid(nino)
}
