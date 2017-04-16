package cc.moooc.actinidia;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
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
                    // load cache
                    in = getActivity().openFileInput(game.getName() + ".png");
                    iv.setImageBitmap(BitmapFactory.decodeStream(in));
                } catch (FileNotFoundException e) {
                    ImageAsyncLoad async = new ImageAsyncLoad("http://moooc.cc/games/"+position+"/banner.jpg",iv,game.getName());
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

                Button button_get_buy = (Button)convertView.findViewById(R.id.button_get_buy);
                button_get_buy.setText(getString(game.getKey().equals("no")?R.string.get:R.string.buy));
                button_get_buy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                Button button_update = (Button)convertView.findViewById(R.id.button_update);
                button_update.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                Button button_delete = (Button)convertView.findViewById(R.id.button_delete);
                button_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                Button button_run = (Button)convertView.findViewById(R.id.button_run);
                button_run.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
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
            int begin=0,end;
            while((end = game_list_str.indexOf("\n\n",begin))>0) {
                String s = game_list_str.substring(begin,end);
                begin = end+2;
                int p = s.indexOf('=');
                if (p<0) break;
                int e = s.indexOf('\n',p);
                String info_name = s.substring(p+1,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_description = s.substring(p+1,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_author = s.substring(p+1,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_date = s.substring(p+1,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_version = s.substring(p+1,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_star = s.substring(p+1,e);
                p = s.indexOf('=',e);
                e = s.indexOf('\n',p);
                String info_key = s.substring(p+1,e);
                p = s.indexOf('=',e);
                String info_size = s.substring(p+1,s.length());
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
        private String name;
        private ImageView iv;

        public ImageAsyncLoad(String imageURL, ImageView iv, String game_name) {
            super();
            this.imageURL = imageURL;
            this.name = game_name;
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
                    out = getActivity().openFileOutput(name + ".png", Context.MODE_PRIVATE);
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
     * @return a string read from specific url, empty string if failed
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
                content+='\n';
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
     * Load an image from Internet.
     * @param url A URL string
     * @return A bitmap load from Internet, null if failed
     */
    public static Bitmap getHttpImage(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.getResponseCode();
            InputStream in = connection.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(in);
            return bmp;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null){
                connection.disconnect();
            }
        }
        return null;
    }
}
