package br.com.arnaldmartins.melhortempo.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.arnaldmartins.melhortempo.GridAdapter;
import br.com.arnaldmartins.melhortempo.Mensagem;
import br.com.arnaldmartins.melhortempo.R;

public class MainActivity extends AppCompatActivity {

    GridView gridView;
    Spinner spnTempo;
    TextView txtAbrirContagem, txtVoltaTeorica;
    LinearLayout linLayConfig;
    Button btnIniciar, btnLimpar, btnTempo, btnMais, btnMenos;
    Chronometer cron;
    List<String> tempos = new ArrayList<>();
    int numColunas = 1;
    static int contCol=0, contLine=1;
    DecimalFormat dfSec = new DecimalFormat("00");
    DecimalFormat dfMill = new DecimalFormat("000");
    boolean cronRodando = false;
    public final String ACTION_USB_PERMISSION = "br.com.arnaldmartins.melhortempo.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    IntentFilter filter;
    // vator para guardar as posições na lista de cada parcial da volta teórica
    int voltaTeorica[];
    int contParcial=0;
    int contAbrirContagem=1;

    // teste troca de repositorio git. Arnald 23-12-2019


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // fazer com que o display permaneça aceso para esta activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        spnTempo = findViewById(R.id.spnTempo);
        txtAbrirContagem = findViewById(R.id.txtAbrirContagem);
        txtVoltaTeorica = findViewById(R.id.txtVoltaTeorica);
        txtVoltaTeorica.setVisibility(View.GONE);
        linLayConfig = findViewById(R.id.linearLayConfig);
        cron = findViewById(R.id.cronometro);
        gridView = findViewById(R.id.gridview);
        btnMais = findViewById(R.id.btnMais);
        btnMenos = findViewById(R.id.btnMenos);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnTempo = findViewById(R.id.btnTempo);
        btnTempo.setEnabled(false);
        btnLimpar = findViewById(R.id.btnLimpar);

        String[] parcial=new String[]{"1","2","3"};
        final ArrayAdapter<String> strArrParciais= new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, parcial);
        strArrParciais.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTempo.setAdapter(strArrParciais);
        spnTempo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // definir numero de colunas para o gridView
                numColunas = (i == 0 ? 2 : i+3);
                voltaTeorica = new int[i+1];
                zerarVoltaTeorica();
                limparGrid();
                gridView.setNumColumns(numColunas);
                txtAbrirContagem.setText(voltaTeorica.length+"");
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //Nada a fazer.
            }
        });

        // eventos de botões
        btnIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnIniciar.getText().toString().equalsIgnoreCase("INICIAR")) {
                    txtVoltaTeorica.setVisibility(View.GONE);
                    txtVoltaTeorica.setText("");
                    iniciarCronometro();
                }
                else {
                    pararCronometro();
                    calculaVoltaTeorica();
                }
            }
        });
        btnLimpar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                limparGrid();
                zerarVoltaTeorica();
            }
        });
        btnTempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tomaTempo();
            }
        });

        btnMais.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int limite = Integer.parseInt(spnTempo.getSelectedItem().toString());
                    int cont = Integer.parseInt(txtAbrirContagem.getText().toString());
                    int novoCont = cont+1 <= limite ? cont+1 : cont;
                    txtAbrirContagem.setText(novoCont+"");
                }catch (Exception e){}
            }
        });
        btnMenos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int cont = Integer.parseInt(txtAbrirContagem.getText().toString());
                    int novoCont = cont-1 > 0 ? cont-1: cont;
                    txtAbrirContagem.setText(novoCont+"");
                }catch (Exception e){}
            }
        });

        // objetos para leitura da usb serial
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        // registro do receiver que vai monitorar a porta usb serial
        //... passado para onOptionsItemSelected
    }


    @Override
    protected void onStart() {
        super.onStart();
        spnTempo.setSelection(2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        try {
            MenuItem menuActivitySensor = menu.add("CONFIG");
            MenuItem menuSensor = menu.add("SENSOR");
            menuSensor.setIcon(android.R.drawable.ic_media_play);
            menuSensor.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        catch (Exception e){
            Mensagem.mensagemToast("ERRO criando menus ["+e.getMessage()+"]", MainActivity.this);
        }

        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        try{
            if(item.getTitle().equals("SENSOR")){
                item.setIcon(android.R.drawable.ic_media_pause);
                item.setTitle("PARAR");
                registerReceiver(broadcastReceiver, filter);
                ligarSensor();
            }
            else if(item.getTitle().equals("PARAR")){
                desligarSensor();
                pararCronometro();
                item.setIcon(android.R.drawable.ic_media_play);
                item.setTitle("SENSOR");
            }
            else {
                try{
                    unregisterReceiver(broadcastReceiver);
                }catch (Exception e){}
                Intent abrirActivitySensor = new Intent(this, PilotoActivity.class);
                startActivity(abrirActivitySensor);
            }

        }
        catch (Exception e){
            Mensagem.mensagemToast("ERRO ao selecionar menu ["+e.getMessage()+"]", MainActivity.this);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            if(cronRodando) tomaTempo();
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK){
            onBackPressed();
        }
        return true;
    }


    private void iniciarCronometro(){
        cron.setBase(SystemClock.elapsedRealtime());
        cron.start();
        btnIniciar.setText("PARAR");
        btnTempo.setEnabled(true);
        spnTempo.setEnabled(false);
        btnMais.setEnabled(false);
        btnMenos.setEnabled(false);
        btnLimpar.setEnabled(false);
        cronRodando = true;
    }
    private void pararCronometro(){
        cron.stop();
        cron.setBase(SystemClock.elapsedRealtime());
        btnIniciar.setText("INICIAR");
        spnTempo.setEnabled(true);
        btnMais.setEnabled(true);
        btnMenos.setEnabled(true);
        btnLimpar.setEnabled(true);
        btnTempo.setEnabled(false);
        cronRodando = false;
        contAbrirContagem=1;
    }


    private void tomaTempo(){
        try {
            cron.stop();

            // inicio linha
            if ((contCol + numColunas) % numColunas == 0 || numColunas == 2) {
                String seqLine = dfSec.format((contCol + numColunas) / numColunas);
                tempos.add(seqLine);
                tempos.add(cron.getText().toString());
                contCol = contCol + 2;
                if(contCol > numColunas) calculaParcial();
            }
            // fim linha
            else if ((contCol + 2) % numColunas == 0 && numColunas > 2) {
                tempos.add(cron.getText().toString());
                contCol++;
                if(contCol > numColunas) calculaParcial();

                int min = 0, seg = 0, mil = 0;
                for (int i = 1; i < (numColunas - 1); i++) {
                    String[] timeStr = tempos.get(contCol - i).split(":");
                    min += Integer.parseInt(timeStr[0]);
                    seg += Integer.parseInt(timeStr[1]);
                    mil += Integer.parseInt(timeStr[2]);
                }
                int tempoVolta = (min * 60 * 1000) + (seg * 1000) + mil;
                int minutes = (int) (tempoVolta / (60 * 1000));
                tempoVolta = (int) (tempoVolta % (60 * 1000));
                int seconds = (int) (tempoVolta / 1000);
                tempoVolta = (int) (tempoVolta % 1000);
                int milliseconds = (int) (((int) tempoVolta % 1000));
                String strVolta = minutes + ":";
                strVolta += dfSec.format(seconds) + ":";
                strVolta += dfMill.format(milliseconds);
                tempos.add(strVolta);
                contCol++;
            }
            // meio (tempos)
            else {
                tempos.add(cron.getText().toString());
                contCol++;
                if(contCol > numColunas) calculaParcial();
            }

            gridView.setAdapter(new GridAdapter(MainActivity.this, tempos, numColunas, voltaTeorica));
            gridView.smoothScrollToPosition(tempos.size()-1);

        }catch (Exception e){
            Mensagem.TrataErroToast(e, "ERRO tomaTempo()", MainActivity.this);
        }finally {
            cron.setBase(SystemClock.elapsedRealtime());
            cron.start();
        }

    }


    private void calculaParcial(){
        int min = 0, seg = 0, mil = 0;
        String[] timeStr;
        timeStr = tempos.get(tempos.size()-1).split(":");
        min = Integer.parseInt(timeStr[0]);
        seg = Integer.parseInt(timeStr[1]);
        mil = Integer.parseInt(timeStr[2]);
        int tempoAtual = (min * 60 * 1000) + (seg * 1000) + mil;
        int posParcial = voltaTeorica[contParcial];
        timeStr = tempos.get(posParcial).split(":");
        min = Integer.parseInt(timeStr[0]);
        seg = Integer.parseInt(timeStr[1]);
        mil = Integer.parseInt(timeStr[2]);
        int menorParcial = (min * 60 * 1000) + (seg * 1000) + mil;
        if(tempoAtual < menorParcial){
            voltaTeorica[contParcial] = tempos.size()-1;
        }
        if(contParcial < voltaTeorica.length-1)
            contParcial++;
        else
            contParcial = 0;
    }


    private void zerarVoltaTeorica(){
        int tamanhoVetor = voltaTeorica.length;
        if(tamanhoVetor == 1)
            voltaTeorica[0] = 1;
        else{
            for(int i=0; i < tamanhoVetor; i++){
                voltaTeorica[i] = i+1;
            }
        }
        txtVoltaTeorica.setVisibility(View.GONE);
        txtVoltaTeorica.setText("");
        contParcial=0;
    }


    private void calculaVoltaTeorica(){
        if(tempos.size() > numColunas && numColunas > 2) {
            txtVoltaTeorica.setVisibility(View.VISIBLE);
            int min = 0, seg = 0, mil = 0;
            String strVolta;
            for (int i = 0; i < voltaTeorica.length; i++) {
                String timeStr[] = tempos.get(voltaTeorica[i]).split(":");
                min += Integer.parseInt(timeStr[0]);
                seg += Integer.parseInt(timeStr[1]);
                mil += Integer.parseInt(timeStr[2]);
                txtVoltaTeorica.append(tempos.get(voltaTeorica[i]) + " - ");
            }
            int tempoVolta = (min * 60 * 1000) + (seg * 1000) + mil;
            int minutes = (int) (tempoVolta / (60 * 1000));
            tempoVolta = (int) (tempoVolta % (60 * 1000));
            int seconds = (int) (tempoVolta / 1000);
            tempoVolta = (int) (tempoVolta % 1000);
            int milliseconds = (int) (((int) tempoVolta % 1000));
            strVolta = minutes + ":";
            strVolta += dfSec.format(seconds) + ":";
            strVolta += dfMill.format(milliseconds);
            txtVoltaTeorica.append("-> " + strVolta);
        }
    }


    private void limparGrid(){
        tempos.clear();
        contCol=0;
        gridView.setAdapter(new GridAdapter(MainActivity.this, new ArrayList<String>(), numColunas, voltaTeorica));
    }



    public void ligarSensor() {
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

    public void desligarSensor() {
        try {
            if(serialPort != null) serialPort.close();
            //unregisterReceiver(broadcastReceiver);
            //Mensagem.mensagemToast("Serial Connection Closed!", MainActivity.this);
        }catch (Exception e){
            Mensagem.mensagemToast(e.getMessage(), MainActivity.this);
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
                            Mensagem.mensagemToast("Serial Connection Opened!", MainActivity.this);

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            Mensagem.mensagemToast("PORT NOT OPEN", MainActivity.this);
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        Mensagem.mensagemToast("PORT IS NULL", MainActivity.this);
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    Mensagem.mensagemToast("PERM NOT GRANTED", MainActivity.this);
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                ligarSensor();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                desligarSensor();
            }
        }
    };


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                if(data.equalsIgnoreCase("1")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(cronRodando)
                                tomaTempo();
                            else {
                                if(contAbrirContagem == Integer.parseInt(txtAbrirContagem.getText().toString()))
                                    iniciarCronometro();
                                else contAbrirContagem++;
                            }
                        }
                    });
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };
}


