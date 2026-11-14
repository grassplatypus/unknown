package com.break8t.unknown.ui.guestbook

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.break8t.unknown.config.BalanceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

/** 견본 목소리(TTS)를 쓸 수 있는지. 확인하는 동안은 STARTING이다. */
enum class TtsStatus { STARTING, READY, UNAVAILABLE }

/** 지금 나는 소리. 시각은 화면의 남은 초 계산용이라 System.currentTimeMillis다. */
sealed interface GuestbookSound {
    data object Idle : GuestbookSound
    data object Starting : GuestbookSound
    data class Recording(val startedMs: Long) : GuestbookSound
    /** [key]는 항목 id 또는 [DRAFT_KEY]. 길이를 모르면(TTS) [lengthMs]가 null이다. */
    data class Playing(val key: String, val startedMs: Long, val lengthMs: Long?) : GuestbookSound
}

/** 아직 남기지 않은 녹음. */
data class GuestbookDraft(val path: String, val lengthSec: Int)

const val DRAFT_KEY = "draft"

/**
 * 방명록의 녹음, 녹음 재생, 견본 읽기. 한 번에 하나만 소리를 내고, 새로 시작하면 하던 것을 멈춘다.
 * 파일과 기기 호출은 한 줄짜리 작업 스레드에서 해서 메인 스레드를 막지 않고 상태를 잠그지 않아도 된다.
 * 기존 AssetVoicePlayer(주행 음성)와는 따로 돈다.
 */
class GuestbookAudio(context: Context, private val config: BalanceConfig = BalanceConfig.DEFAULT) {
    private val app = context.applicationContext
    @OptIn(ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(1)
    private val worker = CoroutineScope(SupervisorJob() + io)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val dir = File(app.filesDir, "guestbook")
    private val draftFile = File(dir, "draft.m4a")

    private val _sound = MutableStateFlow<GuestbookSound>(GuestbookSound.Idle)
    val sound: StateFlow<GuestbookSound> = _sound
    private val _tts = MutableStateFlow(TtsStatus.STARTING)
    val tts: StateFlow<TtsStatus> = _tts
    private val _draft = MutableStateFlow<GuestbookDraft?>(null)
    val draft: StateFlow<GuestbookDraft?> = _draft

    private var recorder: MediaRecorder? = null
    private var recordStart = 0L
    private var player: MediaPlayer? = null
    private var utterance: String? = null
    private var focus: AudioFocusRequest? = null
    private var speech: TextToSpeech? = null

    // The engine binds asynchronously, so the constructor is cheap; the language check is an IPC and runs on the worker.
    private val engine: TextToSpeech? = runCatching {
        TextToSpeech(app) { status -> worker.launch { setUpTts(status) } }
    }.onFailure { _tts.value = TtsStatus.UNAVAILABLE }.getOrNull()

    /** 견본 문장을 한국어로 읽는다. TTS를 못 쓰면 아무것도 하지 않는다. */
    fun speak(key: String, text: String) {
        worker.launch {
            stopAll()
            val e = speech ?: return@launch
            if (!requestFocus()) return@launch
            val id = "$key/${UUID.randomUUID()}"
            utterance = id
            _sound.value = GuestbookSound.Playing(key, System.currentTimeMillis(), null)
            if (e.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) stopAll()
        }
    }

    /** 녹음 파일을 튼다. [key]는 항목 id 또는 [DRAFT_KEY]. */
    fun play(key: String, path: String) {
        worker.launch {
            stopAll()
            _sound.value = GuestbookSound.Starting
            try {
                val mp = MediaPlayer().also { player = it }
                mp.setAudioAttributes(ATTRIBUTES)
                mp.setDataSource(path)
                mp.setOnCompletionListener { worker.launch { if (player === mp) stopAll() } }
                mp.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "playback error $what/$extra")
                    worker.launch { if (player === mp) stopAll() }
                    true
                }
                mp.prepare()
                if (!requestFocus()) return@launch stopAll()
                mp.start()
                _sound.value = GuestbookSound.Playing(key, System.currentTimeMillis(), mp.duration.toLong())
            } catch (e: Exception) {
                Log.w(TAG, "cannot play $key", e)
                stopAll()
            }
        }
    }

    /** 내던 소리를 멈춘다. 녹음 중이면 녹음도 멈추고 그때까지를 [draft]로 남긴다. */
    fun stop() {
        worker.launch { if (recorder != null) finishRecording() else stopAll() }
    }

    /** 새로 녹음한다. 전에 녹음해 둔 것은 지운다. */
    fun record() {
        worker.launch {
            stopAll()
            discard()
            _sound.value = GuestbookSound.Starting
            try {
                dir.mkdirs()
                val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(app) else @Suppress("DEPRECATION") MediaRecorder()
                recorder = r
                r.setAudioSource(MediaRecorder.AudioSource.MIC)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setOutputFile(draftFile.path)
                // The screen stops at the limit; this only catches a stalled screen.
                r.setMaxDuration(config.guestbookMaxSec * 1000 + BACKSTOP_MS)
                r.setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) worker.launch { finishRecording() }
                }
                r.prepare()
                r.start()
                recordStart = SystemClock.elapsedRealtime()
                _sound.value = GuestbookSound.Recording(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.w(TAG, "cannot record", e)
                stopAll()
                discard()
            }
        }
    }

    /** 녹음해 둔 것을 지운다. */
    fun discardDraft() {
        worker.launch {
            if ((_sound.value as? GuestbookSound.Playing)?.key == DRAFT_KEY) stopAll()
            discard()
        }
    }

    /** 녹음해 둔 것을 방명록 파일로 옮기고 그 경로와 길이를 돌려준다. 없거나 옮기지 못하면 null. */
    suspend fun keepDraft(): GuestbookDraft? = withContext(io) {
        if ((_sound.value as? GuestbookSound.Playing)?.key == DRAFT_KEY) stopAll()
        val d = _draft.value ?: return@withContext null
        val target = File(dir, "${UUID.randomUUID()}.m4a")
        if (!File(d.path).renameTo(target)) return@withContext null
        _draft.value = null
        GuestbookDraft(target.path, d.lengthSec)
    }

    /** 지운 항목의 녹음 파일을 지운다. 그 항목을 틀고 있었으면 멈춘다. */
    fun delete(entry: GuestbookEntry) {
        worker.launch {
            if ((_sound.value as? GuestbookSound.Playing)?.key == entry.id) stopAll()
            entry.audioPath?.let { if (!File(it).delete()) Log.w(TAG, "cannot delete ${entry.id}") }
        }
    }

    /** 화면을 떠나거나 주행으로 덮일 때. 모든 소리를 멈추고 남기지 않은 녹음을 지운다. */
    fun release() {
        worker.launch {
            stopAll()
            discard()
            runCatching { engine?.shutdown() }
            speech = null
            worker.cancel()
        }
    }

    private fun setUpTts(status: Int) {
        val e = engine
        val lang = if (status == TextToSpeech.SUCCESS && e != null) runCatching { e.setLanguage(Locale.KOREAN) }.getOrDefault(-2) else -2
        if (e == null || lang < TextToSpeech.LANG_AVAILABLE) {
            _tts.value = TtsStatus.UNAVAILABLE
            return
        }
        e.setAudioAttributes(ATTRIBUTES)
        e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) = ended(id)
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) = ended(id)
            override fun onStop(id: String?, interrupted: Boolean) = ended(id)
        })
        speech = e
        _tts.value = TtsStatus.READY
    }

    private fun ended(id: String?) {
        worker.launch { if (id != null && id == utterance) stopAll() }
    }

    private fun finishRecording() {
        val r = recorder ?: return
        val elapsed = SystemClock.elapsedRealtime() - recordStart
        // stop() throws when nothing was captured yet; then there is no usable file.
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        recorder = null
        _sound.value = GuestbookSound.Idle
        if (ok && draftFile.exists()) _draft.value = GuestbookDraft(draftFile.path, recordedSeconds(elapsed, config))
        else discard()
    }

    // Every way a sound ends comes here, so focus and the state never stay behind.
    private fun stopAll() {
        recorder?.let { r -> runCatching { r.stop() }; runCatching { r.release() } }
        recorder = null
        player?.let { runCatching { it.release() } }
        player = null
        if (utterance != null) runCatching { speech?.stop() }
        utterance = null
        focus?.let { runCatching { audio.abandonAudioFocusRequest(it) } }
        focus = null
        _sound.value = GuestbookSound.Idle
    }

    private fun discard() {
        if (draftFile.exists() && !draftFile.delete()) Log.w(TAG, "cannot delete draft")
        _draft.value = null
    }

    private fun requestFocus(): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(ATTRIBUTES)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    worker.launch { if (recorder == null) stopAll() }
                }
            }
            .build()
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "audio focus denied")
            return false
        }
        focus = request
        return true
    }

    private companion object {
        const val TAG = "Guestbook"
        const val BACKSTOP_MS = 1000
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}
