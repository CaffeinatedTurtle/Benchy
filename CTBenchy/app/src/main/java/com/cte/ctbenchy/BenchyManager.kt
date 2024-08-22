package com.cte.ctbenchy
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
class BenchyManager {

    // Define mode values
    companion object {
        const val TAG ="BenchManager"
        const val MODE_UNIDIRECTIONAL = 0x00.toByte()
        const val MODE_BIDIRECTIONAL = 0x01.toByte()
        const val MODE_PROGRAM = 0x02.toByte()
        val modeNames    = listOf("Uni-Directional", "Bi-Directional", "program")


        // Define servo channel indexes
        const val SERVO_CHANNEL_1 = 0
        const val SERVO_CHANNEL_2 = 1
        const val SERVO_CHANNEL_3 = 2
        const val SERVO_CHANNEL_4 = 3

        const val RUDDER = SERVO_CHANNEL_1
        const val THROTTLE = SERVO_CHANNEL_2

        // Define message types
        const val BENCHY_CONFIG = 0x10.toByte()
        const val BENCHY_OP = 0x20.toByte()

        // Define switch names for specific switch index values
        const val SWITCH_STBD = 0
        const val SWITCH_PORT = 1
        const val SWITCH_AFT = 2
        const val SWITCH_HORN = 3
        const val SWITCH_MOTOR = 4




        fun getRudder(data: Benchy): Int {
            return  ((data.operation.servoValues[RUDDER].toInt() and 0xFF) * 180 / 255) - 90 // Map 0..255 to -90..90
        }

        fun setRudder(data: Benchy, value: Int) {
           // rudder servo scalse -90 to 90 degrees but is limited in the screen slider to -60 to + 60
            var scaledValue = ((value + 90) * 255) / 180 // Map -90..90 to 0..255
            data.operation.servoValues[RUDDER] = (scaledValue and 0xff).toByte()
         }

        fun getThrottle(data: Benchy): Int {

            return if (data.config.mode == MODE_BIDIRECTIONAL) {
                ((data.operation.servoValues[THROTTLE].toInt() and 0xFF) * 180 / 255) - 90 // Map 0..255 to -90..90
            } else {
                ((data.operation.servoValues[THROTTLE].toInt() and 0xFF) * 180 / 255) // Map 0..255 to 0 .. 180
            }
        }

        fun setThrottle(data: Benchy, value: Int) {
            var scaledValue = if (data.config.mode == MODE_BIDIRECTIONAL) {
                ((value + 90) * 255) / 180 // Map -90..90 to 0..255
            } else {
                (value  * 255) / 180 // Map -90..90 to 0..255
            }
            data.operation.servoValues[THROTTLE] = (scaledValue and 0xff).toByte()
        }

        fun getSwitchValue(data: Benchy, index: Int): Boolean {
            return (data.operation.switchValue.toInt() and (1 shl index)) != 0
        }

        fun setSwitchValue(data: Benchy, index: Int, value: Boolean): Byte {
            require(index >= 0 && index < 8) { "Index must be between 0 and 7" }

            return if (value) {
                // Set the bit at index in switchValue
                (data.operation.switchValue.toInt() or (1 shl index)).toByte()
            } else {
                // Clear the bit at index in switchValue
                (data.operation.switchValue.toInt() and (1 shl index).inv()).toByte()
            }
        }

        fun getSwitchName(index: Int): String {
            return when (index) {
                SWITCH_STBD -> "stbd"
                SWITCH_PORT -> "port"
                SWITCH_AFT -> "aft"
                SWITCH_MOTOR -> "motor"
                SWITCH_HORN -> "horn"
                else -> "unknown"
            }
        }


        fun printBenchy(benchy: Benchy) {
            val config = benchy.config as? Configuration ?: return
            Log.i(TAG,"Mode: ${modeNames[config.mode.toInt()]}")
            Log.i(TAG,"MAC Address: ${config.macAddress.joinToString(":") { "%02X".format(it) }}")
            val op = benchy.operation as? Operation ?: return
            Log.i(TAG,"Switch Value: ${op.switchValue}")
            Log.i(TAG,"Throttle  ${BenchyManager.getThrottle(benchy)}")
            Log.i(TAG,"rudder  ${BenchyManager.getRudder(benchy)}")

        }
    }


    data class Configuration(
        var mode: Byte,
        val macAddress: ByteArray
    ) {
        init {
            require(macAddress.size == 6) { "MAC address must be 6 bytes long." }
        }

        fun toByteArray(): ByteArray {
            val buffer = ByteBuffer.allocate(7)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.put(mode)
            buffer.put(macAddress)
            return buffer.array()
        }

        companion object {
            fun fromByteArray(data: ByteArray): Configuration {
                val buffer = ByteBuffer.wrap(data)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val mode = buffer.get()
                val macAddress = ByteArray(6)
                buffer.get(macAddress)
                return Configuration(mode, macAddress)
            }
        }
    }

    data class Operation(
        var switchValue: Byte,
        val servoValues: ByteArray
    ) {
        init {
            require(servoValues.size == 4) { "Servo values array must be 4 bytes long." }
        }

        fun toByteArray(): ByteArray {
            val buffer = ByteBuffer.allocate(5)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.put(switchValue)
            buffer.put(servoValues)
            return buffer.array()
        }

        companion object {
            fun fromByteArray(data: ByteArray): Operation {
                val buffer = ByteBuffer.wrap(data)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val switchValue = buffer.get()
                val servoValues = ByteArray(4)
                buffer.get(servoValues)
                return Operation(switchValue, servoValues)
            }
        }
    }

    data class Benchy(
        val config: Configuration,
        val operation: Operation
    ) {
        fun toByteArray(): ByteArray {
            val configBytes = config.toByteArray()
            val operationBytes = operation.toByteArray()

            val buffer = ByteBuffer.allocate(configBytes.size + operationBytes.size)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.put(configBytes)
            buffer.put(operationBytes)
            return buffer.array()
        }

        companion object {
            fun fromByteArray(data: ByteArray): Benchy {
                val configData = data.copyOfRange(0, 7)
                val operationData = data.copyOfRange(7, 12)

                val config = Configuration.fromByteArray(configData)
                val operation = Operation.fromByteArray(operationData)

                return Benchy(config, operation)
            }
        }
    }



}

