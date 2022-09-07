package org.tyndalebt.storyproduceradv.controller.adapter;

public class DownloadDS {
    String fileName;
    String URL;
    Boolean checked;

    public DownloadDS(String name, String url, Boolean checked) {
        this.fileName = name;
        this.URL = url;
        this.checked = checked;
    }

    public String getName() {
        return fileName;
    }

    public String getURL() {
        return URL;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

}
