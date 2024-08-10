package com.home.apphomemanager_v5.model.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseEntity {

    private FirebaseDatabase database;

    private DatabaseReference mDatabase;

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
}