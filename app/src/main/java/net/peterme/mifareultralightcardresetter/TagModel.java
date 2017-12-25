package net.peterme.mifareultralightcardresetter;

import android.util.Log;

public class TagModel {
    public long id;
    public PageModel[] pages;
    public String name;

    public TagModel(String name, PageModel[] pages) {
        this.id = pages[0].data;
        this.id = this.id << 4 * 8;
        this.id = this.id | (0x00000000ffffffffL & pages[1].data);
        this.pages = pages;
        this.name = name;
    }

    public TagModel() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPages(PageModel[] pages) {
        this.id = pages[0].data;
        this.id = this.id << 4 * 8;
        this.id = this.id | (0x00000000ffffffffL & pages[1].data);
        this.pages = pages;
    }
}
