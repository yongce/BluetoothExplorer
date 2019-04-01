package me.ycdev.android.ble.common

import com.google.common.truth.Truth.assertThat
import me.ycdev.android.lib.common.utils.EncodingUtils.fromHexString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BleAdvertiseDataTest {
    @Test
    fun parse_case1() {
        val rawData = "020102" +
                "120954696357617463682050726f2049364633" +
                "020af9" +
                "0303b2a1" +
                "000000000000000000000000000000000000000000000000000000000000000000"
        val data = BleAdvertiseData.Parser().parse(fromHexString(rawData))
        assertThat(data.payloadSize).isEqualTo(29)
        assertThat(data.flags).isEqualTo(0x02) // connectible
        assertThat(data.serviceUuids.size).isEqualTo(1)
        assertThat(data.serviceUuids[0]).isEqualTo("a1b2")
        assertThat(data.localNameShort).isNull()
        assertThat(data.deviceName).isEqualTo("TicWatch Pro I6F3")
        assertThat(data.txPowerLevel).isEqualTo(-7)
        assertThat(data.serviceData.size).isEqualTo(0)
        assertThat(data.manufacturerData.size()).isEqualTo(0)
    }

    @Test
    fun parse_case2() {
        val rawData = "07FF79001F2E3D4C" +
                "020A01" +
                "0303B2A1" +
                "06160518135799" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000"
        val data = BleAdvertiseData.Parser().parse(fromHexString(rawData))
        assertThat(data.payloadSize).isEqualTo(22)
        assertThat(data.flags).isEqualTo(BleAdvertiseData.FLAGS_NONE) // not connectible
        assertThat(data.serviceUuids.size).isEqualTo(1)
        assertThat(data.serviceUuids[0]).isEqualTo("a1b2")
        assertThat(data.localNameShort).isNull()
        assertThat(data.deviceName).isNull()
        assertThat(data.txPowerLevel).isEqualTo(1)

        assertThat(data.serviceData.size).isEqualTo(1)
        assertThat(data.serviceData["1805"]).isEqualTo("135799") // Time Service UUID

        assertThat(data.manufacturerData.size()).isEqualTo(1)
        assertThat(data.manufacturerData[0x0079]).isEqualTo("1f2e3d4c")
    }

    @Test
    fun parse_case3() {
        val rawData = "07FF79001F2E3D4C" +
                "05FF14133739" +
                "0303B2A1" +
                "06160518135799" +
                "05162CFE4648" +
                "00000000000000000000000000000000000000000000000000000000000000"
        val data = BleAdvertiseData.Parser().parse(fromHexString(rawData))
        assertThat(data.payloadSize).isEqualTo(31)
        assertThat(data.flags).isEqualTo(BleAdvertiseData.FLAGS_NONE) // not connectible
        assertThat(data.serviceUuids.size).isEqualTo(1)
        assertThat(data.serviceUuids[0]).isEqualTo("a1b2")
        assertThat(data.localNameShort).isNull()
        assertThat(data.deviceName).isNull()
        assertThat(data.txPowerLevel).isEqualTo(BleAdvertiseData.TX_POWER_LEVEL_NONE)

        assertThat(data.serviceData.size).isEqualTo(2)
        assertThat(data.serviceData["1805"]).isEqualTo("135799") // Time Service UUID
        assertThat(data.serviceData["fe2c"]).isEqualTo("4648") // GFP Service UUID

        assertThat(data.manufacturerData.size()).isEqualTo(2)
        assertThat(data.manufacturerData[0x0079]).isEqualTo("1f2e3d4c")
        assertThat(data.manufacturerData[0x1314]).isEqualTo("3739")
    }

    @Test
    fun parse_case4() {
        val rawData = "020102020aeb06162cfe0130000a260102060601111e110b06084c69627261"
        val data = BleAdvertiseData.Parser().parse(fromHexString(rawData))
        assertThat(data.payloadSize).isEqualTo(31)
        assertThat(data.flags).isEqualTo(0x02)
        assertThat(data.serviceUuids.size).isEqualTo(0)
        assertThat(data.localNameShort).isEqualTo("Libra")
        assertThat(data.deviceName).isNull()
        assertThat(data.txPowerLevel).isEqualTo(-21)

        assertThat(data.serviceData.size).isEqualTo(1)
        assertThat(data.serviceData["fe2c"]).isEqualTo("013000")

        assertThat(data.manufacturerData.size()).isEqualTo(0)
    }
}