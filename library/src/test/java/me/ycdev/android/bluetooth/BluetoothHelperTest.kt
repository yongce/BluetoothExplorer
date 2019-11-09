package me.ycdev.android.bluetooth

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.UUID

class BluetoothHelperTest {
    @Test
    fun addressStr() {
        val address = byteArrayOf(
            0x00.toByte(), 0x11.toByte(), 0x22.toByte(), 0x33.toByte(), 0xAA.toByte(), 0xBB.toByte()
        )
        assertThat(BluetoothHelper.addressStr(address)).isEqualTo("00:11:22:33:AA:BB")
    }

    @Test
    fun parseAddressStr() {
        val address = byteArrayOf(
            0x00.toByte(), 0x11.toByte(), 0x22.toByte(), 0x33.toByte(), 0xAA.toByte(), 0xBB.toByte()
        )
        assertThat(BluetoothHelper.parseAddressStr("00:11:22:33:AA:BB")).isEqualTo(address)
    }

    @Test
    fun createUuid() {
        System.out.println("New UUID created: " + UUID.randomUUID())
    }
}
