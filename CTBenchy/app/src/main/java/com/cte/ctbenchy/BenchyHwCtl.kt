package com.cte.ctbenchy

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.cte.bluetooth.BluetoothHandler
import com.cte.bluetooth.BluetoothService
import com.cte.bluetooth.BluetoothViewModel
import com.cte.bluetooth.IBluetoothListener
import java.util.UUID

class BenchyHwCtl() : IBluetoothListener {
    private final val TAG = "BenchyHwCtl"
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHandler: BluetoothHandler? = null
    private lateinit var context: Activity
    private lateinit var model: BluetoothViewModel
    private var bluetoothService: BluetoothService? = null

    // Code to manage Service lifecycle.
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.i(TAG, "service connected")
            bluetoothService = (service as BluetoothService.LocalBinder).service
             bluetoothHandler?.let{
                it.bluetoothService = bluetoothService
            }
            bluetoothService?.let{
                if (!it.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    context.finish()
                }
            }

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.i(TAG, "disconnected");
            bluetoothService = null
        }
    }

    fun initialize(ctx: MainActivity, viewModel: BluetoothViewModel) {
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothAdapter?.let {
            bluetoothHandler =
                BluetoothHandler(viewModel, it)
        }

        val gattServiceIntent = Intent(ctx, BluetoothService::class.java)
        ctx.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        bluetoothHandler?.let{
            ctx.registerReceiver(
                it.gattUpdateReceiver,
                it.makeGattUpdateIntentFilter()
            )
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
            var found = false;
            for (device in it) {

                Log.i(TAG, "device ${device.device.name}  ${device.scanRecord}")
                device.scanRecord?.serviceUuids?.let { services ->
                    Log.i(TAG, "service data found ${services}")
                    for (uuid in services) {
                        Log.i(TAG, " uuid ${uuid.toString()}")
                        if (uuid.uuid == SERVICE_UUID) {
                            Log.i(TAG, "Found Benchy")
                            viewModel?.selectDevice(device)
                            bluetoothHandler?.stopScan()

                            bluetoothHandler?.connect()

                            found = true
                            break
                        }
                    }
                }
                if (found) break
            }


        })
        viewModel.services!!.observe(ctx, androidx.lifecycle.Observer {
            Log.i(TAG, "found services")
            for (service in it) {
                Log.i(TAG, "is Benchy " + service.uuid.toString())
                Log.i(TAG, "service uuid " + service.uuid)
            }
        })

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
    }

    override fun onDisconnect() {
        Log.i(TAG, "Benchy disconnect")
    }

    fun onResume(ctx:Context) {
        bluetoothHandler?.let { handler ->
            ctx.registerReceiver(
                handler.gattUpdateReceiver,
                handler.makeGattUpdateIntentFilter()
            )
        }

    }

    fun onPause(ctx:Context) {
        bluetoothHandler?.let { handler ->
            ctx.unregisterReceiver(handler.gattUpdateReceiver)

        }
    }

        fun onDestroy(ctx:Context) {
            bluetoothService?.let {
                ctx.unbindService(serviceConnection)
            }
            bluetoothService = null
        }


}