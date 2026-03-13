package com.medlabel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    private String imagePath;
    private AnnotationMode mode = AnnotationMode.KEYPOINT;
    private final Annotation annotation = new Annotation();

    private Point boxStart;
    private Rectangle currentBox;

    public ImagePanel() {
        setBackground(Color.DARK_GRAY);
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (image == null) {
                    return;
                }
                if (mode == AnnotationMode.BOX) {
                    boxStart = e.getPoint();
                    currentBox = new Rectangle(boxStart);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (image == null || mode != AnnotationMode.BOX || boxStart == null) {
                    return;
                }
                int x = Math.min(boxStart.x, e.getX());
                int y = Math.min(boxStart.y, e.getY());
                int w = Math.abs(e.getX() - boxStart.x);
                int h = Math.abs(e.getY() - boxStart.y);
                currentBox = new Rectangle(x, y, w, h);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (image == null) {
                    return;
                }
                if (mode == AnnotationMode.BOX && currentBox != null && currentBox.width > 5 && currentBox.height > 5) {
                    int id = annotation.getBoxes().size() + 1;
                    String defaultName = "框" + id;
                    String name = promptName("请输入框选名称", defaultName);
                    if (name != null) {
                        annotation.getBoxes().add(new Annotation.Box(id, name, currentBox.x, currentBox.y, currentBox.width, currentBox.height));
                    }
                    currentBox = null;
                    boxStart = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (image == null) {
                    return;
                }
                if (mode == AnnotationMode.KEYPOINT && SwingUtilities.isLeftMouseButton(e)) {
                    int id = annotation.getKeypoints().size() + 1;
                    String defaultName = "点" + id;
                    String name = promptName("请输入关键点名称", defaultName);
                    if (name != null) {
                        annotation.getKeypoints().add(new Annotation.Keypoint(id, name, e.getX(), e.getY()));
                    }
                    repaint();
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public boolean loadImage(Component parent) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File file = chooser.getSelectedFile();
        try {
            image = ImageIO.read(file);
            imagePath = file.getAbsolutePath();
            annotation.getKeypoints().clear();
            annotation.getBoxes().clear();
            annotation.setImagePath(imagePath);
            currentBox = null;
            boxStart = null;
            revalidate();
            repaint();
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "无法加载影像：" + ex.getMessage());
            return false;
        }
    }

    public void setMode(AnnotationMode mode) {
        this.mode = mode;
    }

    private String promptName(String title, String defaultName) {
        String input = (String) JOptionPane.showInputDialog(
                this,
                title,
                "标注命名",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultName
        );
        if (input == null) {
            return null;
        }
        String name = input.trim();
        return name.isEmpty() ? defaultName : name;
    }

    public boolean saveAnnotations(Component parent) {
        if (image == null) {
            JOptionPane.showMessageDialog(parent, "请先加载影像。");
            return false;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("annotations.json"));
        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File file = chooser.getSelectedFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(annotation, writer);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "保存失败：" + ex.getMessage());
            return false;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(800, 600);
        }
        return new Dimension(image.getWidth(), image.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (image != null) {
            g2.drawImage(image, 0, 0, this);
        }

        g2.setColor(new Color(255, 0, 0, 180));
        for (Annotation.Keypoint kp : annotation.getKeypoints()) {
            int r = 5;
            g2.fillOval(kp.getX() - r, kp.getY() - r, r * 2, r * 2);
            g2.drawString(kp.getName() + "(K" + kp.getId() + ")", kp.getX() + 6, kp.getY() - 6);
        }

        g2.setColor(new Color(0, 255, 0, 180));
        for (Annotation.Box box : annotation.getBoxes()) {
            g2.draw(new Rectangle2D.Double(box.getX(), box.getY(), box.getWidth(), box.getHeight()));
            g2.drawString(box.getName() + "(B" + box.getId() + ")", box.getX() + 4, box.getY() + 14);
        }

        if (currentBox != null) {
            g2.setColor(new Color(0, 200, 255, 180));
            g2.draw(currentBox);
        }

        g2.dispose();
    }
}
