package com.scanner.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.scanlibrary.ScanActivity;
import com.scanlibrary.Utils;


public class MainActivity extends ActionBarActivity implements OnClickListener {

    // ===========================================================
    // Constants
    // ===========================================================

    private static final int REQUEST_CODE_SCAN = 47;

    private static final String SAVED_SCANNED_HHOTO = "scanned_photo";

    public static final int REQUEST_CAMERA = 1;

    // ===========================================================
    // Fields
    // ===========================================================

    private final ViewHolder viewHolder = new ViewHolder();

    private String scannedPhoto;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getters & Setters
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewHolder.prepare(findViewById(android.R.id.content));

        if (savedInstanceState != null) {
            scannedPhoto = savedInstanceState.getString(SAVED_SCANNED_HHOTO);
        }

        if (scannedPhoto != null) {
            viewHolder.image.setImageBitmap(Utils.getBitmapFromLocation(scannedPhoto));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.equals(viewHolder.scabBtn)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Check Permissions Now
                // Callback onRequestPermissionsResult interceptado na Activity MainActivity
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MainActivity.REQUEST_CAMERA);
            } else {
                // permission has been granted, continue as usual
                onScanButtonClicked();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();
                    onScanButtonClicked();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewHolder.scabBtn.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewHolder.scabBtn.setOnClickListener(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            String imgPath = data.getStringExtra(ScanActivity.RESULT_IMAGE_PATH);
            Bitmap bitmap = Utils.getBitmapFromLocation(imgPath);
            viewHolder.image.setImageBitmap(bitmap);
//            Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
//            Bitmap bitmap = null;
//            try {
//                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                getContentResolver().delete(uri, null, null);
//                viewHolder.image.setImageBitmap(bitmap);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_SCANNED_HHOTO, scannedPhoto);
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private void onScanButtonClicked() {
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanActivity.EXTRA_BRAND_IMG_RES, R.drawable.ic_crop_white_24dp);
        intent.putExtra(ScanActivity.EXTRA_TITLE, "Crop Document");
        intent.putExtra(ScanActivity.EXTRA_ACTION_BAR_COLOR, R.color.green);
        intent.putExtra(ScanActivity.EXTRA_LANGUAGE, "en");
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private static class ViewHolder {

        ImageView image;
        View scabBtn;

        void prepare(View parent) {
            image = (ImageView) parent.findViewById(R.id.image);
            scabBtn = parent.findViewById(R.id.scan);
        }
    }

}
