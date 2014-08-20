package com.lemuelinchrist.android.hymns.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.lemuelinchrist.android.hymns.entities.Hymn;
import com.lemuelinchrist.android.hymns.entities.Stanza;

import java.util.ArrayList;

/**
 * Created by lemuelcantos on 24/7/13.
 */
public class HymnsDao {

    private final Context context;

    public String getHymnNoFromCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex("_id"));
    }

    public Cursor getFilteredHymns(String selectedHymnGroup, String filter) {
        return getIndexList(selectedHymnGroup, filter);
    }

    public static enum HymnFields {
        _id, hymn_group, first_stanza_line, first_chorus_line, main_category, sub_category, meter, author,
        composer, time, key, tune, no, parent_hymn, sheet_music_link, verse, related

    }

    public static enum StanzaFields {
        parent_hymn, no, text, note, ID
    }


    private SQLiteDatabase database;
    private HymnsSqliteHelper dbHelper;

    public HymnsDao(Context context) {
        this.context = context
        ;
        dbHelper = HymnsSqliteHelper.getInstance(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void save(Hymn hymn) {
        Log.i(HymnsDao.class.getSimpleName(), "Saving hymn to database: " + hymn.getHymnId());
        ContentValues values = new ContentValues();
        values.put(HymnFields._id.toString(), hymn.getHymnId());
        values.put(HymnFields.hymn_group.toString(), hymn.getGroup());
        values.put(HymnFields.first_stanza_line.toString(), hymn.getFirstStanzaLine());
        values.put(HymnFields.first_chorus_line.toString(), hymn.getFirstChorusLine());
        values.put(HymnFields.main_category.toString(), hymn.getMainCategory());
        values.put(HymnFields.sub_category.toString(), hymn.getSubCategory());
        values.put(HymnFields.meter.toString(), hymn.getMeter());
        values.put(HymnFields.author.toString(), hymn.getAuthor());
        values.put(HymnFields.composer.toString(), hymn.getComposer());
        values.put(HymnFields.time.toString(), hymn.getTime());
        values.put(HymnFields.key.toString(), hymn.getKey());
        values.put(HymnFields.tune.toString(), hymn.getTune());
        values.put(HymnFields.no.toString(), hymn.getNo());
        database.insert("hymns", null, values);

        for (Stanza stanza : hymn.getStanzas()) {
            values = new ContentValues();
            values.put(StanzaFields.parent_hymn.toString(), hymn.getHymnId());
            values.put(StanzaFields.no.toString(), stanza.getNo());
            values.put(StanzaFields.text.toString(), stanza.getText());
            values.put(StanzaFields.note.toString(), stanza.getNote());
            database.insert("stanza", null, values);
        }


        Log.d(HymnsDao.class.getSimpleName(), "Save Complete!");

    }

    public Cursor getAllHymnsOfSameLanguage(String hymnGroup) {

        return getIndexList(hymnGroup, null);

    }

    private Cursor getIndexList(String hymnGroup, String filter) {
        if (filter != null)
            filter = filter.replace("'", "''");

        String groupClause = "";
        String likeClause = "";
        if (filter != null && !filter.equals("")) {
            likeClause = " WHERE stanza_chorus LIKE " + "'%" + filter.trim() + "%' ";
        } else {
            groupClause = " and (hymn_group='" + hymnGroup + "') ";
        }


        return database.rawQuery("select * from(" +
                "select first_stanza_line as stanza_chorus, no, _id, hymn_group from hymns where stanza_chorus NOT NULL " + groupClause + " \n" +
                "union\n" +
                "select first_chorus_line as stanza_chorus, no, _id, hymn_group from hymns where stanza_chorus NOT NULL " + groupClause +
                ") " + likeClause + "order by lower(trim(trim(stanza_chorus,'\"'),\"'\")) COLLATE LOCALIZED ASC ", null);
    }

    public Hymn get(String hymnId) {
        if (hymnId == null) return null;
        Cursor cursor = null;
        try {
            Log.i(HymnsDao.class.getName(), "Querying Hymn:" + hymnId);

            cursor = database.rawQuery("select * from hymns where _id='" + hymnId + "'", null);

            cursor.moveToNext();
            Hymn hymn = getHymn(cursor);

            cursor.close();

            // GET stanzas
            cursor = database.rawQuery("select * from stanza where parent_hymn='" + hymnId + "' order by n_order", null);
            Log.d(HymnsDao.class.getSimpleName(), "Stanza Retrieved from DB. No of stanzas: " + cursor.getCount());
            ArrayList<Stanza> stanzas = new ArrayList<Stanza>();
            while (cursor.moveToNext()) {

                Stanza stanza = new Stanza();
                stanza.setNo(cursor.getString(cursor.getColumnIndex(StanzaFields.no.toString())));
                stanza.setText(cursor.getString(cursor.getColumnIndex(StanzaFields.text.toString())));
                stanza.setNote(cursor.getString(cursor.getColumnIndex(StanzaFields.note.toString())));
                stanza.setParentHymn(cursor.getString(cursor.getColumnIndex(StanzaFields.parent_hymn.toString())));


                Log.i(HymnsDao.class.getSimpleName(), "Done building Stanza: \n" + stanza.toString());
                stanzas.add(stanza);

            }
            ;

            hymn.setStanzas(stanzas);
            cursor.close();

            hymn.setNewTune(hymn.getGroup().equals("BF") && hymn.getTune() != null);
            // *** Get Parent Hymn then Merge!
            Hymn parentHymn = get(hymn.getParentHymn());
            if (parentHymn != null) {
                if (isEmpty(hymn.getAuthor()))
                    hymn.setAuthor(parentHymn.getAuthor());
                if (isEmpty(hymn.getComposer()))
                    hymn.setComposer(parentHymn.getComposer());
                if (isEmpty(hymn.getTime()))
                    hymn.setTime(parentHymn.getTime());
                if (isEmpty(hymn.getTune()))
                    hymn.setTune(parentHymn.getTune());
                if (isEmpty(hymn.getSheetMusicLink()))
                    hymn.setSheetMusicLink(parentHymn.getSheetMusicLink());
                if (isEmpty(hymn.getKey()))
                    hymn.setKey(parentHymn.getKey());
                if (isEmpty(hymn.getMainCategory()))
                    hymn.setMainCategory(parentHymn.getMainCategory());
                if (isEmpty(hymn.getSubCategory()))
                    hymn.setSubCategory(parentHymn.getSubCategory());
                if (isEmpty(hymn.getMeter()))
                    hymn.setMeter(parentHymn.getMeter());
                if (isEmpty(hymn.getVerse()))
                    hymn.setVerse(parentHymn.getVerse());
                if (hymn.getStanzas() == null || hymn.getStanzas().isEmpty())
                    hymn.setStanzas(parentHymn.getStanzas());

                hymn.addRelated(parentHymn.getRelated());
            }


            return hymn;
        } catch (Exception e) {
            Log.e(HymnsDao.class.getName(), "Error in dao.get()" + e.toString());
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private boolean isEmpty(String field) {
        return (field == null || field.equals(""));
    }

    private Hymn getHymn(Cursor cursor) {
        Hymn hymn = new Hymn(context);
        hymn.setHymnId(cursor.getString(cursor.getColumnIndex(HymnFields._id.toString())));
        hymn.setGroup(cursor.getString(cursor.getColumnIndex(HymnFields.hymn_group.toString())));
        hymn.setFirstStanzaLine(cursor.getString(cursor.getColumnIndex(HymnFields.first_stanza_line.toString())));
        hymn.setFirstChorusLine(cursor.getString(cursor.getColumnIndex(HymnFields.first_chorus_line.toString())));
        hymn.setMainCategory(cursor.getString(cursor.getColumnIndex(HymnFields.main_category.toString())));
        hymn.setSubCategory(cursor.getString(cursor.getColumnIndex(HymnFields.sub_category.toString())));
        hymn.setMeter(cursor.getString(cursor.getColumnIndex(HymnFields.meter.toString())));
        hymn.setAuthor(cursor.getString(cursor.getColumnIndex(HymnFields.author.toString())));
        hymn.setComposer(cursor.getString(cursor.getColumnIndex(HymnFields.composer.toString())));
        hymn.setTime(cursor.getString(cursor.getColumnIndex(HymnFields.time.toString())));
        hymn.setKey(cursor.getString(cursor.getColumnIndex(HymnFields.key.toString())));
        hymn.setTune(cursor.getString(cursor.getColumnIndex(HymnFields.tune.toString())));
        hymn.setNo(cursor.getString(cursor.getColumnIndex(HymnFields.no.toString())));
        hymn.setParentHymn(cursor.getString(cursor.getColumnIndex(HymnFields.parent_hymn.toString())));
        hymn.setSheetMusicLink(cursor.getString(cursor.getColumnIndex(HymnFields.sheet_music_link.toString())));
        hymn.setVerse(cursor.getString(cursor.getColumnIndex(HymnFields.verse.toString())));
        hymn.addRelated(cursor.getString(cursor.getColumnIndex(HymnFields.related.toString())));

        if (hymn.getTune() != null) hymn.setTune(hymn.getTune().trim());
        return hymn;
    }


}


