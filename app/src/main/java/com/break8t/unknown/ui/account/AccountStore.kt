package com.break8t.unknown.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.break8t.unknown.config.BalanceConfig

/** 이 차에 저장된 계정 목록. 화면을 나누면 Activity가 다시 만들어지므로 프로세스에 하나만 둔다(시연용). */
object AccountStore {
    var accounts by mutableStateOf(DemoAccounts.take(DemoSeedCount))
        private set

    /** 마지막으로 쓴 계정. SC-00을 시연할 수 있게 구름농부로 시작한다. */
    var lastUsedId by mutableStateOf<String?>(DemoAccounts.first().id)
        private set

    // 카운트다운과 "이미 보였음"을 같은 곳에 둬야 Activity를 다시 만들어도 다시 시작하지 않는다.
    var autoLogin by mutableStateOf<AutoLogin?>(null)
        private set
    private var autoLoginOffered = false

    fun find(id: String?): Account? = accounts.firstOrNull { it.id == id }

    /** 앱을 켜고 처음 계정 선택에 왔을 때 한 번만 카운트다운을 건다. */
    fun offerAutoLogin(nowMs: Long) {
        if (autoLoginOffered) return
        autoLoginOffered = true
        autoLogin = AutoLogin.offer(accounts, lastUsedId, nowMs)
    }

    fun stopAutoLogin() {
        autoLogin = autoLogin?.touched()
    }

    fun signIn(id: String) {
        lastUsedId = id
        stopAutoLogin()
    }

    fun add(config: BalanceConfig): Account? =
        nextDemoAccount(accounts, config)?.also { accounts = accounts + it }

    /** 프로필 관리는 로그인 전에 열리므로 어느 계정이든 지울 수 있다. 마지막으로 쓴 계정이면 자동 시작도 거둔다. */
    fun delete(id: String) {
        val account = find(id) ?: return
        accounts = accounts - account
        if (lastUsedId == id) lastUsedId = null
        if (autoLogin?.targetId == id) autoLogin = null
    }

    /** 설정에서 닉네임을 바꾸면 계정 선택 카드에도 같은 이름이 보인다. */
    fun rename(id: String, nickname: String) {
        accounts = accounts.map { if (it.id == id) it.copy(nickname = nickname) else it }
    }

    /** 설정에서 프로필 테두리를 바꾸면 계정 선택 카드에도 같은 테두리가 보인다. */
    fun setFrame(id: String, frame: String) {
        accounts = accounts.map { if (it.id == id) it.copy(frame = frame) else it }
    }

    fun setPin(id: String, pin: String?) {
        accounts = accounts.map { if (it.id == id) it.copy(pin = pin) else it }
    }
}
