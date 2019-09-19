package br.com.arnaldmartins.melhortempo.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import br.com.arnaldmartins.melhortempo.DateParse;
import br.com.arnaldmartins.melhortempo.Mensagem;
import br.com.arnaldmartins.melhortempo.R;

public class SensorActivity extends AppCompatActivity {

    public final String ACTION_USB_PERMISSION = "br.com.arnaldmartins.melhortempo.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    TextView txtLog;
    ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        // Este comando faz com que a tela fique sempre acesa para esta activity, mas SOMENTE para ESTA activity. Arnald 19-12-2014
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        txtLog = findViewById(R.id.txtLog);
        scroll = findViewById(R.id.scrollSensor);

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
        //Mensagem.mensagemToast("RECEIVER REGISTRADO", SensorActivity.this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        onClickStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        onClickStop();
    }

    public void onClickStart() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                Mensagem.mensagemToast(""+deviceVID, this);
                if (deviceVID == 6790 || deviceVID == 9025)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
    }

    public void onClickStop() {
        try {
            serialPort.close();
            Mensagem.mensagemToast("Serial Connection Closed!", SensorActivity.this);
        }catch (Exception e){
            Mensagem.mensagemToast(e.getMessage(), SensorActivity.this);
        }
    }



    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            //setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(txtLog,"Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            Mensagem.mensagemToast("PORT NOT OPEN", SensorActivity.this);
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        Mensagem.mensagemToast("PORT IS NULL", SensorActivity.this);
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    Mensagem.mensagemToast("PERM NOT GRANTED", SensorActivity.this);
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop();
            }
        }
    };


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                //data.concat("/n");
                tvAppend(txtLog, data);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    public void onClickClear(View view) {
        txtLog.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(DateParse.dateToString(new Date(), "HH:mm:ss")+" -> "+ ftext +"\n");
                scroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }


}
