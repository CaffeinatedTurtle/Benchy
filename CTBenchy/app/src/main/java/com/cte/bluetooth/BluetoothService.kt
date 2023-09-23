package com.cte.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque


/**
 * BTLE service to manage GATT devices
 */
@SuppressLint("MissingPermission")
class BluetoothService : Service() {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    private var rqueue = ConcurrentLinkedDeque<BTrequest>()
    private var isQueueRunning = false;

    private var bluetoothSocket: BluetoothSocket? = null

    private var isReady =false;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            Log.i(TAG, "newstate " + newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                connectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(
                    TAG,
                    "Attempting service discovery:"
                )
                rqueue.clear()
                mBluetoothGatt?.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                connectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
                rqueue.clear()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.v(TAG, "services discovered status:" + status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                execute()
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }


        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.v(TAG, "MTU exchanged")
            isQueueRunning =false;
            isReady=true;
            execute()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "read characteristic " + characteristic.uuid.toString())
                next()
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, null)
            }

        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            characteristic?.let {
                Log.v(
                    TAG,
                    "wrote characteristic " + characteristic.uuid.toString() + " status:" + status
                )
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                next()
            }

        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.v(TAG, "characteristic changed " + characteristic.uuid.toString())
            next();
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.v(TAG, "wrote descriptor " + descriptor?.uuid.toString())
            next()
        }

    }

    private val mBinder = LocalBinder()

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() = if (mBluetoothGatt == null) null else mBluetoothGatt!!.services

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic
    ){
        broadcastUpdate(action, characteristic, null)
    }

    private fun broadcastUpdate(
        action: String,
        data: ByteArray
    ){
        broadcastUpdate(action, null, data)
    }

        private fun broadcastUpdate(
            action: String,
            characteristic: BluetoothGattCharacteristic?,
            data: ByteArray?
        ) {
        val intent = Intent(action)
        characteristic?.let{
            intent.putExtra(EXTRA_UUID, it.uuid.toString())
            intent.putExtra(EXTRA_DATA, it.value)
        }
        data?.let {
            intent.putExtra(EXTRA_DATA, it)
        }

        sendBroadcast(intent)
    }




    inner class LocalBinder : Binder() {
        val service: BluetoothService
            get() = this@BluetoothService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }

        bluetoothAdapter = bluetoothManager!!.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    fun exchangeGattMtu(mtu: Int) {
        var retry = 5
        var status: Boolean? = false
        while (false == status && retry > 0) {
            status = mBluetoothGatt?.requestMtu(mtu)
            retry--
        }
    }

    fun pair(address: String?){
        val device = bluetoothAdapter!!.getRemoteDevice(address)
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            device.createBond();
        }
    }

    fun isPaired(device: BluetoothDevice?) :Boolean{
        device?.let {
            return it.bondState == BluetoothDevice.BOND_BONDED;
        }
        return false;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(address: String?): Boolean {
        Log.i(TAG, "try and connect " + address)
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address == bluetoothDeviceAddress
            && mBluetoothGatt != null
        ) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            if (mBluetoothGatt!!.connect()) {
                connectionState = STATE_CONNECTING
                return true
            } else {
                return false
            }
        }

        val device = bluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback, TRANSPORT_LE)
        Log.d(TAG, "Trying to create a new connection.")
        bluetoothDeviceAddress = address
        connectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.let {
          it.disconnect();
        }
        isReady=false;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        mBluetoothGatt?.let {
            it.close() ;
            mBluetoothGatt = null
        }

    }

    fun isConnected() :Boolean{
        return (connectionState== STATE_CONNECTED);
    }

    /**
     * Request a READ on a given `BluetoothGattCharacteristic`. The READ result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to READ from.
     */
    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        Log.i(TAG, "READ req characteristic " + characteristic.uuid.toString())
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    /**
     * Request a WRITE on a given `BluetoothGattCharacteristic`. The READ result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to READ from.
     */
   private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        Log.i(TAG, "WRITE req characteristic " + characteristic.uuid.toString())
        mBluetoothGatt!!.writeCharacteristic(characteristic)
    }



    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    private fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        // This is specific to enable notifications the characteristice
        if ( characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            Log.i(TAG, "update descriptor")
            val descriptor = characteristic.getDescriptor(
                UUID.fromString(BluetoothAttributes.CLIENT_CHARACTERISTIC_CONFIG)
            )
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }

    }


    fun postWriteCharacteristic(characteristic: BluetoothGattCharacteristic){
        Log.i(TAG, "post write " + characteristic.uuid)
        rqueue.offer(BTrequest(BTRequestType.WRITE, characteristic, false))
        execute()
    }

    fun postReadCharacteristic(characteristic: BluetoothGattCharacteristic){
        Log.i(TAG, "post read " + characteristic.uuid)
        rqueue.offer(BTrequest(BTRequestType.READ, characteristic, false))
        execute()
    }
    fun postNotifyCharacteristic(characteristic: BluetoothGattCharacteristic, enable: Boolean){
        Log.i(TAG, "post notify " + characteristic.uuid)
        rqueue.offer(BTrequest(BTRequestType.NOTIFY, characteristic, enable))
        execute()
    }

    fun execute() {
        synchronized(this) {
            Log.d(TAG, "execute: queue size=" + rqueue.size + "Processing= " + isQueueRunning)
            if (!isReady){
                return
            }
            if (isQueueRunning) {
                return
            }
            isQueueRunning = true
        }
        next()
    }

    /**
     * Get the next queued request, if any, and perform the requested
     * operation
     */
    operator fun next() {
        var request: BTrequest? = null
        synchronized(this) {
           request = rqueue.poll()
            if (request == null) {
                isQueueRunning = false
                return
            }

        if (request!!.type == BTRequestType.READ) {
            readCharacteristic(request!!.characteristic)
        } else if (request!!.type == BTRequestType.WRITE) {
            writeCharacteristic(request!!.characteristic)
        } else if (request!!.type == BTRequestType.NOTIFY){
            setCharacteristicNotification(request!!.characteristic, request!!.enable)
        }
        }
    }

    fun unpairDevice(device: BluetoothDevice): Boolean {
        return try {
            val rv =
                invokeBluetoothDeviceMethod(device, "removeBond") as Boolean
            Log.i(TAG, "Un-Pair status:" + rv)
            rv
        } catch (e: Exception) {
            Log.e(TAG, "Un-Pair: exception: " + e.message)
            false
        }
    }

    fun getDevice(address: ByteArray) :BluetoothDevice?{
        var btDevice: BluetoothDevice? = null
        var macAddressStr = Utility.ByteArrayToMacAddrString(address)
        val bondedDevices: Set<BluetoothDevice> = bluetoothAdapter!!.getBondedDevices()
        for (dev in bondedDevices) {
            if (dev.address.contains(macAddressStr)){
                return dev;
            }
        }
        return btDevice;
    }







    @Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class
    )
    private fun invokeBluetoothDeviceMethod(
        dev: BluetoothDevice,
        methodName: String,
        vararg args: Any
    ): Any {
        val c: Class<*> = dev.javaClass
        val m = c.getMethod(methodName)
        m.isAccessible = true
        return m.invoke(dev, *args)
    }


    enum class BTRequestType{
        NOTIFY,WRITE,READ
    }
    inner class BTrequest(
        type: BTRequestType,
        characteristic: BluetoothGattCharacteristic,
        enable: Boolean
    ){
        val type = type
        val characteristic = characteristic
        val enable = enable
    }



    companion object {
        private val TAG = BluetoothService::class.java.simpleName

        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2

        val SPP_CONNECT_RETRY = 3

        val ACTION_GATT_CONNECTED = "com.cte.bluetooth.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.cte.bluetooth.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED =
            "com.cte.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "ccom.cte.bluetooth.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "com.cte.bluetooth.EXTRA_DATA"
        val EXTRA_UUID = "ccom.cte.bluetooth.EXTRA_UUID"



    }
}
