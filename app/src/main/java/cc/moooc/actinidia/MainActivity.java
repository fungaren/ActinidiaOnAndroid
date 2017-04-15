package cc.moooc.actinidia;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private File actinidiaDir;  // sdcard/ActinidiaGames
    private String[] gameList;  // game folders

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = getLayoutInflater().inflate(R.layout.layout_main,null,true);
        setContentView(v);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_local:
                // Permission's check
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                        MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // available
                    listLocalGames();
                } else {
                    // Request for permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                }
                break;
            case R.id.menu_about:
                Intent i = new Intent(this,AboutActivity.class);
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void listLocalGames(){
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
            // Dismiss the dialog before goto another activity
            dialog.dismiss();
            // Directory of the selected game
            launchGame(new File(actinidiaDir, gameList[which]));
        }
    };

    /**
     * Play in GameActivity
     * @param gameDir eg. ActinidiaGames/res-rpg
     */
    private void launchGame(File gameDir){
        boolean vertical;
        try {
            Reader config = new FileReader(new File(gameDir,"config.ini"));
            Properties p = new Properties();
            p.load(config);
            vertical = p.getProperty("orientation","horizontal").equals("vertical");
            config.close();
        }
        catch (IOException e) {
            vertical = false;
        }

        // Game start
        Intent i = new Intent(MainActivity.this, GameActivity.class);
        i.putExtra("vertical",vertical);
        i.putExtra("gameDir",gameDir);
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // obtained permission
                    listLocalGames();
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
