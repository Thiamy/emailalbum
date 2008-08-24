/*
 * Thumbnail.java
 *
 * Created on 4 août 2008, 11:20
 */

package com.kg.emailalbum.creator.ui;

import com.kg.util.ImageUtil;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author  gaudin
 */
public class Thumbnail extends javax.swing.JPanel implements TableCellRenderer {
    
    BufferedImage originalImage = null;

    public Thumbnail() {
        this(null);
    }

    /** Creates new form Thumbnail */
    public Thumbnail(BufferedImage originalImage) {
        System.out.println("Init thumbnail");
        initComponents();
        setPreferredSize(new Dimension(200, 300));
        setMinimumSize(new Dimension(100, 150));
        this.originalImage = originalImage;
        resize();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        imageLabel = new javax.swing.JLabel();
        textLabel = new javax.swing.JLabel();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        setLayout(new java.awt.GridBagLayout());

        imageLabel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                imageLabelComponentResized(evt);
            }
        });
        add(imageLabel, new java.awt.GridBagConstraints());

        textLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        textLabel.setText("C:\\machin\\truc.png");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(textLabel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

private void imageLabelComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_imageLabelComponentResized
}//GEN-LAST:event_imageLabelComponentResized

private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
    resize();
}//GEN-LAST:event_formComponentResized


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel imageLabel;
    private javax.swing.JLabel textLabel;
    // End of variables declaration//GEN-END:variables

    private void resize() {
        if(originalImage != null) {
            imageLabel.setIcon(new ImageIcon(ImageUtil.resize(originalImage, imageLabel.getSize())));
        } else {
            try {
                imageLabel.setIcon(new ImageIcon(ImageUtil.resize(ImageIO.read(getClass().getResource("/com/kg/ui/images/none.png")), imageLabel.getSize())));
            } catch (IOException e) {
                System.err.println("Packaging error ! Missing default image !");
                e.printStackTrace();
            }
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
