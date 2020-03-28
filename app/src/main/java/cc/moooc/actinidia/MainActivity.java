package cc.moooc.actinidia;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Properties;

public class MainActivity extends Activity
{
    private static final int PERMISSION_REQUEST_CODE = 1;
    private String[] gameList;  // resource files under the sdcard directory

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = getLayoutInflater().inflate(R.layout.layout_main,null,true);
        setContentView(v);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_local:
                // Permission's check
                if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // available
                    listLocalGames();
                } else {
                    // Request for permission
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                }
                break;
            case R.id.menu_about:
                Intent i = new Intent(this,AboutActivity.class);
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void listLocalGames() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            File sdCardDir = Environment.getExternalStorageDirectory();
            File[] files = sdCardDir.listFiles();     // list games
            LinkedList<String> list = new LinkedList<>();

            for (File game_res : files)
            {
                if (game_res.isFile() && game_res.getName().endsWith(".res"))
                    list.push(game_res.getName());
            }
            gameList = list.toArray(new String[list.size()]);
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
            // the selected game resource file
            File gameRes = new File(Environment.getExternalStorageDirectory(), gameList[which]);
            try {
                // Install (decompress) the compactFile to `game_dir/{id}.res`
                FileUtil.inflate(getCacheDir(), gameRes);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            launchGame(new File(getCacheDir(), gameRes.getName()));
        }
    };

    /**
     * Play in GameActivity
     */
    public void launchGame(File gameRes) {
        boolean vertical;
        int preferred_width;
        int preferred_height;
        if (!gameRes.isFile() || !gameRes.getName().endsWith(".res")) {
            Log.e("ACTINIDIA", "Cannot locate to resource file: "+ gameRes.getAbsolutePath());
            return;
        }
        Log.e("ACTINIDIA", "Found " + gameRes.getAbsolutePath());
        ResourcePack pack;
        try {
            pack = new ResourcePack(gameRes);
        } catch (IOException e) {
            Toast.makeText(this, R.string.failed_to_load_res, Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }
        try {
            byte[] data = pack.readResource("config.ini");
            InputStream in = new ByteArrayInputStream(data);
            Properties p = new Properties();
            p.load(in);
            vertical = p.getProperty("orientation","horizontal").equals("vertical");
            preferred_width = Integer.parseInt(p.getProperty("preferred_width","0"));
            preferred_height = Integer.parseInt(p.getProperty("preferred_height","0"));
            in.close();
        }
        catch (IOException e) {
            vertical = false;
            preferred_width = 0;
            preferred_height = 0;
        }

        // Game start
        Intent i = new Intent(MainActivity.this, GameActivity.class);
        i.putExtra("vertical", vertical);
        i.putExtra("preferred_width", preferred_width);
        i.putExtra("preferred_height", preferred_height);
        i.putExtra("gameDir", gameRes.getParentFile());
        i.putExtra("gameRes", gameRes);
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
