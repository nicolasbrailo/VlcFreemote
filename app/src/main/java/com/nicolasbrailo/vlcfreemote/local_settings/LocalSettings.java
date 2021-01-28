package com.nicolasbrailo.vlcfreemote.local_settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

abstract class LocalSettings extends SQLiteOpenHelper implements BaseColumns {

    LocalSettings(Context context, final String DbName, int DbVersion) {
        super(context, DbName, null, DbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getCreateTableSQL());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // This DB doesn't save anything important, just delete everything
        db.execSQL(getDeleteTableSQL());
        db.execSQL(getCreateTableSQL());
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int i, int i1) {
        onUpgrade(db, i, i1);
    }

    void insert(final String table, final ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            db.insertOrThrow(table, null, values);
        } finally {
            db.close();
        }
    }

    <Args> void run(final String query, Args[] args) {
        SQLiteDatabase db = this.getWritableDatabase();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            db.execSQL(query, args);
        } finally {
            db.close();
        }
    }

    interface QueryReadCallback {
        void onCursorReady(final Cursor res);
    }

    void readQuery(final String query, final String[] args, final String[] columns,
                   final QueryReadCallback cb) {
        final SQLiteDatabase db = getReadableDatabase();
        final Cursor res = db.rawQuery(query, args);

        for (String col : columns) {
            if (res.getColumnIndex(col) == -1) {
                return;
            }
        }

        try {
            cb.onCursorReady(res);
        } finally {
            res.close();
            db.close();
        }
    }

    abstract protected String getDeleteTableSQL();
    abstract protected String getCreateTableSQL();
}
