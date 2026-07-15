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
    private TextView pHValueText, statusText;
    private Button captureButton, calibrateButton;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private boolean isCalibrating = false;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupCamera();
        setupButtons();

        cameraExecutor = Executors.newSingleThreadExecutor();

        statusText.setText("Сначала нажмите 'Калибровка' и сфоткайте БЕЛЫЙ лист");
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        pHValueText = findViewById(R.id.pHValueText);
        statusText = findViewById(R.id.statusText);
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
            statusText.setText("Сфоткайте БЕЛЫЙ лист бумаги для калибровки");
            Toast.makeText(this, "Наведите камеру на БЕЛЫЙ лист и нажмите 'Сделать фото'", Toast.LENGTH_LONG).show();
        });

        captureButton.setOnClickListener(v -> {
            if (imageCapture == null) {
                Toast.makeText(this, "Камера не инициализирована", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isCalibrating && !ColorAnalyzer.isCalibrated()) {
                Toast.makeText(this, "Сначала выполните калибровку по белому листу!", Toast.LENGTH_LONG).show();
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
                                Toast.makeText(MainActivity.this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "Ошибка: " + exception.getMessage(),
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
        statusText.setText("✅ Калибровка выполнена! Теперь можно измерять pH");
        pHValueText.setText("pH: готов");
        Toast.makeText(this, "Калибровка успешна! Теперь фоткайте pH-полоски", Toast.LENGTH_LONG).show();
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

        if (pH >= 0) {
            String resultText = String.format(java.util.Locale.getDefault(),
                    "pH: %.1f\nЦвет: %s", pH, hexColor);
            pHValueText.setText(resultText);

            if (pH < 6) {
                pHValueText.setTextColor(getColor(android.R.color.holo_red_dark));
                statusText.setText("🔴 Кислая среда");
            } else if (pH > 8) {
                pHValueText.setTextColor(getColor(android.R.color.holo_blue_dark));
                statusText.setText("🔵 Щелочная среда");
            } else {
                pHValueText.setTextColor(getColor(android.R.color.holo_green_dark));
                statusText.setText("🟢 Нейтральная среда");
            }

            Toast.makeText(this, "Цвет объекта: " + hexColor + ", pH: " + pH, Toast.LENGTH_LONG).show();
        } else {
            pHValueText.setText("Ошибка");
            statusText.setText("⚠️ Сначала выполните калибровку по белому листу");
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
                Toast.makeText(this, "Разрешение на камеру необходимо", Toast.LENGTH_LONG).show();
            }
        }
    }
}