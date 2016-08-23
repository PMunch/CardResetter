package net.peterme.mifareultralightcardresetter;

public class Page {
    public boolean locked;
    public int data;
    public Page(boolean locked, int data){
        this.locked = locked;
        this.data = data;
    }
}
