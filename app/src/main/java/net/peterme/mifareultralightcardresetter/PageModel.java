package net.peterme.mifareultralightcardresetter;

public class PageModel {
    public boolean locked;
    public int data;

    public PageModel(boolean locked, int data) {
        this.locked = locked;
        this.data = data;
    }
}
