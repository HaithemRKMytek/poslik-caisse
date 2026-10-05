package com.poslik.caisse.data.local

import com.poslik.caisse.data.remote.RegisterRemoteDataSource
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.repository.ClaimResult
import com.poslik.caisse.domain.repository.RegisterRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomRegisterRepository @Inject constructor(private val registerDao: RegisterDao, private val remote: RegisterRemoteDataSource) :
    RegisterRepository {

    override fun observeRegisterCode(): Flow<RegisterCode?> = registerDao.observeCode().map { code -> code?.let(::RegisterCode) }

    override suspend fun claim(code: RegisterCode): ClaimResult {
        // La caisse est déjà configurée : on ne remet jamais le compteur à zéro.
        registerDao.get()?.let { existing ->
            return if (existing.registerCode == code.value) ClaimResult.Success else ClaimResult.AlreadyTaken
        }
        val result = remote.claim(code)
        if (result == ClaimResult.Success) {
            registerDao.insert(RegisterConfigEntity(registerCode = code.value, lastNumber = 0))
        }
        return result
    }
}
