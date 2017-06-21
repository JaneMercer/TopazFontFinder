package com.example.dezzer.topazfontfinderint;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
//import org.opencv.core.Core;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import  org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;



import static org.opencv.core.CvType.CV_8UC3;

public class LoadImage extends AppCompatActivity {

  //  private static int RESULT_LOAD_IMAGE = 1;
  final int PIC_CROP = 1;
    private static final int MY_PERM_REQ_READ = 101;
    private static final int MY_PERM_REQ_WRITE = 102;
    private static final String TAG = "LoadImg";

    static{
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG,"=======================OpenCV not loaded");
        }
        else{

            Log.d(TAG,"=========================OpenCV  loaded");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        Button buttonLoadImage = (Button) findViewById(R.id.addImg);


        permissionCheck(); //CHECKS FOR THE PERMISSION FOR API 23+

        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, 0);
            }
        });

    }

    @TargetApi(23)
    public void permissionCheck()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if( permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERM_REQ_READ);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        switch(requestCode){
//            case 0: {
        if (resultCode == RESULT_OK && null != data) {
            final Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            final String picturePath = cursor.getString(columnIndex);
            cursor.close();

            findViewById(R.id.include1).setVisibility(View.GONE);
            findViewById(R.id.include2).setVisibility(View.VISIBLE);

            //release mode
//            ImageView imageView = (ImageView) findViewById(R.id.imgView);
//            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            //OpenCV image preparation**********************************************
            Bitmap bmp32 = JPGtoRGB888(BitmapFactory.decodeFile(picturePath));
            Mat sImage = new Mat();
            Mat grayImage = new Mat();
            Mat blurImage = new Mat();
            Mat thresImage = new Mat();
            Mat binImage = new Mat();
            Mat[] character_array = new Mat[10];
            Bitmap temp = null;

            Utils.bitmapToMat(bmp32, sImage);

            Imgproc.cvtColor(sImage, grayImage, Imgproc.COLOR_BGR2GRAY); //градации серого
            Imgproc.GaussianBlur(grayImage,blurImage,new Size(3, 3),0); //размытие
            Imgproc.adaptiveThreshold(blurImage, thresImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);

            Imgproc.Canny(thresImage, binImage, 10, 100, 3, true); //контур

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            //----------------------------
            Imgproc.findContours(binImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

            hierarchy.release();
            Imgproc.drawContours(binImage, contours, -1, new Scalar(255, 255, 255));//, 2, 8, hierarchy, 0, new Point());


            MatOfPoint2f approxCurve = new MatOfPoint2f();

            //For each contour found
            for (int i = 0; i < contours.size(); i++) {
                //Convert contours(i) from MatOfPoint to MatOfPoint2f
                MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
                //Processing on mMOP2f1 which is in type MatOfPoint2f
                double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                // Get bounding rect of contour
                Rect rect = Imgproc.boundingRect(points);

                // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                Imgproc.rectangle(binImage, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 3);

              //  Imgproc.rectangle(sImage, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 1);


                if(i==1)
                {
                    character_array[i] = binImage.submat(rect);
                    temp = Bitmap.createBitmap(character_array[i].cols(), character_array[i].rows(), Bitmap.Config.ARGB_8888);
                       Utils.matToBitmap(character_array[i], temp);

                        ImageView iv = (ImageView) findViewById(R.id.imgView);
                       iv.setImageBitmap(temp);
                }

            }
            approxCurve.release();

            //debug mode-------------------------------
       //     temp = Bitmap.createBitmap(binImage.cols(), binImage.rows(), Bitmap.Config.ARGB_8888);
         //   Utils.matToBitmap(binImage, temp);

       //     ImageView iv = (ImageView) findViewById(R.id.imgView);
         //   iv.setImageBitmap(temp);
            //----------------------------
            //end of OpenCV image preparation**********************************************
            sImage.release();
            grayImage.release();
            blurImage.release();
            binImage.release();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_load_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private Bitmap JPGtoRGB888(Bitmap img){
        Bitmap result = null;

        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

//        get jpeg pixels, each int is the color value of one pixel
        img.getPixels(pixels,0,img.getWidth(),0,0,img.getWidth(),img.getHeight());

//        create bitmap in appropriate format
        result = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.ARGB_8888);

//        Set RGB pixels
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());

        return result;
    }


    //-----------------------------------------------------------------------------
    public void cancelBtn ( View view) {
        findViewById(R.id.include2).setVisibility(View.GONE);
        findViewById(R.id.include1).setVisibility(View.VISIBLE);
    }

    public void go_to_actv ( View view) {
        Intent intent = new Intent(this, characterList.class);
        startActivity(intent);
    }


}




