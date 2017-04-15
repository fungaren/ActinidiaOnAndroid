package cc.moooc.actinidia;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Game list fragment
 */

public class GameListFragment extends ListFragment {
    private List<Game> games = new ArrayList<>();
    private GameArrayAdapter adapter;

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
            if (null == convertView){
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_list_single,parent,false);
            }
            Game game = games.get(position);

            return convertView;
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
            String game_list_str = HttpUtil.getHttpContent("http://moooc.cc/games.php","utf-8");
            if (game_list_str.isEmpty()) return null;
            String[] game_list = game_list_str.split("\\n\\n");
            for (String s : game_list) {
                int p = s.indexOf('=');
                if (p<0) break;
                int e = s.indexOf('\n',p);
                String info_name = s.substring(p,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_description = s.substring(p,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_author = s.substring(p,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_date = s.substring(p,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_version = s.substring(p,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_star = s.substring(p,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_key = s.substring(p,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_size = s.substring(p,e);
                games.add(new Game(info_name, info_description, info_author, info_date, info_version,
                        Integer.parseInt(info_star), info_key, Integer.parseInt(info_size)));
            }
            return null;
        }
    }

    // Load feature image
    private class ImageAsyncLoad extends AsyncTask<Void,Void,Void> {
        private String imageURL;
        private Bitmap bmp = null;
        private ImageView iv;

        public ImageAsyncLoad(String imageURL, ImageView iv) {
            super();
            this.imageURL = imageURL;
            this.iv = iv;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            iv.setImageBitmap(bmp);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... v) {

            return null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new GameArrayAdapter(games);
        this.setListAdapter(adapter);

        // Load articles
        GameAsyncLoad async = new GameAsyncLoad(games,adapter);
        async.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list,container,false);
        return v;
    }
}

class HttpUtil {
    /**
     * Read a string from Internet
     * @param url a URL string
     * @param charset eg. "utf-8"
     * @return a string read from specific url
     */
    public static String getHttpContent(String url, String charset) {
        HttpURLConnection connection = null;
        String content = "";
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.getResponseCode();
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            String line;
            while ((line = reader.readLine()) != null) {
                content+=line;
            }
            return content;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection !=null){
                connection.disconnect();
            }
        }
        return "";
    }

    /**
     * Load an image from Internet
     * @param url A URL string
     * @param context A context object
     * @return A bitmap load from Internet
     */
    public static Bitmap getHttpImage(String url, Context context) {
        HttpURLConnection connection = null;
        OutputStream out = null;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.getResponseCode();
            InputStream in = connection.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(in);
            // image cache
            out = context.openFileOutput(url.hashCode() + ".png", Context.MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.PNG,100,out);
            return bmp;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null){
                connection.disconnect();
            }
            if (out != null) {
                try {out.close();}
                catch (IOException e)
                {e.printStackTrace();}
            }
        }
        return null;
    }
}
