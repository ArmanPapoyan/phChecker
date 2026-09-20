package arman.papoyan.phchecker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView pHValueText, statusText, productNameText, changeProductText;
    private Button captureButton, calibrateButton;
    private CaptureOverlayView captureOverlay;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private boolean isCalibrating = false;
    private FoodItem selectedFood;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private androidx.camera.core.Camera camera;
    private Button flashButton;
    private boolean isTorchOn = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String foodName = getIntent().getStringExtra(ProductSelectionActivity.EXTRA_FOOD_NAME);
        selectedFood = FoodDatabase.findByName(foodName);
        if (selectedFood == null) {
            Toast.makeText(this, "Product not selected, returning to list", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupCamera();
        setupButtons();

        cameraExecutor = Executors.newSingleThreadExecutor();

        productNameText.setText(selectedFood.name);
        changeProductText.setOnClickListener(v -> finish());

        statusText.setText("First, tap 'Calibrate' and take a photo of a WHITE sheet");
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        pHValueText = findViewById(R.id.pHValueText);
        statusText = findViewById(R.id.statusText);
        productNameText = findViewById(R.id.productNameText);
        changeProductText = findViewById(R.id.changeProductText);
        captureButton = findViewById(R.id.captureButton);
        calibrateButton = findViewById(R.id.calibrateButton);
        flashButton = findViewById(R.id.flashButton);
        captureOverlay = findViewById(R.id.captureOverlay);

    }

    private void setupCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void setupButtons() {
        flashButton.setOnClickListener(v -> toggleTorch());
        calibrateButton.setOnClickListener(v -> {
            isCalibrating = true;
            statusText.setText("Take a photo of a WHITE sheet of paper for calibration");
            Toast.makeText(this, "Point camera at a WHITE sheet and tap 'Take Photo'", Toast.LENGTH_LONG).show();
        });

        captureButton.setOnClickListener(v -> {
            if (imageCapture == null) {
                Toast.makeText(this, "Camera is not initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isCalibrating && !ColorAnalyzer.isCalibrated()) {
                Toast.makeText(this, "Please calibrate on a white sheet first!", Toast.LENGTH_LONG).show();
                return;
            }

            takePhoto();
        });
    }
    private void toggleTorch() {
        if (camera == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show();
            return;
        }

        isTorchOn = !isTorchOn;
        camera.getCameraControl().enableTorch(isTorchOn);

        flashButton.setText(isTorchOn ? "🔦 ON" : "🔦 OFF");

        ColorAnalyzer.resetCalibration();
        statusText.setText("Light changed. Please recalibrate on a WHITE sheet.");
        pHValueText.setText("pH: --");
        Toast.makeText(this,
                isTorchOn ? "Flash ON. Recalibrate on white sheet."
                        : "Flash OFF. Recalibrate on white sheet.",
                Toast.LENGTH_LONG).show();
    }
    private void takePhoto() {
        File photoFile = new File(getExternalFilesDir(null), "temp_photo.jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        runOnUiThread(() -> {
                            Bitmap bitmap = loadBitmapWithRotation(photoFile);
                            if (bitmap != null) {
                                if (isCalibrating) {
                                    calibrateWithWhite(bitmap);
                                } else {
                                    analyzeImage(bitmap);
                                }
                                bitmap.recycle();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to load photo", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "Error: " + exception.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void calibrateWithWhite(Bitmap bitmap) {
        int centerX = bitmap.getWidth() / 2 - 150;
        int centerY = bitmap.getHeight() / 2 - 75;

        if (centerX < 0) centerX = 0;
        if (centerY < 0) centerY = 0;

        int width = Math.min(300, bitmap.getWidth() - centerX);
        int height = Math.min(150, bitmap.getHeight() - centerY);

        Bitmap roiBitmap = Bitmap.createBitmap(bitmap, centerX, centerY, width, height);
        Bitmap blurredRoi = Bitmap.createScaledBitmap(
                roiBitmap,
                Math.max(1, width / 8),
                Math.max(1, height / 8),
                true);

        ColorAnalyzer.calibrateWithWhiteFromBitmap(blurredRoi);

        roiBitmap.recycle();
        blurredRoi.recycle();

        isCalibrating = false;
        statusText.setText("✅ Calibration complete! Take a photo of the pH strip for \"" + selectedFood.name + "\"");
        pHValueText.setText("pH: Ready");
        Toast.makeText(this, "Calibration successful! Now photograph the pH strip", Toast.LENGTH_LONG).show();
    }

    private void analyzeImage(Bitmap bitmap) {
        android.util.Log.d("Camera", "Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        int centerX = bitmap.getWidth() / 2 - 150;
        int centerY = bitmap.getHeight() / 2 - 75;

        if (centerX < 0) centerX = 0;
        if (centerY < 0) centerY = 0;

        int width = Math.min(300, bitmap.getWidth() - centerX);
        int height = Math.min(150, bitmap.getHeight() - centerY);

        // 1) Вырезаем ROI
        Bitmap roiBitmap = Bitmap.createBitmap(bitmap, centerX, centerY, width, height);

        // 2) Размываем через уменьшение
        Bitmap blurredRoi = Bitmap.createScaledBitmap(
                roiBitmap,
                Math.max(1, width / 8),
                Math.max(1, height / 8),
                true);

        // 3) СНАЧАЛА считаем цвет и pH
        float[] objectColor = ColorAnalyzer.getAverageColorFromBitmap(blurredRoi);
        String hexColor = ColorAnalyzer.getHexColor(objectColor);
        float pH = ColorAnalyzer.estimatePHFromBitmap(blurredRoi);

        // 4) ТОЛЬКО ТЕПЕРЬ освобождаем
        roiBitmap.recycle();
        blurredRoi.recycle();

        android.util.Log.d("Camera", "ROI color: " + hexColor + ", pH: " + pH);

        if (pH < 0) {
            pHValueText.setText("Error");
            statusText.setText("⚠️ Please calibrate on a white sheet first");
            return;
        }

        String resultText = String.format(java.util.Locale.getDefault(),
                "pH: %.1f\nColor: %s", pH, hexColor);
        pHValueText.setText(resultText);

        FoodItem.Freshness freshness = selectedFood.classify(pH);

        switch (freshness) {
            case FRESH:
                pHValueText.setTextColor(getColor(android.R.color.holo_green_dark));
                statusText.setText("🟢 Looks fresh: " + selectedFood.name);
                break;
            case SPOILED:
                pHValueText.setTextColor(getColor(android.R.color.holo_red_dark));
                statusText.setText("🔴 Looks spoiled: " + selectedFood.name);
                break;
            case UNCERTAIN:
            default:
                pHValueText.setTextColor(getColor(android.R.color.holo_orange_dark));
                statusText.setText("⚠️ pH does not give a clear answer for: " + selectedFood.name);
                break;
        }

        if (selectedFood.pHUnreliable) {
            Toast.makeText(this, selectedFood.reliabilityNote, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Object color: " + hexColor + ", pH: " +
                    String.format(java.util.Locale.getDefault(), "%.1f", pH), Toast.LENGTH_LONG).show();
        }
    }    private Bitmap loadBitmapWithRotation(File file) {
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bmp == null) return null;

        try {
            androidx.exifinterface.media.ExifInterface exif =
                    new androidx.exifinterface.media.ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (orientation) {
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bmp;
            }

            Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0,
                    bmp.getWidth(), bmp.getHeight(), matrix, true);
            if (rotated != bmp) {
                bmp.recycle();
            }
            return rotated;

        } catch (java.io.IOException e) {
            e.printStackTrace();
            return bmp;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }
}