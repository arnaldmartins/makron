package br.com.arnaldmartins.melhortempo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Arnald on 11/07/13.
 */
public class Mensagem {

    public static void mensagemToast(String pMens, Context context){
        Toast.makeText(context, pMens, Toast.LENGTH_LONG).show();
        Log.i(context.getString(R.string.app_name), pMens);
    }

    public static void mensagemDialog(String pMens, Activity activity){
        AlertDialog.Builder msg = new AlertDialog.Builder(activity);
        msg.setTitle("Mensagem");
        msg.setMessage(pMens);
        msg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        msg.show();
        //AlertDialog dialog = msg.create();
        //dialog.show();
        Log.i(activity.getString(R.string.app_name), pMens);
    }

    public static void TrataErroToast(Exception e, String pMens, Context context){
        mensagemToast(pMens + ": [" + e.getMessage() + "]", context);
        Log.e(context.getString(R.string.app_name), pMens, e);
    }

    public static void TrataErroDialog(Exception e, String pMens, Activity activity){
        AlertDialog.Builder msg = new AlertDialog.Builder(activity);
        msg.setTitle("Mensagem");
        msg.setMessage(pMens + " [" + e.getMessage() + "]");
        msg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        AlertDialog dialog = msg.create();
        dialog.show();
        Log.e(activity.getString(R.string.app_name), pMens, e);
    }


    public static void ativarGPS(final Activity context){
        AlertDialog.Builder ativarGPS = new AlertDialog.Builder(context);
        ativarGPS.setMessage("Seu sistema de GPS está desativado, você precisa ativa-lo")
                .setTitle("GPS Desativado")
                .setCancelable(false)
                .setPositiveButton("Ativar", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        ativarGPS.show();
    }

}
