
package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketGetter;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.glutil.EglManager;
import com.google.protobuf.InvalidProtocolBufferException;


import androidx.camera.lifecycle.ProcessCameraProvider;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.framework.AndroidAssetUtil;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.lang.Math;


/**
 * Main activity of MediaPipe example apps.
 */
public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    private static final String BINARY_GRAPH_NAME = "pose_tracking_gpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "pose_landmarks";
    private static final int NUM_HANDS = 2;
    private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.BACK;
    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
    // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
    // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
    // corner, whereas MediaPipe in general assumes the image origin is at top-left.
    private static final boolean FLIP_FRAMES_VERTICALLY = true;

    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        System.loadLibrary("opencv_java3");
    }


    private ProcessCameraProvider cameraProvider;
    // для управление камерой.


    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;
    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private FrameProcessor processor;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;
    // ApplicationInfo for retrieving metadata defined in the manifest.
    private ApplicationInfo applicationInfo;
    // Handles camera access via the {@link CameraX} Jetpack support library.
    private CameraXPreviewHelper cameraHelper;

    private Handler cameraSwitchHandler;

    // Переменная класса для отслеживания повторений
    private int scoreCount = 0;
    // Переменная для отслеживания предыдущего состояния руки
    private boolean wasHandNearMouth = false;

    private CameraHelper.CameraFacing currentCameraFacing = CameraHelper.CameraFacing.FRONT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutResId());

        previewDisplayView = findViewById(R.id.preview_display_surface);
        setupPreviewDisplayView();


        try {
            applicationInfo =
                    getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Cannot find application info: " + e);
        }
        // Инициализируется менеджер активов для MediaPipe и создается обработчик кадров
        // (FrameProcessor) для обработки видеопотока.и графов
        AndroidAssetUtil.initializeNativeAssetManager(this);
        eglManager = new EglManager(null);
        processor =
                new FrameProcessor(
                        this,
                        eglManager.getNativeContext(),
                        BINARY_GRAPH_NAME,
                        INPUT_VIDEO_STREAM_NAME,
                        OUTPUT_VIDEO_STREAM_NAME);
        processor
                .getVideoSurfaceOutput()
                .setFlipY(FLIP_FRAMES_VERTICALLY);


        //Добавляется обратный вызов для обработки пакетов данных,
        // содержащих информацию о позе человека, полученную из видеопотока
//        if (Log.isLoggable(TAG, Log.VERBOSE)) {
        processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> {
                    Log.v(TAG, "Received Pose landmarks packet.");
//                    try {
////                        NormalizedLandmarkList poseLandmarks = PacketGetter.getProto(packet, NormalizedLandmarkList.class);
//                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
//                        NormalizedLandmarkList poseLandmarks = NormalizedLandmarkList.parseFrom(landmarksRaw);
//                        Log.v(TAG, "[TS:" + packet.getTimestamp() + "] " + getPoseLandmarksDebugString(poseLandmarks));
//                        SurfaceHolder srh = previewDisplayView.getHolder();


                    try {
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        NormalizedLandmarkList poseLandmarks = NormalizedLandmarkList.parseFrom(landmarksRaw);
                        final boolean isHandRaised = TestExample.isHandNearMouth(poseLandmarks);
                        updateUIWithHandStatus(isHandRaised);

                        ArrayList<PoseLandMark> poseMarkers = new ArrayList<>();
                        for (NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
                            poseMarkers.add(new PoseLandMark(landmark.getX(), landmark.getY(), landmark.getVisibility()));
                        }

                        double rightAngle = getAngle(poseMarkers.get(16), poseMarkers.get(14), poseMarkers.get(12));
                        double leftAngle = getAngle(poseMarkers.get(15), poseMarkers.get(13), poseMarkers.get(11));
                        updateUIWithPoseAngles(rightAngle, leftAngle); // Метод для обновления UI с углами

                    } catch (InvalidProtocolBufferException exception) {
                        Log.e(TAG, "failed to get proto.", exception);
                    }

                }
        );
        /*processor.addPacketCallback(
                "throttled_input_video_cpu",
                (packet) ->{
                    Log.d("Raw Image","Receive image with ts: "+packet.getTimestamp());
                    Bitmap image = AndroidPacketGetter.getBitmapFromRgba(packet);
                }
        );*/

        // Проверяются и запрашиваются разрешения камеры,
        // инициализируется помощник предварительного просмотра камеры и запускается камера.
        PermissionHelper.checkAndRequestCameraPermissions(this);

        Button switchCameraButton = findViewById(R.id.switch_camera_button);
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        // Добавление кнопки в layout
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        //viewGroup.addView(switchCameraButton);

        // Инициализация cameraHelper
        cameraHelper = new CameraXPreviewHelper();
        startCamera();
    }

    private void updateUIWithPoseAngles(final double rightAngle, final double leftAngle) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView rightAngleTextView = findViewById(R.id.right_angle_text_view);
                TextView leftAngleTextView = findViewById(R.id.left_angle_text_view);
                rightAngleTextView.setText(String.format("Угол правого сустава: %.2f", rightAngle));
                leftAngleTextView.setText(String.format("Угол левого сустава: %.2f", leftAngle));
            }
        });
    }


    class PoseLandMark {
        private float x;
        private float y;
        private float visibility;

        public PoseLandMark(float x, float y, float visibility) {
            this.x = x;
            this.y = y;
            this.visibility = visibility;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getVisibility() {
            return visibility;
        }
    }

    public void switchCamera() {
        if (currentCameraFacing == CameraHelper.CameraFacing.BACK) {
            currentCameraFacing = CameraHelper.CameraFacing.FRONT;
        } else {
            currentCameraFacing = CameraHelper.CameraFacing.BACK;
        }
        //cameraHelper.stopCamera();

        // Перезапуск камеры с новой настройкой
        cameraHelper.startCamera(
                this, currentCameraFacing, /*unusedSurfaceTexture=*/ null, cameraTargetResolution());
    }


    // Used to obtain the content view for this application. If you are extending this class, and
    // have a custom layout, override this method and return the custom layout.
    protected int getContentViewLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onResume() {
        super.onResume();
        converter =
                new ExternalTextureConverter(
                        eglManager.getContext(), 2);
        converter.setFlipY(FLIP_FRAMES_VERTICALLY);
        converter.setConsumer(processor);
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        converter.close();

        // Hide preview display until we re-open the camera again.
        previewDisplayView.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onCameraStarted(SurfaceTexture surfaceTexture) {
        previewFrameTexture = surfaceTexture;

        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView.setVisibility(View.VISIBLE);
    }

    protected Size cameraTargetResolution() {
        return null; // No preference and let the camera (helper) decide.
    }

    public void startCamera() {
        // Убедитесь, что cameraHelper был инициализирован
        if (cameraHelper == null) {
            cameraHelper = new CameraXPreviewHelper();
        }
        cameraHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    onCameraStarted(surfaceTexture);
                });
        cameraHelper.startCamera(
                this, currentCameraFacing, /*unusedSurfaceTexture=*/ null, cameraTargetResolution());
    }


    // Проверка на поднятие руки
    // Метод для обновления UI
    private void updateUIWithHandStatus(final boolean isHandNearMouth) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView statusTextView = findViewById(R.id.status_text_view);
                TextView scoreTextView = findViewById(R.id.score_text_view);
                // Проверяем, изменилось ли состояние руки с "не прижата" на "прижата"
                if (isHandNearMouth && !wasHandNearMouth) {
                    scoreCount++; // Увеличиваем счетчик только если рука была поднята
                    statusTextView.setText("Рука прижата. Можете опускать руку.");
                    statusTextView.setTextColor(Color.GREEN);
                    scoreTextView.setText("Повторений: " + scoreCount);
                } else if (!isHandNearMouth) {
                    statusTextView.setText("Рука не прижата");
                    statusTextView.setTextColor(Color.RED);
                }
                // Обновляем состояние для следующего вызова
                wasHandNearMouth = isHandNearMouth;
            }
        });
    }

    // Метод для сброса счетчика повторений
    public void resetScoreCount() {
        scoreCount = 0;
        wasHandNearMouth = false; // Также сбрасываем состояние руки
    }


    protected Size computeViewSize(int width, int height) {
        return new Size(width, height);
    }

    protected void onPreviewDisplaySurfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        Size viewSize = computeViewSize(width, height);
        Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
        boolean isCameraRotated = cameraHelper.isCameraRotated();

        //displaySize.getHeight();
        //displaySize.getWidth();


        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture,
                isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    private void setupPreviewDisplayView() {
        previewDisplayView.setVisibility(View.GONE);
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        //viewGroup.addView(previewDisplayView);

        previewDisplayView
                .getHolder()
                .addCallback(
                        new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                                Log.d("Surface", "Surface Created");

                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                onPreviewDisplaySurfaceChanged(holder, format, width, height);
                                //  width , height 가 720,1280
                                Log.d("Surface", "Surface Changed");
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(null);
                                Log.d("Surface", "Surface destroy");
                            }

                        });

    }

    //[0.0 , 1.0]  normazlized  coordinate -> image width, height
    private String getPoseLandmarksDebugString(NormalizedLandmarkList poseLandmarks) {
        String poseLandmarkStr = "Pose landmarks: " + poseLandmarks.getLandmarkCount() + "\n";
        ArrayList<PoseLandMark> poseMarkers = new ArrayList<PoseLandMark>();
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            PoseLandMark marker = new PoseLandMark(landmark.getX(), landmark.getY(), landmark.getVisibility());
//          poseLandmarkStr += "\tLandmark ["+ landmarkIndex+ "]: ("+ (landmark.getX()*720)+ ", "+ (landmark.getY()*1280)+ ", "+ landmark.getVisibility()+ ")\n";
            ++landmarkIndex;
            poseMarkers.add(marker);
        }
        // Get Angle of Positions
        double rightAngle = getAngle(poseMarkers.get(16), poseMarkers.get(14), poseMarkers.get(12));
        double leftAngle = getAngle(poseMarkers.get(15), poseMarkers.get(13), poseMarkers.get(11));
        double rightKnee = getAngle(poseMarkers.get(24), poseMarkers.get(26), poseMarkers.get(28));
        double leftKnee = getAngle(poseMarkers.get(23), poseMarkers.get(25), poseMarkers.get(27));
        double rightShoulder = getAngle(poseMarkers.get(14), poseMarkers.get(12), poseMarkers.get(24));
        double leftShoulder = getAngle(poseMarkers.get(13), poseMarkers.get(11), poseMarkers.get(23));
        Log.v(TAG, "======Degree Of Position]======\n" +
                "rightAngle :" + rightAngle + "\n" +
                "leftAngle :" + leftAngle + "\n" +
                "rightHip :" + rightKnee + "\n" +
                "leftHip :" + leftKnee + "\n" +
                "rightShoulder :" + rightShoulder + "\n" +
                "leftShoulder :" + leftShoulder + "\n");
        return poseLandmarkStr;
    }

    static double getAngle(PoseLandMark firstPoint, PoseLandMark midPoint, PoseLandMark lastPoint) {
        double result =
                Math.toDegrees(
                        Math.atan2(lastPoint.getY() - midPoint.getY(), lastPoint.getX() - midPoint.getX())
                                - Math.atan2(firstPoint.getY() - midPoint.getY(), firstPoint.getX() - midPoint.getX()));

        result = Math.abs(result); // Angle should never be negative
        if (result > 180) {
            result = (360.0 - result); // Always get the acute representation of the angle
        }
        return result;
    }
}