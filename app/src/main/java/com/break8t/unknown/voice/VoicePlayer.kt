package com.break8t.unknown.voice

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface VoicePlayer {
    fun play(line: VoiceLine)
    fun release()
}

/** 앱에 들어 있는 목소리 묶음 목록이다. 이름은 voice/<id>/pack.json의 "name"이고 없으면 id다. */
suspend fun loadVoicePacks(context: Context): List<VoicePack> = withContext(Dispatchers.IO) {
    val assets = context.assets
    discoverPacks(listVoiceAssets(assets)) { id -> packName(assets, id) }
}

/** 고른 묶음의 녹음을 튼다. 이미 말하는 중이면 새 줄은 버린다(대기열 없음). */
class AssetVoicePlayer(
    context: Context,
    private val packId: StateFlow<String>,
    private val enabled: StateFlow<Boolean>,
) : VoicePlayer {
    private val context = context.applicationContext
    private val audio = this.context.getSystemService(AudioManager::class.java)

    // 파일 열기와 prepare가 막히므로 메인 스레드를 쓰지 않는다. 한 줄로 돌려서 아래 상태를 잠그지 않아도 된다.
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val busy = AtomicBoolean(false)
    private val reportedMissing = ConcurrentHashMap.newKeySet<String>()
    private val files by lazy { listVoiceAssets(this.context.assets) }
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    override fun play(line: VoiceLine) {
        if (!enabled.value || !busy.compareAndSet(false, true)) return
        worker.launch {
            try {
                start(line)
            } catch (e: Exception) {
                Log.w(TAG, "cannot play $line", e)
                finish()
            }
        }
    }

    override fun release() {
        worker.launch {
            finish()
            worker.cancel()
        }
    }

    private fun start(line: VoiceLine) {
        val pack = packId.value
        val path = resolveVoiceFile(pack, line, files)
        if (path == null) {
            if (reportedMissing.add("$pack/${line.fileName}")) Log.d(TAG, "no recording for $pack/${line.fileName}")
            return finish()
        }
        val mp = MediaPlayer().also { player = it }
        mp.setAudioAttributes(ATTRIBUTES)
        context.assets.openFd(path).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        mp.setOnCompletionListener { finishIfCurrent(mp) }
        mp.setOnErrorListener { _, what, extra ->
            Log.w(TAG, "playback error $what/$extra for $path")
            finishIfCurrent(mp)
            true
        }
        mp.prepare()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(ATTRIBUTES)
            .setOnAudioFocusChangeListener { change ->
                // 짧은 한 줄이라 다른 소리에 밀리면 이어 틀지 않고 끝낸다.
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    finishIfCurrent(mp)
                }
            }
            .build()
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "audio focus denied for $line")
            return finish()
        }
        focus = request
        mp.start()
    }

    // 리스너는 메인 루퍼에서 온다. 그사이 다음 줄이 시작됐으면 그 줄을 끊지 않는다.
    private fun finishIfCurrent(mp: MediaPlayer) {
        worker.launch { if (player === mp) finish() }
    }

    // 모든 끝나는 경로가 여기로 온다. 빠뜨리면 busy가 남아 다시는 말하지 않는다.
    private fun finish() {
        player?.let { runCatching { it.release() } }
        player = null
        focus?.let { runCatching { audio.abandonAudioFocusRequest(it) } }
        focus = null
        busy.set(false)
    }

    private companion object {
        const val TAG = "Voice"

        // 길안내 음성으로 분류하면 차량 오디오가 음악을 줄이고 그 위에 들려준다.
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}

/** 디스크를 읽으므로 메인 스레드에서 부르지 않는다. 읽지 못하면 묶음이 없는 것으로 본다. */
internal fun listVoiceAssets(assets: AssetManager): Set<String> = try {
    assets.list(VOICE_DIR).orEmpty().flatMapTo(HashSet()) { id ->
        assets.list("$VOICE_DIR/$id").orEmpty().map { "$VOICE_DIR/$id/$it" }
    }
} catch (e: Exception) {
    Log.w("Voice", "cannot list voice assets", e)
    emptySet()
}

private fun packName(assets: AssetManager, id: String): String? = try {
    assets.open("$VOICE_DIR/$id/pack.json").bufferedReader().use { JSONObject(it.readText()).optString("name") }
} catch (e: Exception) {
    null
}
