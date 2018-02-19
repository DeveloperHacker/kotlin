// WITH_RUNTIME

import kotlin.test.assertEquals
import kotlin.collections.listOf

sealed class Device

class Phone() : Device() {
    val screenOff = "Turning screen off"
}

class Computer() : Device() {
    val screenSaverOn = "Turning screen saver on..."
}

fun box() : String {
    var devices = listOf(Phone(), Phone(), Computer())
    while (devices is like (val p is Phone)) {
        assertEquals(p.screenOff, "Turning screen off")
        devices = devices.drop(1)
    }
    assertEquals(devices.size, 1)
    val computer = devices[0]
    while (computer !is Computer) devices = devices.drop(1)
    assertEquals(computer.screenSaverOn, "Turning screen saver on...")
    assertEquals(devices.size, 1)
    return "OK"
}
