package com.pujhones.bicita.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.pujhones.bicita.R;
import com.pujhones.bicita.model.BiciUsuario;
import com.pujhones.bicita.model.ElementoListaAmigo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import de.hdodenhof.circleimageview.CircleImageView;

public class AgregarAmigoSolicitudesFragment extends Fragment {

    static class ElementoListaViewHolder {
        TextView nombre;
        CircleImageView imagenPerfil;
    }

    private class CustomArrayAdapter extends ArrayAdapter<ElementoListaAmigo> {
        private ArrayList<ElementoListaAmigo> list;

        //this custom adapter receives an ArrayList of RowData objects.
        //RowData is my class that represents the data for a single row and could be anything.
        public CustomArrayAdapter(Context context, int textViewResourceId,
                                  ArrayList<ElementoListaAmigo> rowDataList) {
            //populate the local list with data.
            super(context, textViewResourceId, rowDataList);
            this.list = new ArrayList<ElementoListaAmigo>();
            this.list.addAll(rowDataList);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            //creating the ViewHolder we defined earlier.
            final AmigosActivity.ElementoListaViewHolder holder = new AmigosActivity
                    .ElementoListaViewHolder();

            //creating LayoutInflator for inflating the row layout.
            LayoutInflater inflator = (LayoutInflater) this.getContext().getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);

            //inflating the row layout we defined earlier.
            convertView = inflator.inflate(R.layout.elemento_lista_amigo, null);

            //setting the views into the ViewHolder.
            holder.nombre = (TextView) convertView.findViewById(R.id.txtNombre);
            holder.imagenPerfil = (CircleImageView) convertView.findViewById(R.id.imgPerfil);

            holder.nombre.setText(list.get(position).getNombre());
            final Context context = holder.imagenPerfil.getContext();

            AsyncTask<String, Void, Drawable> task = new AsyncTask<String, Void, Drawable>() {

                protected Exception exception = null;

                protected Drawable doInBackground(String... urls) {
                    Drawable d;
                    InputStream is = null;
                    Log.e(TAG, "Descargando imagen.");
                    try {
                        is = (InputStream) new URL(list.get(position).getPhotoURL()).getContent();
                        d = Drawable.createFromStream(is, null);
                        return d;
                    } catch (IOException e) {
                        exception = e;
                        Log.e(TAG, e.getMessage());
                    }
                    return null;
                }
                protected void onPostExecute(Drawable d) {
                    if (exception == null) {
                        holder.imagenPerfil.setImageDrawable(d);
                    } else {
                        int idImagen = context.getResources().getIdentifier("horus",
                                "drawable", context.getPackageName());
                        holder.imagenPerfil.setImageResource(idImagen);
                    }
                }
            };

            task.execute();
            //return the row view.
            return convertView;
        }
    }

    protected final String TAG = "SOLICITUDES";

    protected final String URL_USUARIOS = "biciusuarios/";

    FirebaseDatabase fireDB;

    ArrayList<ElementoListaAmigo> solicitudes = new ArrayList<>();
    ArrayList<BiciUsuario> solicitudesUsuarios = new ArrayList<>();

    ListView lstAmigos;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agregar_amigo_solicitudes, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        fireDB = FirebaseDatabase.getInstance();

        Log.i(TAG, FirebaseAuth.getInstance().getCurrentUser().getUid());
        Log.i(TAG, FirebaseAuth.getInstance().getCurrentUser().getEmail());

        View v = getView();

        lstAmigos = (ListView) v.findViewById(R.id.lstAmigos);

        lstAmigos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent in = new Intent(view.getContext(), VerAmigoActivity.class);
                in.putExtra("usuario", solicitudesUsuarios.get(i));
                in.putExtra("tipo", "solicitud");
                startActivity(in);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "Cargando solicitudes.");
        cargarAmigosDesdeDB();
    }

    public void cargarAmigosDesdeDB() {
        Query queryAmigos2 = fireDB.getReference("amistades/").orderByChild("amigo2").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

        queryAmigos2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                queryBiciUsuariosSnapshot(dataSnapshot, "amigo1");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        });
    }

    protected void queryBiciUsuariosSnapshot(DataSnapshot dataSnapshot, String amigoFieldName) {
        Iterator<DataSnapshot> iter = dataSnapshot.getChildren().iterator();
        solicitudes = new ArrayList<>();
        while (iter.hasNext()) {
            DataSnapshot ds = iter.next();
            if (ds.child("estado").getValue(String.class).equals("solicitado")) {
                String uid = ds.child(amigoFieldName).getValue(String.class);
                Query queryAmigo = fireDB.getReference("biciusuarios/" + uid);
                Log.i(TAG, uid);
                queryAmigo.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot2) {
                        BiciUsuario u = dataSnapshot2.getValue(BiciUsuario.class);
                        u.setUid(dataSnapshot2.getKey());
                        solicitudesUsuarios.add(u);
                        solicitudes.add(new ElementoListaAmigo(u.getNombre(), u.getPhotoURL()));
                        Log.i(TAG, solicitudes.toString());
                        cargarAmigosAVista();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Getting Post failed, log a message
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                        // ...
                    }
                });
            }

        }
    }

    protected void cargarAmigosAVista() {
        AgregarAmigoSolicitudesFragment.CustomArrayAdapter dataAdapter = new
                AgregarAmigoSolicitudesFragment.CustomArrayAdapter(getContext(), R.id.txtNombre,
                solicitudes);
        lstAmigos.setAdapter(dataAdapter);
    }
}
