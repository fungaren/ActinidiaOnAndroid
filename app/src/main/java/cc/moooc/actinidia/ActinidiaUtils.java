package cc.moooc.actinidia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.net.ssl.HttpsURLConnection;

class HttpUtil {
    /**
     * Read a string from Internet
     * @param url a URL string
     * @param charset eg. "utf-8"
     * @return a string read from specific url, empty string if failed
     */
    public static String getHttpContent(String url, String charset) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.getResponseCode();
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            StringBuilder sb = new StringBuilder();
            char[] bytes = new char[1024];
            int size;
            while ((size=reader.read(bytes)) > 0) {
                sb.append(bytes,0, size);
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

    public interface DownloadCallback {
        void updateDownloadState(int nDownloadedBytes);
    }

    /**
     * Download file form Internet.
     * @param url A URL string
     * @param dest file path to save
     * @param cb a callback to know how many bytes have been downloaded.
     * @return size of file, -1 for error.
     */
    public static int downloadFile(String url, File dest, DownloadCallback cb) {
        HttpsURLConnection connection = null;
        if (dest.exists()) dest.delete();
        FileOutputStream out = null;
        int total_size = 0;
        try {
            connection = (HttpsURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.getResponseCode();
            InputStream in = connection.getInputStream();
            out = new FileOutputStream(dest);
            int size;
            byte[] bytes = new byte[1024];
            while ((size=in.read(bytes)) > 0) {
                out.write(bytes,0,size);
                total_size += size;
                if (cb != null)
                    cb.updateDownloadState(total_size);
            }
        } catch (IOException e) {
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try{
                if (out != null)
                    out.close();
            } catch (IOException e) {

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
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection)new URL(url).openConnection();
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
            if (connection != null) {
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
    public static void deleteDir(File dir) {
        if (dir.exists()) {
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
    public static void unzip(File dir, File zipFile) throws IOException {
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
                while ((size=in.read(bytes)) > 0) {
                    out.write(bytes,0, size);
                }
                out.close();
            }
        } catch (ZipException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (zip != null)
                    zip.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Inflate
     * @param dir Destination path
     * @param compactFile Source file deflated by zlib
     */
    public static void inflate(File dir, File compactFile) throws IOException {
        InflaterInputStream in = null;
        try {
            in = new InflaterInputStream(new FileInputStream(compactFile));
            OutputStream out = new FileOutputStream(new File(dir, compactFile.getName()));
            byte[] bytes = new byte[1024];
            int size;
            while ((size=in.read(bytes)) > 0) {
                out.write(bytes,0, size);
            }
            out.close();
        } catch (ZipException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

/*
    Resource file in memory:
    - /filename
    - /foldername/filename
    - /foldername/filename
    - /filename
    - /foldername/foldername/filename
    - /filename
    - /filename
*/
class ResourcePack
{
    private static final int TYPE_FOLDER = 0x01;
    private static final int TYPE_FILE = 0x00;
    private static final String MAGIC = "ACTINIDIA";

    private static final int ENTITY_TYPE = 1;      // uint8_t type
    private static final int ENTITY_RESERVED = 1;  // uint8_t reserved
    private static final int ENTITY_NAMESIZE = 2;  // uint16_t nameSize
    //private final int ENTITY_CHECKSUM = 4;  // uint32_t checksum
    private static final int ENTITY_DATASIZE = 4;  // uint32_t bytes
    private static final int ENTITY_SIZE = ENTITY_TYPE +
        ENTITY_RESERVED + ENTITY_NAMESIZE + ENTITY_DATASIZE;

    public class ResourceFile {
        String path;
        byte[] data;
        ResourceFile(String path, byte[] data)
        {
            this.path = path;
            this.data = data;
        }
    };
    
    public interface ParserCallback {
        void newFolder(String path);
        void newFile(InputStream in, String path, int dataSize) throws IOException;
    }

    // All resources will be loaded into memory, make sure enough free space.
    private LinkedList<ResourceFile> list = new LinkedList<>();

    private static void parsePack(InputStream in, ParserCallback cb) throws IOException
    {
        String relativePath = ".";
        LinkedList<Integer> folder_size = new LinkedList<>();

        for (int i = 0; i < MAGIC.length(); i++) {
            if (MAGIC.charAt(i) != in.read())
            {
                throw(new IOException("Invalid resouce pack"));
            }
        }

        do {
            byte[] entity = new byte[ENTITY_SIZE];
            in.read(entity);
            int type = 0xff & entity[0];
            int reserved = 0xff & entity[1];
            int nameSize =  (0xff & entity[2]) +
                            ((0xff & entity[3]) << 8);
            int dataSize =  (0xff & entity[4]) +
                            ((0xff & entity[5]) << 8) +
                            ((0xff & entity[6]) << 16) +
                            ((0xff & entity[7]) << 24);

            // read name
            byte[] _name = new byte[nameSize];
            in.read(_name);
            String name = new String(_name, "utf-8");
            relativePath += '/';
            relativePath += name;

            if (type == TYPE_FOLDER)
            {
                cb.newFolder(relativePath);

                // empty folder
                if (dataSize == 0)
                {
                    // calc remaining bytes in this folder to be read
                    if (!folder_size.isEmpty())
                    {
                        /*
                         * !!! NOTICE !!!
                         * pop() & push() methods of a LinkedList proceed at the front of
                         * the linked list head. Thus we need use getFirst() rather than getLast().
                         */
                        Integer i = folder_size.getFirst() - nameSize - ENTITY_SIZE;
                        folder_size.pop();
                        folder_size.push(i);
                    }

                    // back to parent folder
                    relativePath = relativePath.substring(0, relativePath.lastIndexOf('/'));
                }
                else
                { // not empty folder

                    // calc remaining bytes in this folder to be read
                    if (!folder_size.isEmpty())
                    {
                        Integer i = folder_size.getFirst() - nameSize - ENTITY_SIZE - dataSize;
                        folder_size.pop();
                        folder_size.push(i);
                    }

                    // enter subfolder
                    folder_size.push(dataSize);
                }
            }
            else if (type == TYPE_FILE)
            {
                // calc remaining bytes in this folder to be read
                Integer i = folder_size.getFirst() - nameSize - ENTITY_SIZE - dataSize;
                folder_size.pop();
                folder_size.push(i);

                if (dataSize != 0)
                {
                    // dump file or read file
                    cb.newFile(in, relativePath, dataSize);
                }

                // back to parent folder
                relativePath = relativePath.substring(0, relativePath.lastIndexOf('/'));
            }

            while (!folder_size.isEmpty() && folder_size.getFirst() == 0) {
                // no more files/folders to load
                folder_size.pop();
                relativePath = relativePath.substring(0, relativePath.lastIndexOf('/'));
            }
        } while (!relativePath.equals("."));
    }

    // Open a package and construct a ResourcePack instance.
    public ResourcePack(File resFile) throws IOException
    {
        InputStream in = new FileInputStream(resFile);
        parsePack(
            in,
            new ParserCallback() {
                // new folder
                public void newFolder(String path) {
                    ;
                }
                // new file: read into RAM
                public void newFile(InputStream in, String path, int dataSize) throws IOException {
                    byte[] data = new byte[dataSize];
                    in.read(data);
                    list.push(new ResourceFile(path, data));
                }
            }
        );
        in.close();
    }

    // Read a file by path, the path should be like "path/to/file.jpg"
    public byte[] readResource(String path)
    {
        path = "./game/" + path;
        ListIterator<ResourceFile> i = list.listIterator();
        while (i.hasNext())
        {
            ResourceFile file = i.next();
            if (file.path.equals(path))
                return file.data;
        }
        return null;
    }
};
