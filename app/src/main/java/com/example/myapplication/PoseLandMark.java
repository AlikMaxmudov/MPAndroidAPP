package com.example.myapplication;

public class PoseLandMark {
    float x, y;
    private boolean visibility; // Используйте тип boolean для переменной visibility

    PoseLandMark(float x, float y, boolean visibility) {
        this.x = x;
        this.y = y;
        this.visibility = visibility;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setVisible(boolean visibility) {
        this.visibility = visibility;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean getVisible() {
        return visibility;
    }
}
