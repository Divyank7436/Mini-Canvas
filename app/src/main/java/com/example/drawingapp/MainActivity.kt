package com.example.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.icu.text.CaseMap.Title
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.drawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog : Dialog? = null

    private val openGalleryLauncher :ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode== RESULT_OK && result.data!=null ){
                val imageBackground : ImageView = findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }

     private val requestPermission: ActivityResultLauncher<Array<String>> =
         registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
             permissions ->
             permissions.entries.forEach{
                 val permissionName = it.key
                 val isGranted = it.value
                 if(isGranted){
                     Toast.makeText(this, "Permission Granted for Storage", Toast.LENGTH_LONG).show()
                     val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                     openGalleryLauncher.launch(pickIntent)


                 }else {
                     if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                         Toast.makeText(this, "Permission Denied for Storage", Toast.LENGTH_LONG)
                             .show()
                     }

                 }

             }



         }
    private var binding : ActivityMainBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {


        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        setSupportActionBar(binding?.mainToolbar)
        if(supportActionBar!=null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "MINI CANVAS"
        }



        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColor = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColor[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )

        val ibBrush : ImageButton=findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }


        val galleryButton : ImageButton = findViewById(R.id.ib_gallery)
        galleryButton.setOnClickListener{
            requestPermissionStorage()
        }

        val undoButton : ImageButton = findViewById(R.id.undo)
        undoButton.setOnClickListener{
        drawingView?.onClickUndo()
        }

      val redoButton : ImageButton = findViewById(R.id.reverse)
        redoButton.setOnClickListener{
            drawingView?.onClickRedo()
        }


        val saveButton : ImageButton = findViewById(R.id.ib_save)
        saveButton.setOnClickListener{
        if(isReadStorageAllowed()){
            showProgressBar()
            lifecycleScope.launch{
                val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
                saveBitmapFiles(getBitmapFromView(flDrawingView))
            }
        }
        }


    }
    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn : ImageButton=brushDialog.findViewById(R.id.ib_small_brush)
      smallBtn.setOnClickListener{
          drawingView?.setSizeForBrush(10.toFloat())
          brushDialog.dismiss()
      }
        val mediumBtn : ImageButton=brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn : ImageButton=brushDialog.findViewById(R.id.large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }
    fun paintClicked(view : View){
        if(view!== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)

            )
            mImageButtonCurrentPaint=view

        }
    }


   private  fun showRationaleAlertDialog(
        title: String,
        message: String,
    ){
        val builder : AlertDialog.Builder =AlertDialog.Builder(this)
        builder.setTitle(title)
        .setMessage(message)
            .setCancelable(false)
            .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton("Cancel"){
            dialog, _->
            dialog.dismiss()

        }

        builder.create().show()
    }

    private fun isReadStorageAllowed() : Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionStorage(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
        )){
            showRationaleAlertDialog("Allow Access to Media", "To edit your photos, MinCanvas needs access to storage on your device ")
        }else{
            requestPermission.launch(
                arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            )
        }
    }


    private fun getBitmapFromView(view : View) : Bitmap{
        val removedBitmap = Bitmap.createBitmap(view.width,
            view.height, Bitmap.Config.ARGB_8888 )
        val canvas = Canvas(removedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return removedBitmap

    }



    private suspend fun saveBitmapFiles(mBitmap: Bitmap) : String {
        var result =""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){}

            try {
                val bytes =  ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.PNG, 90,bytes)

                val f = File(externalCacheDir?.absoluteFile.toString() + File.separator +
                "MinCanvas_" + System.currentTimeMillis()/1000 + ".png")

                val fo = FileOutputStream(f)
                fo.write(bytes.toByteArray())
                fo.close()


                result = f.absolutePath
                 runOnUiThread{
                     dismissProgressBar()
                     if(result.isNotEmpty()){
                         Toast.makeText(this@MainActivity,"File Saved Successfully :$result", Toast.LENGTH_LONG).show()
                         share(result)
                     }else{
                         Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_LONG).show()
                     }
                 }

            }
            catch (e:Exception){
                result = ""
                e.printStackTrace()
            }

        }
        return result
    }

    private fun showProgressBar(){
         customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun dismissProgressBar(){
        if(customProgressDialog!=null){
            customProgressDialog?.dismiss()
        }
        customProgressDialog=null
    }


    private fun share(result : String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }

    }

}