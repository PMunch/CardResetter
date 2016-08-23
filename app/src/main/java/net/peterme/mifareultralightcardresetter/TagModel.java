package net.peterme.mifareultralightcardresetter;


public class TagModel {
    public long id;
    public Page[] pages;
    public String name;
    public TagModel(String name, Page[] pages){
        this.id = pages[0].data;
        this.id = this.id << 4*8;
        this.id = this.id | pages[1].data;
        this.pages = pages;
        this.name = name;
    }
    public TagModel(){
    }
    public void setName(String name){
        this.name = name;
    }
    public void setPages(Page[] pages){
        this.id = pages[0].data;
        this.id = this.id << 4*8;
        this.id = this.id | pages[1].data;
        this.pages = pages;
    }
}
