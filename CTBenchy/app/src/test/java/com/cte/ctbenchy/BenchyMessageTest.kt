package com.cte.ctbenchy

import org.junit.Test
import org.junit.Assert.*
import java.lang.Byte


class BenchyManagerTest {

    private lateinit var manager: BenchyManager


    fun setUp() {
        manager = BenchyManager()
    }


    fun tearDown() {
        // Clean up resources if needed
    }

    @Test
    fun testCreateConfigMessage() {
        val config = BenchyManager.Configuration(
            mode = BenchyManager.MODE_BIDIRECTIONAL,
            macAddress = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte())
        )
        val message = manager.createConfigMessage(config)

        assertEquals(BenchyManager.BENCHY_CONFIG, message.messageType)
        assertTrue(message.payload is BenchyManager.Configuration)

        val parsedConfig = message.payload as BenchyManager.Configuration
        assertEquals(config.mode, parsedConfig.mode)
        assertArrayEquals(config.macAddress, parsedConfig.macAddress)
    }

    @Test
    fun testCreateOpMessage() {
        val op = BenchyManager.Operation(
            switchValue = 0b10101010.toByte(),
            servoValues = byteArrayOf(10, 20, 30, 40)
        )
        val message = manager.createOpMessage(op)

        assertEquals(BenchyManager.BENCHY_OP, message.messageType)
        assertTrue(message.payload is BenchyManager.Operation)

        val parsedOp = message.payload as BenchyManager.Operation
        assertEquals(op.switchValue, parsedOp.switchValue)
        assertArrayEquals(op.servoValues, parsedOp.servoValues)
    }

    @Test
    fun testParseConfigMessage() {
        val rawConfig = byteArrayOf(
            BenchyManager.BENCHY_CONFIG,
            BenchyManager.MODE_PROGRAM,
            0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte()
        )

        val parsedMessage = manager.parse(rawConfig)
        assertNotNull(parsedMessage)
        assertEquals(BenchyManager.BENCHY_CONFIG, parsedMessage!!.messageType)
        assertTrue(parsedMessage.payload is BenchyManager.Configuration)

        val parsedConfig = parsedMessage.payload as BenchyManager.Configuration
        assertEquals(BenchyManager.MODE_PROGRAM, parsedConfig.mode)
        assertArrayEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte()), parsedConfig.macAddress)
    }

    @Test
    fun testParseOpMessage() {
        val rawOp = byteArrayOf(
            BenchyManager.BENCHY_OP,
            0b11001100.toByte(),
            10, 20, 30, 40
        )

        val parsedMessage = manager.parse(rawOp)
        assertNotNull(parsedMessage)
        assertEquals(BenchyManager.BENCHY_OP, parsedMessage!!.messageType)
        assertTrue(parsedMessage.payload is BenchyManager.Operation)

        val parsedOp = parsedMessage.payload as BenchyManager.Operation
        assertEquals(0b11001100.toByte(), parsedOp.switchValue)
        assertArrayEquals(byteArrayOf(10, 20, 30, 40), parsedOp.servoValues)
    }

    @Test
    fun testGetRawConfigMessage() {
        val config = BenchyManager.Configuration(
            mode = BenchyManager.MODE_UNIDIRECTIONAL,
            macAddress = byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x12, 0x34, 0x56)
        )
        val message = BenchyManager.BenchyMessage(BenchyManager.BENCHY_CONFIG, config)

        val rawMessage = manager.getRaw(message)
        assertArrayEquals(
            byteArrayOf(
                BenchyManager.BENCHY_CONFIG,
                BenchyManager.MODE_UNIDIRECTIONAL,
                0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x12, 0x34, 0x56
            ),
            rawMessage
        )
    }

    @Test
    fun testGetRawOpMessage() {
        val op = BenchyManager.Operation(
            switchValue = 0b01010101.toByte(),
            servoValues = byteArrayOf(5, 15, 25, 35)
        )
        val message = BenchyManager.BenchyMessage(BenchyManager.BENCHY_OP, op)

        val rawMessage = manager.getRaw(message)
        assertArrayEquals(
            byteArrayOf(
                BenchyManager.BENCHY_OP,
                0b01010101.toByte(),
                5, 15, 25, 35
            ),
            rawMessage
        )
    }

    @Test
    fun testGetSwitchValue() {
        assertTrue(manager.getSwitchValue(0b01010101.toByte(), 0))
        assertFalse(manager.getSwitchValue(0b01010101.toByte(), 1))
        assertTrue(manager.getSwitchValue(0b01010101.toByte(), 2))
        assertFalse(manager.getSwitchValue(0b01010101.toByte(), 3))
        assertTrue(manager.getSwitchValue(0b01010101.toByte(), 4))
        assertFalse(manager.getSwitchValue(0b01010101.toByte(), 5))
        assertTrue(manager.getSwitchValue(0b01010101.toByte(), 6))
        assertFalse(manager.getSwitchValue(0b01010101.toByte(), 7))
    }

    @Test
    fun testSetSwitchValue() {
        val switchValue = 0.toByte()
        manager.setSwitchValue(switchValue, 1, true)
        assertEquals(0b00000010.toByte(), switchValue)

        manager.setSwitchValue(switchValue, 3, true)
        assertEquals(0b00001010.toByte(), switchValue)

        manager.setSwitchValue(switchValue, 5, true)
        assertEquals(0b00101010.toByte(), switchValue)
    }

    @Test
    fun testGetSwitchName() {
        assertEquals("stbd", manager.getSwitchName(BenchyManager.SWITCH_STBD))
        assertEquals("port", manager.getSwitchName(BenchyManager.SWITCH_PORT))
        assertEquals("aft", manager.getSwitchName(BenchyManager.SWITCH_AFT))
        assertEquals("motor", manager.getSwitchName(BenchyManager.SWITCH_MOTOR))
        assertEquals("horn", manager.getSwitchName(BenchyManager.SWITCH_HORN))
        assertEquals("unknown", manager.getSwitchName(10))
    }
}
