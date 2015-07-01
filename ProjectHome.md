# What is EmailAlbum ? #

Some webmails such as Yahoo Mail are not handy enough for viewing a set of pictures that someone sent you.

EmailAlbum allows you to select pictures located in a local folder and automatically generates a lightweight executable jar file containing :
  * your pictures scaled down to fit a 1024 x 768 screen
  * a tiny java program allowing to display the pictures (scaled-up to fit the screen) and extract them on you local disk

Opening (double-clicking) this jar file with Windows 2000/XP/Vista automatically executes it, the only requirement is that a Java virtual machine (JRE) version 1.4.2 or higher has to be installed.

Other systems might start the viewer by running the command line :
```
java -jar MyPicturesAlbum.jar
```

(where `MyPicturesAlbum.jar` has to be replaced by the name of the generated album jar file.)

The EmailAlbum generator itself is packed in a single lightweight (49 **Ko**) self-executable jar file. Starting it should be as easy as opening it with Widows 2000/XP/Vista.

The command line is :
```
java -jar emailalbum-1.3.0.jar
```

EmailAlbum has been awarded the 100% Free label from SoftPedia :

[![](http://mac.softpedia.com/base_img/softpedia_free_award_f.gif)](http://mac.softpedia.com/progClean/EmailAlbum-Clean-66442.html)

# And my mobile phone ? #

Mobile phones cannot natively open jar files, so most of them won't be able to open EmailAlbums... **unless you have an Android phone** !

I have published an Android Application called EmailAlbum Creator + Viewer, which open EmailAlbum jar files, and any ZIP file containing pictures ! And with the latest version, you can now even create albums !

Creator features : pick photos from your phone gallery, rotate them, order them, add captions...

Viewer features : direct open Gmail attachments, save pictures to sdcard, animated pictures transitions, set picture as wallpaper or contact icon, slideshow...

To install EmailAlbum, search for it in the android market or scan this QR Code :

![http://chart.apis.google.com/chart?cht=qr&chs=100x100&chl=market://search?q=pname:com.kg.emailalbum.mobile&kg=null.png](http://chart.apis.google.com/chart?cht=qr&chs=100x100&chl=market://search?q=pname:com.kg.emailalbum.mobile&kg=null.png)

If you don't have Android Market on your phone (or other Android Device), here is the .apk direct download :
http://emailalbum.googlecode.com/files/EmailAlbumAndroid-2.5.1.apk

Ah, by the way, it's free ! ;-) And the source will also be published under GPLv3 soon.

# What's new in version 1.3.0 ? #
  * You can now add a caption for each picture. The caption is displayed when viewing the album.
  * Unselect pictures using the red cross icon instead of clicking the picture itself.
# What's new in version 1.2.0 ? #
  * User can now define the resolution to be used for the generated album pictures.
# What's new in version 1.1.0 ? #
  * Implementation of Resource Bundles to internationalise user interface of both creator and viewer.
  * Progress monitor while :
    * creating an album file
    * saving all pictures in viewer
  * Viewer now allows to go back in pictures history : use keys up, left, page up or backspace
  * Pictures with a portrait orientation were previously stored wiht a lower resolution than landscape pictures. They are now both using the same resolution.

# Disclaimer #
This application is in the earliest possible stage. I just coded it as I needed a solution to send pictures of my daughter to her grand'ma who is always fighting with Yahoo Mail when we send her an email with a dozen of pics. Now, she can open them all with one click, but no real testing has been done.

You can use EmailAlbum freely, if you find this useful, you can just send me an email so that I know that I am not the only one who is using it ;-), but there is no real active support guaranteed !