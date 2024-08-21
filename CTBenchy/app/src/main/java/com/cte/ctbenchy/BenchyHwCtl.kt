package com.cte.ctbenchy

import android.app.Activity
import android.content.Context
import android.util.Log
import com.cte.bluetooth.BluetoothHandler
import com.cte.bluetooth.BluetoothMgr
import com.cte.bluetooth.BluetoothViewModel
import com.cte.bluetooth.IBluetoothMgr
import com.cte.ctbenchy.BenchyManager.Companion.MODE_BIDIRECTIONAL
import com.cte.ctbenchy.BenchyManager.Companion.RUDDER
import com.cte.ctbenchy.BenchyManager.Companion.printBenchy
import com.cte.ctbenchy.ui.BenchyViewModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.and
import kotlin.experimental.xor

class BenchyHwCtl(val benchyViewModel: BenchyViewModel) : IBluetoothMgr {
    private final val TAG = "BenchyHwCtl"
    private var bluetoothHandler: BluetoothHandler? = null
    private lateinit var context: Activity
    private lateinit var model: BluetoothViewModel
    private val  dataQueued: AtomicBoolean = AtomicBoolean(false)
    private var benchy : BenchyManager.Benchy = initializeBenchy()

    private fun initializeBenchy(): BenchyManager.Benchy {
        // initialize with dummy values
        val config = BenchyManager.Configuration(
            mode =  MODE_BIDIRECTIONAL, // Replace with actual mode value
            macAddress = byteArrayOf(
                0x01,
                0x02,
                0x03,
                0x04,
                0x05,
                0x06
            ) // Replace with actual MAC address
        )

        // Example initialization for Operation
        val operation = BenchyManager.Operation(
            switchValue = 0x00, // Replace with actual switch value
            servoValues = byteArrayOf(128.toByte(), 128.toByte(), 0, 0) // initialize rudder to midpoint throttle to midpoint for bidirectional operation
        )

        // Initialize Benchy with the created config and operation objects
        return BenchyManager.Benchy(
            config = config,
            operation = operation
        )
    }
    fun initialize(ctx: MainActivity) {
        // initialize the bluetooth handler and search for a device that
        // exposes the service UUID
        Log.i(TAG, "INITIALIZE bluetooth handler")
        this.bluetoothHandler = BluetoothHandler(ctx, this, SERVICE_UUID)
        bluetoothHandler?.scan()
        benchyViewModel.setConnectionState(BluetoothMgr.STATE_SCANNING)


    }

    fun toggleLed(led: Byte) {
        Log.i(TAG,"BATMAN led ${led}")
        var ledMask = benchyViewModel.uiState.value.ledMask
        ledMask = ledMask xor led
        benchy.operation.switchValue = ledMask
        bluetoothHandler?.writeCharacteristic(BENCHY_CHARACTERISTIC_UUID, benchy.toByteArray())
        //bluetoothHandler?.readCharacteristic(LED_CHARACTERISTIC_UUID)
    }

    fun setLedMask(ledMask: Byte) {
        benchy.operation.switchValue = ledMask
        val bytes = ByteArray(1)
        bytes[0] = ledMask
        bluetoothHandler?.writeCharacteristic(BENCHY_CHARACTERISTIC_UUID, benchy.toByteArray())
        // bluetoothHandler?.readCharacteristic(LED_CHARACTERISTIC_UUID)
    }

    fun setMode(mode: Int) {
        benchy.config.mode = mode.toByte()
        bluetoothHandler?.writeCharacteristic(BENCHY_CHARACTERISTIC_UUID, benchy.toByteArray())
        //bluetoothHandler?.readCharacteristic(MODE_CHARACTERISTIC_UUID)
    }

    fun setThrottle(throttle: Int) {
        Log.i(TAG,"BATMAN sete throttle to ${throttle}")
        BenchyManager.setThrottle(benchy,throttle)
        if (bluetoothHandler?.currentQueueSize() ?: -1 < 5) {
            dataQueued.set(false)
        }
        if (dataQueued.compareAndSet(false, true)) {
            // add atomic mutex to only allow one throttle command at a time otherwise
            // they all queue up and that's creates lag. the slider move much faster than
            // the bluetooth can keep up

            bluetoothHandler?.writeCharacteristic(BENCHY_CHARACTERISTIC_UUID, benchy.toByteArray())
        }

    }

    fun setRudder(rudder: Int) {
        Log.i(TAG,"BATMAN set rudder to ${rudder}")
        BenchyManager.setRudder(benchy,rudder)
        Log.i(
            TAG,
            "lock ${dataQueued.get()} queuesize ${bluetoothHandler?.currentQueueSize() ?: -1}"
        )
        if ((bluetoothHandler?.currentQueueSize() ?: -1) < 5) {
            dataQueued.set(false)
        }
        if (dataQueued.compareAndSet(false, true)) {
             bluetoothHandler?.writeCharacteristic(BENCHY_CHARACTERISTIC_UUID, benchy.toByteArray())
        }

    }


    companion object {
        val SERVICE_UUID = UUID.fromString("7507cee3-db32-4e5a-bd6b-96b62887129e")
        val BENCHY_CHARACTERISTIC_UUID = UUID.fromString("d7c1861c-beff-430f-9a72-fc05c6cc997d")
        val PORT_LED = 2.toByte()
        val STBD_LED = 1.toByte()
        val STERN_LED = 4.toByte()
        val HORN = 8.toByte()
        val MOTOR_SOUND = 16.toByte()
        val MODE_UNI = 0.toByte()
        val MODE_BI = 1.toByte()
        val MODE_PROG = 2.toByte()



    }


    override fun onConnect() {
        Log.i(TAG, "Benchy connect")
        Log.i(TAG, " read characteristics and load initial model")
        bluetoothHandler?.enableNotifyAll()
        bluetoothHandler?.writeCharacteristic(BENCHY_CHARACTERISTIC_UUID, benchy.toByteArray())
    }

    override fun onDisconnect() {
        Log.i(TAG, "Benchy disconnected")
    }

    override fun onServicesDiscovered() {
        Log.i(TAG,"Services discovered")
    }

    override fun onDataRecieved(uuid: UUID, value: ByteArray) {
        Log.i(
            TAG,
            "receive a value for ${uuid.toString()} ${Utility.ByteArraytoHex(value, "%02x")}"
        )
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        if ((bluetoothHandler?.currentQueueSize() ?: 0) < 4) {
            dataQueued.set(false)

        }
        when (uuid) {
            BENCHY_CHARACTERISTIC_UUID -> {
                benchy = BenchyManager.Benchy.fromByteArray(value)
                benchyViewModel.setRudder(BenchyManager.getRudder(benchy).toShort())
                benchyViewModel.setThrottle(BenchyManager.getThrottle(benchy).toShort())
                benchyViewModel.setLedMask(benchy.operation.switchValue)
                dataQueued.set(false)
                printBenchy(benchy)

            }

        }
    }

    override fun onConnectionStateChanged(state: Int) {
        benchyViewModel.setConnectionState(state)
    }

    fun onResume(ctx: Context) {
        bluetoothHandler?.connect()
    }

    fun onPause(ctx: Context) {
        bluetoothHandler?.disconnect()
    }

    fun onDestroy(ctx: Context) {
        bluetoothHandler?.disconnect()
    }


}

