package com.cte.ctbenchy

class BenchyManager {

    // Define mode values
    companion object {
        const val MODE_UNIDIRECTIONAL = 0x01.toByte()
        const val MODE_BIDIRECTIONAL = 0x02.toByte()
        const val MODE_PROGRAM = 0x03.toByte()

        // Define servo channel indexes
        const val SERVO_CHANNEL_1 = 0
        const val SERVO_CHANNEL_2 = 1
        const val SERVO_CHANNEL_3 = 2
        const val SERVO_CHANNEL_4 = 3

        // Define message types
        const val BENCHY_CONFIG = 0x10.toByte()
        const val BENCHY_OP = 0x20.toByte()

        // Define switch names for specific switch index values
        const val SWITCH_STBD = 0
        const val SWITCH_PORT = 1
        const val SWITCH_AFT = 2
        const val SWITCH_MOTOR = 3
        const val SWITCH_HORN = 4
    }

    data class Configuration(
        var mode: Byte = 0,
        var macAddress: ByteArray = ByteArray(6)
    )

    data class Operation(
        var switchValue: Byte = 0,
        var servoValues: ByteArray = ByteArray(4)
    )

    data class BenchyMessage(
        var messageType: Byte = 0,
        var payload: Any? = null
    )

    fun createConfigMessage(config: Configuration): BenchyMessage {
        return BenchyMessage(BENCHY_CONFIG, config)
    }

    fun createOpMessage(op: Operation): BenchyMessage {
        return BenchyMessage(BENCHY_OP, op)
    }

    fun parse(rawMessage: ByteArray): BenchyMessage? {
        if (rawMessage.isEmpty()) return null

        val messageType = rawMessage[0]
        val payload = when (messageType) {
            BENCHY_CONFIG -> {
                if (rawMessage.size < 7) return null
                val config = Configuration(
                    mode = rawMessage[1],
                    macAddress = rawMessage.copyOfRange(2, 8)
                )
                config
            }
            BENCHY_OP -> {
                if (rawMessage.size < 6) return null
                val op = Operation(
                    switchValue = rawMessage[1],
                    servoValues = rawMessage.copyOfRange(2, 6)
                )
                op
            }
            else -> return null
        }

        return BenchyMessage(messageType, payload)
    }

    fun getRaw(msg: BenchyMessage): ByteArray {
        return when (msg.messageType) {
            BENCHY_CONFIG -> {
                val config = msg.payload as? Configuration ?: return byteArrayOf()
                byteArrayOf(
                    BENCHY_CONFIG,
                    config.mode,
                    *config.macAddress
                )
            }
            BENCHY_OP -> {
                val op = msg.payload as? Operation ?: return byteArrayOf()
                byteArrayOf(
                    BENCHY_OP,
                    op.switchValue,
                    *op.servoValues
                )
            }
            else -> byteArrayOf()
        }
    }

    fun getSwitchValue(switchValue: Byte, index: Int): Boolean {
        return (switchValue.toInt() and (1 shl index)) != 0
    }

    fun setSwitchValue(switchValue: Byte, index: Int, value: Boolean): Byte {
        require(index >= 0 && index < 8) { "Index must be between 0 and 7" }

        return if (value) {
            // Set the bit at index in switchValue
            (switchValue.toInt() or (1 shl index)).toByte()
        } else {
            // Clear the bit at index in switchValue
            (switchValue.toInt() and (1 shl index).inv()).toByte()
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

    fun printMessage(msg: BenchyMessage) {
        when (msg.messageType) {
            BENCHY_CONFIG -> {
                val config = msg.payload as? Configuration ?: return
                println("Message Type: BENCHY_CONFIG")
                println("Mode: ${config.mode}")
                println("MAC Address: ${config.macAddress.joinToString(":") { "%02X".format(it) }}")
            }
            BENCHY_OP -> {
                val op = msg.payload as? Operation ?: return
                println("Message Type: BENCHY_OP")
                println("Switch Value: ${op.switchValue}")
                println("Servo Values: ${op.servoValues.joinToString(", ") { it.toString() }}")
            }
            else -> println("Unknown Message Type")
        }
    }
}
