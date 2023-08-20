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

package com.cte.bluetooth

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




}
