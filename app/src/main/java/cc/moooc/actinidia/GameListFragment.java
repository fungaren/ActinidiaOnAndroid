package cc.moooc.actinidia;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Game list fragment
 */
public class GameListFragment extends ListFragment {
    private List<Game> games = new ArrayList<>();
    private GameArrayAdapter adapter;
    private ProgressBar progressBar;
    private boolean isDownloading = false;

    private static final String GAME_LIST_CACHE = "cache.txt";
    private static final String GAME_LIST_URL = "https://demo.moooc.cc/game.php";

    private File getCacheImage(int game_id) {
        return new File(getActivity().getCacheDir(), game_id + ".png");
    }

    private File getGameDir(int game_id) {
        return new File(getActivity().getFilesDir(), game_id + "");
    }

    private File getLogFile(int game_id) {
        return new File(getActivity().getFilesDir(), game_id + "/log.ini");
    }

    private File getCompactFile(int game_id) {
        return new File(getActivity().getCacheDir(), game_id +".res");
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

            // a game instance contains some information of the game
            Game game = games.get(position);

            // download the banner picture of the game
            ImageView iv = (ImageView)convertView.findViewById(R.id.imageView_banner);
            InputStream in = null;
            try {
                // load image cache
                in = new FileInputStream(getCacheImage(game.getId()));
                iv.setImageBitmap(BitmapFactory.decodeStream(in));
            } catch (FileNotFoundException e) {
                // no cache, download image
                ImageAsyncLoad async = new ImageAsyncLoad(game.getBanner(), iv, game.getId());
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

            // By default, the game is not installed. so we can not update, delete or run.
            Button btn_update = (Button) convertView.findViewById(R.id.button_update);
            btn_update.setVisibility(View.GONE);
            Button btn_delete = (Button) convertView.findViewById(R.id.button_delete);
            btn_delete.setVisibility(View.GONE);
            Button btn_run = (Button) convertView.findViewById(R.id.button_run);
            btn_run.setVisibility(View.GONE);

            // If user do not possess the game, it displays "unlock", otherwise it displays "download"
            Button btn_unlock = (Button) convertView.findViewById(R.id.button_unlock);
            btn_unlock.setTag(R.id.TAG_CURRENT_GAME, game);
            btn_unlock.setVisibility(View.VISIBLE);
            if (!game.isAvailable())
            {
                // click unlock button to make a new game available
                btn_unlock.setOnClickListener(new View.OnClickListener() {
                    Button btn;
                    @Override
                    public void onClick(View v) {
                        btn = (Button)v;
                        Game game = (Game) v.getTag(R.id.TAG_CURRENT_GAME);
                        // let user confirm to unlock
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getText(R.string.unlock))
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        unlock_game();
                                    }
                                }).setNegativeButton(android.R.string.cancel,null)
                                .show();
                    }

                    // just change the button caption and the click listener
                    void unlock_game() {
                        Game game = (Game)btn.getTag(R.id.TAG_CURRENT_GAME);
                        game.setAvailable(true);

                        btn.setText(getText(R.string.download));
                        btn.setOnClickListener(new ConfirmDownloadListener());
                        Toast.makeText(getActivity(), getText(R.string.unlock_success), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Unlocked, but have not been installed yet
                btn_unlock.setText(getText(R.string.download));
                btn_unlock.setOnClickListener(new ConfirmDownloadListener());
            }

            File game_dir = getGameDir(game.getId());
            if (game_dir.exists())
            {
                // the game is installed, no need to download
                btn_unlock.setVisibility(View.GONE);

                // Check updates
                FileReader reader = null;
                try {
                    // load the date time of installing
                    reader = new FileReader(getLogFile(game.getId()));
                    String[] install_date = new BufferedReader(reader).readLine().split("\\.");
                    if (compareDate(game.getDate().split("\\."), install_date) > 0) {
                        // out of date
                        btn_update.setVisibility(View.VISIBLE);
                        btn_update.setTag(R.id.TAG_CURRENT_GAME, game);
                        btn_update.setOnClickListener(new ConfirmDownloadListener());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null){
                        try { reader.close(); }
                        catch (IOException e) { }
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
                        ma.launchGame(new File(getGameDir(game.getId()), game.getId()+".res"));
                    }
                });
            }

            return convertView;
        }
    }

    // Confirm to download
    private class ConfirmDownloadListener implements View.OnClickListener {
        Game game;
        /**
         * <p>Download the game, unpack and install.</p>
         * <p>Then restart the activity after the game was installed.</p>
         */
        @Override
        public void onClick(View v) {
            if (isDownloading) {
                Toast.makeText(getActivity(),R.string.please_wait, Toast.LENGTH_SHORT).show();
                return;
            }
            game = (Game) v.getTag(R.id.TAG_CURRENT_GAME);
            // Confirm to download
            new AlertDialog.Builder(getActivity()).setTitle(android.R.string.dialog_alert_title)
                    .setMessage(getString(R.string.confirm_download,game.getSize()/(1024*1024f)))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GameAsyncInstall async = new GameAsyncInstall(game);
                            async.execute();
                        }
                    }).setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    // Load games list
    private class GamesAsyncLoad extends AsyncTask<Void,Void,Void> {
        private List<Game> games;
        private GameListFragment.GameArrayAdapter adapter;

        public GamesAsyncLoad(List<Game> articles, GameListFragment.GameArrayAdapter adapter) {
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
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... v) {
            String json = HttpUtil.getHttpContent(GAME_LIST_URL,"utf-8");

            if (json.isEmpty())
                return null;

            // Cache
            OutputStream out = null;
            try {
                out = getActivity().openFileOutput(GAME_LIST_CACHE, Context.MODE_PRIVATE);
                out.write(json.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {out.close();}
                    catch (IOException e)
                    {e.printStackTrace();}
                }
            }

            games.clear();
            try {
                parseRespond(new JSONArray(json), games);
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (bmp != null)
                iv.setImageBitmap(bmp);
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... v) {
            bmp = HttpUtil.getHttpImage(imageURL);
            if (bmp != null) {
                // image cache
                OutputStream out = null;
                try {
                    out = new FileOutputStream(getCacheImage(id));
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
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
        private Game game;
        private ProgressDialog dlg;
        private HttpUtil.DownloadCallback cb = new HttpUtil.DownloadCallback() {
            @Override
            public void updateDownloadState(int nDownloadedBytes) {
                dlg.setProgress(nDownloadedBytes);
            }
        };
        private boolean success = true;

        public GameAsyncInstall(Game game) {
            super();
            this.game = game;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg = new ProgressDialog(getActivity());
            dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dlg.setCancelable(false);
            dlg.setCanceledOnTouchOutside(false);
            dlg.setMessage(getString(R.string.wait_download));
            WindowManager.LayoutParams params = dlg.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dlg.setMax(game.getSize());
            dlg.show();
            getActivity().getWindow().getAttributes().dimAmount = 0.5f;
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            isDownloading = true;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (dlg.isShowing())
                dlg.dismiss();

            getActivity().getWindow().getAttributes().dimAmount = 0f;
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            if (success)
                Toast.makeText(getActivity(), R.string.install_success, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getActivity(), R.string.invalid_resource_pack, Toast.LENGTH_LONG).show();
            isDownloading = false;
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
            File compactFile = getCompactFile(game.getId());
            HttpUtil.downloadFile(game.getGamefile(), compactFile, cb);

            // Check if has installed
            File game_dir = getGameDir(game.getId());
            if (game_dir.exists()) {
                // remove the old and replace
                FileUtil.deleteDir(game_dir);
            }
            game_dir.mkdir();

            try {
                // Install (decompress) the compactFile to `game_dir/{id}.res`
                FileUtil.inflate(game_dir, compactFile);
            } catch (IOException e) {
                compactFile.delete();
                success = false;
                return null;
            }

            // Create LOG_FILE, write install-time
            FileWriter writer = null;
            try {
                writer = new FileWriter(getLogFile(game.getId()));
                writer.write(new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date()).toCharArray());
            } catch (IOException e) {

            } finally {
                if (writer != null) {
                    try { writer.close(); }
                    catch (IOException e) {}
                }
            }
            return null;
        }
    }

    // if d1 > d2 return 1, d1 < d2 return -1, equal return 0
    private static int compareDate(String[] d1, String[] d2) {
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
     * @param json JSONObject
     * @param games List
     */
    void parseRespond(JSONArray json, List<Game> games) {
        try {
            for (int i=0; i < json.length(); i++) {
                JSONObject obj = json.getJSONObject(i);
                Game g = new Game(
                        obj.getInt("id"),
                        obj.getString("name"),
                        obj.getString("description"),
                        obj.getString("author"),
                        obj.getString("date"),
                        obj.getString("version"),
                        obj.getInt("star"),
                        obj.getBoolean("available"),
                        obj.getInt("size"),
                        obj.getString("banner"),
                        obj.getString("gamefile")
                );
                games.add(g);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new GameArrayAdapter(games);
        this.setListAdapter(adapter);

        // Load game list cache
        InputStream in = null;
        try {
            in = getActivity().openFileInput(GAME_LIST_CACHE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            char[] bytes = new char[1024];
            StringBuilder sb = new StringBuilder();
            int size;
            while ((size = reader.read(bytes)) > 0) {
                sb.append(bytes, 0, size);
            }
            parseRespond(new JSONArray(sb.toString()), games);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // normal situation
        } catch (IOException e) {
            e.printStackTrace();
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
        GamesAsyncLoad async = new GamesAsyncLoad(games, adapter);
        async.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container,false);
        progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        // There's no game locally, we must retrieve some games from the server.
        if (games.isEmpty())
            progressBar.setVisibility(View.VISIBLE);
        else
            progressBar.setVisibility(View.INVISIBLE);
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
