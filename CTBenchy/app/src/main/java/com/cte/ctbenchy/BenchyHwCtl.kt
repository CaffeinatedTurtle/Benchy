package com.cte.ctbenchy

import android.app.Activity
import android.content.Context
import android.util.Log
import com.cte.bluetooth.BluetoothHandler
import com.cte.bluetooth.BluetoothViewModel
import com.cte.bluetooth.IBluetoothMgr
import com.cte.ctbenchy.ui.BenchyViewModel
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.experimental.and
import kotlin.experimental.xor

class BenchyHwCtl() : IBluetoothMgr {
    private final val TAG = "BenchyHwCtl"
    private var bluetoothHandler: BluetoothHandler? = null
     private lateinit var context: Activity
    private lateinit var model: BluetoothViewModel
    lateinit var benchyViewModel:BenchyViewModel



    fun initialize(ctx: MainActivity,  benchy:BenchyViewModel) {
        // initialize the bluetooth handler and search for a device that
        // exposes the service UUID
        Log.i(TAG,"BATMAN INITIALIZE handler")
        this.bluetoothHandler = BluetoothHandler(ctx, this, SERVICE_UUID)
        this.benchyViewModel = benchy
        bluetoothHandler?.scan()


    }

    companion object {
        val SERVICE_UUID = UUID.fromString("7507cee3-db32-4e5a-bd6b-96b62887129e")
        val RUDDER_CHARACTERISTIC_UUID = UUID.fromString("d7c1861c-beff-430f-9a72-fc05c6cc997d")
        val THROTTLE_CHARACTERISTIC_UUID = UUID.fromString("87607759-37d1-41b5-b2c8-c44b7c746083")
        val MODE_CHARACTERISTIC_UUID = UUID.fromString("16d68508-2fd4-40a9-ba61-aac41cb81e45")
        val LED_CHARACTERISTIC_UUID = UUID.fromString("3a84a192-d522-46ef-b7c8-36b9fc062490")
    }


    override fun onConnect() {
        Log.i(TAG, "Benchy connect")
        Log.i(TAG," read characteristics and load initial model")
        bluetoothHandler?.let{ handler->
            handler.readCharacteristic(RUDDER_CHARACTERISTIC_UUID)
            handler.readCharacteristic(THROTTLE_CHARACTERISTIC_UUID)
            handler.readCharacteristic(MODE_CHARACTERISTIC_UUID)
            handler.readCharacteristic(LED_CHARACTERISTIC_UUID)
        }

    }

    override fun onDisconnect() {
        Log.i(TAG, "Benchy disconnect")
    }

    override fun onServicesDiscovered() {
        TODO("Not yet implemented")
    }

    override fun onDataRecieved(uuid: UUID, value: ByteArray) {
        Log.i(TAG,"BATMAN receive a value for ${uuid.toString()}")
        val buffer = ByteBuffer.wrap(value)
        when(uuid){
            RUDDER_CHARACTERISTIC_UUID -> {
                 val rudder = buffer.getShort(0)
                benchyViewModel.uiState.value.rudder = rudder.toInt()
                Log.i(TAG,"BATMAN recieve rudder andgle ${rudder} ${Utility.ByteArraytoHex(value,"%02x")}")

            }
            THROTTLE_CHARACTERISTIC_UUID -> {
                val throttle = buffer.getShort(0)
                benchyViewModel.uiState.value.throttle = throttle.toInt()
                Log.i(TAG,"BATMAN set trottle ${Utility.ByteArraytoHex(value,"%02x")}")
            }
            MODE_CHARACTERISTIC_UUID -> {
                val mode = value[0]
                benchyViewModel.uiState.value.mode = mode
                Log.i(TAG,"BATMAN set mode ${Utility.ByteArraytoHex(value,"%02x")}")
            }
            LED_CHARACTERISTIC_UUID -> {
                val led = value[0]
                benchyViewModel.setLedMask(led)

                val red =   benchyViewModel.uiState.value.ledMask and 2
                val white = benchyViewModel.uiState.value.ledMask and 4
                val green = benchyViewModel.uiState.value.ledMask and 1

                Log.i(TAG,"BATMAN set led ${Utility.ByteArraytoHex(value,"%02x")} red:${red} white: ${white} green:${green}")
            }
        }
    }

    fun onResume(ctx:Context) {


    }

    fun onPause(ctx:Context) {

    }

        fun onDestroy(ctx:Context) {
           bluetoothHandler?.disconnect()
        }

    fun toggleRedLed(){
        var ledMask = benchyViewModel.uiState.value.ledMask
        ledMask= ledMask xor 2
        bluetoothHandler?.writeCharacteristic(LED_CHARACTERISTIC_UUID,ledMask)
    }



}

