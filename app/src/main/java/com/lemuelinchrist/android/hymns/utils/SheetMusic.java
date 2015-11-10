package com.lemuelinchrist.android.hymns.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.lemuelinchrist.android.hymns.LyricContainer;
import com.lemuelinchrist.android.hymns.entities.Hymn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by lemuelcantos on 6/12/14.
 */
public class SheetMusic {
    private Context context;

    // to switch to guitar or piano notes, change value to either guitarSvg/ or pianoSvg/
    // then copy corresponding folder from HymnsJpa/data/<folderName> to HymnsForAndroid/app/src/main/assets<folderName>
    private String folderName = null;

    public SheetMusic(Context context) {
        this.context = context;

    }

    public void getSheetMusic(Hymn hymn) {
        // note: switch this value to "onlineOnly" if you want to create a version that doesn't include sheet music svg's.
        final String BRANCH = "somethingElse";

        String sheetMusicLink = hymn.getSheetMusicLink();
        if (sheetMusicLink != null) {

            try {
                // get folder that contains the svg files
                // the folder name will either be "pianoSvg" or "guitarSvg" depending on what is currently
                // in the asset folder
                for (String assetFolder : context.getAssets().list("")) {
                    if(assetFolder.contains("svg")){
                        this.folderName=assetFolder+"/";
                        Log.i(this.getClass().getName(),"Svg folder found. folderName is: " + this.folderName);
                    }

                } if(this.folderName==null) {
                    getSheetMusicOnline(sheetMusicLink);
                }
                String fileName;

                // after getting folder name, its time to get the svg filename. we can get this either from
                // the hymn itself or from its parent.
                if (!hymn.hasOwnSheetMusic()) {
                    fileName = hymn.getParentHymn() + ".svg";
                } else {
                    fileName = hymn.getHymnId() + ".svg";
                }

                generateGuitarSheet(fileName);


            } catch (IOException e) {
                e.printStackTrace();
                getSheetMusicOnline(sheetMusicLink);
            }



        } else {
            Toast.makeText(context, "Sorry! Sheet music not available", Toast.LENGTH_SHORT).show();

        }
    }

    private void getSheetMusicOnline(String sheetMusicLink) {
        Log.w(this.getClass().getName(), "svg files not found! resorting to online resource");
        Intent i = new Intent(Intent.ACTION_VIEW);
        String editedLink = sheetMusicLink.replace("_p.", "_g."); // p is for piano, g for guitar
        i.setData(Uri.parse(editedLink));
        context.startActivity(i);
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    void generateGuitarSheet(String fileName) {
        try {

            AssetManager assetManager = context.getAssets();
            File file;
            InputStream in;
            OutputStream out;
            final File externalStorageSvgDir = new File(Environment.getExternalStorageDirectory(), "musicSheet");


            deleteDirectory(externalStorageSvgDir);
            if (!externalStorageSvgDir.mkdirs())
                Log.w(LyricContainer.class.getSimpleName(), "directory already exists. no need to create one.");

            file = new File(externalStorageSvgDir, fileName);
            in = assetManager.open(folderName + fileName);
            out = new FileOutputStream(file);


            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(
                    Uri.fromFile(file)
            );


            intent.setClassName("org.mozilla.firefox", "org.mozilla.firefox.App");
            //test if chrome exists
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;
            if (!isIntentSafe) {
                Log.i(this.getClass().getName(), "firefox not found, finding chrome");
                intent.setClassName("com.android.chrome", "com.google.android.apps.chrome.Main");
            }


            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Oops! Chrome or Firefox not available! To display music sheet, please install Chrome or Firefox.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Sorry! Sheet music not available", Toast.LENGTH_SHORT).show();
            Log.e(SheetMusic.class.getSimpleName(), e.getMessage());
        }

    }
}
