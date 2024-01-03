package com.cte.ctbenchy

import android.app.Activity
import android.content.Context
import android.util.Log
import com.cte.bluetooth.BluetoothHandler
import com.cte.bluetooth.BluetoothMgr
import com.cte.bluetooth.BluetoothViewModel
import com.cte.bluetooth.IBluetoothMgr
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
    private val rudderQueued: AtomicBoolean = AtomicBoolean(false)
    private val throttleQueued: AtomicBoolean = AtomicBoolean(false)


    fun initialize(ctx: MainActivity) {
        // initialize the bluetooth handler and search for a device that
        // exposes the service UUID
        Log.i(TAG, "INITIALIZE bluetooth handler")
        this.bluetoothHandler = BluetoothHandler(ctx, this, SERVICE_UUID)
        bluetoothHandler?.scan()
        benchyViewModel.setConnectionState(BluetoothMgr.STATE_SCANNING)


    }

    fun toggleLed(led: Byte) {
        var ledMask = benchyViewModel.uiState.value.ledMask
        ledMask = ledMask xor led
        val bytes = ByteArray(1)
        bytes[0] = ledMask
        bluetoothHandler?.writeCharacteristic(LED_CHARACTERISTIC_UUID, bytes)
        //bluetoothHandler?.readCharacteristic(LED_CHARACTERISTIC_UUID)
    }

    fun setLedMask(ledMask: Byte) {
        val bytes = ByteArray(1)
        bytes[0] = ledMask
        bluetoothHandler?.writeCharacteristic(LED_CHARACTERISTIC_UUID, bytes)
        // bluetoothHandler?.readCharacteristic(LED_CHARACTERISTIC_UUID)
    }

    fun setMode(mode: Int) {
        val bytes = ByteArray(1)
        bytes[0] = mode.toByte()
        bluetoothHandler?.writeCharacteristic(MODE_CHARACTERISTIC_UUID, bytes)
        //bluetoothHandler?.readCharacteristic(MODE_CHARACTERISTIC_UUID)
    }

    fun setThrottle(throttle: Int) {
        if (bluetoothHandler?.currentQueueSize() ?: -1 < 5) {
            throttleQueued.set(false)
        }
        if (throttleQueued.compareAndSet(false, true)) {
            val bytes = ByteArray(Int.SIZE_BYTES)
            val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.putInt(throttle)
            // add atomic mutex to only allow one throttle command at a time otherwise
            // they all queue up and that's creates lag. the slider move much faster than
            // the bluetooth can keep up

            bluetoothHandler?.writeCharacteristic(THROTTLE_CHARACTERISTIC_UUID, bytes)
        }

    }

    fun setRudder(rudder: Float) {
        Log.i(
            TAG,
            "lock ${rudderQueued.get()} queuesize ${bluetoothHandler?.currentQueueSize() ?: -1}"
        )
        if ((bluetoothHandler?.currentQueueSize() ?: -1) < 5) {
            rudderQueued.set(false)
        }
        if (rudderQueued.compareAndSet(false, true)) {
            val bytes = ByteArray(Int.SIZE_BYTES)
            val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.putInt(rudder.toInt())
            Log.i(TAG, "write rudder angle ${rudder.toInt()}")
            bluetoothHandler?.writeCharacteristic(RUDDER_CHARACTERISTIC_UUID, bytes)
        }

    }


    companion object {
        val SERVICE_UUID = UUID.fromString("7507cee3-db32-4e5a-bd6b-96b62887129e")
        val RUDDER_CHARACTERISTIC_UUID = UUID.fromString("d7c1861c-beff-430f-9a72-fc05c6cc997d")
        val THROTTLE_CHARACTERISTIC_UUID = UUID.fromString("87607759-37d1-41b5-b2c8-c44b7c746083")
        val MODE_CHARACTERISTIC_UUID = UUID.fromString("16d68508-2fd4-40a9-ba61-aac41cb81e45")
        val LED_CHARACTERISTIC_UUID = UUID.fromString("3a84a192-d522-46ef-b7c8-36b9fc062490")
        val PORT_LED = 2.toByte()
        val STBD_LED = 1.toByte()
        val STERN_LED = 4.toByte()
        val MODE_UNI = 0.toByte()
        val MODE_BI = 1.toByte()
        val MODE_PROG = 2.toByte()
        val MOTOR_SOUND = 16.toByte()
        val HORN = 8.toByte()

    }


    override fun onConnect() {
        Log.i(TAG, "Benchy connect")
        Log.i(TAG, " read characteristics and load initial model")
        bluetoothHandler?.enableNotifyAll()
        bluetoothHandler?.let { handler ->
            handler.readCharacteristic(RUDDER_CHARACTERISTIC_UUID)
            handler.readCharacteristic(THROTTLE_CHARACTERISTIC_UUID)
            handler.readCharacteristic(MODE_CHARACTERISTIC_UUID)
            handler.readCharacteristic(LED_CHARACTERISTIC_UUID)
        }

    }

    override fun onDisconnect() {
        Log.i(TAG, "Benchy disconnected")
    }

    override fun onServicesDiscovered() {
        TODO("Not yet implemented")
    }

    override fun onDataRecieved(uuid: UUID, value: ByteArray) {
        Log.i(
            TAG,
            "receive a value for ${uuid.toString()} ${Utility.ByteArraytoHex(value, "%02x")}"
        )
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        if ((bluetoothHandler?.currentQueueSize() ?: 0) < 4) {
            rudderQueued.set(false)
            throttleQueued.set(false)
        }
        when (uuid) {
            RUDDER_CHARACTERISTIC_UUID -> {
                val rudder = buffer.getShort(0)
                benchyViewModel.setRudder(rudder)
                rudderQueued.set(false)
                Log.i(
                    TAG,
                    "recieve rudder andgle ${rudder} hex: ${
                        Utility.ByteArraytoHex(
                            value,
                            "%02x"
                        )
                    }"
                )

            }

            THROTTLE_CHARACTERISTIC_UUID -> {
                val throttle = buffer.getShort(0)
                benchyViewModel.setThrottle(throttle)
                throttleQueued.set(false)
                Log.i(
                    TAG,
                    "set trottle ${throttle}  hex:${Utility.ByteArraytoHex(value, "%02x")}"
                )
            }

            MODE_CHARACTERISTIC_UUID -> {
                val mode = value[0]
                benchyViewModel?.setMode(mode)
                Log.i(TAG, "set mode ${Utility.ByteArraytoHex(value, "%02x")}")
            }

            LED_CHARACTERISTIC_UUID -> {
                if (value.isEmpty()) {
                    Log.i(TAG, "no data LED")
                } else {
                    val led = value[0]
                    benchyViewModel.setLedMask(led)
                    var ledMask = benchyViewModel.uiState.value.ledMask
                    ledMask?.let {

                        val red = it and 2
                        val white = it and 4
                        val green = it and 1
                        val motor = it and 16
                        val horn = it and 8

                        Log.i(
                            TAG,
                            "set led ${
                                Utility.ByteArraytoHex(
                                    value,
                                    "%02x"
                                )
                            } red:${red} white: ${white} green:${green} motor:${motor} horn:${horn}"
                        )
                    }
                }
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

