package cc.moooc.actinidia;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * AboutActivity
 */
public class AboutActivity extends Activity
{
    private String version = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.layout_about,null,false);
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch(Exception e) {
            e.printStackTrace();
        }
        // current version
        TextView tver = (TextView)v.findViewById(R.id.textView_app_version);
        tver.setText(version);

        // github page
        TextView tvg = (TextView)v.findViewById(R.id.textView_github);
        tvg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mooction/actinidiaonandroid")));
            }
        });

        // visit forum
        Button btn_bbs = (Button)v.findViewById(R.id.button_bbs);
        btn_bbs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bbs.moooc.cc")));
            }
        });

        // check update
        Button btn_update = (Button)v.findViewById(R.id.button_update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncCheckUpdates async = new AsyncCheckUpdates();
                async.execute();
            }
        });

        setContentView(v);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;    // suppress default behaviour
        }
        return super.onOptionsItemSelected(item);
    }

    private class AsyncCheckUpdates extends AsyncTask<Void,Void,Void> {
        private String new_version;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!version.equals(new_version) && !new_version.isEmpty()) {
                new AlertDialog.Builder(AboutActivity.this)
                        .setTitle(R.string.confirm_update)
                        .setMessage(getString(R.string.update_found, new_version))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://demo.moooc.cc/actinidia.apk")));
                            }
                        })
                        .show();
            } else {
                Toast.makeText(AboutActivity.this, R.string.no_update, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params) {
            new_version = HttpUtil.getHttpContent("https://demo.moooc.cc/update.php", "UTF-8");
            return null;
        }
    }
}
