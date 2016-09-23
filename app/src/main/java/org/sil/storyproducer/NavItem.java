package org.sil.storyproducer;

/**
 * Created by Jordan Skomer on 9/27/2015.
 */
public class NavItem {

    private String title;
    private int icon;

    public NavItem(){}

    public NavItem(String title, int icon){
        this.title = title;
        this.icon = icon;
    }

    public String getTitle(){
        return this.title;
    }

    public int getIcon(){
        return this.icon;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setIcon(int icon){
        this.icon = icon;
    }
}
