package cc.moooc.actinidia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

class HttpUtil {
    /**
     * Read a string from Internet
     * @param url a URL string
     * @param charset eg. "utf-8"
     * @return a string read from specific url, empty string if failed
     */
    public static String getHttpContent(String url, String charset) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.getResponseCode();
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            StringBuilder sb = new StringBuilder();
            char[] bytes = new char[1024];
            int size;
            while ((size=reader.read(bytes))>0) {
                sb.append(bytes,0,size);
            }
            return sb.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection !=null) {
                connection.disconnect();
            }
        }
        return "";
    }

    /**
     * Download file form Internet.
     * @param url A URL string
     * @param dest file path to save
     * @return size of file, -1 for error.
     */
    public static int downloadFile(String url, File dest) {
        HttpURLConnection connection = null;
        if (dest.exists()) dest.delete();
        FileOutputStream out = null;
        int total_size = 0;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.getResponseCode();
            InputStream in = connection.getInputStream();
            out = new FileOutputStream(dest);
            int size;
            byte[] bytes = new byte[1024];
            while ((size=in.read(bytes))>0) {
                out.write(bytes,0,size);
                total_size+=size;
            }
        } catch (IOException e) {
            return -1;
        } finally {
            if(connection != null){
                connection.disconnect();
            }
            try{
                if (out!=null)
                    out.close();
            }catch (IOException e){

            }
        }
        return total_size;
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

class FileUtil {
    /**
     * Delete directory recursively.
     * @param dir directory to delete
     */
    public static void deleteDir(File dir){
        if (dir.exists()){
            if (dir.isDirectory()) {
                File[] dirs = dir.listFiles();
                for (File d: dirs) {
                    if (d.isDirectory())
                        deleteDir(d);
                    else
                        d.delete();
                }
            }
            dir.delete();
        }
    }

    /**
     * Unzip
     * @param dir Destination path
     * @param zipFile Source zip file
     */
    public static void unzip(File dir, File zipFile) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                File f = new File(dir, entry.getName());
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                OutputStream out = new FileOutputStream(f);
                InputStream in = zip.getInputStream(entry);
                byte[] bytes = new byte[1024];
                int size;
                while ((size=in.read(bytes))>0) {
                    out.write(bytes,0,size);
                }
                out.close();
            }
        } catch (ZipException e) {

        } catch (IOException e) {

        } finally {
            try {
                if (zip!=null)
                    zip.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
