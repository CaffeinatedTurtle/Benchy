package com.cte.ctbenchy

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.cte.bluetooth.BluetoothHandler
import com.cte.bluetooth.BluetoothViewModel
import com.cte.bluetooth.IBluetoothListener
import java.util.UUID

class BenchyHwCtl() : IBluetoothListener {
    private  final val  TAG = "BenchyHwCtl"
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHandler: BluetoothHandler? = null
    private lateinit var context: Activity
    private lateinit var model: BluetoothViewModel


    fun initialize(ctx:MainActivity,viewModel: BluetoothViewModel){
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothAdapter?.let {
            bluetoothHandler =
                BluetoothHandler(viewModel, it)
        }
        bluetoothHandler?.listener = this;
        bluetoothHandler?.scanLeDevice(true)

       viewModel.deviceList!!.observe(ctx, androidx.lifecycle.Observer {

            if (ActivityCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "NO bluetooth granted")
            }
            var found=false;
            for (device in it) {

                Log.i(TAG, "device ${device.device.name}  ${device.scanRecord}")
                device.scanRecord?.serviceUuids?.let{ services->
                    Log.i(TAG,"service data found ${services}")
                    for (uuid in services){
                        Log.i(TAG, " uuid ${uuid.toString()}")
                        if (uuid.uuid == SERVICE_UUID) {
                            Log.i(TAG, "Found Benchy")
                            viewModel?.selectDevice(device)
                            bluetoothHandler?.connect()
                            found = true
                            break
                        }
                    }
                }
                if (found)break
            }


        })
        viewModel.services!!.observe(ctx,androidx.lifecycle.Observer {
            Log.i(TAG,"found services")
            for (service in it){
                Log.i(TAG,"is Benchy "+service.uuid.toString() )
                Log.i(TAG,"service uuid "+service.uuid)
            }
        })

    }

    companion object{
        val  SERVICE_UUID = UUID.fromString("7507cee3-db32-4e5a-bd6b-96b62887129e")
        val  RUDDER_CHARACTERISTIC_UUID = UUID.fromString("d7c1861c-beff-430f-9a72-fc05c6cc997d")
        val THROTTLE_CHARACTERISTIC_UUID = UUID.fromString("87607759-37d1-41b5-b2c8-c44b7c746083")
        val MODE_CHARACTERISTIC_UUID = UUID.fromString("16d68508-2fd4-40a9-ba61-aac41cb81e45")
       val LED_CHARACTERISTIC_UUID = UUID.fromString("3a84a192-d522-46ef-b7c8-36b9fc062490")
    }


    override fun onConnect() {
        TODO("Not yet implemented")
    }

    override fun onDisconnect() {
        TODO("Not yet implemented")
    }
}