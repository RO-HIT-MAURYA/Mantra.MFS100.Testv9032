package com.mantra.mfs100testv9032;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class MFS100Test extends Activity implements MFS100Event {

    Button btnInit;
    Button btnUninit;
    Button btnSyncCapture;
    Button btnStopCapture;
    Button btnMatchISOTemplate;
    Button btnExtractISOImage;
    Button btnExtractAnsi;
    Button btnClearLog;
    Button btnExtractWSQImage;
    TextView lblMessage;
    EditText txtEventLog;
    ImageView imgFinger;
    CheckBox cbFastDetection;
    private static long mLastClkTime = 0;
    private static long Threshold = 1500;

    private enum ScannerAction {
        Capture, Verify
    }

    byte[] Enroll_Template;
    byte[] Verify_Template;
    private FingerData lastCapFingerData = null;
    ScannerAction scannerAction = ScannerAction.Capture;

    int timeout = 10000;
    MFS100 mfs100 = null;

    private boolean isCaptureRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfs100_sample);

        FindFormControls();
        try {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(),Context.MODE_PRIVATE);
            Log.e("imageIs",sharedPreferences.getString("RawImage","null"));
            Log.e("stringIs",sharedPreferences.getString("FingerISOTemplate","null"));
        } catch (Exception e) {
            Log.e("Error", e.toString());
        }

        try {
            mfs100 = new MFS100(this);
            mfs100.SetApplicationContext(MFS100Test.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onShowLogClick(View view) {

    }

    @Override
    protected void onStart() {
        try {
            if (mfs100 == null) {
                mfs100 = new MFS100(this);
                mfs100.SetApplicationContext(MFS100Test.this);
            } else {
                InitScanner();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStart();
    }

    protected void onStop() {
        try {
		            if (isCaptureRunning) {
		                int ret = mfs100.StopAutoCapture();
		            }
		            Thread.sleep(500);
		//            UnInitScanner();
		        } catch (InterruptedException e) {
		            e.printStackTrace();
        }
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        try {
            if (mfs100 != null) {
                mfs100.Dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void FindFormControls() {
        try {
            btnInit = (Button) findViewById(R.id.btnInit);
            btnUninit = (Button) findViewById(R.id.btnUninit);
            btnMatchISOTemplate = (Button) findViewById(R.id.btnMatchISOTemplate);
            btnExtractISOImage = (Button) findViewById(R.id.btnExtractISOImage);
            btnExtractAnsi = (Button) findViewById(R.id.btnExtractAnsi);
            btnExtractWSQImage = (Button) findViewById(R.id.btnExtractWSQImage);
            btnClearLog = (Button) findViewById(R.id.btnClearLog);
            lblMessage = (TextView) findViewById(R.id.lblMessage);
            txtEventLog = (EditText) findViewById(R.id.txtEventLog);
            imgFinger = (ImageView) findViewById(R.id.imgFinger);
            btnSyncCapture = (Button) findViewById(R.id.btnSyncCapture);
            btnStopCapture = (Button) findViewById(R.id.btnStopCapture);
            cbFastDetection = (CheckBox) findViewById(R.id.cbFastDetection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onControlClicked(View v) {
        if (SystemClock.elapsedRealtime() - mLastClkTime < Threshold) {
            return;
        }
        mLastClkTime = SystemClock.elapsedRealtime();
        try {
            switch (v.getId()) {
                case R.id.btnInit:
                    InitScanner();
                    break;
                case R.id.btnUninit:
                    UnInitScanner();
                    break;
                case R.id.btnSyncCapture:
                    scannerAction = ScannerAction.Capture;
                    if (!isCaptureRunning) {
                        StartSyncCapture();
                    }
                    break;
                case R.id.btnStopCapture:
                    StopCapture();
                    break;
                case R.id.btnMatchISOTemplate:
                    scannerAction = ScannerAction.Verify;
                    if (!isCaptureRunning) {
                        StartSyncCapture();
                    }
                    break;
                case R.id.btnExtractISOImage:
                    ExtractISOImage();
                    break;
                case R.id.btnExtractAnsi:
                    ExtractANSITemplate();
                    break;
                case R.id.btnExtractWSQImage:
                    ExtractWSQImage();
                    break;
                case R.id.btnClearLog:
                    ClearLog();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_mfs100_sample);
        FindFormControls();
        try {
            if (mfs100 == null) {
                mfs100 = new MFS100(this);
                mfs100.SetApplicationContext(this);
            } /*else {
                InitScanner();
            }*/
            if (isCaptureRunning) {
                if (mfs100 != null) {
                    mfs100.StopAutoCapture();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetTextOnUIThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                SetLogOnUIThread(info);
            }
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Init failed, unhandled exception",
                    Toast.LENGTH_LONG).show();
            SetTextOnUIThread("Init failed, unhandled exception");
        }
    }

    private void StartSyncCapture() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SetTextOnUIThread("");
                isCaptureRunning = true;
                try {
                    FingerData fingerData = new FingerData();
                    int ret = mfs100.AutoCapture(fingerData, timeout, cbFastDetection.isChecked());
                    Log.e("StartSyncCapture.RET", "" + ret);
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        lastCapFingerData = fingerData;

                        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);
                        MFS100Test.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imgFinger.setImageBitmap(bitmap);
                            }
                        });

//                        Log.e("RawImage", Base64.encodeToString(fingerData.RawData(), Base64.DEFAULT));
//                        Log.e("FingerISOTemplate", Base64.encodeToString(fingerData.ISOTemplate(), Base64.DEFAULT));
                        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                        sharedPreferences.edit().putString("RawImage", getBase64String(bitmap)).apply();
                        sharedPreferences.edit().putString("FingerISOTemplate",Base64.encodeToString(fingerData.ISOTemplate(),Base64.DEFAULT)).apply();

                        SetTextOnUIThread("Capture Success");
                        String log = "\nQuality: " + fingerData.Quality()
                                + "\nNFIQ: " + fingerData.Nfiq()
                                + "\nWSQ Compress Ratio: "
                                + fingerData.WSQCompressRatio()
                                + "\nImage Dimensions (inch): "
                                + fingerData.InWidth() + "\" X "
                                + fingerData.InHeight() + "\""
                                + "\nImage Area (inch): " + fingerData.InArea()
                                + "\"" + "\nResolution (dpi/ppi): "
                                + fingerData.Resolution() + "\nGray Scale: "
                                + fingerData.GrayScale() + "\nBits Per Pixal: "
                                + fingerData.Bpp() + "\nWSQ Info: "
                                + fingerData.WSQInfo();
                        SetLogOnUIThread(log);
                        SetData2(fingerData);
                    }
                } catch (Exception ex) {
                    SetTextOnUIThread("Error");
                } finally {
                    isCaptureRunning = false;
                }
            }
        }).start();
    }

    private String getBase64String(Bitmap bitmap)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    private void StopCapture() {
        try {
            mfs100.StopAutoCapture();
        } catch (Exception e) {
            SetTextOnUIThread("Error");
        }
    }

    private void ExtractANSITemplate() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[2000]; // length 2000 is mandatory
            byte[] ansiTemplate;
            int dataLen = mfs100.ExtractANSITemplate(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ANSI Template");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                ansiTemplate = new byte[dataLen];
                System.arraycopy(tempData, 0, ansiTemplate, 0, dataLen);
                WriteFile("ANSITemplate.ansi", ansiTemplate);
                SetTextOnUIThread("Extract ANSI Template Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ANSI Template Error", e);
        }
    }

    private void ExtractISOImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] isoImage;

            // ISOType 1 == Regular ISO Image
            // 2 == WSQ Compression ISO Image
            int dataLen = mfs100.ExtractISOImage(lastCapFingerData.RawData(), tempData, 2);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ISO Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                isoImage = new byte[dataLen];
                System.arraycopy(tempData, 0, isoImage, 0, dataLen);
                WriteFile("ISOImage.iso", isoImage);
                SetTextOnUIThread("Extract ISO Image Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ISO Image Error", e);
        }
    }

    private void ExtractWSQImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] wsqImage;
            int dataLen = mfs100.ExtractWSQImage(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract WSQ Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                wsqImage = new byte[dataLen];
                System.arraycopy(tempData, 0, wsqImage, 0, dataLen);
                WriteFile("WSQ.wsq", wsqImage);
                SetTextOnUIThread("Extract WSQ Image Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract WSQ Image Error", e);
        }
    }

    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetLogOnUIThread("Uninit Success");
                SetTextOnUIThread("Uninit Success");
                lastCapFingerData = null;
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    private void WriteFile(String filename, byte[] bytes) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(bytes);
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void WriteFileString(String filename, String data) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            writer.write(data);
            writer.flush();
            writer.close();
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void ClearLog() {
        txtEventLog.post(new Runnable() {
            public void run() {
                try {
                    txtEventLog.setText("", BufferType.EDITABLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void SetTextOnUIThread(final String str) {

        lblMessage.post(new Runnable() {
            public void run() {
                try {
                    lblMessage.setText(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void SetLogOnUIThread(final String str) {

        txtEventLog.post(new Runnable() {
            public void run() {
                try {
                    txtEventLog.append("\n" + str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void SetData2(FingerData fingerData) {
        try {
            if (scannerAction.equals(ScannerAction.Capture)) {
                Enroll_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0,
                        fingerData.ISOTemplate().length);
            } else if (scannerAction.equals(ScannerAction.Verify)) {
                if (Enroll_Template == null) {
                    return;
                }
                Verify_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                        fingerData.ISOTemplate().length);
                int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
                if (ret < 0) {
                    SetTextOnUIThread("Error: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
                } else {
                    if (ret >= 96) {
                        SetTextOnUIThread("Finger matched with score: " + ret);
                    } else {
                        SetTextOnUIThread("Finger not matched, score: " + ret);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            WriteFile("Raw.raw", fingerData.RawData());
            WriteFile("Bitmap.bmp", fingerData.FingerImage());
            WriteFile("ISOTemplate.iso", fingerData.ISOTemplate());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long mLastAttTime=0l;

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {

        if (SystemClock.elapsedRealtime() - mLastAttTime < Threshold) {
            return;
        }
        mLastAttTime = SystemClock.elapsedRealtime();
        int ret;
        if (!hasPermission) {
            SetTextOnUIThread("Permission denied");
            return;
        }
        try {
            if (vid == 1204 || vid == 11279) {
                if (pid == 34323) {
                    ret = mfs100.LoadFirmware();
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        SetTextOnUIThread("Load firmware success");
                    }
                } else if (pid == 4101) {
                    String key = "Without Key";
                    ret = mfs100.Init();
                    if (ret == 0) {
                        showSuccessLog(key);
                    } else {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSuccessLog(String key) {
        try {
            SetTextOnUIThread("Init success");
            String info = "\nKey: " + key + "\nSerial: "
                    + mfs100.GetDeviceInfo().SerialNo() + " Make: "
                    + mfs100.GetDeviceInfo().Make() + " Model: "
                    + mfs100.GetDeviceInfo().Model()
                    + "\nCertificate: " + mfs100.GetCertification();
            SetLogOnUIThread(info);
        } catch (Exception e) {
        }
    }
    long mLastDttTime=0l;
    @Override
    public void OnDeviceDetached() {
        try {

            if (SystemClock.elapsedRealtime() - mLastDttTime < Threshold) {
                return;
            }
            mLastDttTime = SystemClock.elapsedRealtime();
            UnInitScanner();

            SetTextOnUIThread("Device removed");
        } catch (Exception e) {
        }
    }

    @Override
    public void OnHostCheckFailed(String err) {
        try {
            SetLogOnUIThread(err);
            Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
        }
    }

}
