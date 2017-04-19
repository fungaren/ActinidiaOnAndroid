package cc.moooc.actinidia;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Game list fragment
 */
public class GameListFragment extends ListFragment {
    private List<Game> games = new ArrayList<>();
    private List<Integer> assets = new ArrayList<>();
    private GameArrayAdapter adapter;

    private static final String ASSET_FILE = "asset.txt";
    private static final String CACHE_FILE = "cache.txt";
    private static final String BANNER_IMG = "/banner.jpg";
    private static final String GAME_LIST_URL = "http://moooc.cc/games.php";
    private static final String GAME_PATH_URL = "http://moooc.cc/games/";
    private static final String GAME_ZIP = "/game.zip";

    private static final String FREE_GAME = "no";

    private File getCacheImage(int game_id){
        return new File(getActivity().getCacheDir(), game_id + ".png");
    }

    private File getGameDir(int game_id){
        return new File(getActivity().getFilesDir(), game_id + "");
    }

    private File getLogFile(int game_id){
        return new File(getActivity().getFilesDir(), game_id + "/log.ini");
    }

    private File getZipFile(int game_id){
        return new File(getActivity().getCacheDir(), game_id +".zip");
    }

    private class GameArrayAdapter extends BaseAdapter {
        private List<Game> games;

        public GameArrayAdapter(List<Game> games) {
            super();
            this.games = games;
        }

        @Override
        public int getCount() {
            return games.size();
        }

        @Override
        public Object getItem(int position) {
            return games.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_list_single, parent, false);
            }
            ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRect(0, 0, view.getWidth(), view.getHeight());
                }
            };
            LinearLayout layout = (LinearLayout)convertView.findViewById(R.id.linearLayout_game);
            layout.setOutlineProvider(viewOutlineProvider);

            Game game = games.get(position);

            ImageView iv = (ImageView)convertView.findViewById(R.id.imageView_banner);
            InputStream in = null;
            try {
                // load image cache
                in = new FileInputStream(getCacheImage(game.getId()));
                iv.setImageBitmap(BitmapFactory.decodeStream(in));
            } catch (FileNotFoundException e) {
                // download image
                ImageAsyncLoad async = new ImageAsyncLoad(GAME_PATH_URL+game.getId()+BANNER_IMG,iv,game.getId());
                async.execute();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            RatingBar rate = (RatingBar)convertView.findViewById(R.id.ratingBar_star);
            rate.setRating(game.getStar());

            TextView tv_name = (TextView)convertView.findViewById(R.id.textView_name);
            tv_name.setText(game.getName());

            TextView tv_version = (TextView)convertView.findViewById(R.id.textView_game_version);
            tv_version.setText(game.getVersion());

            TextView tv_description = (TextView)convertView.findViewById(R.id.textView_description);
            tv_description.setText(game.getDescription());

            TextView tv_date = (TextView)convertView.findViewById(R.id.textView_date);
            tv_date.setText(game.getDate());

            TextView tv_author = (TextView)convertView.findViewById(R.id.textView_author);
            tv_author.setText(game.getAuthor());

            Button btn_get_buy = (Button) convertView.findViewById(R.id.button_get_buy);
            btn_get_buy.setVisibility(View.VISIBLE);
            Button btn_update = (Button) convertView.findViewById(R.id.button_update);
            btn_update.setVisibility(View.GONE);
            Button btn_delete = (Button) convertView.findViewById(R.id.button_delete);
            btn_delete.setVisibility(View.GONE);
            Button btn_run = (Button) convertView.findViewById(R.id.button_run);
            btn_run.setVisibility(View.GONE);

            // Do NOT possess current game
            if (!assets.contains(game.getId())) {
                btn_get_buy.setText(getString(game.getKey().equals(FREE_GAME) ? R.string.unlock : R.string.buy));
                btn_get_buy.setTag(R.id.TAG_CURRENT_GAME, game);
                btn_get_buy.setOnClickListener(new View.OnClickListener() {
                    EditText editText;
                    Button btn;
                    @Override
                    public void onClick(View v) {
                        Game game = (Game) v.getTag(R.id.TAG_CURRENT_GAME);
                        btn = (Button)v;
                        if (game.getKey().equals(FREE_GAME)) {
                            // free game, unlock directly.
                            assets.add(game.getId());
                            unlock_game();
                        } else {
                            // Require the key
                            editText = new EditText(getActivity());
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(getText(R.string.input_key))
                                    .setView(editText)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Game game = (Game)btn.getTag(R.id.TAG_CURRENT_GAME);
                                            if (editText.getText().toString().equals(game.getKey())) {
                                                assets.add(game.getId());
                                                unlock_game();
                                            } else {
                                                Toast.makeText(getActivity(), getText(R.string.wrong_key),
                                                        Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }).setNegativeButton(android.R.string.cancel,null)
                                    .show();
                        }
                    }

                    // click the button to download
                    void unlock_game() {
                        btn.setText(getText(R.string.download));
                        btn.setOnClickListener(new DownloadListener());
                        Toast.makeText(getActivity(), getText(R.string.unlock_success), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Possessed game
                File game_dir = getGameDir(game.getId());
                if (game_dir.exists()) {
                    // Installed
                    btn_get_buy.setVisibility(View.GONE);

                    // Check update
                    FileReader reader = null;
                    try {
                        reader = new FileReader(getLogFile(game.getId()));
                        String[] install_date = new BufferedReader(reader).readLine().split("\\.");
                        if (compareDate(game.getDate().split("\\."), install_date)>0) {
                            // out of date
                            btn_update.setVisibility(View.VISIBLE);
                            btn_update.setTag(R.id.TAG_CURRENT_GAME, game);
                            btn_update.setOnClickListener(new DownloadListener());
                        }
                    } catch (IOException e){

                    } finally {
                        if (reader!=null){
                            try{reader.close();}
                            catch (IOException e){}
                        }
                    }

                    btn_delete.setVisibility(View.VISIBLE);
                    btn_delete.setTag(R.id.TAG_CURRENT_GAME, game);
                    btn_delete.setOnClickListener(new View.OnClickListener() {
                        Game game;
                        @Override
                        public void onClick(View v) {
                            game = (Game) v.getTag(R.id.TAG_CURRENT_GAME);
                            // Confirm to delete
                            new AlertDialog.Builder(getActivity()).setTitle(android.R.string.dialog_alert_title)
                                    .setMessage(getString(R.string.confirm_delete))
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            FileUtil.deleteDir(getGameDir(game.getId()));
                                            Toast.makeText(getActivity(), R.string.delete_success, Toast.LENGTH_SHORT).show();
                                            // Restart the activity
                                            getActivity().recreate();
                                        }
                                    }).setNegativeButton(android.R.string.cancel, null)
                                    .show();
                        }
                    });
                    btn_run.setVisibility(View.VISIBLE);
                    btn_run.setTag(R.id.TAG_CURRENT_GAME, game);
                    btn_run.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Game game = (Game) v.getTag(R.id.TAG_CURRENT_GAME);
                            // Launch the game
                            MainActivity ma = (MainActivity)getActivity();
                            ma.launchGame(getGameDir(game.getId()));
                        }
                    });
                } else {
                    // Possessed, but haven't installed
                    btn_get_buy.setTag(R.id.TAG_CURRENT_GAME, game);
                    btn_get_buy.setText(getText(R.string.download));
                    btn_get_buy.setOnClickListener(new DownloadListener());
                }
            }
            return convertView;
        }
    }

    // Confirm to download
    private class DownloadListener implements View.OnClickListener {
        Game game;
        /**
         * <p>Download the game, unpack and install.</p>
         * <p>Then restart the activity after the game was installed.</p>
         */
        @Override
        public void onClick(View v) {
            game = (Game) v.getTag(R.id.TAG_CURRENT_GAME);
            // Confirm to download
            new AlertDialog.Builder(getActivity()).setTitle(android.R.string.dialog_alert_title)
                    .setMessage(getString(R.string.confirm_download,game.getSize()/(1024*1024f)))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GameAsyncInstall async = new GameAsyncInstall(game.getId());
                            async.execute();
                        }
                    }).setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    // Load games
    private class GameAsyncLoad extends AsyncTask<Void,Void,Void> {
        private List<Game> games;
        private GameListFragment.GameArrayAdapter adapter;

        public GameAsyncLoad(List<Game> articles, GameListFragment.GameArrayAdapter adapter) {
            super();
            this.games = articles;
            this.adapter = adapter;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... v) {
            String game_list_str = HttpUtil.getHttpContent(GAME_LIST_URL,"utf-8");

            if (game_list_str.isEmpty())
                return null;

            // Cache
            OutputStream out = null;
            try {
                out = getActivity().openFileOutput(CACHE_FILE, Context.MODE_PRIVATE);
                out.write(game_list_str.getBytes());
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {out.close();}
                    catch (IOException e)
                    {e.printStackTrace();}
                }
            }

            games.clear();
            GameListFragment.parseRespond(game_list_str, games);
            return null;
        }
    }

    // Load feature image
    private class ImageAsyncLoad extends AsyncTask<Void,Void,Void> {
        private String imageURL;
        private Bitmap bmp = null;
        private int id;
        private ImageView iv;

        public ImageAsyncLoad(String imageURL, ImageView iv, int game_id) {
            super();
            this.imageURL = imageURL;
            this.id = game_id;
            this.iv = iv;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (bmp!=null)
                iv.setImageBitmap(bmp);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... v) {
            bmp = HttpUtil.getHttpImage(imageURL);
            if (bmp!=null) {
                // image cache
                OutputStream out = null;
                try {
                    out = new FileOutputStream(getCacheImage(id));
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e){
                    e.printStackTrace();
                } finally {
                    if (out != null) {
                        try {out.close();}
                        catch (IOException e)
                        {e.printStackTrace();}
                    }
                }
            }
            return null;
        }
    }

    // Download and install game
    private class GameAsyncInstall extends AsyncTask<Void,Void,Void> {
        private int game_id;
        private AlertDialog dlg;
        public GameAsyncInstall(int game_id) {
            super();
            this.game_id = game_id;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg = new AlertDialog.Builder(getActivity()).setMessage(R.string.wait_download).create();
            dlg.show();
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (dlg.isShowing())
                dlg.dismiss();
            Toast.makeText(getActivity(),R.string.install_success, Toast.LENGTH_SHORT).show();
            // Restart the activity
            getActivity().recreate();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... v) {
            // Download game
            File zipFile = getZipFile(game_id);
            HttpUtil.downloadFile(GAME_PATH_URL + game_id + GAME_ZIP, zipFile);

            // Check if has installed
            File game_dir = getGameDir(game_id);
            if (game_dir.exists()){
                // remove the old and replace
                FileUtil.deleteDir(game_dir);
            }

            // Install (unzip)
            FileUtil.unzip(game_dir, zipFile);

            // Create LOG_FILE, write install-time
            FileWriter writer = null;
            try {
                writer = new FileWriter(getLogFile(game_id));
                writer.write(new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date()).toCharArray());
            } catch (IOException e){

            } finally {
                if (writer!=null){
                    try{writer.close();}
                    catch (IOException e){}
                }
            }
            return null;
        }
    }

    // if d1 > d2 return 1, d1 < d2 return -1, equal return 0
    private static int compareDate(String[] d1, String[] d2){
        int year1 = Integer.parseInt(d1[0]);
        int month1 = Integer.parseInt(d1[1]);
        int day1 = Integer.parseInt(d1[2]);
        int year2 = Integer.parseInt(d2[0]);
        int month2 = Integer.parseInt(d2[1]);
        int day2 = Integer.parseInt(d2[2]);
        if (year1>year2) return 1;
        else if (year1<year2) return -1;
        else if (month1>month2) return 1;
        else if (month1<month2) return -1;
        else if (day1>day2) return 1;
        else if (day1<day2) return -1;
        else return 0;
    }

    /**
     * <p>Parse http respond to List. Remove all '\r' automatically.</p>
     * @param game_list_str string to parse
     * @param games List
     */
    static void parseRespond(String game_list_str, List<Game> games) {
        int id=0,begin=0,end;
        game_list_str = game_list_str.replaceAll("\r","");
        while((end = game_list_str.indexOf("\n\n",begin))>0) {
            String s = game_list_str.substring(begin, end);
            begin = end + 2;
            int p = s.indexOf('=');
            if (p < 0) break;
            int e = s.indexOf('\n', p);
            String info_name = s.substring(p + 1, e);
            p = s.indexOf('=', e);
            e = s.indexOf('\n', p);
            String info_description = s.substring(p + 1, e);
            p = s.indexOf('=', e);
            e = s.indexOf('\n', p);
            String info_author = s.substring(p + 1, e);
            p = s.indexOf('=', e);
            e = s.indexOf('\n', p);
            String info_date = s.substring(p + 1, e);
            p = s.indexOf('=', e);
            e = s.indexOf('\n', p);
            String info_version = s.substring(p + 1, e);
            p = s.indexOf('=', e);
            e = s.indexOf('\n', p);
            String info_star = s.substring(p + 1, e);
            p = s.indexOf('=', e);
            e = s.indexOf('\n', p);
            String info_key = s.substring(p + 1, e);
            p = s.indexOf('=', e);
            String info_size = s.substring(p + 1, s.length());
            games.add(new Game(id, info_name, info_description, info_author, info_date, info_version,
                    Integer.parseInt(info_star), info_key, Integer.parseInt(info_size)));
            ++id;
        }
        Collections.reverse(games);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new GameArrayAdapter(games);
        this.setListAdapter(adapter);

        // Load asset
        InputStream asset_in = null;
        try {
            asset_in = getActivity().openFileInput(ASSET_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(asset_in));
            String line;
            while ((line = reader.readLine())!=null) {
                assets.add(Integer.parseInt(line));
            }
        } catch (IOException e){

        } finally {
            if (asset_in != null) {
                try {
                    asset_in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Load game list cache
        InputStream in = null;
        try {
            in = getActivity().openFileInput(CACHE_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            char[] bytes = new char[1024];
            StringBuilder sb = new StringBuilder();
            int size;
            while ((size=reader.read(bytes))>0) {
                sb.append(bytes,0,size);
            }
            parseRespond(sb.toString(),games);
        } catch (IOException e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Get game list
        GameAsyncLoad async = new GameAsyncLoad(games,adapter);
        async.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list,container,false);
        return v;
    }

    @Override
    public void onDestroy() {
        // Save assets
        OutputStream out = null;
        try {
            out = getActivity().openFileOutput(ASSET_FILE, Context.MODE_PRIVATE);
            for (Integer i : assets) {
                out.write(i.toString().getBytes());
                out.write('\n');
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {out.close();}
                catch (IOException e)
                {e.printStackTrace();}
            }
        }
        super.onDestroy();
    }
}
