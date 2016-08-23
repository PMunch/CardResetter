package net.peterme.mifareultralightcardresetter;


public class Tag {
    public long id;
    public Page[] pages;
    public String name;
    public Tag(String name, Page[] pages){
        this.id = pages[0].data;
        this.id = this.id << 4;
        this.id = this.id & pages[1].data;
        this.pages = pages;
        this.name = name;
    }
}
