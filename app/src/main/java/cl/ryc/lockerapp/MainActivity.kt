package cl.ryc.lockerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.ryc.lockerapp.ui.theme.LockerAppTheme
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {


    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private lateinit var endpoint: UsbEndpoint
    private lateinit var intf: UsbInterface

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            LockerAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainView({
                        connectDevice()
                    }, {
                        GlobalScope.launch {

                            val bytesToSend = byteArrayOf(
                                0x8a.toByte(),
                                0x01,
                                0x01,
                                0x11,
                                0x9b.toByte()
                            )


                            val serial =
                                UsbSerialDevice.createUsbSerialDevice(device, connection).apply {
                                    open()
                                    setBaudRate(9600)
                                    setDataBits(UsbSerialInterface.DATA_BITS_8)
                                    setParity(UsbSerialInterface.PARITY_NONE)
                                    setStopBits(UsbSerialInterface.STOP_BITS_1)
                                    setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                                }

                            serial.write(bytesToSend)
                        }
                    })
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private var usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.apply {
                    // call your method that cleans up and closes communication with the device

                    Toast.makeText(
                        applicationContext,
                        "Dispositivo desconectado",
                        Toast.LENGTH_SHORT
                    ).show()
                    connection?.releaseInterface(intf)
                    connection?.close()
                }
            }
        }
    }

    private fun connectDevice(): String {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager

        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        Log.d("Check_USB", device.toString())

        if (device != null) {
            intf = device!!.getInterface(0)
            endpoint = intf.getEndpoint(1) //writing endpoint

            connection = manager.openDevice(device).apply {
                claimInterface(intf, true)
            }

            Toast.makeText(applicationContext, "Conectado", Toast.LENGTH_SHORT).show()

        }
        return device.toString()
    }
}

@Composable
fun MainView(connectDeviceCallback: () -> String, openDoorCallback: () -> Unit) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Locker android")
        Row {
            Button(onClick = { openDoorCallback() }) {
                Text(text = "Open door")
            }
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(text = connectDeviceCallback())
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LockerAppTheme {
        MainView({ "" }, {})
    }
}