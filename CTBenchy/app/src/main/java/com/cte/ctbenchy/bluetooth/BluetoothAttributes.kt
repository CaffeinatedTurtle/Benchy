/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing showPermissionsFragment and
 * limitations under the License.
 */

/* modifications Copyright (C) 2019-2020 CTE */

package com.connectiphy.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * This class includes subset of standard GATT attributes
 * https://www.bluetooth.com/specifications/gatt/characteristics/
 */
object BluetoothAttributes {
    private val attributes = HashMap<String, String>()
    private val ignore = HashMap<String, String>()
    private val binaryAttributes = HashMap<String, String>()

    val SPP_PROFILE = "00001101-0000-1000-8000-00805F9B34FB"

    val BTLE_BASE = "-0000-1000-8000-00805f9b34fb"


    val GENERIC_ACCESS_SERVICE = "00001800" + BTLE_BASE
    val GENERIC_ATTRIBUTE_SERVICE = "00001801" + BTLE_BASE

    val CLIENT_CHARACTERISTIC_CONFIG = "00002902" + BTLE_BASE

    var HEART_RATE_SERVICE = "0000180d" + BTLE_BASE
    var HEART_RATE_MEASUREMENT = "00002a37" + BTLE_BASE


    val DEVICE_INFORMATION_SERVICE = "0000180a" + BTLE_BASE
    val MANUFACTURER_NAME = "00002a29" + BTLE_BASE
    val MODEL_NUMBER = "00002a24" + BTLE_BASE
    val SERIAL_NUMBER = "00002a25" + BTLE_BASE
    val HARDWARE_REVISION = "00002a27" + BTLE_BASE
    val FIRMWARE_REVISION = "00002a26" + BTLE_BASE
    val SOFTWARE_REVISION = "00002a28" + BTLE_BASE
    val SYSTEM_ID = "00002a23" + BTLE_BASE
    val REGULATORY_CERTIFICATION_DATA_LIST = "00002a2a" + BTLE_BASE
    val PNP_ID = "00002a50" + BTLE_BASE

    val BATTERY_SERVICE = "0000180f" + BTLE_BASE
    val BATTERY_LEVEL = "00002a19" + BTLE_BASE
    val BATTERY_LEVEL_STATE = "00002a1B" + BTLE_BASE
    val BATTERY_POWER_STATE = "00002a1A" + BTLE_BASE

// todo make the names resource lookups for translation

    val C4_BASE = "-9b35-4933-9b10-52ffa9740042"
    val C4_PREFIX = "ef68"
    val C4_V2_BASE = "-584f-4f3e-8671-49bd0ba76fe4"
    val C4_V2_PREFIX = "89ed66"
    
    val C4_SERVICE = arrayOf(C4_PREFIX + "0100" + C4_BASE, C4_V2_PREFIX + "10" + C4_V2_BASE)


        /* UUID value of the Device Name Characteristic, R/W */
    val C4_CHAR_DEV_NAME = arrayOf(C4_PREFIX + "0101" + C4_BASE, C4_V2_PREFIX + "11" + C4_V2_BASE)

    /* UUID value of the Advertising Parameters Characteristic, R/W */
    val C4_CHAR_ADV_PARAM = arrayOf(C4_PREFIX + "0102" + C4_BASE, C4_V2_PREFIX + "12" + C4_V2_BASE)

    /* UUID value of the Connection Parameters Characteristic, R/W */
    val C4_CHAR_CONN_PARAM = arrayOf(C4_PREFIX + "0103" + C4_BASE, C4_V2_PREFIX + "13" + C4_V2_BASE)

    /* UUID value of the Version Info Characteristic, R */
    val C4_CHAR_VER_INFO = arrayOf(C4_PREFIX + "0104" + C4_BASE, C4_V2_PREFIX + "14" + C4_V2_BASE)

    /* UUID value of the Language Select Characteristic, R/W */
    val C4_CHAR_LANGUAGE = arrayOf(C4_PREFIX + "0105" + C4_BASE, C4_V2_PREFIX + "15" + C4_V2_BASE)

    /* UUID value of the PTT Select Characteristic, R/W */
    val C4_CHAR_PTT_SEL = arrayOf(C4_PREFIX + "0106" + C4_BASE, C4_V2_PREFIX + "16" + C4_V2_BASE)
    /* UUID value of the Factory Reset Characteristic, W */
    val C4_CHAR_FACT_RST = arrayOf(C4_PREFIX + "0107" + C4_BASE, C4_V2_PREFIX + "17" + C4_V2_BASE)

    /* UUID value of the Network Status Characteristic, R */
    val C4_CHAR_NET_STAT = arrayOf(C4_PREFIX + "0108" + C4_BASE, C4_V2_PREFIX + "18" + C4_V2_BASE)

    /* UUID value of the Network Create Characteristic, R/W */
    val C4_CHAR_NET_CREATE = arrayOf(C4_PREFIX + "0109" + C4_BASE, C4_V2_PREFIX + "19" + C4_V2_BASE)

    /* UUID value of the Network Access Characteristic, W */
    val C4_CHAR_NET_ACCESS = arrayOf(C4_PREFIX + "010a" + C4_BASE, C4_V2_PREFIX + "1a" + C4_V2_BASE)

    /* TBD: UUID value of the HS/HFP Device Pairing Characteristic, R/W */
    val C4_CHAR_DEV_PAIR = arrayOf(C4_PREFIX + "010b" + C4_BASE, C4_V2_PREFIX + "1b" + C4_V2_BASE)

    /* TBD: UUID value of the Sensor Bonding Characteristic, R/W */
    val C4_CHAR_SENS_BOND = arrayOf(C4_PREFIX + "010c" + C4_BASE, C4_V2_PREFIX + "1c" + C4_V2_BASE)


    val C4_CONSOLE = arrayOf(C4_PREFIX + "0200" + C4_BASE, C4_V2_PREFIX + "30" + C4_V2_BASE)
    val C4_CONSOLE_CHAR_CMD = arrayOf(
        C4_PREFIX + "0201" + C4_BASE,
        C4_V2_PREFIX + "31" + C4_V2_BASE
    )

    // rx is bt to c4 tx is c4 to bt
    val C4_CONSOLE_DATA_RX = arrayOf(C4_PREFIX + "0202" + C4_BASE, C4_V2_PREFIX + "33" + C4_V2_BASE)
    val C4_CONSOLE_DATA_TX = arrayOf(C4_PREFIX + "0203" + C4_BASE, C4_V2_PREFIX + "32" + C4_V2_BASE)

 
    val C4_V2_SERVICE = C4_V2_PREFIX + "10" + C4_V2_BASE


    val C4_CONSOLE_MODE_IDLE = 0;
    val C4_CONSOLE_MODE_CONSOLE = 1;
    val C4_CONSOLE_MODE_DIAG = 2;

    val C4_V1 =0
    val C4_V2 =1


    init {
        ignore.put(GENERIC_ACCESS_SERVICE, "generic access service")
        ignore.put(GENERIC_ATTRIBUTE_SERVICE, "generic access service")
        // Services and their characteristics

        attributes.put(DEVICE_INFORMATION_SERVICE, "Device Information Service")
        attributes.put(MANUFACTURER_NAME, "Manufacturer Name")
        attributes.put(MODEL_NUMBER, "Model Number")
        attributes.put(SERIAL_NUMBER, "Serial Number")
        attributes.put(HARDWARE_REVISION, "Hardware Revision")
        attributes.put(FIRMWARE_REVISION, "Firmware Revision")
        attributes.put(SOFTWARE_REVISION, "Software Revision")
        attributes.put(SYSTEM_ID, "System ID")
        attributes.put(
            REGULATORY_CERTIFICATION_DATA_LIST,
            "IEEE 11073-20601 Regulatory Certification Data List"
        )
        attributes.put(PNP_ID, "Pnp Id")


        attributes.put(BATTERY_SERVICE, "Battery Service")
        attributes.put(BATTERY_LEVEL, "Battery Level")
        attributes.put(BATTERY_LEVEL_STATE, "Battery Level State")
        attributes.put(BATTERY_POWER_STATE, "Battery Power State")

        attributes.put(C4_SERVICE[C4_V1], "C4 Service")
        attributes.put(C4_CHAR_DEV_NAME[C4_V1], "Device Name")
        binaryAttributes.put(C4_CHAR_ADV_PARAM[C4_V1], "Advertising Parameters")
        binaryAttributes.put(C4_CHAR_CONN_PARAM[C4_V1], "Connection Parameters")
        binaryAttributes.put(C4_CHAR_VER_INFO[C4_V1], "Version Info")
        binaryAttributes.put(C4_CHAR_LANGUAGE[C4_V1], "Language Select")
        binaryAttributes.put(C4_CHAR_PTT_SEL[C4_V1], "PTT Select")
        binaryAttributes.put(C4_CHAR_FACT_RST[C4_V1], "Factory Reset")
        binaryAttributes.put(C4_CHAR_NET_STAT[C4_V1], "Network Status")
        binaryAttributes.put(C4_CHAR_NET_CREATE[C4_V1], "Network Create")
        binaryAttributes.put(C4_CHAR_NET_ACCESS[C4_V1], "Network Access")
        binaryAttributes.put(C4_CHAR_DEV_PAIR[C4_V1], "HS/HFP Device Pairing")
        binaryAttributes.put(C4_CHAR_SENS_BOND[C4_V1], "Sensor Bonding")

        attributes.put(C4_CONSOLE[C4_V1],"C4 Console")
        attributes.put(C4_CONSOLE_DATA_TX[C4_V1],"Filename")
        attributes.put(C4_CONSOLE_DATA_RX[C4_V1],"Data")
        binaryAttributes.put(C4_CONSOLE_CHAR_CMD[C4_V1],"Console CMD")

        attributes.put(C4_SERVICE[C4_V2], "C4 Service")
        attributes.put(C4_CHAR_DEV_NAME[C4_V2], "Device Name")
        binaryAttributes.put(C4_CHAR_ADV_PARAM[C4_V2], "Advertising Parameters")
        binaryAttributes.put(C4_CHAR_CONN_PARAM[C4_V2], "Connection Parameters")
        binaryAttributes.put(C4_CHAR_VER_INFO[C4_V2], "Version Info")
        binaryAttributes.put(C4_CHAR_LANGUAGE[C4_V2], "Language Select")
        binaryAttributes.put(C4_CHAR_PTT_SEL[C4_V2], "PTT Select")
        binaryAttributes.put(C4_CHAR_FACT_RST[C4_V2], "Factory Reset")
        binaryAttributes.put(C4_CHAR_NET_STAT[C4_V2], "Network Status")
        binaryAttributes.put(C4_CHAR_NET_CREATE[C4_V2], "Network Create")
        binaryAttributes.put(C4_CHAR_NET_ACCESS[C4_V2], "Network Access")
        binaryAttributes.put(C4_CHAR_DEV_PAIR[C4_V2], "HS/HFP Device Pairing")
        binaryAttributes.put(C4_CHAR_SENS_BOND[C4_V2], "Sensor Bonding")

        attributes.put(C4_CONSOLE[C4_V2],"C4 Console")
        attributes.put(C4_CONSOLE_DATA_TX[C4_V2],"Filename")
        attributes.put(C4_CONSOLE_DATA_RX[C4_V2],"Data")
        binaryAttributes.put(C4_CONSOLE_CHAR_CMD[C4_V2],"Console CMD")


    }

    fun lookup(uuid: String): String {
        return lookup(uuid, "Unknown")
    }

    fun lookup(uuid: String, defaultName: String): String {
        var name = attributes.get(uuid)
        if (name == null){
            name = binaryAttributes.get(uuid)
        }
        return if (name == null) defaultName else name
    }

    fun isBinary(uuid: String): Boolean{
        val name = binaryAttributes.get(uuid);
        return (name != null)
    }

    fun ignore(uuid: String): Boolean {
        val name = ignore.get(uuid)
        return (name != null)
    }

    fun getCharacteristic(list: ConcurrentHashMap<UUID,BluetoothGattCharacteristic>, uuids: Array<String>) : BluetoothGattCharacteristic?{
        for (uuid in uuids){
            if (list.containsKey(UUID.fromString(uuid))){
                return list.get(UUID.fromString(uuid))
            }
        }
        return null
    }

    fun isC4(uuid: String): Boolean{
        return (uuid.startsWith(C4_SERVICE[C4_V1])|| uuid.startsWith(C4_CONSOLE[C4_V1]))
    }
    fun isC4_V2(uuid: String): Boolean{
        return (uuid.startsWith(C4_SERVICE[C4_V2])|| uuid.startsWith(C4_CONSOLE[C4_V2]) )
    }
    
    fun getC4Version(uuid:String):Int{
        if (isC4(uuid)) return C4_V1
        if (isC4_V2(uuid)) return C4_V2;
        return -1;
    }




}
