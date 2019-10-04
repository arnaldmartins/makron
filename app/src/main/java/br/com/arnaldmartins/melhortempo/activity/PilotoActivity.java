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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.GridView;
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

public class PilotoActivity extends AppCompatActivity {


    private SectionsPagerAdapter mSectionsPagerAdapter;
    private static ViewPager mViewPager;

    public final String ACTION_USB_PERMISSION = "br.com.arnaldmartins.melhortempo.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    static Chronometer cron;
    static Button btnParciais, btnAbrirLAP, btnCalculaTeorica;
    static boolean cronRodando;
    static GridView gridView;
    static TextView txtBestLAP, txtLastLAP, txtLAP, txtDiff_LAP, txtVoltaTeorica;
    static TextView txtBest1, txtParc1, txtBest2, txtParc2, txtBest3, txtParc3;
    static List<String> tempos = new ArrayList<>();
    static int numColunas = 1;
    static int contCol=0, contLAP=0;
    static DecimalFormat dfSec = new DecimalFormat("00");
    static DecimalFormat dfMill = new DecimalFormat("000");
    static int bestLAP, lastLAP;
    // vetor para guardar as posições na lista de cada parcial da volta teórica
    static int[] voltaTeorica;
    static int contParcial=0;
    static int contAbrirLAP=1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piloto);

        // fazer com que o display permaneça aceso para esta activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // container de pages
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        // controle usb serial
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

    }


    @Override
    protected void onStart() {
        super.onStart();
        ligarSensor();
    }

    @Override
    protected void onStop() {
        super.onStop();
        desligarSensor();
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
            Mensagem.mensagemToast(e.getMessage(), PilotoActivity.this);
        }
    }


    private void iniciarCronometro(){
        cron.setBase(SystemClock.elapsedRealtime());
        cron.start();
        cronRodando = true;
        btnParciais.setEnabled(false);
        btnAbrirLAP.setEnabled(false);
    }
    private static void pararCronometro(){
        cron.stop();
        cron.setBase(SystemClock.elapsedRealtime());
        cronRodando = false;
        contAbrirLAP = 1;
        btnParciais.setEnabled(true);
        btnAbrirLAP.setEnabled(true);
    }


    private static void resetarLeitura(int parciais, Context ctx) {
        numColunas = (parciais == 1 ? 2 : parciais+2);
        voltaTeorica = new int[parciais];
        zerarVoltaTeorica();
        gridView.setNumColumns(numColunas);
        limparGrid(ctx);
        contLAP = 0;
        bestLAP = 0;
        lastLAP = 0;
        txtLAP.setText("L "+contLAP);
        txtBestLAP.setText("0:00:000");
        txtLastLAP.setText("0:00:000");
        txtDiff_LAP.setText("");

    }
    private static void zerarVoltaTeorica(){
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
    private static void limparGrid(Context ctx){
        tempos.clear();
        contCol=0;
        gridView.setAdapter(new GridAdapter(ctx, new ArrayList<String>(), numColunas, voltaTeorica));
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
                if(numColunas == 2){
                    String[] timeStr = tempos.get(contCol - 1).split(":");
                    int min = Integer.parseInt(timeStr[0]);
                    int seg = Integer.parseInt(timeStr[1]);
                    int mil = Integer.parseInt(timeStr[2]);
                    int tempoVolta = (min * 60 * 1000) + (seg * 1000) + mil;
                    calculaBestLAP(tempoVolta, tempos.get(contCol - 1));
                }
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
                int tempoAux = tempoVolta;
                int minutes = (int) (tempoAux / (60 * 1000));
                tempoAux = (int) (tempoAux % (60 * 1000));
                int seconds = (int) (tempoAux / 1000);
                tempoAux = (int) (tempoAux % 1000);
                int milliseconds = (int) (((int) tempoAux % 1000));
                String strVolta = minutes + ":";
                strVolta += dfSec.format(seconds) + ":";
                strVolta += dfMill.format(milliseconds);
                tempos.add(strVolta);
                contCol++;
                if(bestLAP == 0 || bestLAP > tempoVolta) {
                    bestLAP = tempoVolta;
                    txtBestLAP.setText(strVolta);
                }
                contLAP += 1;
                if(contLAP > 1){
                    int diff = tempoVolta - lastLAP;
                    tempoAux = Math.abs(diff);
                    minutes = (int) (tempoAux / (60 * 1000));
                    tempoAux = (int) (tempoAux % (60 * 1000));
                    seconds = (int) (tempoAux / 1000);
                    tempoAux = (int) (tempoAux % 1000);
                    milliseconds = (int) (((int) tempoAux % 1000));
                    String strDiff = diff < 0 ? "-" : "+";
                    strDiff += seconds + ":";
                    strDiff += dfMill.format(milliseconds);
                    txtDiff_LAP.setText(strDiff);
                }
                lastLAP = tempoVolta;
                txtLastLAP.setText(strVolta);
                txtLAP.setText("L "+ contLAP);
            }
            // meio (tempos)
            else {
                tempos.add(cron.getText().toString());
                contCol++;
                if(contCol > numColunas) calculaParcial();
            }

            gridView.setAdapter(new GridAdapter(PilotoActivity.this, tempos, numColunas, voltaTeorica));
            //gridView.smoothScrollToPosition(tempos.size()-1);

        }catch (Exception e){
            Mensagem.TrataErroToast(e, "ERRO tomaTempo()", PilotoActivity.this);
        }finally {
            cron.setBase(SystemClock.elapsedRealtime());
            cron.start();
        }

    }

    private void calculaParcial() {
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

    private void calculaBestLAP(int tempoVolta, String strTempoVolta){
        if(bestLAP == 0 || bestLAP > tempoVolta) {
            bestLAP = tempoVolta;
            txtBestLAP.setText(strTempoVolta);
        }
        contLAP += 1;
        int tempoAux, minutes, seconds, milliseconds;
        if(contLAP > 1){
            int diff = tempoVolta - lastLAP;
            tempoAux = Math.abs(diff);
            minutes = (int) (tempoAux / (60 * 1000));
            tempoAux = (int) (tempoAux % (60 * 1000));
            seconds = (int) (tempoAux / 1000);
            tempoAux = (int) (tempoAux % 1000);
            milliseconds = (int) (((int) tempoAux % 1000));
            String strDiff = diff < 0 ? "+" : "-";
            strDiff += seconds + ":";
            strDiff += dfMill.format(milliseconds);
            txtDiff_LAP.setText(strDiff);
        }
        lastLAP = tempoVolta;
        txtLastLAP.setText(strTempoVolta);
        txtLAP.setText("L "+ contLAP);
    }


    private static void calculaVoltaTeorica(){
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




    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = null;
            int pagina = getArguments().getInt(ARG_SECTION_NUMBER);
            if (pagina == 1){
                rootView = inflater.inflate(R.layout.fragment_result, container, false);
                gridView = rootView.findViewById(R.id.gridViewResult);
                txtVoltaTeorica = rootView.findViewById(R.id.txtTeoricaResult);
                btnCalculaTeorica = rootView.findViewById(R.id.btnCalculaTeorica);
                btnCalculaTeorica.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pararCronometro();
                        calculaVoltaTeorica();
                    }
                });
            }
            else //{
                if(pagina == 2) {
                    rootView = inflater.inflate(R.layout.fragment_piloto, container, false);
                /*
                } else {
                    rootView = inflater.inflate(R.layout.fragment_piloto2, container, false);
                    txtBest1 = rootView.findViewById(R.id.txtBest1);
                    txtParc1 = rootView.findViewById(R.id.txtParc1);
                    txtBest2 = rootView.findViewById(R.id.txtBest2);
                    txtParc2 = rootView.findViewById(R.id.txtParc2);
                    txtBest3 = rootView.findViewById(R.id.txtBest3);
                    txtParc3 = rootView.findViewById(R.id.txtParc3);
                    mostraOcultaParciais(3);

                }*/
                cron = rootView.findViewById(R.id.cronPiloto1);
                txtBestLAP = rootView.findViewById(R.id.txtBestLAP);
                txtLastLAP = rootView.findViewById(R.id.txtLastLAP);
                txtLAP = rootView.findViewById(R.id.txtLAP);
                txtDiff_LAP = rootView.findViewById(R.id.txtDiff_LAP);
                txtDiff_LAP.setText("");
                btnParciais = rootView.findViewById(R.id.btnParciais);
                btnAbrirLAP = rootView.findViewById(R.id.btnAbrirLAP);
                btnParciais.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            int limite = 3;
                            int cont = Integer.parseInt(btnParciais.getText().toString());
                            int parciais = cont + 1 <= limite ? cont + 1 : 1;
                            btnParciais.setText(parciais + "");
                            btnAbrirLAP.setText("1");

                            mostraOcultaParciais(parciais);

                            resetarLeitura(parciais, container.getContext());

                        } catch (Exception e) {
                        }
                    }
                });
                btnAbrirLAP.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            int limite = Integer.parseInt(btnParciais.getText().toString());
                            int cont = Integer.parseInt(btnAbrirLAP.getText().toString());
                            int novoCont = cont + 1 <= limite ? cont + 1 : 1;
                            btnAbrirLAP.setText(novoCont + "");
                        } catch (Exception e) {
                        }
                    }
                });

                ///*
                mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }
                    @Override
                    public void onPageSelected(int position) {
                        if(position == 0){
                            try {
                                gridView.smoothScrollToPosition(tempos.size()-1);
                            }catch (Exception e){}
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }
                });
                //*/

                // setar page do piloto como foco inicial
                mViewPager.setCurrentItem(1);


            }

            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show total pages.
            return 2;
        }
    }

    private static void mostraOcultaParciais(int parciais){
        if(parciais == 1){
            //txtBest1.setVisibility(View.VISIBLE);txtParc1.setVisibility(View.VISIBLE);
            txtBest2.setVisibility(View.GONE);txtParc2.setVisibility(View.GONE);
            txtBest3.setVisibility(View.GONE);txtParc3.setVisibility(View.GONE);
        }else if(parciais == 2){
            txtBest2.setVisibility(View.VISIBLE);txtParc2.setVisibility(View.VISIBLE);
            txtBest3.setVisibility(View.GONE);txtParc3.setVisibility(View.GONE);
        }else{
            txtBest2.setVisibility(View.VISIBLE);txtParc2.setVisibility(View.VISIBLE);
            txtBest3.setVisibility(View.VISIBLE);txtParc3.setVisibility(View.VISIBLE);
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
                        if (serialPort.open()) {
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            Mensagem.mensagemToast("PORT NOT OPEN", PilotoActivity.this);
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        Mensagem.mensagemToast("PORT IS NULL", PilotoActivity.this);
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    Mensagem.mensagemToast("PERM NOT GRANTED", PilotoActivity.this);
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
                            if(cronRodando) {
                                tomaTempo();
                            }else {
                                if(contAbrirLAP == Integer.parseInt(btnAbrirLAP.getText().toString())){
                                    resetarLeitura(Integer.parseInt(btnParciais.getText().toString()), PilotoActivity.this);
                                    iniciarCronometro();
                                }
                                else contAbrirLAP++;
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
