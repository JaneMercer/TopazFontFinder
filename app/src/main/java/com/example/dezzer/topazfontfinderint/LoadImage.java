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
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
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

import org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


import static org.opencv.core.CvType.CV_8UC3;

public class LoadImage extends AppCompatActivity {

    //  private static int RESULT_LOAD_IMAGE = 1;
    final int PIC_CROP = 1;
    private static final int MY_PERM_REQ_READ = 101;
    private static final int MY_PERM_REQ_WRITE = 102;

    private static final int rectCOUNT = 20; //МАКС КОЛИЧЕСТВО ПРЯМОУГОЛЬНИКОВ
    private static final double minAREA = 700.0; //РАЗМЕР ПРЯМОУГОЛЬНИКОВ
    private Mat[] characters = new Mat[rectCOUNT]; //картинки из прямоугольников
    private Rect[] okRects = new Rect[rectCOUNT];//массив прямоугольников


    public Bitmap[] charactersBit = new Bitmap[10];

    private static final String TAG = "LoadImg";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "=======================OpenCV not loaded");
        } else {

            Log.d(TAG, "=========================OpenCV  loaded");
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

    //-----------------------------------------------------------------------------

    @TargetApi(23)
    public void permissionCheck() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERM_REQ_READ);
        }
    }

    //-----------------------------------------------------------------------------
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

    //-----------------------------------------------------------------------------
    //---------------------------------Image Processing---------------------
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
            Bitmap temp = null;

            Utils.bitmapToMat(bmp32, sImage);

            Imgproc.cvtColor(sImage, grayImage, Imgproc.COLOR_BGR2GRAY); //градации серого
            Imgproc.GaussianBlur(grayImage, blurImage, new Size(3, 3), 0); //размытие
            Imgproc.adaptiveThreshold(blurImage, thresImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);

            Imgproc.Canny(thresImage, binImage, 10, 100, 3, true); //контур

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            //----------------------------
            Imgproc.findContours(binImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

            hierarchy.release();
            Imgproc.drawContours(binImage, contours, -1, new Scalar(255, 255, 255));//, 2, 8, hierarchy, 0, new Point());

            MatOfPoint2f approxCurve = new MatOfPoint2f();

            int j = 0;
            int tempj = 0;
            //For each contour found
            for (int i = 0; i < contours.size() && j < rectCOUNT; i++) {
                //Convert contours(i) from MatOfPoint to MatOfPoint2f
                MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
                //Processing on mMOP2f1 which is in type MatOfPoint2f
                double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                // Get bounding rect of contour
                Rect rect = Imgproc.boundingRect(points);

                tempj = isright(rect, j);
                j = tempj;

            }

//DRAWS ONLY RIGHT RECTANGLES
            for (int i = 0; i < rectCOUNT; i++) {
                // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                if (okRects[i] != null) {
                    Imgproc.rectangle(binImage, new Point(okRects[i].x, okRects[i].y), new Point(okRects[i].x + okRects[i].width, okRects[i].y + okRects[i].height), new Scalar(255, 0, 0), 2);
/*
                //PUTS 1 RECTANGLE IMAGE INTO BITMAP ARRAY charactersBit //fix this
                if(i==0)
                {
                    characters[i] = binImage.submat(okRects[i]);
                    charactersBit[i] = Bitmap.createBitmap(characters[i].cols(), characters[i].rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(characters[i], charactersBit[i]);

                }*/
                }
            }
            approxCurve.release();

            //debug mode-------------------------------
            //UNCOMMENT THIS FOR REGULAR IMAGE OUTPUT
            temp = Bitmap.createBitmap(binImage.cols(), binImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(binImage, temp);

            ImageView iv = (ImageView) findViewById(R.id.imgView);
            iv.setImageBitmap(temp);

            //   ImageView iv = (ImageView) findViewById(R.id.characterImg);
            //        iv.setImageBitmap(charactersBit[0]);  //ERROR  charactersBit[1]=null WHY?

            //----------------------------
            //end of OpenCV image preparation**********************************************
            sImage.release();
            grayImage.release();
            blurImage.release();
            //   binImage.release();
        }
    }

    //-----------------------------------------------------------------------------
    private int isright(Rect rect, int j) { //Is rectangle right (not in another rectangle or does not include existing one)
        double area = 0;
        boolean isx1;
        boolean isx2;
        boolean isx3;

        boolean isy1;
        boolean isy2;
        boolean isy3;
        int i = 0;
        area = rect.area();
        if (area > minAREA) //rectangle is ok size
            {
                for ( i = 0; i < j; i++) {

                    isx1 = rect.x >= okRects[i].x;
                    isx2 = rect.x <= okRects[i].x+okRects[i].width;
                    isx3 = rect.x+rect.width <= okRects[i].x+okRects[i].width;
                 //   isx4 = rect.x+rect.width <= okRects[i].x;

                    isy1 = rect.y >= okRects[i].y;
                    isy2 = rect.y <= okRects[i].y+okRects[i].height;
                    isy3 = rect.y+rect.height <=okRects[i].y+okRects[i].height;

                    if(isx1 && isx2 && isx3 && isy1 && isy2 && isy3) //this rec is inside existing one (exit func)
                    { return j;}
                       else if (!isx1 && !isx3 && !isy1 && !isy3) {okRects[i] = rect;return j;}//this rec is over existing one (change existing to this)
                }

                okRects[j] = rect;
                j++;

            }
        return j;
    }
    //-----------------------------------------------------------------------------
    private Bitmap JPGtoRGB888(Bitmap img) {
        Bitmap result = null;

        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

//        get jpeg pixels, each int is the color value of one pixel
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

//        create bitmap in appropriate format
        result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

//        Set RGB pixels
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());

        return result;
    }
    //-----------------------------------------------------------------------------
    //---------------------------------Character List Creation--------------------- DOES NOT WORK

    private void pupulateListView() {
        ArrayAdapter<Mat> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.charListView);
        list.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<Mat> //елементи списку
    {
        public MyListAdapter() {
            super(LoadImage.this, R.layout.content_character_list, characters);
        }

        @NonNull
        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            //чи є view для роботи над ним
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.content_character_list, parent, false);
            }
            //зображення
      /*      ImageView iv = (ImageView) findViewById(R.id.characterImg);
                    iv.setImageBitmap(charactersBit[i]);*/

//show recognized character
            //      TextView recognizedText = (TextView) itemView.findViewById(R.id.recognizedChar);
            //    recognizedText.setText(currentChar.getName());


            return itemView;
        }


    }


    //-----------------------------------------------------------------------------
    public void cancelBtn(View view) {
        findViewById(R.id.include2).setVisibility(View.GONE);
        findViewById(R.id.include1).setVisibility(View.VISIBLE);
    }

    public void go_to_actv(View view) { //Go to charactyer list (Bugged)

        findViewById(R.id.include1).setVisibility(View.GONE);
        findViewById(R.id.include2).setVisibility(View.GONE);
        findViewById(R.id.include3).setVisibility(View.VISIBLE);

        pupulateListView();
    }


}




