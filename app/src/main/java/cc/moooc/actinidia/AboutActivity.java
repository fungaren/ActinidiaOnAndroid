package cc.moooc.actinidia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * AboutActivity
 */

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.layout_about,null,false);

        String version = "";
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;}
        catch(Exception e) {e.printStackTrace();}
        TextView tver = (TextView)v.findViewById(R.id.textView_app_version);
        tver.setText(version);

        TextView tvg = (TextView)v.findViewById(R.id.textView_github);
        tvg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mooction/actinidiaonandroid")));
            }
        });

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
}
