// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class Device

class Phone() : Device() {
    val screenOff = "Turning screen off"
}

class Computer() : Device() {
    val screenSaverOn = "Turning screen saver on..."
}

fun goIdle(device: Device) = when {
    device is like val p is Phone -> p.screenOff
    device is like val c is Computer -> c.screenSaverOn
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun box() : String {
    val phone = Phone()
    val computer = Computer()

    assertEquals(goIdle(phone), "Turning screen off")
    assertEquals(goIdle(computer), "Turning screen saver on...")

    return "OK"
}
