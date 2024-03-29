/*
 * EmailAlbum.java
 *
 * Created on 4 août 2008, 15:45
 */
package com.kg.emailalbum.viewer.ui;

import com.kg.util.ImageUtil;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

/**
 *
 * @author  gaudin
 */
public class EmailAlbum extends javax.swing.JFrame {

    SortedSet pictures = new TreeSet();
    //Iterator iPics = null;
    Stack history = null, followers = null;
    BufferedImage currentImage = null, resizedImage = null, tmpBuf = null;
    String currentImageName = null;
    boolean popupJustHidden = false;
    ResourceBundle bundle = ResourceBundle.getBundle(this.getClass().getName());

    /** Creates new form EmailAlbum */
    public EmailAlbum() {
        try {
            getRootPane().setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
            setVisible(true);
            setExtendedState(MAXIMIZED_BOTH);
            addKeyListener(new KeyListener() {

                public void keyTyped(KeyEvent e) {
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_BACK_SPACE:
                        case KeyEvent.VK_LEFT:
                        case KeyEvent.VK_UP:
                        case KeyEvent.VK_PAGE_UP:
                            back();
                            break;
                        default:
                            next();
                    }
                }
            });
            addMouseListener(new MouseListener() {

                public void mouseClicked(MouseEvent e) {
                }

                public void mousePressed(MouseEvent e) {
                    repaint();
                    maybeShowPopup(e);
                }

                public void mouseReleased(MouseEvent e) {
                    repaint();
                    maybeShowPopup(e);
                    if (!e.isPopupTrigger() && !popupJustHidden) {
                        next();
                    } else {
                        popupJustHidden = false;
                    }
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }

                private void maybeShowPopup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        rightBtnMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            initComponents();

            Properties pics = new Properties();

            pics.load(getClass().getResourceAsStream("/com/kg/emailalbum/viewer/pictures/content"));
            pictures.addAll(pics.keySet());
            start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void start() {
        Iterator iPics = pictures.iterator();
        history = new Stack();
        followers = new Stack();
        followers.addAll(pictures);
        Collections.sort(followers, new Comparator() {

            public int compare(Object o1, Object o2) {
                return -((String) o1).compareTo(o2);
            }
        });
        next();
    }

    private void next() {
        if (!followers.isEmpty()) {
            if (currentImageName != null) {
                history.push(currentImageName);
            }
            display((String) followers.pop());
        } else {
            System.exit(0);
        }
    }

    private void back() {
        if (!history.isEmpty()) {
            followers.push(currentImageName);
            display((String) history.pop());
        }
    }

    private void display(String imageName) {
        try {
            currentImageName = imageName;
            currentImage = ImageIO.read(getClass().getResourceAsStream("/com/kg/emailalbum/viewer/pictures/" + imageName));
            repaint();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void paint(Graphics g) {
        if (currentImage != null) {
            resizedImage = ImageUtil.resize(currentImage, getSize());
            tmpBuf = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            int offsetX = (getWidth() - resizedImage.getWidth()) / 2;
            int offsetY = (getHeight() - resizedImage.getHeight()) / 2;
            tmpBuf.getGraphics().drawImage(resizedImage, offsetX, offsetY, null);
            g.drawImage(tmpBuf, 0, 0, null);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rightBtnMenu = new javax.swing.JPopupMenu();
        menuSavePicture = new javax.swing.JMenuItem();
        menuSaveAllPictures = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        menuAbout = new javax.swing.JMenuItem();

        rightBtnMenu.setBackground(java.awt.SystemColor.control);
        rightBtnMenu.setLightWeightPopupEnabled(false);
        rightBtnMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
                rightBtnMenuPopupMenuWillBecomeInvisible(evt);
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        menuSavePicture.setBackground(java.awt.SystemColor.control);
        menuSavePicture.setText(bundle.getString("option.save")); // NOI18N
        menuSavePicture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSavePictureActionPerformed(evt);
            }
        });
        rightBtnMenu.add(menuSavePicture);

        menuSaveAllPictures.setBackground(java.awt.SystemColor.control);
        menuSaveAllPictures.setText(bundle.getString("option.saveAll")); // NOI18N
        menuSaveAllPictures.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveAllPicturesActionPerformed(evt);
            }
        });
        rightBtnMenu.add(menuSaveAllPictures);

        jSeparator1.setBackground(java.awt.SystemColor.control);
        rightBtnMenu.add(jSeparator1);

        menuAbout.setBackground(java.awt.SystemColor.control);
        menuAbout.setText(bundle.getString("option.about")); // NOI18N
        menuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        rightBtnMenu.add(menuAbout);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void rightBtnMenuPopupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_rightBtnMenuPopupMenuWillBecomeInvisible
    popupJustHidden = true;
}//GEN-LAST:event_rightBtnMenuPopupMenuWillBecomeInvisible

private void menuSavePictureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSavePictureActionPerformed
    saveCurrentImage();
}//GEN-LAST:event_menuSavePictureActionPerformed

private void menuSaveAllPicturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveAllPicturesActionPerformed
    saveAllImages();
}//GEN-LAST:event_menuSaveAllPicturesActionPerformed

private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
    JOptionPane.showMessageDialog(this, bundle.getString("about.message"), bundle.getString("about.title"), JOptionPane.PLAIN_MESSAGE);
}//GEN-LAST:event_menuAboutActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new EmailAlbum().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JMenuItem menuAbout;
    private javax.swing.JMenuItem menuSaveAllPictures;
    private javax.swing.JMenuItem menuSavePicture;
    private javax.swing.JPopupMenu rightBtnMenu;
    // End of variables declaration//GEN-END:variables

    private void saveAllImages() {
        JFileChooser fileSelector = new JFileChooser();
        fileSelector.setMultiSelectionEnabled(false);
        fileSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int returnVal = fileSelector.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            ProgressMonitor  monitor = new ProgressMonitor(null, bundle.getString("monitor.title"), bundle.getString("monitor.firstNote"), 0, pictures.size() - 1);
            File saveDir = fileSelector.getSelectedFile();
            Iterator imagesToSave = pictures.iterator();
            File saveFile = null;
            InputStream imageInput = null;
            OutputStream imageOutput = null;
            String imageName = null;
            int i = 0;
            while(imagesToSave.hasNext() && !monitor.isCanceled()) {
                imageName = (String) imagesToSave.next();
                saveFile = new File(saveDir, imageName);
                monitor.setNote(imageName);
                monitor.setProgress(i++);
                try {
                    imageInput = getClass().getResourceAsStream("/com/kg/emailalbum/viewer/pictures/" + imageName);
                    imageOutput = new FileOutputStream(saveFile);
                    while(imageInput.available() > 0) {
                        imageOutput.write(imageInput.read());
                    }
                    imageInput.close();
                    imageOutput.close();
               } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            monitor.close();
        }
    }
    // End of variables declaration

    private void saveCurrentImage() {
        JFileChooser fileSelector = new JFileChooser();
        fileSelector.setMultiSelectionEnabled(false);
        fileSelector.setSelectedFile(new File(currentImageName));
        fileSelector.addChoosableFileFilter(ImageUtil.getJpegFilter());
        fileSelector.setDialogType(JFileChooser.SAVE_DIALOG);
        
        int returnVal = fileSelector.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileSelector.getSelectedFile();
            BufferedImage tmpImage = new BufferedImage(currentImage.getWidth(), currentImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            tmpImage.getGraphics().drawImage(currentImage, 0, 0, null);
            try {
                ImageIO.write(tmpImage, "jpg", saveFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

}
