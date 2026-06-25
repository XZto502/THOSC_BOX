import com.illposed.osc.OSCMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.floatOrNull
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration

@Volatile
var activeOsuHelperProcess: Process? = null

fun checkAndStartOsuHelper() {
    if (activeMode != "osu") return
    if (activeOsuHelperProcess != null && activeOsuHelperProcess!!.isAlive) {
        return
    }
    val candidates = listOf(
        java.io.File("gosumemory.exe"),
        java.io.File("gosumemory/gosumemory.exe"),
        java.io.File("tosu.exe"),
        java.io.File("tosu/tosu.exe")
    )
    val exeFile = candidates.firstOrNull { it.exists() && it.isFile }
    if (exeFile != null) {
        try {
            val pb = ProcessBuilder(exeFile.absolutePath)
                .directory(exeFile.parentFile ?: java.io.File("."))
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
            pb.redirectError(ProcessBuilder.Redirect.DISCARD)
            activeOsuHelperProcess = pb.start()
            println("Info: Automatically started osu! helper from ${exeFile.absolutePath}")
        } catch (e: Exception) {
            println("Warning: Failed to start osu! helper: ${e.message}")
        }
    }
}

fun stopOsuHelper() {
    activeOsuHelperProcess?.let { process ->
        if (process.isAlive) {
            process.destroy()
            println("Info: Stopped osu! helper process.")
        }
        activeOsuHelperProcess = null
    }
}

fun runOsuScannerLoop() {
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    var lastOsuState = -1
    var lastOsuMiss = -1
    var lastChatboxTime = 0L

    while (true) {
        if (activeMode != "osu") {
            Thread.sleep(1000)
            continue
        }

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:24050/json"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val jsonText = response.body()
                val root = Json.parseToJsonElement(jsonText).jsonObject
                
                val menu = root["menu"]?.jsonObject
                val state = menu?.get("state")?.jsonPrimitive?.intOrNull ?: 0
                
                val bm = menu?.get("bm")?.jsonObject
                val metadata = bm?.get("metadata")?.jsonObject
                val artist = metadata?.get("artist")?.jsonPrimitive?.content ?: ""
                val title = metadata?.get("title")?.jsonPrimitive?.content ?: ""
                val difficulty = metadata?.get("difficulty")?.jsonPrimitive?.content ?: ""

                val stats = bm?.get("stats")?.jsonObject
                val stars = stats?.get("SR")?.jsonPrimitive?.floatOrNull ?: 0f
                
                val bpmObj = stats?.get("BPM")?.jsonObject
                val bpmMin = bpmObj?.get("min")?.jsonPrimitive?.floatOrNull ?: 0f
                val bpmMax = bpmObj?.get("max")?.jsonPrimitive?.floatOrNull ?: 0f
                val bpm = maxOf(bpmMin, bpmMax)

                val modsObj = menu?.get("mods")?.jsonObject
                val modsNum = modsObj?.get("num")?.jsonPrimitive?.intOrNull ?: 0
                val modsStrRaw = modsObj?.get("str")?.jsonPrimitive?.content ?: ""
                val modsStr = if (modsStrRaw.isEmpty() || modsStrRaw.equals("nm", ignoreCase = true)) "NM" else modsStrRaw

                val gameplay = root["gameplay"]?.jsonObject
                val ppObj = gameplay?.get("pp")?.jsonObject
                val ppCurrent = ppObj?.get("current")?.jsonPrimitive?.floatOrNull ?: 0f
                val ppFc = ppObj?.get("fc")?.jsonPrimitive?.floatOrNull ?: 0f

                val score = gameplay?.get("score")?.jsonPrimitive?.intOrNull ?: 0
                val accuracy = gameplay?.get("accuracy")?.jsonPrimitive?.floatOrNull ?: 0f
                
                val combo = gameplay?.get("combo")?.jsonObject
                val currentCombo = combo?.get("current")?.jsonPrimitive?.intOrNull ?: 0
                val maxCombo = combo?.get("max")?.jsonPrimitive?.intOrNull ?: 0

                val hits = gameplay?.get("hits")?.jsonObject
                val miss = hits?.get("0")?.jsonPrimitive?.intOrNull ?: 0
                val hit300 = hits?.get("300")?.jsonPrimitive?.intOrNull ?: 0
                val hit100 = hits?.get("100")?.jsonPrimitive?.intOrNull ?: 0
                val hit50 = hits?.get("50")?.jsonPrimitive?.intOrNull ?: 0
                val gradeObj = hits?.get("grade")?.jsonObject
                val grade = gradeObj?.get("current")?.jsonPrimitive?.content ?: ""

                val hp = gameplay?.get("hp")?.jsonObject
                val hpNormal = hp?.get("normal")?.jsonPrimitive?.floatOrNull ?: 0f

                val stateText = when (state) {
                    2 -> "Menu"
                    4 -> "Playing"
                    else -> "Idle"
                }
                activeStatus = ScannerStatus.PLAYING("osu! ($stateText) [$modsStr]")
                java.awt.EventQueue.invokeLater {
                    updateTrayLabels()
                }

                java.awt.EventQueue.invokeLater {
                    mainWindow?.apply {
                        gameCard.updateValue("osu! ($stateText) [$modsStr]")
                        charaCard.updateValue(if (title.isNotEmpty()) "$artist - $title" else "N/A")
                        stageCard.updateValue(if (difficulty.isNotEmpty()) "$difficulty (${String.format(java.util.Locale.US, "%.2f", stars)}*)" else "N/A")
                        
                        if (state == 4) {
                            scoreCard.updateValue(score.toString())
                            livesCard.updateValue(miss.toString())
                            bombsCard.updateValue(currentCombo.toString())
                            powerCard.updateValue(maxCombo.toString())
                            grazeCard.updateValue(String.format(java.util.Locale.US, "%.2f%%", accuracy))
                            
                            val ppCurrentStr = String.format(java.util.Locale.US, "%.0f", ppCurrent)
                            val ppFcStr = String.format(java.util.Locale.US, "%.0f", ppFc)
                            pointCard.updateValue("${grade.ifEmpty { "N/A" }} [PP: $ppCurrentStr / $ppFcStr]")
                        } else {
                            scoreCard.updateValue("0")
                            livesCard.updateValue("0")
                            bombsCard.updateValue("0")
                            powerCard.updateValue("0")
                            grazeCard.updateValue("0.00%")
                            pointCard.updateValue("N/A")
                        }
                    }
                }

                try {
                    activeOscSender?.apply {
                        send(OSCMessage("/avatar/parameters/OsuStatus", listOf(state)))
                        send(OSCMessage("/avatar/parameters/OsuScore", listOf(if (state == 4) score else 0)))
                        send(OSCMessage("/avatar/parameters/OsuCombo", listOf(if (state == 4) currentCombo else 0)))
                        send(OSCMessage("/avatar/parameters/OsuMaxCombo", listOf(if (state == 4) maxCombo else 0)))
                        send(OSCMessage("/avatar/parameters/OsuAccuracy", listOf(if (state == 4) (accuracy / 100f) else 0f)))
                        send(OSCMessage("/avatar/parameters/OsuMiss", listOf(if (state == 4) miss else 0)))
                        send(OSCMessage("/avatar/parameters/OsuGrade", listOf(if (state == 4) grade else "")))
                        send(OSCMessage("/avatar/parameters/OsuBPM", listOf(bpm)))
                        send(OSCMessage("/avatar/parameters/OsuStars", listOf(stars)))
                        
                        val oscHp = if (hpNormal > 1f) hpNormal / 200f else hpNormal
                        send(OSCMessage("/avatar/parameters/OsuHP", listOf(if (state == 4) oscHp else 0f)))

                        // New parameters
                        send(OSCMessage("/avatar/parameters/OsuPPCurrent", listOf(if (state == 4) ppCurrent else 0f)))
                        send(OSCMessage("/avatar/parameters/OsuPPFC", listOf(if (state == 4) ppFc else 0f)))
                        send(OSCMessage("/avatar/parameters/OsuModsNum", listOf(modsNum)))
                        send(OSCMessage("/avatar/parameters/OsuModsStr", listOf(modsStr)))
                        send(OSCMessage("/avatar/parameters/OsuHit300", listOf(if (state == 4) hit300 else 0)))
                        send(OSCMessage("/avatar/parameters/OsuHit100", listOf(if (state == 4) hit100 else 0)))
                        send(OSCMessage("/avatar/parameters/OsuHit50", listOf(if (state == 4) hit50 else 0)))
                    }
                } catch (e: Exception) {
                    // Ignore
                }

                if (activeEnableChatbox && state == 4) {
                    val now = System.currentTimeMillis()
                    if (state != lastOsuState || miss != lastOsuMiss || (now - lastChatboxTime > 8000)) {
                        val ppCurrentStr = String.format(java.util.Locale.US, "%.0f", ppCurrent)
                        val ppFcStr = String.format(java.util.Locale.US, "%.0f", ppFc)
                        
                        val parts = mutableListOf<String>()
                        val gameStr = if (activeChatboxShowGame) {
                            when (activeLang) {
                                "zh" -> "正在玩: $artist - $title"
                                "ja" -> "プレイ中: $artist - $title"
                                else -> "Playing: $artist - $title"
                            }
                        } else null

                        val stageStr = if (activeChatboxShowStage) {
                            "[$difficulty] (${String.format(java.util.Locale.US, "%.2f", stars)}*)"
                        } else null

                        val charaStr = if (activeChatboxShowChara && modsStr.isNotEmpty()) {
                            "[$modsStr]"
                        } else null

                        val songInfoParts = mutableListOf<String>()
                        if (gameStr != null) songInfoParts.add(gameStr)
                        if (stageStr != null) songInfoParts.add(stageStr)
                        if (charaStr != null) songInfoParts.add(charaStr)

                        if (songInfoParts.isNotEmpty()) {
                            parts.add(songInfoParts.joinToString(" "))
                        }

                        if (activeChatboxShowBomb) {
                            parts.add("Combo: ${currentCombo}x")
                        }
                        if (activeChatboxShowScore) {
                            parts.add("PP: $ppCurrentStr/$ppFcStr")
                        }
                        if (activeChatboxShowAcc) {
                            parts.add("Acc: ${String.format(java.util.Locale.US, "%.2f%%", accuracy)}")
                        }
                        if (activeChatboxShowMiss) {
                            parts.add("Miss: $miss")
                        }

                        val contentText = parts.joinToString(" | ")
                        val chatboxText = if (contentText.isNotEmpty()) "[osu!] $contentText" else ""

                        if (chatboxText.isNotEmpty()) {
                            try {
                                activeOscSender?.send(OSCMessage("/chatbox/input", listOf(chatboxText, true, false)))
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        lastChatboxTime = now
                        lastOsuState = state
                        lastOsuMiss = miss
                    }
                }
            } else {
                handleOsuOffline()
            }
        } catch (e: Exception) {
            handleOsuOffline()
        }

        Thread.sleep(300)
    }
}

private fun handleOsuOffline() {
    checkAndStartOsuHelper()
    activeStatus = ScannerStatus.SCANNING
    java.awt.EventQueue.invokeLater {
        updateTrayLabels()
        mainWindow?.apply {
            val statusText = when (activeLang) {
                "zh" -> "osu! (gosumemory 未运行)"
                "ja" -> "osu! (gosumemory 未起動)"
                else -> "osu! (gosumemory not running)"
            }
            gameCard.updateValue(statusText)
            charaCard.updateValue("N/A")
            stageCard.updateValue("N/A")
            scoreCard.updateValue("0")
            livesCard.updateValue("0")
            bombsCard.updateValue("0")
            powerCard.updateValue("0")
            grazeCard.updateValue("0.00%")
            pointCard.updateValue("N/A")
        }
    }
    Thread.sleep(2000)
}
