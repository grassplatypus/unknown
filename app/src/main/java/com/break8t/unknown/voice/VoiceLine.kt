package com.break8t.unknown.voice

/** 앱이 캐릭터 목소리로 말하는 순간이다. [fileName]은 녹음 파일 이름이라 상수 이름이 바뀌어도 그대로 둔다.
 *  목록은 assets/voice/default/README.md와 같아야 한다(VoiceAssetsTest가 확인한다). */
enum class VoiceLine(val fileName: String) {
    RAID_ENTER("raid_enter"),
    RAID_EXIT("raid_exit"),
    REWARD_READY("reward_ready"),
    CROPS_READY("crops_ready"),
    POSTCARD_NEW("postcard_new"),
    FIELD_DROP_ARRIVED("field_drop_arrived"),
    LEVEL_UP("level_up"),
}

/** 녹음 묶음 하나다. [id]는 assets/voice/ 아래 폴더 이름이다. */
data class VoicePack(val id: String, val name: String)
