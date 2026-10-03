package com.home.apphomemanager_v5.model.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseEntity {

    private FirebaseDatabase database;

    private DatabaseReference mDatabase;

    private ValueEventListener valueEventListener;

    public void FirebaseInicialize(String path){
        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference().child(path);
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }

    public void setDatabase(FirebaseDatabase database) {
        this.database = database;
    }

    public DatabaseReference getmDatabase() {
        return mDatabase;
    }

    public void setmDatabase(DatabaseReference mDatabase) {
        this.mDatabase = mDatabase;
    }

    /** Registra o listener guardando a referência, para que possa ser removido em {@link #disconnect()}. */
    public void addValueEventListener(ValueEventListener listener){
        removeValueEventListener();
        valueEventListener = listener;
        mDatabase.addValueEventListener(listener);
    }

    public void removeValueEventListener(){
        if (mDatabase != null && valueEventListener != null) {
            mDatabase.removeEventListener(valueEventListener);
            valueEventListener = null;
        }
    }

    public void disconnect(){
        removeValueEventListener();
    }
}
