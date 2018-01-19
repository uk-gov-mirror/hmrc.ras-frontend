/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import helpers.helpers.I18nHelper
import play.api.libs.json.Json

case class WhatDoYouWantToDo(userChoice: Option[String])

object WhatDoYouWantToDo extends I18nHelper{
  implicit val formats = Json.format[WhatDoYouWantToDo]
  val SINGLE = Messages("single.lookup.radio")
  val BULK = Messages("bulk.lookup.radio")
  val RESULT = Messages("result.radio")
}
