package com.cte.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import com.cte.bluetooth.BluetoothService

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList


interface IBluetoothHandler {
    fun scanLeDevice(enable: Boolean)
    fun connect()
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
    fun unpair(device: BluetoothDevice?)
    fun pair(device: BluetoothDevice)
    fun getPairedDevice(address:ByteArray):BluetoothDevice?
}

interface IBluetoothListener{
    fun onConnect()
    fun onDisconnect()
}

@SuppressLint("MissingPermission")
class BluetoothHandler(
    internal var bluetoothViewModel: BluetoothViewModel,
    internal var bluetoothAdapter: BluetoothAdapter
) :
    IBluetoothHandler {


    private val TAG = BluetoothHandler::class.java.simpleName

    private var isScanning: Boolean = false
    private var handler: Handler? = null
    private var scanner: BluetoothLeScanner? = null
    private var deviceList = ConcurrentHashMap<String, ScanResult>()
    private var devices: ArrayList<ScanResult>? = null
    private var isConnected: Boolean = false
    private var updatedCharacteristics =
        ConcurrentHashMap<BluetoothGattCharacteristic, CharSequence>()
    private lateinit var readCharacteristicList: List<BluetoothGattCharacteristic>

    lateinit var listener: IBluetoothListener

    private var currentNumberOfDevices = 0
    var bluetoothService: BluetoothService? = null

    fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of READ
    //                        or notification operations.
    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.i(TAG, "Action: " + action)
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                // this get received the first time the device trie to set the notification
                // config and the notification complete doesn't happen
                // we nened to reset it
                Log.d(TAG, "Bonded ")
                disconnect()
                connect()

            }
            if (BluetoothService.ACTION_GATT_CONNECTED == action) {
                isConnected = true
                listener.onConnect()
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED == action) {
                isConnected = false
                listener.onDisconnect()

                // update data model
            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.
                Log.d(TAG, "update services ")
                var serviceList = ArrayList<BluetoothGattService>()

                for (service in bluetoothService!!.supportedGattServices!!) {

                    Log.d(TAG, " services " + service.uuid.toString())
                    if (!BluetoothAttributes.ignore(service.uuid.toString())) {
                        serviceList.add(service)
                    }
                }
                bluetoothService?.exchangeGattMtu(512)

                bluetoothViewModel.setServices(serviceList)
            } else if (BluetoothService.ACTION_DATA_AVAILABLE == action) {
                var uuid = intent.getStringExtra(BluetoothService.EXTRA_UUID)
                var data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA)
                if (data != null) {
                    if (uuid != null) {
                        updateCharacteristic(uuid, data)

                    }
                }
            }
        }
    }


        private val leScanCallback = object : ScanCallback() {
    
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.device.name?.let {
                    deviceList[result.device.name] = result

                    result.device.uuids?.let {
                        for (uuit in it) {
                           // Log.i(TAG, "uuid " + uuit.uuid.toString())
                        }
                    }
                    result.scanRecord?.serviceUuids?.let {
                        for (uuit in it) {
                            //Log.i(TAG, "uuid " + uuit.uuid.toString())
                        }
                    }

                }
                if (deviceList.size != currentNumberOfDevices) {
                    Log.i(TAG, "Scan push  " + deviceList.size)
                    devices = ArrayList(deviceList.values)
                    bluetoothViewModel.setDevices(devices!!)
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
            handler = Handler()
            scanner = bluetoothAdapter.bluetoothLeScanner
        }

        fun scan() {

            scanLeDevice(true)
        }



        override fun scanLeDevice(enable: Boolean) {
            disconnect()
            Log.i(TAG, "Scanning")
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                deviceList.clear()
                devices = ArrayList(deviceList.values)
                bluetoothViewModel.setDevices(devices!!)
                handler!!.postDelayed({
                    if (isScanning) {
                        scanner?.stopScan(leScanCallback)
                        isScanning = false
                        Log.i(TAG, "stop scanning ")
                        bluetoothViewModel.setScanning(isScanning)
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
            bluetoothViewModel.setScanning(isScanning)

        }

        override fun connect() {
            Log.i(TAG,"BATMAN connect to ${bluetoothService} ${bluetoothViewModel.selectedDevice!!.value!!.device.address}")
            handler?.let{
                it.postDelayed({
                    // add delay for scanning to stop
                    var result =
                        bluetoothService?.connect(bluetoothViewModel.selectedDevice!!.value!!.device.address)

                }, 600)
            }
            var result =
                bluetoothService?.connect(bluetoothViewModel.selectedDevice!!.value!!.device.address)
        }


        override fun disconnect() {
            Log.i(
                TAG,
                "disconnect " + isConnected + " " + bluetoothService?.isConnected() + "paired" + bluetoothService?.isPaired(
                    bluetoothViewModel.selectedDevice?.value?.device
                )
            );

            if (isConnected) {
                bluetoothService?.disconnect()
                bluetoothService?.close()
            }
        }


        override fun stopScan() {
            isScanning = false;
            scanner!!.stopScan(leScanCallback)
        }

        override fun enableNotifyAll() {
            Log.i(TAG, "enable notify all")
            bluetoothViewModel.characteristics?.let {
                it.value?.let {
                    enableNotify(ArrayList(it.values))
                }
            }
        }


        private fun enableNotify(readList: List<BluetoothGattCharacteristic>) {
            readCharacteristicList = readList;
            for (char in readList) {
                if (isCharacteristicNotifiable(char)) {
                    notifyCharacteristic(char, true)
                }
            }
        }


        override fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
            if (bluetoothService!!.isConnected()) {
                if (isCharacteristicReadable(characteristic)) {
                    bluetoothService?.postReadCharacteristic(characteristic)
                } else {
                    Log.e(TAG, "characteristic not readable")
                }
            }
        }

        override fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
            if (bluetoothService!!.isConnected()) {
                if (isCharacteristicWritable(characteristic)) {
                    bluetoothService?.postWriteCharacteristic(characteristic)
                } else {
                    Log.e(TAG, "characteristic not writable")
                }
            }
        }

        override fun notifyCharacteristic(
            characteristic: BluetoothGattCharacteristic,
            enable: Boolean
        ) {
            if (bluetoothService!!.isConnected()) {
                if (isCharacteristicNotifiable(characteristic)) {
                    bluetoothService?.postNotifyCharacteristic(characteristic, enable)
                } else {
                    Log.e(TAG, "characteristic not notifiable")
                }
            }
        }


        override fun setNewCharacteristicValue(
            characteristic: BluetoothGattCharacteristic,
            charSequence: CharSequence
        ) {
            updatedCharacteristics.put(characteristic, charSequence)
        }

        override fun writeChangedCharacteristics() {
            for (changed in updatedCharacteristics) {
                Log.i(TAG, "update " + changed.key.uuid.toString() + " with:" + changed.value)
                if (BluetoothAttributes.isBinary(changed.key.uuid.toString())) {
                    changed.key.value = Utility.hexStringToByteArray(changed.value.toString())
                } else {
                    changed.key.value = changed.value.toString().toByteArray()
                }
                bluetoothService?.postWriteCharacteristic(changed.key)
            }
            updatedCharacteristics.clear()
        }


        override fun unpair(device: BluetoothDevice?) {
            device?.let {
                if (it.bondState == BluetoothDevice.BOND_BONDED) {
                    if (it.name.contains("CTBenchy")) {
                        Log.i(TAG, "Unpair Peiker")
                        bluetoothService?.unpairDevice(it)
                    }
                }
            }

        }


        override fun pair(device: BluetoothDevice) {
            device?.let {
                if (it.bondState != BluetoothDevice.BOND_BONDED) {
                    it.createBond()
                }
            }
        }


        override fun getPairedDevice(address: ByteArray): BluetoothDevice? {
            return bluetoothService?.getDevice(address)
        }


        private fun updateCharacteristic(uuid: String, data: ByteArray) {

            var currentList = bluetoothViewModel.characteristics!!.value!!
            var characteristic = currentList.get(UUID.fromString(uuid))
            characteristic?.let {
                characteristic?.value = data;
                bluetoothViewModel.setCharacteristic(characteristic)
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



companion object {

        private val REQUEST_ENABLE_BT = 1
        // Stops scanning after 10 seconds.
        private val SCAN_PERIOD: Long = 100000
    }
}

