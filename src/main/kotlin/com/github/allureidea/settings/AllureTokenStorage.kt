package com.github.allureidea.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object AllureTokenStorage {

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("AllureTestOps", "apiToken")
    )

    fun getToken(): String? =
        PasswordSafe.instance.getPassword(credentialAttributes)

    fun setToken(token: String?) {
        PasswordSafe.instance.setPassword(credentialAttributes, token)
    }
}
