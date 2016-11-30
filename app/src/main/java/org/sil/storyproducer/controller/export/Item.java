package org.sil.storyproducer.controller.export;

public class Item implements Comparable<Item>{
    private String name;
    private String data;
    private String date;
    private String path;
    //private String image;
    private boolean isFile;

    public Item(String n,String d, String dt, String p, boolean isFl)
    {
        name = n;
        data = d;
        date = dt;
        path = p;
        isFile = isFl;
    }
    public String getName()
    {
        return name;
    }
    public String getData()
    {
        return data;
    }
    public String getDate()
    {
        return date;
    }
    public String getPath()
    {
        return path;
    }
    //public String getImage() { return image; }
    public boolean isFile() { return isFile; }
    //public boolean getIsFile() { return isFile; }
    public int compareTo(Item o) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }
}