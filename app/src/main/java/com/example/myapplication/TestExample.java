package com.example.myapplication;
import com.google.mediapipe.formats.proto.LandmarkProto;

public class TestExample {

    // Индексы ключевых точек для модели MediaPipe Pose.
    private static final int LEFT_SHOULDER_INDEX = 11;
    private static final int LEFT_ELBOW_INDEX = 13;
    private static final int LEFT_WRIST_INDEX = 15;
    private static final int RIGHT_SHOULDER_INDEX = 12;
    private static final int RIGHT_ELBOW_INDEX = 14;
    private static final int RIGHT_WRIST_INDEX = 16;
    // Для модели MediaPipe Holistic
    // индексы FaceMesh для рта.
    private static final int MOUTH_LEFT_INDEX = 0;
    private static final int MOUTH_RIGHT_INDEX = 1;

    // Индексы z-координаты для запястья.
    private static final int LEFT_WRIST_Z_INDEX = 15;
    private static final int RIGHT_WRIST_Z_INDEX = 16;

    // Пороговые значения для определения, находится ли рука около рта
    private static double HORIZONTAL_THRESHOLD = 0.1; // Горизонтальный порог
    private static double VERTICAL_THRESHOLD = 0.1; // Вертикальный порог
    private static final double Z_THRESHOLD = 0.1; // Порог для z-координаты

    public static boolean isHandNearMouth(LandmarkProto.NormalizedLandmarkList poseLandmarks) {
        if (poseLandmarks == null || poseLandmarks.getLandmarkCount() == 0) {
            return false;
        }

        // Получаем координаты ключевых точекА
        LandmarkProto.NormalizedLandmark leftWrist = poseLandmarks.getLandmark(LEFT_WRIST_INDEX);
        LandmarkProto.NormalizedLandmark rightWrist = poseLandmarks.getLandmark(RIGHT_WRIST_INDEX);
        LandmarkProto.NormalizedLandmark mouthLeft = poseLandmarks.getLandmark(MOUTH_LEFT_INDEX);
        LandmarkProto.NormalizedLandmark mouthRight = poseLandmarks.getLandmark(MOUTH_RIGHT_INDEX);

        // Рассчитываем центр рта
        PoseLandMark mouthCenter = createMouthCenter(
                new PoseLandMark(mouthLeft.getX(), mouthLeft.getY(), mouthLeft.getVisibility() > 0.5),
                new PoseLandMark(mouthRight.getX(), mouthRight.getY(), mouthRight.getVisibility() > 0.5)
        );

        // Добавляем z-координату, если она доступна
        double leftWristZ = poseLandmarks.getLandmark(LEFT_WRIST_Z_INDEX).getZ();
        double rightWristZ = poseLandmarks.getLandmark(RIGHT_WRIST_Z_INDEX).getZ();

        // Расстояние от запястья до центра рта
        double leftWristToMouthDistance = calculateDistance(leftWrist, mouthCenter.getX(), mouthCenter.getY(), mouthCenter.getVisible());
        double rightWristToMouthDistance = calculateDistance(rightWrist, mouthCenter.getX(), mouthCenter.getY(), mouthCenter.getVisible());

        // Вычисляем угол между плечом, локтем и запястьем
        double leftArmAngle = calculateAngle(
                new PoseLandMark(poseLandmarks.getLandmark(LEFT_SHOULDER_INDEX).getX(), poseLandmarks.getLandmark(LEFT_SHOULDER_INDEX).getY(), true),
                new PoseLandMark(poseLandmarks.getLandmark(LEFT_ELBOW_INDEX).getX(), poseLandmarks.getLandmark(LEFT_ELBOW_INDEX).getY(), true),
                new PoseLandMark(leftWrist.getX(), leftWrist.getY(), true)
        );
        double rightArmAngle = calculateAngle(
                new PoseLandMark(poseLandmarks.getLandmark(RIGHT_SHOULDER_INDEX).getX(), poseLandmarks.getLandmark(RIGHT_SHOULDER_INDEX).getY(), true),
                new PoseLandMark(poseLandmarks.getLandmark(RIGHT_ELBOW_INDEX).getX(), poseLandmarks.getLandmark(RIGHT_ELBOW_INDEX).getY(), true),
                new PoseLandMark(rightWrist.getX(), rightWrist.getY(), true)
        );

        // Проверяем, находится ли рука в пределах порогов от центра рта
        boolean isLeftHandNearMouth = leftWristToMouthDistance < HORIZONTAL_THRESHOLD &&
                Math.abs(leftWrist.getY() - mouthCenter.getY()) < VERTICAL_THRESHOLD &&
                leftArmAngle < Math.toRadians(90) && // Угол меньше 90 градусов
                leftWristZ < Z_THRESHOLD; // Z-координата находится в пределах порога

        boolean isRightHandNearMouth = rightWristToMouthDistance < HORIZONTAL_THRESHOLD &&
                Math.abs(rightWrist.getY() - mouthCenter.getY()) < VERTICAL_THRESHOLD &&
                rightArmAngle < Math.toRadians(90) && // Угол меньше 90 градусов
                rightWristZ < Z_THRESHOLD; // Z-координата находится в пределах порога


        boolean isSitting = isSitting(poseLandmarks);

        // Если человек сидит, используем другие пороговые значения или логику
        if (isSitting) {
            // Например, изменяем пороговые значения
            HORIZONTAL_THRESHOLD = 0.2; // Пример нового порога
            VERTICAL_THRESHOLD = 0.3; // Пример нового порога
            double Z_THRESHOLD_STANDING = 0.3;

            isLeftHandNearMouth = isLeftHandNearMouth && leftWristZ < Z_THRESHOLD_STANDING;
            isRightHandNearMouth = isRightHandNearMouth && rightWristZ < Z_THRESHOLD_STANDING;
        }

        return isLeftHandNearMouth || isRightHandNearMouth;
    }

    // Центр рта
    private static PoseLandMark createMouthCenter(PoseLandMark mouthLeft, PoseLandMark mouthRight) {
        float mouthX = (mouthLeft.getX() + mouthRight.getX()) / 2;
        float mouthY = (mouthLeft.getY() + mouthRight.getY()) / 2;
        boolean visibility = mouthLeft.getVisible() && mouthRight.getVisible();
        return new PoseLandMark(mouthX, mouthY, visibility);
    }

    // Определяем расстояние между двумя точками.
    private static double calculateDistance(LandmarkProto.NormalizedLandmark landmark, double x, double y, boolean visible) {
        if (!visible) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(
                Math.pow(landmark.getX() - x, 2) +
                        Math.pow(landmark.getY() - y, 2)
        );
    }

    // Функция для вычисления угла между тремя точками
    private static double calculateAngle(PoseLandMark a, PoseLandMark b, PoseLandMark c) {
        double ab = Math.sqrt(Math.pow(b.getX() - a.getX(), 2) + Math.pow(b.getY() - a.getY(), 2));
        double bc = Math.sqrt(Math.pow(b.getX() - c.getX(), 2) + Math.pow(b.getY() - c.getY(), 2));
        double ac = Math.sqrt(Math.pow(c.getX() - a.getX(), 2) + Math.pow(c.getY() - a.getY(), 2));
        return Math.acos((bc * bc + ab * ab - ac * ac) / (2 * bc * ab));
    }

    public static boolean isSitting(LandmarkProto.NormalizedLandmarkList poseLandmarks) {
        // Используйте ключевые точки, такие как бедра и колени, чтобы определить, сидит ли человек
        // Например, если угол между бедром и коленом больше определенного порога, можно считать, что человек сидит
        PoseLandMark leftShoulder = new PoseLandMark(poseLandmarks.getLandmark(LEFT_SHOULDER_INDEX).getX(), poseLandmarks.getLandmark(LEFT_SHOULDER_INDEX).getY(), true);
        PoseLandMark leftHip = new PoseLandMark(poseLandmarks.getLandmark(23).getX(), poseLandmarks.getLandmark(23).getY(), true);

        double shoulderToHipAngle = calculateAngle(leftShoulder, leftHip, new PoseLandMark(leftHip.getX(), leftShoulder.getY(), true));

        return shoulderToHipAngle < Math.toRadians(25);

        // Вспомогательный класс для представления ключевых точек
//        private static class PoseLandMark {
//            private float x;
//            private float y;
//            private boolean visible;
//
//            public PoseLandMark(float x, float y, boolean visible) {
//                this.x = x;
//                this.y = y;
//                this.visible = visible;
//            }
//
//            public float getX() {
//                return x;
//            }
//
//            public float getY() {
//                return y;
//            }
//
//            public boolean getVisible() {
//                return visible;
//            }
//        }
//    }
//

    }
}

