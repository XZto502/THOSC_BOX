/*
 * THOSC_BOX - Touhou Project VRChat OSC Bridge
 * Copyright (C) 2026 Sanae-Koishi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.OSCPortOut
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
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress

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
    val onSteam: Boolean = false
)

/**
 * Finds process ID by process name.
 */
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

// Tracks the last print time for memory read errors to prevent console spamming.
private val lastReadErrorTimes = mutableMapOf<Long, Long>()

fun getModuleBaseAddress(processId: Int, processHandle: WinNT.HANDLE, moduleName: String): Long? {
    // Method 1: Try using CreateToolhelp32Snapshot and Module32FirstW/NextW to enumerate process modules and get base address
    var snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
        Tlhelp32.TH32CS_SNAPMODULE,
        WinDef.DWORD(processId.toLong())
    )
    if (snapshot == WinNT.INVALID_HANDLE_VALUE) {
        // If the 64-bit module snapshot fails, attempt to use the 32-bit module snapshot for WOW64 processes
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
            // Ignore enumeration exceptions and proceed to try other methods
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot)
        }
    }

    // Method 2: If the Toolhelp snapshot fails, use EnumProcessModulesEx (supports cross-bitness 64-bit -> 32-bit module enumeration)
    val psapi = com.sun.jna.platform.win32.Psapi.INSTANCE
    val cbNeeded = IntByReference()
    
    // LIST_MODULES_ALL (3), LIST_MODULES_32BIT (1), LIST_MODULES_64BIT (2)
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

    // Method 3: Fallback using standard EnumProcessModules
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
        // Invalid pointer validation: if the pointer read is 0, less than 0x10000 (reserved zero page), or greater than/equal to 0xFFFF0000 (invalid high memory), it is determined as invalid.
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

fun getSpellName(gameId: String, spellId: Int): String {
    val th14Map = mapOf(
        100 to "辉针「鬼之杰作」 (Shining Needle \"Oni's Masterpiece\")",
        101 to "辉针「鬼之杰作」 (Shining Needle \"Oni's Masterpiece\")",
        102 to "辉针「鬼之杰作之山」 (Shining Needle \"Oni's Masterpiece of Needle Mountain\")",
        103 to "辉针「鬼之杰作之山」 (Shining Needle \"Oni's Masterpiece of Needle Mountain\")",
        104 to "小槌「变得大吧」 (Grow Bigger, Little Mallet)",
        105 to "小槌「变得大吧」 (Grow Bigger, Little Mallet)",
        106 to "小槌「变得更大吧」 (Grow Even Bigger, Little Mallet)",
        107 to "小槌「变得更大吧」 (Grow Even Bigger, Little Mallet)",
        108 to "「打出小槌的袭击」 (Attack of the Shinmyoumaru)",
        109 to "「打出小槌的袭击」 (Attack of the Shinmyoumaru)",
        110 to "「逆袭的打出小槌」 (Counterattack of the Shinmyoumaru)",
        111 to "「逆袭的打出小槌」 (Counterattack of the Shinmyoumaru)",
        112 to "「一寸之子的巨大反击」 (Giant Counterattack of the One-Inch Boy)",
        113 to "「一寸之子的巨大反击」 (Giant Counterattack of the One-Inch Boy)",
        114 to "「小人国之乱」 (Rebellion of the Kobito)",
        115 to "「小人国之乱」 (Rebellion of the Kobito)",
        116 to "「七个小人」 (Seven Kobitos)",
        117 to "「七个小人」 (Seven Kobitos)",
        118 to "「壁橱中的小人七个」 (Seven Kobitos in the Closet)",
        119 to "「壁橱中的小人七个」 (Seven Kobitos in the Closet)"
    )
    val th16Map = mapOf(
        106 to "「背后的秘仪」 (Backside Ceremony)",
        107 to "「背后的秘仪」 (Backside Ceremony)",
        108 to "秘仪「后门之魂」 (Secret Ceremony \"Behind-Door Souls\")",
        109 to "秘仪「后门之魂」 (Secret Ceremony \"Behind-Door Souls\")",
        110 to "秘仪「背叛的樱吹雪」 (Secret Ceremony \"Betrayal Cherry Blossom Blizzard\")",
        111 to "秘仪「背叛的樱吹雪」 (Secret Ceremony \"Betrayal Cherry Blossom Blizzard\")",
        112 to "秘仪「里七星」 (Secret Ceremony \"Reverse Seven Stars\")",
        113 to "秘仪「里七星」 (Secret Ceremony \"Reverse Seven Stars\")"
    )
    val th17Map = mapOf(
        84 to "线形「线性雕刻物」 (Linear \"Linear Sculpture\")",
        85 to "线形「线性雕刻物」 (Linear \"Linear Sculpture\")",
        86 to "埴轮「偶像防卫队」 (Haniwa \"Idol Defense Force\")",
        87 to "埴轮「偶像防卫队」 (Haniwa \"Idol Defense Force\")"
    )
    val th18Map = mapOf(
        84 to "「无主之物的买卖」 (Trading of Ownerless Goods)",
        85 to "「无主之物的买卖」 (Trading of Ownerless Goods)",
        86 to "「弹幕无产阶级化」 (Danmaku Proletariat)",
        87 to "「弹幕无产阶级化」 (Danmaku Proletariat)"
    )
    return when (gameId) {
        "th14" -> th14Map[spellId] ?: "Spell ID $spellId"
        "th16" -> th16Map[spellId] ?: "Spell ID $spellId"
        "th17" -> th17Map[spellId] ?: "Spell ID $spellId"
        "th18" -> th18Map[spellId] ?: "Spell ID $spellId"
        else -> "Spell ID $spellId"
    }
}

fun getDifficultyName(gameId: String, difficulty: Int): String {
    return when (difficulty) {
        0 -> "Easy"
        1 -> "Normal"
        2 -> "Hard"
        3 -> "Lunatic"
        4 -> "Extra"
        5 -> if (gameId == "th07") "Phantasm" else "Difficulty $difficulty"
        else -> "Difficulty $difficulty"
    }
}

fun main() {

    // 1. Read JSON configuration file from classpath
    val configStream = object {}.javaClass.getResourceAsStream("/game_data.json")
    if (configStream == null) {
        println("Configuration file not found: game_data.json")
        return
    }

    val configJson = configStream.bufferedReader().use { it.readText() }
    val games = Json.decodeFromString<List<GameConfig>>(configJson)

    // 2. Initialize OSC sender (VRChat default receiving port is 9000)
    val oscSender = OSCPortOut(InetSocketAddress("127.0.0.1", 9000))

    println("=== THOSC_BOX - Touhou Project OSC Bridge ===")
    println("Supported games (${games.size}):")
    games.forEachIndexed { index, game ->
        val steamTag = if (game.onSteam) " [Steam]" else ""
        println("  ${index + 1}. ${game.name}$steamTag")
    }
    println()
    println("Starting to monitor game processes...")

    // 3. Main loop: locate game process and read memory
    while (true) {
        var activeGameConfig: GameConfig? = null
        var processId: Int? = null

        // Iterate through configured games to see if any are running
        for (game in games) {
            val pid = getProcessIdByName(game.processName)
            if (pid != null) {
                activeGameConfig = game
                processId = pid
                break
            }
        }

        if (activeGameConfig != null && processId != null) {
            println("Game running detected: ${activeGameConfig.name} (PID: $processId)")

            // Request permissions for VM read and synchronization
            val processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_VM_READ or WinNT.PROCESS_QUERY_INFORMATION or WinNT.SYNCHRONIZE,
                false,
                processId
            )

            if (processHandle == null) {
                println("Failed to open process handle, administrator privileges might be required. Error code: ${Kernel32.INSTANCE.GetLastError()}")
                Thread.sleep(5000)
                continue
            }

            try {
                // Retrieve module base address (auto-detect, supports Steam version ASLR)
                val moduleBase = getModuleBaseAddress(processId, processHandle, activeGameConfig.processName)
                val baseAddr = if (moduleBase != null) {
                    println("Module base address detected: 0x${moduleBase.toString(16).uppercase()}")
                    moduleBase
                } else {
                    // Fallback to default base address
                    val defaultBase = hexToLong(activeGameConfig.defaultBase)
                    println("Using default base address: 0x${defaultBase.toString(16).uppercase()}")
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

                // Send game index to VRChat for logic checking in the Animator
                val gameIndex = games.indexOf(activeGameConfig)

                var lastPrintTime = 0L
                var lastChatboxTime = 0L
                var lastScore = -1
                var lastMiss = -1
                var lastBomb = -1
                var lastStageValue = -1
                var lastSpellId = -2

                // Session variables for incremental delta-tracking
                var lastRawLives: Int? = null
                var lastRawBombs: Int? = null
                var cumulativeMisses = 0
                var cumulativeBombs = 0

                // Continuously read and send while the game is running
                while (true) {
                    // Wait for 100 milliseconds using WaitForSingleObject. If it returns 258 (WAIT_TIMEOUT), the process is still running.
                    val waitResult = Kernel32.INSTANCE.WaitForSingleObject(processHandle, 100)
                    if (waitResult == 0) { // WAIT_OBJECT_0: Process has exited
                        println("\nGame process has exited.")
                        break
                    } else if (waitResult != 258) { // Non WAIT_TIMEOUT
                        println("\nWaitForSingleObject failed or returned abnormal value: $waitResult, error code: ${Kernel32.INSTANCE.GetLastError()}")
                        break
                    }

                    // Dynamically resolve physical memory addresses
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

                    // Read memory values
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

                    // If score resets (e.g., starting a new game or restarting), clear cumulative values and reinitialize bases
                    if (lastScore != -1 && score < lastScore) {
                        cumulativeMisses = 0
                        cumulativeBombs = 0
                        lastRawLives = null
                        lastRawBombs = null
                    }

                    // Calculate Misses incrementally (death count)
                    if (missAddr == 0L) {
                        lastRawLives = null
                    } else {
                        if (lastRawLives == null) {
                            lastRawLives = rawLives
                        } else {
                            if (rawLives < lastRawLives) {
                                val diff = lastRawLives - rawLives
                                // Restrict change to a reasonable range to prevent abnormal spikes during loading/uninitialization
                                if (diff in 1..8) {
                                    cumulativeMisses += diff
                                }
                            }
                            lastRawLives = rawLives
                        }
                    }

                    // Calculate Bombs incrementally (bomb usage count)
                    if (bombAddr == 0L) {
                        lastRawBombs = null
                    } else {
                        if (lastRawBombs == null) {
                            lastRawBombs = rawBombs
                        } else {
                            if (rawBombs < lastRawBombs) {
                                val diff = lastRawBombs - rawBombs
                                if (activeGameConfig.id == "th10" || activeGameConfig.id == "th11") {
                                    // Special mechanic optimization (TH10/TH11): uses P (Power) to release Bombs
                                    // Only count as bomb used when life count has not decreased (excludes power drop due to death)
                                    // Each bomb release consumes 20 points of Power (i.e. 1.00 Power)
                                    val playerDied = missAddr != 0L && rawLives < (lastRawLives ?: rawLives)
                                    if (!playerDied && diff in 15..25) {
                                        cumulativeBombs += 1
                                    }
                                } else {
                                    // Normal games: directly detect decrease in remaining bomb count
                                    if (diff in 1..8) {
                                        cumulativeBombs += diff
                                    }
                                }
                            }
                            lastRawBombs = rawBombs
                        }
                    }

                    // Construct and send OSC messages
                    val difficultyStr = if (rawDifficulty in 0..10) {
                        getDifficultyName(activeGameConfig.id, rawDifficulty)
                    } else {
                        null
                    }
                    val gameNameWithDiff = if (difficultyStr != null) {
                        "${activeGameConfig.name} [$difficultyStr]"
                    } else {
                        activeGameConfig.name
                    }

                    oscSender.send(OSCMessage("/avatar/parameters/TouhouGameID", listOf(gameIndex)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouGameName", listOf(activeGameConfig.name)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouScore", listOf(score)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouMiss", listOf(cumulativeMisses)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouBomb", listOf(cumulativeBombs)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouDifficulty", listOf(rawDifficulty)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouDifficultyName", listOf(difficultyStr ?: "")))

                    // Read active spell card ID
                    val activeSpellId = readActiveSpellId(processHandle, baseAddr, activeGameConfig)
                    // spellId is valid if it is not null and not equal to -1 (inactive value in ZUN games)
                    val spellActive = activeSpellId != null && activeSpellId != -1
                    val spellStr = if (spellActive) getSpellName(activeGameConfig.id, activeSpellId!!) else null

                    oscSender.send(OSCMessage("/avatar/parameters/TouhouStage", listOf(oscStageValue)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouSpellID", listOf(activeSpellId ?: -1)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouSpellActive", listOf(spellActive)))
                    oscSender.send(OSCMessage("/avatar/parameters/TouhouSpellName", listOf(spellStr ?: "")))

                    val stageAndSpellStr = if (spellActive && spellStr != null) {
                        "$stageStr | Spell: $spellStr"
                    } else {
                        stageStr
                    }

                    // Real-time console output (prevent frequent flashing, updates on change or every 500ms)
                    val now = System.currentTimeMillis()
                    if (now - lastPrintTime >= 500 || score != lastScore || cumulativeMisses != lastMiss || cumulativeBombs != lastBomb || rawStage != lastStageValue || (activeSpellId ?: -1) != lastSpellId) {
                        print("\r[Live Data] Game: $gameNameWithDiff | Stage/Spell: $stageAndSpellStr | Score: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs        ")
                        System.out.flush()
                        lastPrintTime = now
                        lastScore = score
                        lastMiss = cumulativeMisses
                        lastBomb = cumulativeBombs
                        lastStageValue = rawStage
                        lastSpellId = activeSpellId ?: -1
                    }

                    // Send status message to VRChat Chatbox every 2 seconds (2000ms)
                    if (now - lastChatboxTime >= 2000) {
                        val chatboxText = "Game: $gameNameWithDiff | Stage/Spell: $stageAndSpellStr | Score: $score | Miss: $cumulativeMisses | Bomb: $cumulativeBombs"
                        oscSender.send(OSCMessage("/chatbox/input", listOf(chatboxText, true, false)))
                        lastChatboxTime = now
                    }
                }
            } catch (e: Exception) {
                println("\nError occurred while reading memory: ${e.message}")
            } finally {
                Kernel32.INSTANCE.CloseHandle(processHandle)
            }
        } else {
            // No game process found, wait for 2 seconds and retry
            Thread.sleep(2000)
        }
    }
}