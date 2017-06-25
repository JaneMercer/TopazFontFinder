package com.example.dezzer.topazfontfinderint;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


import static org.opencv.core.CvType.CV_8UC3;

public class LoadImage extends AppCompatActivity {

    //  private static int RESULT_LOAD_IMAGE = 1;
    final int PIC_CROP = 1;
    private static final int MY_PERM_REQ_READ = 101;
    private static final int MY_PERM_REQ_WRITE = 102;
    private static final int DEFAULT_THRESHOLD = 350;
    private static final int rectCOUNT = 20; //МАКС КОЛИЧЕСТВО ПРЯМОУГОЛЬНИКОВ
    private static final double minAREA = 700.0; //РАЗМЕР ПРЯМОУГОЛЬНИКОВ

    private Mat[] characters = new Mat[rectCOUNT]; //картинки из прямоугольников
    private Rect[] okRects = new Rect[rectCOUNT];//массив прямоугольников

    private List<Mat> charactersMat = new ArrayList<Mat>(); //list mat для сравнения контуров потом
    private List<Bitmap> charactersBit = new ArrayList<Bitmap>(); //list битмапов из картинок
    private List<String> recognizedCharList = new ArrayList<String>();//list соответствующих картинкам букв

    //  public Bitmap[] charactersBit = new Bitmap[rectCOUNT];

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

        File direct = new File(Environment.getExternalStorageDirectory()+"/TopazFonts");

        if(!direct.exists()) {
            if(direct.mkdir()); //directory is created;
        }

        AssetDatabaseOpenHelper adb = new AssetDatabaseOpenHelper(this);
        SQLiteDatabase db = adb.openDatabase();
        /*відкрити зображення з бд - тестування*/
       Cursor c = db.rawQuery("SELECT image FROM fonts WHERE char = ?",  new String[] {"a"});
        c.moveToFirst(); //для перемещения в цикле есть moveToNext()
        String temp = Environment.getExternalStorageDirectory() + c.getString(c.getColumnIndex("image"));
   //    String temp = c.getString(c.getColumnIndex("image"));
        Log.d("MyApp", "cnt: "+temp);
        File imgFile = new  File(temp);


if(imgFile.exists()){

    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

    ImageView myImage = (ImageView) findViewById(R.id.test);

    myImage.setImageBitmap(myBitmap);

}
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
            Bitmap temp2 = null;

            Utils.bitmapToMat(bmp32, sImage);
            //----------------------------RESIZING LOADED IMAGE//----------------------------
            Log.d(TAG, "=======================W" + Integer.toString(sImage.width()));
            while (sImage.width() > 700 && sImage.height() > 700) {
                Imgproc.resize(sImage, sImage, new Size(), 0.6, 0.6, Imgproc.INTER_AREA);
                Log.d(TAG, "=======================W" + Integer.toString(sImage.width()));
            }
            //----------------------------
            Imgproc.cvtColor(sImage, grayImage, Imgproc.COLOR_BGR2GRAY); //градации серого
            Imgproc.GaussianBlur(grayImage, blurImage, new Size(3, 3), 0); //размытие
            Imgproc.adaptiveThreshold(blurImage, thresImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);

            Imgproc.Canny(thresImage, binImage, 10, 100, 3, true); //контур

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            //----------------------------
            Imgproc.findContours(binImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

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

                    charactersMat.add(i,binImage.submat(okRects[i]));
                    //PUTS  RECTANGLE IMAGE INTO BITMAP LIST charactersBit
                    characters[i] = sImage.submat(okRects[i]);

                    charactersBit.add(i, Bitmap.createBitmap(characters[i].cols(), characters[i].rows(), Bitmap.Config.ARGB_8888));
                    Utils.matToBitmap(characters[i], charactersBit.get(i));
                    recognizedCharList.add(i, null);


                }
            }
            approxCurve.release();

            //debug mode-------------------------------
            //UNCOMMENT THIS FOR REGULAR IMAGE OUTPUT
            temp = Bitmap.createBitmap(binImage.cols(), binImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(binImage, temp);

            ImageView iv = (ImageView) findViewById(R.id.imgView);
            iv.setImageBitmap(temp);

            temp2 = Bitmap.createBitmap(sImage.cols(), sImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(sImage, temp2);

            ImageView iv3 = (ImageView) findViewById(R.id.imageView3);
            iv3.setImageBitmap(temp2);


            //----------------------------
            //end of OpenCV image preparation**********************************************
            sImage.release();
            grayImage.release();
            blurImage.release();
            binImage.release();
        }
    }

    //-----------------------------------------------------------------------------
    private int isright(Rect rect, int j) { //Is rectangle right (not in another rectangle or does not include existing one)
        double area = 0;
        int i = 0;
        area = rect.area();
        if (area > minAREA) //rectangle is ok size
        {
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
    //---------------------------------Character List Creation---------------------

    private void pupulateListView() {

        ArrayAdapter<Bitmap> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.charListView);
        list.setItemsCanFocus(true);
        list.setAdapter(adapter);
    }


    private class MyListAdapter extends ArrayAdapter<Bitmap> //елементи списку
    {


        public MyListAdapter() {
            super(LoadImage.this, R.layout.content_character_list, charactersBit);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //ViewHolder holder = null;
            final ViewHolder holder;
            if (convertView == null) {

                holder = new ViewHolder();
                LayoutInflater inflater = LoadImage.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.content_character_list, null);
                holder.characterImg = (ImageView) convertView.findViewById(R.id.characterImg);
                holder.recognizedChar = (EditText) convertView.findViewById(R.id.recognizedChar);

                convertView.setTag(holder);

            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.ref = position;

            holder.characterImg.setImageBitmap(charactersBit.get(position)); //sets images
            holder.recognizedChar.setText(recognizedCharList.get(position));//sets characters

            //-----------------------------ENTER CHARACTERS
            holder.recognizedChar.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    // TODO Auto-generated method stub
                    recognizedCharList.set(holder.ref, arg0.toString());
                    //   Log.d(TAG, "============Char===========" + holder.ref + recognizedCharList.get(holder.ref));
                }
            });

            //-----------------------------DELETE UNWANTED
            convertView.setOnTouchListener(new View.OnTouchListener() {

                int initialX = 0;
                final float slop = ViewConfiguration.get(LoadImage.this).getScaledTouchSlop();

                @Override
                public boolean onTouch(final View view, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        //x = (int) event.getX();
                        initialX = (int) event.getX();
                        while(initialX == 0)
                        {view.setPadding(0, initialX, 0, 0); initialX--;}
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {


                        int currentX = (int) event.getX();
                        int offset = currentX - initialX;
                        if (Math.abs(offset) > slop) {
                            view.setPadding(offset, 0, 0, 0);

                            if (offset > DEFAULT_THRESHOLD) {

                                charactersBit.remove(holder.ref);
                                recognizedCharList.remove(holder.ref);
                                charactersMat.remove(holder.ref);
                                notifyDataSetChanged();

                            }
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        // Animate back if no action was performed.

                        ValueAnimator animator = ValueAnimator.ofInt(view.getPaddingLeft(), 0);
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                view.setPadding((Integer) valueAnimator.getAnimatedValue(), 0, 0, 0);
                            }
                        });
                        animator.setDuration(150);
                        animator.start();
                    }

                    return true;
                }

            });
            //-----------------------------

            return convertView;


        }

        private class ViewHolder {
            ImageView characterImg;
            EditText recognizedChar;
            int ref;
        }


    }


    //-----------------------------Navigation buttons------------------------------------------------

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




