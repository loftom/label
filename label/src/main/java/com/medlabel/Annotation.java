package com.medlabel;

import java.util.ArrayList;
import java.util.List;

public class Annotation {
    private String imagePath;
    private final List<Keypoint> keypoints = new ArrayList<>();
    private final List<Box> boxes = new ArrayList<>();

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<Keypoint> getKeypoints() {
        return keypoints;
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    public static class Keypoint {
        private int id;
        private String name;
        private int x;
        private int y;

        public Keypoint(int id, String name, int x, int y) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    public static class Box {
        private int id;
        private String name;
        private int x;
        private int y;
        private int width;
        private int height;

        public Box(int id, String name, int x, int y, int width, int height) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
