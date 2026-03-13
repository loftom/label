package com.medlabel;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("医疗影像辅助分析系统（作业版）");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1100, 800);
        frame.setLocationRelativeTo(null);

        ImagePanel imagePanel = new ImagePanel();

        JButton loadButton = new JButton("加载影像");
        JButton keypointButton = new JButton("关键点模式");
        JButton boxButton = new JButton("框选模式");
        JButton saveButton = new JButton("保存标注");
        JLabel statusLabel = new JLabel("未加载影像");

        loadButton.addActionListener(e -> {
            if (imagePanel.loadImage(frame)) {
                statusLabel.setText("已加载影像");
            }
        });

        keypointButton.addActionListener(e -> {
            imagePanel.setMode(AnnotationMode.KEYPOINT);
            statusLabel.setText("当前模式：关键点");
        });

        boxButton.addActionListener(e -> {
            imagePanel.setMode(AnnotationMode.BOX);
            statusLabel.setText("当前模式：框选");
        });

        saveButton.addActionListener(e -> {
            if (imagePanel.saveAnnotations(frame)) {
                statusLabel.setText("标注已保存");
            }
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(loadButton);
        toolbar.add(keypointButton);
        toolbar.add(boxButton);
        toolbar.add(saveButton);
        toolbar.add(statusLabel);

        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(new JScrollPane(imagePanel), BorderLayout.CENTER);

        frame.setVisible(true);
    }
}
