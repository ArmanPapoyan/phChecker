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

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private boolean isCalibrating = false;
    private FoodItem selectedFood;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

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
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void setupButtons() {
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

    private void takePhoto() {
        File photoFile = new File(getExternalFilesDir(null), "temp_photo.jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        runOnUiThread(() -> {
                            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
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
        int centerX = bitmap.getWidth() / 2 - 100;
        int centerY = bitmap.getHeight() / 2 - 50;

        ColorAnalyzer.calibrateWithWhite(bitmap,
                Math.max(0, centerX), Math.max(0, centerY), 200, 100);

        isCalibrating = false;
        statusText.setText("✅ Calibration complete! Take a photo of the pH strip for \"" + selectedFood.name + "\"");
        pHValueText.setText("pH: Ready");
        Toast.makeText(this, "Calibration successful! Now photograph the pH strip", Toast.LENGTH_LONG).show();
    }

    private void analyzeImage(Bitmap bitmap) {
        int centerX = bitmap.getWidth() / 2 - 100;
        int centerY = bitmap.getHeight() / 2 - 50;

        if (centerX < 0) centerX = 0;
        if (centerY < 0) centerY = 0;

        int width = Math.min(200, bitmap.getWidth() - centerX);
        int height = Math.min(100, bitmap.getHeight() - centerY);

        float[] objectColor = ColorAnalyzer.getAverageColor(bitmap, centerX, centerY, width, height);
        String hexColor = ColorAnalyzer.getHexColor(objectColor);

        float pH = ColorAnalyzer.estimatePH(bitmap, centerX, centerY, width, height);

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