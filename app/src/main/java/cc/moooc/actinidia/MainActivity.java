package cc.moooc.actinidia;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private File actinidiaDir;  // sdcard/ActinidiaGames
    private String[] gameList;  // game folders
    private File gameDir;       // eg. ActinidiaGames/res-rpg

    private static final int PERMISSION_REQUEST_CODE = 1;

    /**
     * Preparation
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Permission's check
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // available
            listGames();
        } else {
            // Request for permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }

        // Update UI
        View v = getLayoutInflater().inflate(R.layout.layout_main,null,true);
        Button tvh = (Button)v.findViewById(R.id.button_choose);
        tvh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listGames();
            }
        });

        String version = "";
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;}
        catch(Exception e) {e.printStackTrace();}
        TextView tver = (TextView)v.findViewById(R.id.textView_version);
        tver.setText(version);

        TextView tvg = (TextView)v.findViewById(R.id.textView_github);
        tvg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mooction/actinidiaonandroid")));
            }
        });
        setContentView(v);
    }

    private void listGames(){
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

            File sdCardDir = Environment.getExternalStorageDirectory();
            actinidiaDir = new File(sdCardDir, "ActinidiaGames");
            if(!actinidiaDir.exists()){
                actinidiaDir.mkdir();           // create directories
            }
            gameList = actinidiaDir.list();     // list games

            new AlertDialog.Builder(this)       // show dialog
                    .setTitle(getString(R.string.choose_game))
                    .setItems(gameList, dlg_listener).show();
        }
    }

    private DialogInterface.OnClickListener dlg_listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Directory of the selected game
            gameDir = new File(actinidiaDir, gameList[which]);
            boolean vertical;
            try {
                Reader config = new FileReader(new File(gameDir,"config.ini"));
                Properties p = new Properties();
                p.load(config);
                vertical = p.getProperty("orientation").equals("vertical");
                config.close();
            }
            catch (IOException e) {
                vertical = false;
            }

            dialog.dismiss();       // Must dismiss the dialog before goto another activity

            // Game start
            Intent i = new Intent(MainActivity.this, GameActivity.class);
            i.putExtra("vertical",vertical);
            i.putExtra("gameDir",gameDir);
            startActivity(i);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // obtained permission
                    listGames();
                } else {
                    // refused
                    Toast.makeText(this, R.string.warn, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
}
