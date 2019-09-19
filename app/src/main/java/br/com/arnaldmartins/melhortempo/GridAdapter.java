package br.com.arnaldmartins.melhortempo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;


public class GridAdapter extends BaseAdapter {
    private Context ctx;
    private LayoutInflater mInflater;
    private List<String> lista;
    private int numColunas;
    int voltaTeorica[];

    public GridAdapter(Context ctx, List<String> lista, int numColunas, int voltaTeorica[]){
        this.ctx = ctx;
        this.lista = lista;
        this.numColunas = numColunas;
        this.voltaTeorica = voltaTeorica;
    }

    @Override
    public int getCount() {
        return lista.size();
    }

    @Override
    public Object getItem(int position) {
        return lista.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        TextView txtView = new TextView(ctx);
        // inicio linha
        if((position+numColunas) % numColunas == 0){
            txtView.setBackgroundColor(ctx.getResources().getColor(R.color.cinza_linha));
        }
        // fim linha
        else if((position+1) % numColunas == 0 && numColunas >2){
            txtView.setTextColor(Color.BLUE);
            txtView.setBackgroundColor(ctx.getResources().getColor(R.color.branco_total));
        }
        // meio linha (tempos)
        else{
            txtView.setBackgroundColor(ctx.getResources().getColor(R.color.verde_grid));
            for(int i=0; i < voltaTeorica.length; i++){
                if(position == voltaTeorica[i]){ //&& position > numColunas){
                    txtView.setTextColor(Color.RED);
                }
            }
        }
        txtView.setGravity(Gravity.CENTER);
        txtView.setTypeface(null, Typeface.BOLD);
        txtView.setTextSize(15);
        txtView.setText(lista.get(position));

        return txtView;
    }

}
