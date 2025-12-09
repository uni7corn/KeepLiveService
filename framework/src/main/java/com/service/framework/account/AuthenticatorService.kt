package com.service.framework.account

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 账户认证服务
 *
 * 系统通过此服务与账户认证器通信
 */
class AuthenticatorService : Service() {

    private lateinit var authenticator: FwAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = FwAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}
