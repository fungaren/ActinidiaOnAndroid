package cc.moooc.actinidia;

/**
 * Game
 */

public class Game {
    private int id;
    private String name;
    private String description;
    private String author;
    private String date;
    private String version;
    private int star;
    private boolean available;
    private int size;
    private String banner;
    private String gamefile;

    public Game(int id, String name, String description, String author, String date,
                String version, int star, boolean available, int size, String banner, String gamefile) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.author = author;
        this.date = date;
        this.version = version;
        this.star = star;
        this.available = available;
        this.size = size;
        this.banner = banner;
        this.gamefile = gamefile;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public String getVersion() {
        return version;
    }

    public int getStar() {
        return star;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getSize() {
        return size;
    }

    public String getBanner() {
        return banner;
    }

    public String getGamefile() {
        return gamefile;
    }
}
