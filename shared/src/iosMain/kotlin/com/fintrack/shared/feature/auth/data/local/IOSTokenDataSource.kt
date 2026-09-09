package com.fintrack.shared.feature.auth.data.local

import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.*
import platform.Security.*
import platform.CoreFoundation.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSTokenDataSource : TokenDataSource {

    private val _accessTokenFlow = MutableStateFlow<String?>(null)
    private val _refreshTokenFlow = MutableStateFlow<String?>(null)

    init {
        _accessTokenFlow.update { loadTokenFromKeychain("access_token") }
        _refreshTokenFlow.update { loadTokenFromKeychain("refresh_token") }
    }

    override val accessToken: Flow<String?> = _accessTokenFlow
    override val refreshToken: Flow<String?> = _refreshTokenFlow

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        saveTokenToKeychain("access_token", accessToken)
        saveTokenToKeychain("refresh_token", refreshToken)
        _accessTokenFlow.update { accessToken }
        _refreshTokenFlow.update { refreshToken }
    }

    override suspend fun clearTokens() {
        deleteTokenFromKeychain("access_token")
        deleteTokenFromKeychain("refresh_token")
        _accessTokenFlow.update { null }
        _refreshTokenFlow.update { null }
    }

    private fun loadTokenFromKeychain(account: String): String? = memScoped {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain("com.fintrack.auth" as NSString))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account as NSString))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)

        if (status == errSecSuccess) {
            val data = CFBridgingRelease(result.value) as? NSData
            data?.let {
                return@memScoped NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
            }
        }
        null
    }

    private fun saveTokenToKeychain(account: String, token: String) = memScoped {
        deleteTokenFromKeychain(account)

        val tokenData = token.encodeToByteArray().toNSData()

        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain("com.fintrack.auth" as NSString))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account as NSString))
        CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(tokenData))
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)

        SecItemAdd(query as CFDictionaryRef, null)
    }

    private fun deleteTokenFromKeychain(account: String) = memScoped {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain("com.fintrack.auth" as NSString))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account as NSString))

        SecItemDelete(query as CFDictionaryRef)
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}

actual fun createTokenDataSource(): TokenDataSource {
    return IOSTokenDataSource()
}
