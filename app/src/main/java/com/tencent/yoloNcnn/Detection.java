package com.tencent.yoloNcnn;

public class Detection {
    private int classId; // Renamed to avoid conflict with Java keyword 'class'
    private double prod;
    private double x;
    private double y;
    private double w;
    private double h;

    // Getter and Setter for classId
    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    // Getter and Setter for prod
    public double getProd() {
        return prod;
    }

    public void setProd(double prod) {
        this.prod = prod;
    }

    // Getter and Setter for x
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    // Getter and Setter for y
    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    // Getter and Setter for w
    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    // Getter and Setter for h
    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }
}
