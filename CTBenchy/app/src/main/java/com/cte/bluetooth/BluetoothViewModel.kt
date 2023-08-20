package com.cte.bluetooth


import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList


class BluetoothViewModel : ViewModel() {
    private val TAG = BluetoothViewModel::class.java.simpleName


    private var _isScanning = MutableLiveData<Boolean>()


    val isScanning: LiveData<Boolean> = _isScanning

    fun setScanning(scanning: Boolean) {
        _isScanning.postValue(scanning)
    }

    private var _deviceList = MutableLiveData<List<ScanResult>>()


    fun setDevices(devices: ArrayList<ScanResult>) {
        _deviceList.postValue(devices)
    }


    val deviceList: LiveData<List<ScanResult>>? = _deviceList

    private var _selectedDevice = MutableLiveData<ScanResult>()

    @SuppressLint("MissingPermission")
    fun selectDevice(result: ScanResult) {
        Log.i(TAG, "select " + result.device.name)
        _selectedDevice.value = result
    }

    val selectedDevice: LiveData<ScanResult>? = _selectedDevice


    private var _services = MutableLiveData<List<BluetoothGattService>>()

    fun setServices(services: List<BluetoothGattService>) {
        _services.postValue(services)
        var tempchars = ConcurrentHashMap<UUID,BluetoothGattCharacteristic>()
        for (service in services){
            for(characteristic in service.characteristics){
                 tempchars.put(characteristic.uuid,characteristic)
            }
        }
        _characteristics.value=tempchars
        Log.i(TAG,"update services and characteristices "+tempchars.size)

    }

    val services: LiveData<List<BluetoothGattService>>? = _services

    private var _selectedService = MutableLiveData<BluetoothGattService>()

    fun selectService(service: BluetoothGattService) {
        _selectedService.value = service
        setCharacteristics(toHashMap(service.characteristics))
    }

    private fun toHashMap(chars: List<BluetoothGattCharacteristic>): ConcurrentHashMap<UUID, BluetoothGattCharacteristic>{
        var tempchars = ConcurrentHashMap<UUID,BluetoothGattCharacteristic>()
        for(characteristic in chars){
            tempchars.put(characteristic.uuid,characteristic)
        }

        return tempchars

    }

    val selectedService: LiveData<BluetoothGattService>? = _selectedService


    var _characteristics = MutableLiveData<ConcurrentHashMap<UUID, BluetoothGattCharacteristic>>()

    fun setCharacteristics(characteristics: ConcurrentHashMap<UUID, BluetoothGattCharacteristic>) {
        _characteristics.value = characteristics
    }

    val characteristics: LiveData<ConcurrentHashMap<UUID, BluetoothGattCharacteristic>>? = _characteristics

    var _characteristic = MutableLiveData< BluetoothGattCharacteristic>()

    fun setCharacteristic(characteristic:  BluetoothGattCharacteristic) {
        _characteristic.value = characteristic
        _characteristics.value?.put(characteristic.uuid,characteristic)

    }

    val characteristic: LiveData<BluetoothGattCharacteristic>? = _characteristic

}

