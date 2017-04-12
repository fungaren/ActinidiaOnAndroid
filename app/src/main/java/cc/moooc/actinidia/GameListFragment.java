package cc.moooc.actinidia;

import android.app.ListFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Game list fragment
 */

public class GameListFragment extends ListFragment {
    private List<Game> articles = new ArrayList<>();
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

    // Load Articles
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

        adapter = new GameArrayAdapter(articles);
        this.setListAdapter(adapter);

        // Load articles
        GameAsyncLoad async = new GameAsyncLoad(articles,adapter);
        async.execute();
    }
}
