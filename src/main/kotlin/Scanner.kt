import com.illposed.osc.OSCMessage
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Tlhelp32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import kotlinx.serialization.Serializable

interface PsapiExt : StdCallLibrary {
    fun EnumProcessModulesEx(
        hProcess: WinNT.HANDLE,
        lphModule: Pointer?,
        cb: Int,
        lpcbNeeded: IntByReference,
        dwFilterFlag: Int
    ): Boolean

    companion object {
        val INSTANCE: PsapiExt = Native.load(
            "psapi",
            PsapiExt::class.java,
            W32APIOptions.DEFAULT_OPTIONS
        )
    }
}

@Serializable
data class GameConfig(
    val id: String,
    val name: String,
    val processName: String,
    val defaultBase: String,
    val scoreOffset: List<String>,
    val missOffset: List<String>,
    val bombOffset: List<String>,
    val stageOffset: List<String> = emptyList(),
    val characterOffset: List<String> = emptyList(),
    val characterType: String = "int32",
    val subshotOffset: List<String> = emptyList(),
    val subshotType: String = "int32",
    val stageStartsFrom: Int = 1,
    val bossManagerOffset: String? = null,
    val bossIndexOffset: String? = null,
    val bossSpellIdOffset: String? = null,
    val difficultyOffset: List<String> = emptyList(),
    val difficultyType: String = "int32",
    val scoreType: String = "int32",
    val missType: String = "int32",
    val bombType: String = "int32",
    val scoreMultiplier: Int = 1,
    val onSteam: Boolean = false,
    val grazeOffset: List<String> = emptyList(),
    val grazeType: String = "none",
    val powerOffset: List<String> = emptyList(),
    val powerType: String = "none",
    val pointOffset: List<String> = emptyList(),
    val pointType: String = "none",
    val cherryMaxOffset: List<String> = emptyList(),
    val cherryMaxType: String = "none"
)

sealed class ScannerStatus {
    object SCANNING : ScannerStatus()
    data class PLAYING(val gameName: String) : ScannerStatus()
}

@Volatile
var activeStatus: ScannerStatus = ScannerStatus.SCANNING

private val lastReadErrorTimes = mutableMapOf<Long, Long>()

fun getProcessIdByName(processName: String): Int? {
    val snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, WinDef.DWORD(0))
    val entry = Tlhelp32.PROCESSENTRY32.ByReference()
    try {
        if (Kernel32.INSTANCE.Process32First(snapshot, entry)) {
            do {
                val exeFile = Native.toString(entry.szExeFile)
                if (exeFile.equals(processName, ignoreCase = true)) {
                    return entry.th32ProcessID.toInt()
                }
            } while (Kernel32.INSTANCE.Process32Next(snapshot, entry))
        }
    } finally {
        Kernel32.INSTANCE.CloseHandle(snapshot)
    }
    return null
}

fun getModuleBaseAddress(processId: Int, processHandle: WinNT.HANDLE, moduleName: String): Long? {
    var snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
        Tlhelp32.TH32CS_SNAPMODULE,
        WinDef.DWORD(processId.toLong())
    )
    if (snapshot == WinNT.INVALID_HANDLE_VALUE) {
        snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
            Tlhelp32.TH32CS_SNAPMODULE32,
            WinDef.DWORD(processId.toLong())
        )
    }
    
    if (snapshot != WinNT.INVALID_HANDLE_VALUE) {
        val entry = Tlhelp32.MODULEENTRY32W.ByReference()
        try {
            if (Kernel32.INSTANCE.Module32FirstW(snapshot, entry)) {
                do {
                    val name = Native.toString(entry.szModule)
                    if (name.equals(moduleName, ignoreCase = true)) {
                        return Pointer.nativeValue(entry.modBaseAddr)
                    }
                } while (Kernel32.INSTANCE.Module32NextW(snapshot, entry))
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot)
        }
    }

    val psapi = com.sun.jna.platform.win32.Psapi.INSTANCE
    val cbNeeded = IntByReference()
    
    for (filterFlag in listOf(3, 1, 2)) {
        cbNeeded.value = 0
        val success = PsapiExt.INSTANCE.EnumProcessModulesEx(
            processHandle,
            null,
            0,
            cbNeeded,
            filterFlag
        )
        if (success && cbNeeded.value > 0) {
            val size = cbNeeded.value
            val modulesMem = Memory(size.toLong())
            val successRead = PsapiExt.INSTANCE.EnumProcessModulesEx(
                processHandle,
                modulesMem,
                size,
                cbNeeded,
                filterFlag
            )
            if (successRead) {
                val moduleCount = cbNeeded.value / Native.POINTER_SIZE
                for (i in 0 until moduleCount) {
                    val ptrVal = modulesMem.getPointer(i.toLong() * Native.POINTER_SIZE) ?: continue
                    val pathBuffer = CharArray(512)
                    val hModule = WinDef.HMODULE()
                    hModule.pointer = ptrVal
                    val len = psapi.GetModuleFileNameExW(processHandle, hModule, pathBuffer, pathBuffer.size)
                    if (len > 0) {
                        val fullPath = String(pathBuffer, 0, len)
                        val name = fullPath.substringAfterLast('\\').substringAfterLast('/')
                        if (name.equals(moduleName, ignoreCase = true)) {
                            return Pointer.nativeValue(ptrVal)
                        }
                    }
                }
            }
        }
    }

    cbNeeded.value = 0
    val hModulesFallback = arrayOfNulls<WinDef.HMODULE>(1024)
    if (psapi.EnumProcessModules(processHandle, hModulesFallback, hModulesFallback.size * Native.POINTER_SIZE, cbNeeded)) {
        val moduleCount = cbNeeded.value / Native.POINTER_SIZE
        for (i in 0 until moduleCount) {
            val hModule = hModulesFallback[i] ?: continue
            val pathBuffer = CharArray(512)
            val len = psapi.GetModuleFileNameExW(processHandle, hModule, pathBuffer, pathBuffer.size)
            if (len > 0) {
                val fullPath = String(pathBuffer, 0, len)
                val name = fullPath.substringAfterLast('\\').substringAfterLast('/')
                if (name.equals(moduleName, ignoreCase = true)) {
                    return Pointer.nativeValue(hModule.pointer)
                }
            }
        }
    }

    val err = Kernel32.INSTANCE.GetLastError()
    println("\n[Warning] Failed to retrieve module base address (both Toolhelp and PSAPI methods failed), error code: $err")
    return null
}

fun resolveAddressPath(processHandle: WinNT.HANDLE, baseAddr: Long, path: List<String>, name: String): Long {
    if (path.isEmpty()) return 0L
    var addr = baseAddr
    for (i in 0 until path.size - 1) {
        val offset = hexToLong(path[i])
        val nextAddr = addr + offset
        val ptrVal = readPointer(processHandle, nextAddr)
        if (ptrVal < 0x10000L || ptrVal >= 0xFFFF0000L) {
            val now = System.currentTimeMillis()
            val lastTime = lastReadErrorTimes[nextAddr] ?: 0L
            if (now - lastTime > 3000) {
                val ptrHex = "0x" + ptrVal.toString(16).uppercase()
                println("\n[Warning] Failed to resolve pointer chain for $name (read invalid pointer: $ptrHex at address: 0x${nextAddr.toString(16).uppercase()})")
                lastReadErrorTimes[nextAddr] = now
            }
            return 0L
        }
        addr = ptrVal
    }
    return addr + hexToLong(path.last())
}

fun readPointer(processHandle: WinNT.HANDLE, address: Long): Long {
    val output = Memory(4)
    val bytesRead = IntByReference()
    val success = Kernel32.INSTANCE.ReadProcessMemory(
        processHandle,
        Pointer(address),
        output,
        4,
        bytesRead
    )
    if (!success) {
        return 0L
    }
    return output.getInt(0).toLong() and 0xFFFFFFFFL
}

fun readMemoryValue(processHandle: WinNT.HANDLE, address: Long, type: String, name: String): Number {
    if (address == 0L) return 0
    val byteCount = when (type.lowercase()) {
        "byte", "int8" -> 1
        "int16", "short" -> 2
        "int32", "int", "float" -> 4
        "int64", "long" -> 8
        else -> 4
    }
    val output = Memory(byteCount.toLong())
    val bytesRead = IntByReference()
    val success = Kernel32.INSTANCE.ReadProcessMemory(
        processHandle,
        Pointer(address),
        output,
        byteCount,
        bytesRead
    )
    if (!success) {
        val err = Kernel32.INSTANCE.GetLastError()
        val now = System.currentTimeMillis()
        val lastTime = lastReadErrorTimes[address] ?: 0L
        if (now - lastTime > 3000) {
            println("\n[Error] Failed to read $name (address: 0x${address.toString(16).uppercase()}), error code: $err")
            lastReadErrorTimes[address] = now
        }
        return 0
    }
    return when (type.lowercase()) {
        "byte", "int8" -> output.getByte(0).toInt() and 0xFF
        "int16", "short" -> output.getShort(0).toInt() and 0xFFFF
        "int32", "int" -> output.getInt(0)
        "int64", "long" -> output.getLong(0)
        "float" -> output.getFloat(0)
        else -> output.getInt(0)
    }
}

fun hexToLong(hex: String): Long = hex.removePrefix("0x").toLong(16)

fun readActiveSpellId(processHandle: WinNT.HANDLE, baseAddr: Long, config: GameConfig): Int? {
    val managerOffset = config.bossManagerOffset ?: return null
    val indexOffset = config.bossIndexOffset ?: return null
    val spellIdOffset = config.bossSpellIdOffset ?: return null

    val bossManagerPtr = readPointer(processHandle, baseAddr + hexToLong(managerOffset))
    if (bossManagerPtr < 0x10000L || bossManagerPtr >= 0xFFFF0000L) return null

    val bossIndex = readMemoryValue(processHandle, bossManagerPtr + hexToLong(indexOffset), "int32", "BossIndex").toInt()
    if (bossIndex < 0 || bossIndex > 100) return null

    val spellId = readMemoryValue(processHandle, bossManagerPtr + bossIndex * 4 + hexToLong(spellIdOffset), "int32", "SpellID").toInt()
    return spellId
}

fun runScannerLoop(games: List<GameConfig>) {
    while (true) {
        var activeGameConfig: GameConfig? = null
        var processId: Int? = null

        for (game in games) {
            val pid = getProcessIdByName(game.processName)
            if (pid != null) {
                activeGameConfig = game
                processId = pid
                break
            }
        }

        if (activeGameConfig != null && processId != null) {
            val gameLocalizedName = getLocalizedGameName(activeGameConfig.id, activeLang)
            
            // Update Tray Status & GUI Status Pill
            activeStatus = ScannerStatus.PLAYING(gameLocalizedName)
            java.awt.EventQueue.invokeLater {
                updateTrayLabels()
            }

            val gameDetectedMsg = when (activeLang) {
                "zh" -> "检测到游戏正在运行: $gameLocalizedName (PID: $processId)"
                "ja" -> "ゲームの起動を検出しました: $gameLocalizedName (PID: $processId)"
                else -> "Game running detected: $gameLocalizedName (PID: $processId)"
            }
            println(gameDetectedMsg)

            val processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_VM_READ or WinNT.PROCESS_QUERY_INFORMATION or WinNT.SYNCHRONIZE,
                false,
                processId
            )

            if (processHandle == null) {
                val failMsg = when (activeLang) {
                    "zh" -> "无法打开进程句柄，可能需要管理员权限。错误代码: ${Kernel32.INSTANCE.GetLastError()}"
                    "ja" -> "プロセスハンドルのオープンに失敗しました。管理者権限が必要な可能性があります。エラーコード: ${Kernel32.INSTANCE.GetLastError()}"
                    else -> "Failed to open process handle, administrator privileges might be required. Error code: ${Kernel32.INSTANCE.GetLastError()}"
                }
                println(failMsg)
                Thread.sleep(5000)
                continue
            }

            try {
                val moduleBase = getModuleBaseAddress(processId, processHandle, activeGameConfig.processName)
                val baseAddr = if (moduleBase != null) {
                    val baseMsg = when (activeLang) {
                        "zh" -> "检测到模块基址: 0x${moduleBase.toString(16).uppercase()}"
                        "ja" -> "モジュールベースアドレスを検出しました: 0x${moduleBase.toString(16).uppercase()}"
                        else -> "Module base address detected: 0x${moduleBase.toString(16).uppercase()}"
                    }
                    println(baseMsg)
                    moduleBase
                } else {
                    val defaultBase = hexToLong(activeGameConfig.defaultBase)
                    val baseFallbackMsg = when (activeLang) {
                        "zh" -> "使用默认基址: 0x${defaultBase.toString(16).uppercase()}"
                        "ja" -> "デフォルトのベースアドレスを使用します: 0x${defaultBase.toString(16).uppercase()}"
                        else -> "Using default base address: 0x${defaultBase.toString(16).uppercase()}"
                    }
                    println(baseFallbackMsg)
                    defaultBase
                }

                println("Score offset: ${activeGameConfig.scoreOffset}")
                println("Miss offset: ${activeGameConfig.missOffset}")
                println("Bomb offset: ${activeGameConfig.bombOffset}")
                println("Stage offset: ${activeGameConfig.stageOffset}")
                if (activeGameConfig.bossManagerOffset != null) {
                    println("Boss manager offset: ${activeGameConfig.bossManagerOffset}")
                }
                if (activeGameConfig.difficultyOffset.isNotEmpty()) {
                    println("Difficulty offset: ${activeGameConfig.difficultyOffset}")
                }

                val gameIndex = games.indexOf(activeGameConfig)

                var lastChatboxTime = 0L
                var lastScore = -1
                var lastMiss = -1
                var lastBomb = -1
                var lastStageValue = -1
                var lastCharacter = -1
                var lastSubshot = -1

                var lastRawLives: Int? = null
                var lastRawBombs: Int? = null
                var cumulativeMisses = 0
                var cumulativeBombs = 0

                while (true) {
                    val waitResult = Kernel32.INSTANCE.WaitForSingleObject(processHandle, 100)
                    if (waitResult == 0) {
                        val exitMsg = when (activeLang) {
                            "zh" -> "\n游戏进程已退出。"
                            "ja" -> "\nゲームプロセスが終了しました。"
                            else -> "\nGame process has exited."
                        }
                        println(exitMsg)
                        break
                    } else if (waitResult != 258) {
                        val abnormalMsg = when (activeLang) {
                            "zh" -> "\nWaitForSingleObject 失败或返回异常值: $waitResult, 错误代码: ${Kernel32.INSTANCE.GetLastError()}"
                            "ja" -> "\nWaitForSingleObject が失敗したか、異常値を返しました: $waitResult, エラーコード: ${Kernel32.INSTANCE.GetLastError()}"
                            else -> "\nWaitForSingleObject failed or returned abnormal value: $waitResult, error code: ${Kernel32.INSTANCE.GetLastError()}"
                        }
                        println(abnormalMsg)
                        break
                    }

                    val scoreAddr = resolveAddressPath(processHandle, baseAddr, activeGameConfig.scoreOffset, "Score")
                    val missAddr = resolveAddressPath(processHandle, baseAddr, activeGameConfig.missOffset, "Miss")
                    val bombAddr = if (activeGameConfig.bombType.lowercase() != "none" && activeGameConfig.bombOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.bombOffset, "Bomb")
                    } else {
                        0L
                    }
                    val stageAddr = if (activeGameConfig.stageOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.stageOffset, "Stage")
                    } else {
                        0L
                    }
                    val difficultyAddr = if (activeGameConfig.difficultyOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.difficultyOffset, "Difficulty")
                    } else {
                        0L
                    }
                    val characterAddr = if (activeGameConfig.characterOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.characterOffset, "Character")
                    } else {
                        0L
                    }
                    val subshotAddr = if (activeGameConfig.subshotOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.subshotOffset, "Subshot")
                    } else {
                        0L
                    }
                    
                    val grazeAddr = if (activeGameConfig.grazeOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.grazeOffset, "Graze")
                    } else {
                        0L
                    }
                    val powerAddr = if (activeGameConfig.powerOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.powerOffset, "Power")
                    } else {
                        0L
                    }
                    val pointAddr = if (activeGameConfig.pointOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.pointOffset, "Point")
                    } else {
                        0L
                    }
                    val cherryMaxAddr = if (activeGameConfig.cherryMaxOffset.isNotEmpty()) {
                        resolveAddressPath(processHandle, baseAddr, activeGameConfig.cherryMaxOffset, "CherryMax")
                    } else {
                        0L
                    }

                    val rawScore = readMemoryValue(processHandle, scoreAddr, activeGameConfig.scoreType, "Score").toLong()
                    val score = (rawScore * activeGameConfig.scoreMultiplier).toInt()

                    val rawLives = if (missAddr != 0L) {
                        readMemoryValue(processHandle, missAddr, activeGameConfig.missType, "Miss").toFloat().toInt()
                    } else {
                        0
                    }

                    val rawBombs = if (bombAddr != 0L) {
                        readMemoryValue(processHandle, bombAddr, activeGameConfig.bombType, "Bomb").toFloat().toInt()
                    } else {
                        0
                    }

                    val rawStage = if (stageAddr != 0L) {
                        readMemoryValue(processHandle, stageAddr, "int32", "Stage").toInt()
                    } else {
                        0
                    }

                    val rawDifficulty = if (difficultyAddr != 0L) {
                        readMemoryValue(processHandle, difficultyAddr, activeGameConfig.difficultyType, "Difficulty").toInt()
                    } else {
                        -1
                    }

                    val rawCharacter = if (characterAddr != 0L) {
                        readMemoryValue(processHandle, characterAddr, activeGameConfig.characterType, "Character").toInt()
                    } else {
                        0
                    }
                    val rawSubshot = if (subshotAddr != 0L) {
                        readMemoryValue(processHandle, subshotAddr, activeGameConfig.subshotType, "Subshot").toInt()
                    } else {
                        0
                    }
                    
                    val rawGraze = if (grazeAddr != 0L) {
                        readMemoryValue(processHandle, grazeAddr, activeGameConfig.grazeType, "Graze").toInt()
                    } else {
                        0
                    }
                    val rawPower = if (powerAddr != 0L) {
                        readMemoryValue(processHandle, powerAddr, activeGameConfig.powerType, "Power")
                    } else {
                        0.0f
                    }
                    val rawPoint = if (pointAddr != 0L) {
                        readMemoryValue(processHandle, pointAddr, activeGameConfig.pointType, "Point").toLong()
                    } else {
                        0L
                    }
                    val rawCherryMax = if (cherryMaxAddr != 0L) {
                        readMemoryValue(processHandle, cherryMaxAddr, activeGameConfig.cherryMaxType, "CherryMax").toInt()
                    } else {
                        0
                    }

                    val characterName = getCharaAndShottypeName(activeGameConfig.id, rawCharacter, rawSubshot, activeLang)

                    val stageIndex = rawStage - activeGameConfig.stageStartsFrom
                    val stageStr = if (stageIndex >= 0) {
                        when (stageIndex) {
                            in 0..5 -> "Stage ${stageIndex + 1}"
                            6 -> "Extra"
                            7 -> if (activeGameConfig.id == "th07") "Phantasm" else "Stage 8"
                            else -> "Stage ${stageIndex + 1}"
                        }
                    } else {
                        "N/A"
                    }
                    val oscStageValue = if (stageIndex in 0..7) stageIndex + 1 else 0

                    if (lastScore != -1 && score < lastScore) {
                        cumulativeMisses = 0
                        cumulativeBombs = 0
                        lastRawLives = null
                        lastRawBombs = null
                    }

                    if (missAddr == 0L) {
                        lastRawLives = null
                    } else {
                        if (lastRawLives == null) {
                            lastRawLives = rawLives
                        } else {
                            if (rawLives < lastRawLives) {
                                val diff = lastRawLives - rawLives
                                if (diff in 1..8) {
                                    cumulativeMisses += diff
                                }
                            }
                            lastRawLives = rawLives
                        }
                    }

                    if (bombAddr == 0L) {
                        lastRawBombs = null
                    } else {
                        if (lastRawBombs == null) {
                            lastRawBombs = rawBombs
                        } else {
                            if (rawBombs < lastRawBombs) {
                                val diff = lastRawBombs - rawBombs
                                if (activeGameConfig.id == "th10" || activeGameConfig.id == "th11") {
                                    val playerDied = missAddr != 0L && rawLives < (lastRawLives ?: rawLives)
                                    if (!playerDied && diff in 15..25) {
                                        cumulativeBombs += 1
                                    }
                                } else {
                                    if (diff in 1..8) {
                                        cumulativeBombs += diff
                                    }
                                }
                            }
                            lastRawBombs = rawBombs
                        }
                    }

                    val (powerFloat, powerRawInt) = when (activeGameConfig.id) {
                        "th06", "th07", "th08" -> {
                            val p = rawPower.toFloat()
                            Pair(p, p.toInt())
                        }
                        "th10", "th11" -> {
                            val p = rawPower.toFloat() / 20.0f
                            Pair(p, rawPower.toInt())
                        }
                        "th12", "th13", "th14", "th15", "th16", "th17", "th18", "th20" -> {
                            val p = rawPower.toFloat() / 100.0f
                            Pair(p, rawPower.toInt())
                        }
                        else -> Pair(rawPower.toFloat(), rawPower.toInt())
                    }

                    val pointValue = when (activeGameConfig.id) {
                        "th13", "th14", "th15", "th16", "th17" -> {
                            (rawPoint / 100).toInt()
                        }
                        else -> rawPoint.toInt()
                    }

                    val difficultyStr = if (rawDifficulty in 0..10) {
                        getDifficultyName(activeGameConfig.id, rawDifficulty, activeLang)
                    } else {
                        null
                    }
                    val gameNameWithDiff = if (difficultyStr != null) {
                        "${getLocalizedGameName(activeGameConfig.id, activeLang)} [$difficultyStr]"
                    } else {
                        getLocalizedGameName(activeGameConfig.id, activeLang)
                    }

                    try {
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouGameID", listOf(gameIndex)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouGameName", listOf(getLocalizedGameName(activeGameConfig.id, activeLang))))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouScore", listOf(score)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouMiss", listOf(cumulativeMisses)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouBomb", listOf(cumulativeBombs)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouDifficulty", listOf(rawDifficulty)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouDifficultyName", listOf(difficultyStr ?: "")))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouCharacter", listOf(rawCharacter)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSubshot", listOf(rawSubshot)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouCharacterName", listOf(characterName)))
                        
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouGraze", listOf(rawGraze)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouPower", listOf(powerFloat)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouPowerRaw", listOf(powerRawInt)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouPoint", listOf(pointValue)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouCherryMax", listOf(rawCherryMax)))
                    } catch (e: Exception) {
                        // Ignore
                    }

                    val activeSpellId = readActiveSpellId(processHandle, baseAddr, activeGameConfig)
                    val spellActive = activeSpellId != null && activeSpellId != -1
                    val spellStr = if (spellActive) getSpellName(activeGameConfig.id, activeSpellId!!, activeLang) else null

                    try {
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouStage", listOf(oscStageValue)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSpellID", listOf(activeSpellId ?: -1)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSpellActive", listOf(spellActive)))
                        activeOscSender?.send(OSCMessage("/avatar/parameters/TouhouSpellName", listOf(spellStr ?: "")))
                    } catch (e: Exception) {
                        // Ignore
                    }

                    updateLiveStats(
                        gameName = gameNameWithDiff,
                        characterName = characterName,
                        stage = stageStr,
                        score = score,
                        miss = cumulativeMisses,
                        bomb = cumulativeBombs,
                        graze = rawGraze,
                        power = powerFloat,
                        point = pointValue
                    )

                    val now = System.currentTimeMillis()
                    if (score != lastScore || cumulativeMisses != lastMiss || cumulativeBombs != lastBomb || rawStage != lastStageValue || rawCharacter != lastCharacter || rawSubshot != lastSubshot) {
                        val consoleText = when (activeLang) {
                            "zh" -> "\r[实时数据] 正在玩: $gameNameWithDiff | 机体: $characterName | 关卡: $stageStr | 分数: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs                                "
                            "ja" -> "\r[リアルタイムデータ] プレイ中: $gameNameWithDiff | 自機: $characterName | ステージ: $stageStr | スコア: $score | 被弾: $cumulativeMisses | ボム: $cumulativeBombs                                "
                            else -> "\r[Live Data] Playing: $gameNameWithDiff | Chara: $characterName | Stage: $stageStr | Score: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs                                "
                        }
                        print(consoleText)
                        System.out.flush()
                        lastScore = score
                        lastMiss = cumulativeMisses
                        lastBomb = cumulativeBombs
                        lastStageValue = rawStage
                        lastCharacter = rawCharacter
                        lastSubshot = rawSubshot
                    }

                    if (now - lastChatboxTime >= 2000) {
                        if (activeEnableChatbox) {
                            val chatboxText = when (activeLang) {
                                "zh" -> "正在玩: $gameNameWithDiff | 机体: $characterName | 关卡: $stageStr | 分数: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs"
                                "ja" -> "プレイ中: $gameNameWithDiff | 自機: $characterName | ステージ: $stageStr | スコア: $score | 被弾: $cumulativeMisses | ボム: $cumulativeBombs"
                                else -> "Playing: $gameNameWithDiff | Chara: $characterName | Stage: $stageStr | Score: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs"
                            }
                            try {
                                activeOscSender?.send(OSCMessage("/chatbox/input", listOf(chatboxText, true, false)))
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        lastChatboxTime = now
                    }
                }
            } catch (e: Exception) {
                val errorMsg = when (activeLang) {
                    "zh" -> "\n读取内存时发生错误: ${e.message}"
                    "ja" -> "\nメモリ読み込み中にエラーが発生しました: ${e.message}"
                    else -> "\nError occurred while reading memory: ${e.message}"
                }
                println(errorMsg)
            } finally {
                Kernel32.INSTANCE.CloseHandle(processHandle)
                
                activeStatus = ScannerStatus.SCANNING
                java.awt.EventQueue.invokeLater {
                    updateTrayLabels()
                    mainWindow?.updateScanningStatus()
                }
            }
        } else {
            java.awt.EventQueue.invokeLater {
                mainWindow?.updateScanningStatus()
            }
            Thread.sleep(2000)
        }
    }
}
