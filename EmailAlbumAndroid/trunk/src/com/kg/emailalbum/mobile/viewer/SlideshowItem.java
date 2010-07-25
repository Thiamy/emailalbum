package com.kg.emailalbum.mobile.viewer;

import java.util.Set;
import java.util.TreeSet;

import android.graphics.Bitmap;
import android.net.Uri;

import com.kg.emailalbum.mobile.gallery.Tag;

public class SlideshowItem {
    public String name;
    public Uri uri;
    public String caption;
    public Bitmap bitmap;
    public Set<Tag> tags = new TreeSet<Tag>();
    
    public String getShortName() {
        if(name != null) {
            return name.substring(name.lastIndexOf('/') + 1);
        } else return "";
    }
}
