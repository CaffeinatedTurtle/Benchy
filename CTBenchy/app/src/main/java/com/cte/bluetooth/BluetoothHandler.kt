package com.cte.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList


interface IBluetoothHandler {
    fun scanLeDevice(enable: Boolean)
    fun connect(device: BluetoothDevice)
    fun disconnect()
    fun stopScan()
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic)
    fun notifyCharacteristic(characteristic: BluetoothGattCharacteristic,enable: Boolean)
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic)
    fun enableNotifyAll()
    fun setNewCharacteristicValue(
        characteristic: BluetoothGattCharacteristic,
        charSequence: CharSequence
    )

    fun writeChangedCharacteristics()

}



class BluetoothHandler(
    ctx:Context,
    listener:IBluetoothMgr,
    serviceUUID: UUID?
) :
    IBluetoothMgr {


    private val TAG = BluetoothHandler::class.java.simpleName

    private var isScanning: Boolean = false
    private var handler: Handler? = null
    private var scanner: BluetoothLeScanner? = null
    private var deviceList = ConcurrentHashMap<String, ScanResult>()
    private var devices: ArrayList<ScanResult>? = null
    private var isConnected: Boolean = false
    private var isConnecting: Boolean = false
    private var updatedCharacteristics =
        ConcurrentHashMap<BluetoothGattCharacteristic, ByteArray>()
    private var currentCharacteristics =  ConcurrentHashMap<BluetoothGattCharacteristic, ByteArray>()
    private lateinit var readCharacteristicList: List<BluetoothGattCharacteristic>
    var serviceList = ArrayList<BluetoothGattService>()
    private val listener=listener


    private var currentNumberOfDevices = 0
    private  var bluetoothMgr: BluetoothMgr? = null



        private val leScanCallback = object : ScanCallback() {
    
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (ActivityCompat.checkSelfPermission(
                        ctx,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i(TAG,"BATMAN MISSING PERMISSIONS")
                    return
                }
                result.device.name?.let {
                    deviceList[result.device.name] = result

                    result.device.uuids?.let {
                        for (uuit in it) {
                           // Log.i(TAG, "uuid " + uuit.uuid.toString())
                        }
                    }
                    result.scanRecord?.serviceUuids?.let {
                        for (uuit in it) {
                            Log.i(TAG, "uuid " + uuit.uuid.toString())
                            if (uuit.uuid == serviceUUID){
                                // found a device that
                                if (!isConnecting) {
                                    isConnecting=true
                                    stopScan()
                                    connect(result.device)
                                }
                            }
                        }
                    }

                }
                if (deviceList.size != currentNumberOfDevices) {
                    Log.i(TAG, "Scan push  " + deviceList.size)
                    devices = ArrayList(deviceList.values)
                    currentNumberOfDevices = deviceList.size
                }
                super.onScanResult(callbackType, result)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.i(TAG, "Scan error " + errorCode)
                super.onScanFailed(errorCode)
            }

        }

        init {
            handler = Handler(Looper.getMainLooper())
            bluetoothMgr = BluetoothMgr()
            Log.i(TAG, "INTIALIZE BLUEtoOth MGR")
            bluetoothMgr?.initialize(ctx,this)
            scanner = bluetoothMgr?.getScanner()
            scanner?.let{
                scan()
            }

        }

        fun scan() {

            scanLeDevice(true)
        }



       @SuppressLint("MissingPermission")
       fun scanLeDevice(enable: Boolean) {
           if (isConnected) {
               disconnect()
           }

            Log.i(TAG, "Scanning")
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                deviceList.clear()
                devices = ArrayList(deviceList.values)
                 handler!!.postDelayed({
                    if (isScanning) {

                        Log.i(TAG,"BATMAN ${scanner}")
                        scanner?.stopScan(leScanCallback)
                        isScanning = false
                        Log.i(TAG, "stop scanning ")

                    }
                }, SCAN_PERIOD)

                isScanning = true
                Log.i(TAG, "Start scanning ")
                scanner?.startScan(leScanCallback)


            } else {
                isScanning = false
                scanner!!.flushPendingScanResults(leScanCallback)
                scanner!!.stopScan(leScanCallback)

                //  bluetoothAdapter!!.stopLeScan(mLeScanCallback)
            }


        }

        fun connect(device: BluetoothDevice) {
            Log.i(TAG,"BATMAN connect to ${bluetoothMgr} ${device.address}")
            handler?.let{
                it.postDelayed({
                    // add delay for scanning to stop
                    var result =
                        bluetoothMgr?.connect(device.address)

                }, 600)
            }

        }


       fun disconnect() {
            Log.i(
                TAG,
                "disconnect ${ isConnected } ${bluetoothMgr?.isConnected()}")

            if (isConnected) {
                bluetoothMgr?.disconnect()
                bluetoothMgr?.close()
            }
        }


       @SuppressLint("MissingPermission")
       fun stopScan() {
            isScanning = false;
            scanner!!.stopScan(leScanCallback)
        }

    fun enableNotifyAll() {
            Log.i(TAG, "enable notify all")
            bluetoothMgr?.let{ mgr->
                mgr.supportedGattServices?.let{ services->
                  for (service in services){

                      service.characteristics?.let{ charct->
                          enableNotify(charct)
                      }
                  }


                }

            }


        }

   fun setNewCharacteristicValue(
        characteristic: BluetoothGattCharacteristic,
        charSequence: CharSequence
    ) {
        TODO("Not yet implemented")
    }


    private fun enableNotify(readList: List<BluetoothGattCharacteristic>) {
            readCharacteristicList = readList;
            for (char in readList) {
                if (isCharacteristicNotifiable(char)) {
                    notifyCharacteristic(char.uuid, true)
                }
            }
        }


    fun readCharacteristic(uuid:UUID) {
             bluetoothMgr?.let{mgr->
                 if (mgr.isConnected()) {
                     val characteristic = mgr.getCharacteristic(uuid)
                     characteristic?.let {
                         if (isCharacteristicReadable(characteristic)) {
                             mgr.postReadCharacteristic(characteristic)
                         } else {
                             Log.e(TAG, "characteristic not readable")
                         }
                     }
                }
            }

        }

   fun writeCharacteristic(uuid : UUID,value: ByteArray) {
            bluetoothMgr?.let {mgr->
                if (mgr.isConnected()) {
                    val characteristic = mgr.getCharacteristic(uuid)
                    characteristic?.let {
                         if (isCharacteristicWritable(characteristic)) {
                            mgr?.postWriteCharacteristic(characteristic,value)
                        } else {
                            Log.e(TAG, "characteristic not writable")
                        }
                    }
                }
            }

        }

    fun notifyCharacteristic(
           uuid:UUID,
            enable: Boolean
        ) {
            bluetoothMgr?.let { mgr->
                if (mgr.isConnected()) {
                    val characteristic = mgr.getCharacteristic(uuid)
                    characteristic?.let {
                        if (isCharacteristicNotifiable(characteristic)) {
                            mgr?.postNotifyCharacteristic(characteristic, enable)
                        } else {
                            Log.e(TAG, "characteristic not notifiable")
                        }
                    }
                }
            }

        }


 fun setNewCharacteristicValue(
            characteristic: BluetoothGattCharacteristic,
            byteArray: ByteArray
        ) {
            updatedCharacteristics[characteristic] = byteArray
        }

fun writeChangedCharacteristics() {
            bluetoothMgr?.let { mgr->
                for (changed in updatedCharacteristics) {
                    Log.i(TAG, "update " + changed.key.uuid.toString() + " with:" + changed.value)
                    if (BluetoothAttributes.isBinary(changed.key.uuid.toString())) {
                        changed.key.value = Utility.hexStringToByteArray(changed.value.toString())
                    } else {
                        changed.key.value = changed.value.toString().toByteArray()
                    }
                    mgr.postWriteCharacteristic(changed.key,changed.value)
                }
                updatedCharacteristics.clear()
            }


        }





        private fun updateCharacteristic(uuid: String, data: ByteArray) {
            Log.i(TAG,"updating ${uuid} ${data}")

            var characteristic = bluetoothMgr?.getCharacteristic(UUID.fromString(uuid))
            characteristic?.let {

            }
        }

        /**
         * @return Returns **true** if property is writable
         */
        fun isCharacteristicWritable(chararacteristic: BluetoothGattCharacteristic): Boolean {
            return chararacteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        }

        /**
         * @return Returns **true** if property is Readable
         */
        fun isCharacteristicReadable(chararacteristic: BluetoothGattCharacteristic): Boolean {
            return chararacteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
        }

        /**
         * @return Returns **true** if property is supports notification
         */
        fun isCharacteristicNotifiable(chararacteristic: BluetoothGattCharacteristic): Boolean {
            return chararacteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        }

    override fun onConnect() {
        isConnected = true
        isConnecting=false
        listener.onConnect()
    }

    override fun onDisconnect() {
        isConnected = false
        listener.onDisconnect()
    }


    override fun onServicesDiscovered() {

        for (service in bluetoothMgr?.supportedGattServices!!) {

            Log.d(TAG, " services " + service.uuid.toString())
            if (!BluetoothAttributes.ignore(service.uuid.toString())) {
                serviceList.add(service)
            }
        }
        bluetoothMgr?.exchangeGattMtu(512)
    }

    override fun onDataRecieved(uuid: UUID, value: ByteArray) {
        value?.let{ value->
            uuid?.let{
                Log.i(TAG,"data available ${it} ${Utility.ByteArraytoHex(value,"%02x")}")
                listener.onDataRecieved(uuid,value)
            }
        }
    }

companion object {

        private val REQUEST_ENABLE_BT = 1
        // Stops scanning after 10 seconds.
        private val SCAN_PERIOD: Long = 100000
    }


}

