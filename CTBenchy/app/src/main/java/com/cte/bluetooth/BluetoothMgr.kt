package com.cte.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque



interface IBluetoothMgr{
    fun onConnect()
    fun onDisconnect()

    fun onServicesDiscovered()

    fun onDataRecieved(uuid:UUID,value:ByteArray)
}

/**
 * BTLE service to manage GATT devices
 */
@SuppressLint("MissingPermission")
class BluetoothMgr {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    private var rqueue = ConcurrentLinkedDeque<BTrequest>()
    private var isQueueRunning = false;
    lateinit var listener:IBluetoothMgr
    lateinit var ctx:Context




    private var isReady =false;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            Log.i(TAG, "newstate " + newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                 connectionState = STATE_CONNECTED
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(
                    TAG,
                    "Attempting service discovery:"
                )
                rqueue.clear()
                mBluetoothGatt?.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                 connectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                rqueue.clear()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.v(TAG, "services discovered status:" + status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                 for (service in gatt.services){
                     service.characteristics
                 }

                 listener.onServicesDiscovered()
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
            listener.onConnect()
            execute()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "read characteristic " + characteristic.uuid.toString())
                next()
                listener.onDataRecieved(characteristic.uuid, value)

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

                next()
            }

        }


        override fun onCharacteristicChanged (gatt:BluetoothGatt,
       characteristic:   BluetoothGattCharacteristic,
        value:ByteArray) {
            Log.v(TAG, "characteristic changed " + characteristic.uuid.toString())
            listener.onDataRecieved(characteristic.uuid, value)
            next();


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



    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() = mBluetoothGatt?.services

   fun getSupportedCharacteristics (service:BluetoothGattService): List<BluetoothGattCharacteristic>? {
       return service.characteristics
   }

    fun getCharacteristic(uuid:UUID):BluetoothGattCharacteristic?{
        mBluetoothGatt?.services?.let { services->
            for (service in services){
                for (characteristic in service.characteristics){
                    if(characteristic.uuid == uuid) return characteristic
                }
            }

        }
        return null
    }


    fun getScanner():BluetoothLeScanner?{
        return bluetoothAdapter?.bluetoothLeScanner
    }






    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(ctx:Context, listener:IBluetoothMgr): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        this.ctx=ctx
        this.listener=listener
        if (bluetoothManager == null) {
            bluetoothManager = this.ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
        mBluetoothGatt = device.connectGatt(ctx, false, mGattCallback, TRANSPORT_LE)
        Log.d(TAG, "Trying to create a new connection. ${device.address}")
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
   @RequiresApi(Build.VERSION_CODES.TIRAMISU)
   private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value:ByteArray) {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        Log.i(TAG, "WRITE req characteristic " + characteristic.uuid.toString())
        mBluetoothGatt?.writeCharacteristic(characteristic,value,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
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


    fun postWriteCharacteristic(characteristic: BluetoothGattCharacteristic,value:ByteArray){
        Log.i(TAG, "post write " + characteristic.uuid)
        rqueue.offer(BTrequest(BTRequestType.WRITE, characteristic,value, false))
        execute()
    }

    fun postReadCharacteristic(characteristic: BluetoothGattCharacteristic){
        Log.i(TAG, "post read " + characteristic.uuid)
        rqueue.offer(BTrequest(BTRequestType.READ, characteristic, null,false))
        execute()
    }
    fun postNotifyCharacteristic(characteristic: BluetoothGattCharacteristic, enable: Boolean){
        Log.i(TAG, "post notify " + characteristic.uuid)
        rqueue.offer(BTrequest(BTRequestType.NOTIFY, characteristic,null, enable))
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
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    operator fun next() {
        var request: BTrequest? = null
        synchronized(this) {
           request = rqueue.poll()
            if (request == null) {
                isQueueRunning = false
                return
            }
        request?.let{ req->
            when (req.type) {
                BTRequestType.READ -> {
                    readCharacteristic(req.characteristic)
                }
                BTRequestType.WRITE -> {
                    req.newValue?.let {
                        writeCharacteristic(req.characteristic, req.newValue,)
                    }
                }
                BTRequestType.NOTIFY -> {
                    setCharacteristicNotification(request!!.characteristic, request!!.enable)
                }
            }
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
        newValue:ByteArray?,
        enable: Boolean
    ){
        val type = type
        val characteristic = characteristic
        val enable = enable
        val newValue = newValue
    }



    companion object {
        private val TAG = BluetoothMgr::class.java.simpleName

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
