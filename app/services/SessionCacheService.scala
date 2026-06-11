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
import play.api.mvc.Request
import repository.RasSessionCacheRepository
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionCacheService @Inject() (sessionCacheRepository: RasSessionCacheRepository)(implicit ec: ExecutionContext) {

  val RAS_SESSION_KEY: String = "ras_session"

  def fetchRasSession()(implicit request: Request[?]): Future[Option[RasSession]] =
    sessionCacheRepository.getFromSession[RasSession](DataKey(RAS_SESSION_KEY))

  def cacheName(value: MemberName)(implicit request: Request[?]): Future[Option[RasSession]] =
    cache(CacheKey.Name, Some(value))

  def cacheNino(value: MemberNino)(implicit request: Request[?]): Future[Option[RasSession]] =
    cache(CacheKey.Nino, Some(value))

  def cacheDob(value: MemberDateOfBirth)(implicit request: Request[?]): Future[Option[RasSession]] =
    cache(CacheKey.Dob, Some(value))

  def cacheUploadResponse(value: UploadResponse)(implicit request: Request[?]): Future[Option[RasSession]] =
    cache(CacheKey.UploadResponse, Some(value))

  def cacheFile(value: File)(implicit request: Request[?]): Future[Option[RasSession]] =
    cache(CacheKey.File, Some(value))

  def cacheResidencyStatusResult(value: ResidencyStatusResult)(implicit
    request: Request[?]
  ): Future[Option[RasSession]] = cache(CacheKey.StatusResult, Some(value))

  def resetCacheName()(implicit request: Request[?]): Future[Option[RasSession]] = cache(CacheKey.Name)
  def resetCacheNino()(implicit request: Request[?]): Future[Option[RasSession]] = cache(CacheKey.Nino)
  def resetCacheDob()(implicit request: Request[?]): Future[Option[RasSession]]  = cache(CacheKey.Dob)

  def resetCacheUploadResponse()(implicit request: Request[?]): Future[Option[RasSession]] = cache(
    CacheKey.UploadResponse
  )

  def resetCacheFile()(implicit request: Request[?]): Future[Option[RasSession]] = cache(CacheKey.File)

  def resetCacheResidencyStatusResult()(implicit request: Request[?]): Future[Option[RasSession]] = cache(
    CacheKey.StatusResult
  )

  def resetRasSession()(implicit request: Request[?]): Future[Option[RasSession]] = cache(CacheKey.All)

  private def cache[T](key: CacheKey[T], value: Option[T] = None)(implicit
    request: Request[?]
  ): Future[Option[RasSession]] =
    for {
      currentSession <- fetchRasSession()
      session         = currentSession.getOrElse(RasSession.cleanSession)
      _              <- sessionCacheRepository
                          .putSession[RasSession](DataKey(RAS_SESSION_KEY), session.selectKeysToCache(session, key, value))
      updatedSession <- fetchRasSession()
    } yield updatedSession

}
