package cc.moooc.actinidia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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
    private boolean vertical;

    /**
     * Make preparations.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

            File sdCardDir = Environment.getExternalStorageDirectory();

            actinidiaDir = new File(sdCardDir, "ActinidiaGames");
            if(!actinidiaDir.exists()){
                actinidiaDir.mkdir();           // create directories
            }
            gameList = actinidiaDir.list();     // list games
        }

        AlertDialog dlg = new AlertDialog.Builder(this)
        .setTitle("Pick a game")
        .setItems(gameList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /* games menu */
                gameDir = new File(actinidiaDir, gameList[which]);

                try {
                    Reader config = new FileReader(new File(gameDir,"config.ini"));
                    Properties p = new Properties();
                    p.load(config);
                    vertical = p.getProperty("orientation").equals("vertical");
                }
                catch (IOException e){
                    vertical = false;
                }

                Intent i = new Intent(MainActivity.this, GameActivity.class);
                i.putExtra("vertical",vertical);
                i.putExtra("gameDir",gameDir);
                startActivity(i);
            }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                MainActivity.this.finish();
            }
        }).show();


        View v = getLayoutInflater().inflate(R.layout.layout_main,null,true);
        TextView tvh = (TextView)v.findViewById(R.id.textView_hello);
        tvh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "hello", Toast.LENGTH_SHORT).show();
            }
        });
        TextView tvs = (TextView)v.findViewById(R.id.textView_site);
        tvs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://moooc.cc")));
            }
        });
        TextView tvg = (TextView)v.findViewById(R.id.textView_github);
        tvg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mooction/actinidiaonandroid")));
            }
        });
        setContentView(v);
    }
}
