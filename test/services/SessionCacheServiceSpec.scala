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

package services

import models.*
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repository.RasSessionCacheRepository
import uk.gov.hmrc.http.SessionKeys
import utils.{RandomNino, RasTestHelper}

import java.util.UUID
import scala.concurrent.Future

class SessionCacheServiceSpec extends AnyWordSpec with RasTestHelper with OptionValues {

  val sessionId: String            = UUID.randomUUID.toString
  val name: MemberName             = MemberName("John", "Johnson")
  val nino: MemberNino             = MemberNino(RandomNino.generate)
  val memberDob: MemberDateOfBirth = MemberDateOfBirth(RasDate(Some("12"), Some("12"), Some("2012")))

  val memberDetails: MemberDetails =
    MemberDetails(name, RandomNino.generate, RasDate(Some("1"), Some("1"), Some("1999")))

  val uploadResponse: UploadResponse = UploadResponse("111", Some("error error"))

  val residencyStatusResult: ResidencyStatusResult =
    ResidencyStatusResult("uk", Some("uk"), "2000", "2001", "John Johnson", "1-1-1999", nino.nino)

  val reference: File = File("someReference")

  val rasSession: RasSession =
    RasSession(name, nino, memberDob, Some(residencyStatusResult), Some(uploadResponse), Some(reference))

  val emptyRasSession: RasSession = RasSession.cleanSession

  val sessionRepository                              = new RasSessionCacheRepository(mongoComponent, applicationConfig)
  val sessionPair: (String, String)                  = SessionKeys.sessionId -> sessionId
  given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(sessionPair)

  val sessionCacheService: SessionCacheService = new SessionCacheService(sessionRepository)

  class Setup(initializeCache: Boolean = true) {

    await {
      if (initializeCache) {
        for {
          _ <- sessionCacheService.cacheName(name)
          _ <- sessionCacheService.cacheDob(memberDob)
          _ <- sessionCacheService.cacheNino(nino)
          _ <- sessionCacheService.cacheUploadResponse(uploadResponse)
          _ <- sessionCacheService.cacheFile(reference)
          _ <- sessionCacheService.cacheResidencyStatusResult(residencyStatusResult)
        } yield ()
      } else {
        sessionRepository.cacheRepo.deleteEntity(request)
      }
    }

  }

  "cacheName" must {
    "save name in session" in new Setup(false) {
      val result: Future[Option[RasSession]] = sessionCacheService.cacheName(name)
      await(result).value shouldBe emptyRasSession.copy(name = name)
    }

    "replace existing name in session" in new Setup(true) {
      val newName: MemberName                = MemberName("John", "Doe")
      val result: Future[Option[RasSession]] = sessionCacheService.cacheName(newName)
      await(result).value shouldBe rasSession.copy(name = newName)
    }
  }

  "cacheDob" must {
    "save dob in session" in new Setup(false) {
      val result: Future[Option[RasSession]] = sessionCacheService.cacheDob(memberDob)
      await(result).value shouldBe emptyRasSession.copy(dateOfBirth = memberDob)
    }

    "replace existing dob in session" in new Setup(true) {
      val newDob: MemberDateOfBirth          = MemberDateOfBirth(RasDate(Some("1"), Some("1"), Some("2000")))
      val result: Future[Option[RasSession]] = sessionCacheService.cacheDob(newDob)
      await(result).value shouldBe rasSession.copy(dateOfBirth = newDob)
    }
  }

  "cacheNino" must {
    "save nino in session" in new Setup(false) {
      val result: Future[Option[RasSession]] = sessionCacheService.cacheNino(nino)
      await(result).value shouldBe emptyRasSession.copy(nino = nino)
    }

    "replace existing nino in session" in new Setup(true) {
      val newNino: MemberNino                = MemberNino(RandomNino.generate)
      val result: Future[Option[RasSession]] = sessionCacheService.cacheNino(newNino)
      await(result).value shouldBe rasSession.copy(nino = newNino)
    }
  }

  "cacheUploadResponse" must {
    "save upload response in session" in new Setup(false) {
      val result: Future[Option[RasSession]] = sessionCacheService.cacheUploadResponse(uploadResponse)
      await(result).value shouldBe emptyRasSession.copy(uploadResponse = Some(uploadResponse))
    }

    "replace existing upload response in session" in new Setup(true) {
      val newUploadResponse: UploadResponse  = uploadResponse.copy(code = "000", reason = Some("reason"))
      val result: Future[Option[RasSession]] = sessionCacheService.cacheUploadResponse(newUploadResponse)
      await(result).value shouldBe rasSession.copy(uploadResponse = Some(newUploadResponse))
    }
  }

  "cacheFile" must {
    "save file reference in session" in new Setup(false) {
      val result: Future[Option[RasSession]] = sessionCacheService.cacheFile(reference)
      await(result).value shouldBe emptyRasSession.copy(file = Some(reference))
    }

    "replace existing file reference in session" in new Setup(true) {
      val newReference: File                 = reference.copy(UUID.randomUUID().toString)
      val result: Future[Option[RasSession]] = sessionCacheService.cacheFile(newReference)
      await(result).value shouldBe rasSession.copy(file = Some(newReference))
    }
  }

  "cacheResidencyStatusResult" must {
    "save residency status result in session" in new Setup(false) {
      val result: Future[Option[RasSession]] = sessionCacheService.cacheResidencyStatusResult(residencyStatusResult)
      await(result).value shouldBe emptyRasSession.copy(residencyStatusResult = Some(residencyStatusResult))
    }

    "replace existing residency status result in session" in new Setup(true) {
      val newResidencyStatusResult: ResidencyStatusResult = residencyStatusResult.copy(currentTaxYear = "2023")
      val result: Future[Option[RasSession]]              = sessionCacheService.cacheResidencyStatusResult(newResidencyStatusResult)
      await(result).value shouldBe rasSession.copy(residencyStatusResult = Some(newResidencyStatusResult))
    }
  }

  "reset" must {
    "clear data" in new Setup() {
      await(sessionCacheService.resetCacheName()).value.name       shouldBe RasSession.cleanMemberName
      await(sessionCacheService.resetCacheNino()).value.nino       shouldBe RasSession.cleanMemberNino
      await(sessionCacheService.resetCacheDob()).value.dateOfBirth shouldBe RasSession.cleanMemberDateOfBirth

      await(sessionCacheService.resetCacheUploadResponse()).value.uploadResponse               shouldBe None
      await(sessionCacheService.resetCacheFile()).value.file                                   shouldBe None
      await(sessionCacheService.resetCacheResidencyStatusResult()).value.residencyStatusResult shouldBe None

      await(sessionCacheService.resetRasSession()).value shouldBe RasSession.cleanSession
    }
  }

  "fetchRasSession" must {
    "return all session data if data in repository" in new Setup() {
      val result: Future[Option[RasSession]] = sessionCacheService.fetchRasSession()
      await(result).value shouldBe rasSession
    }

    "return empty session if no data in repository" in new Setup(false) {
      val result: Future[Option[RasSession]] = sessionCacheService.fetchRasSession()
      await(result) shouldBe None
    }
  }

}
