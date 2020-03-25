package me.ycdev.android.bluetooth.sig

import android.util.SparseArray

object CompanyIds {
    private val knownIds = SparseArray<Id>()
    init {
        knownIds.put(0x0006, Id(0x0006, "Microsoft"))
        knownIds.put(0x004C, Id(0x004C, "Apple"))
        knownIds.put(0x00E0, Id(0x00E0, "Google"))
        knownIds.put(0x01AB, Id(0x01AB, "Facebook"))
        knownIds.put(0x018E, Id(0x018E, "Fitbit"))

        knownIds.put(0x011C, Id(0x011C, "Baidu"))
        knownIds.put(0x013A, Id(0x013A, "Tencent"))
        knownIds.put(0x025C, Id(0x025C, "NetEase"))

        knownIds.put(0x027D, Id(0x027D, "Huawei"))
        knownIds.put(0x038F, Id(0x038F, "Xiaomi"))
        knownIds.put(0x079A, Id(0x079A, "Oppo"))
        knownIds.put(0x0837, Id(0x0837, "Vivo"))
        knownIds.put(0x072F, Id(0x072F, "OnePlus"))
        knownIds.put(0x02C5, Id(0x02C5, "Lenovo"))
        knownIds.put(0x022F, Id(0x022F, "Huami"))
        knownIds.put(0x0157, Id(0x0157, "Huami"))
        knownIds.put(0x0764, Id(0x0764, "Mobvoi"))

        knownIds.put(0x000A, Id(0x000A, "Qualcomm"))
        knownIds.put(0x001D, Id(0x001D, "Qualcomm"))
        knownIds.put(0x008C, Id(0x008C, "Qualcomm"))
        knownIds.put(0x00B8, Id(0x00B8, "Qualcomm"))
        knownIds.put(0x00D7, Id(0x00D7, "Qualcomm"))
        knownIds.put(0x00D8, Id(0x00D8, "Qualcomm"))
        knownIds.put(0x011A, Id(0x011A, "Qualcomm"))
        knownIds.put(0x03E3, Id(0x03E3, "Qualcomm"))
        knownIds.put(0x000F, Id(0x000F, "Broadcom"))
        knownIds.put(0x0046, Id(0x0046, "MediaTek"))
    }

    fun getId(number: Int): Id? {
        return knownIds.get(number)
    }

    data class Id(val number: Int, val name: String)
}