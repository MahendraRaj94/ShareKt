package com.mahendra.myapplication

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.MediaStore.*
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.messages.NearbyPermissions
import com.kotlinpermissions.KotlinPermissions

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import kotlin.coroutines.coroutineContext
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.kotlinpermissions.notNull
import com.mahendra.myapplication.view.DiscoveredItemFragment
import com.mahendra.myapplication.view.discover.DummyContent
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.net.URI
import java.nio.charset.Charset
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(),DiscoveredItemFragment.OnListFragmentInteractionListener {
    final val SELECT_IMAGE = 100
    final val ENDPOINT_EXTRA : String = BuildConfig.APPLICATION_ID.plus("endpoint")

    override fun onListFragmentInteraction(item: DummyContent.DummyItem?) {

    }

    var endPoints : MutableList<String> = ArrayList<String>()

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        setSupportActionBar(toolbar)
//
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initViews()

    }

    private fun initViews() {
        textView.text = "Start Advertising"
        textView.setOnClickListener { v ->

            KotlinPermissions.with(this)
                .permissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                    , Manifest.permission.BLUETOOTH
                    , Manifest.permission.BLUETOOTH_ADMIN
                    , Manifest.permission.ACCESS_WIFI_STATE
                    , Manifest.permission.CHANGE_WIFI_STATE
                ,Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .onAccepted {
                    startAdvertising()
                    selectImage("")

                }.onDenied {

                }.ask()
        }
        fab.setOnClickListener { v ->
            startDiscovery()
//            supportFragmentManager
//                .beginTransaction()
//                .add(DiscoveredItemFragment.newInstance(1),DiscoveredItemFragment.javaClass.canonicalName)
//                .commit()

        }

        btnSend.setOnClickListener(View.OnClickListener {
            Log.d("EndPoints", endPoints.toString())
            Log.d("EndPoints", ""+endPoints.size)
            Nearby.getConnectionsClient(this).sendPayload(endPoints, Payload.fromBytes(etMessage.text.toString().toByteArray(
                Charset.defaultCharset())))
        })
    }

    fun selectImage(endPointId: String){
        val intent : Intent = Intent(Intent.ACTION_PICK)
        intent.putExtra(ENDPOINT_EXTRA,endPointId)
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("image/*")
        startActivityForResult(intent,SELECT_IMAGE)
    }

    fun startDiscovery(){
        val discoveryOptions : DiscoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        if(BluetoothAdapter.getDefaultAdapter() == null){
            Toast.makeText(this, "Bluetooth is required", Toast.LENGTH_SHORT).show()

        }else {
            if(BluetoothAdapter.getDefaultAdapter().isEnabled){
                Nearby.getConnectionsClient(this).stopDiscovery()
                Nearby.getConnectionsClient(this)
                    .startDiscovery(BuildConfig.APPLICATION_ID,mDiscoveryCallback,discoveryOptions)
                    .addOnCompleteListener { a ->
                    }.addOnSuccessListener { a ->
                        Log.d("Nearby ","Started Discovering")
                        Toast.makeText(this, "Started Discovering", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { a ->
                        a.printStackTrace()
                        Log.d("Nearby ","Failed Discovering")

                    }
            }else{
                Toast.makeText(this, "Turn on bluetooth and wifi", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private val mDiscoveryCallback : EndpointDiscoveryCallback = object : EndpointDiscoveryCallback(){
        override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {
            Log.d("onEndpointFound", "End point -> ${p0}")
            Log.d("onEndpointFound", "End point Name -> ${p1.endpointName}")
            Log.d("onEndpointFound", "Service Id -> ${p1.serviceId}")
            requestionConnection(p0)
        }

        override fun onEndpointLost(p0: String) {
            Log.d("onEndpointLost", "End point -> ${p0}")
        }


    }

    private val mConnectionLifeCycleCallback : ConnectionLifecycleCallback = object : ConnectionLifecycleCallback(){
        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
            Log.d("onConnectionResult", "End point -> ${p0}")
            Log.d("onConnectionResult", "End point status -> ${p1.status}")
        }

        override fun onDisconnected(p0: String) {
            Log.d("onDisconnected", "End point -> ${p0}")

        }

        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            Log.d("onConnectionInitiated", "End point -> ${p0}")
            if(!endPoints.contains(p0)) {
                endPoints.add(p0)
            }
            establishConnection(p0,p1)
        }

    }

    private fun requestionConnection(endPointId: String){
        Nearby.getConnectionsClient(this).requestConnection("Mahendra",endPointId,mConnectionLifeCycleCallback)
    }

    private val mPayloadCallback : PayloadCallback = object  :  PayloadCallback(){
        override fun onPayloadReceived(p0: String, p1: Payload) {
            Log.d("onPayloadReceived", "".plus(p1.asFile()!!.size))
            val file : File = p1.asFile()!!.asJavaFile()!!
            if(file.exists()){
                val bitmap : Bitmap = BitmapFactory.decodeFile(file.absolutePath)
                bitmap.notNull {
                    imageView.setImageBitmap(bitmap)
                }
            }

//            Toast.makeText(this@MainActivity, String(p1.asBytes()!!),Toast.LENGTH_SHORT).show()
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            Log.d("onPayloadTransferUpdate", "End point -> ${p0}")
            Log.d("onPayloadTransferUpdate", "End point Total bytes Update -> ${p1.totalBytes}")
        }

    }

    private fun establishConnection(endPointId: String, p1: ConnectionInfo) {
        Nearby.getConnectionsClient(this).acceptConnection(endPointId,mPayloadCallback)
    }
    fun startAdvertising(){

        val advertisingOptions : AdvertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build();
        if(BluetoothAdapter.getDefaultAdapter() == null){
            Toast.makeText(this, "Bluetooth is required", Toast.LENGTH_SHORT).show()

        }else {
            if(BluetoothAdapter.getDefaultAdapter().isEnabled){
                Nearby.getConnectionsClient(this).stopAdvertising()
                val endpoint : String = UUID.randomUUID().toString()
                Nearby.getConnectionsClient(this)
                    .startAdvertising(endpoint, BuildConfig.APPLICATION_ID, mConnectionLifeCycleCallback , advertisingOptions)
                    .addOnCompleteListener { a ->
                    }.addOnSuccessListener { a ->
                        Toast.makeText(this, "Started Advertising", Toast.LENGTH_SHORT).show()
                        Log.d("Nearby ","Started Advertising")
                    }.addOnFailureListener { a ->
                        if(a is ApiException){
                            Log.d("ApiException ",a.statusMessage)
                            Log.d("ApiException ","".plus(a.statusCode))
                        }
                        a.printStackTrace()
                        Log.d("Nearby ","Failed Advertising")

                    }
            }else{
                Toast.makeText(this, "Turn on bluetooth and wifi", Toast.LENGTH_SHORT).show()

            }

        }
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this,SecondActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK && data != null){
//             var endpointId : String = data.getStringExtra(ENDPOINT_EXTRA)
           data.data.notNull {
//                val descriptor : ParcelFileDescriptor = contentResolver.openFileDescriptor(data.data,"r")
               val payload = Payload.fromFile(File(getPathFromUri(data.data)))
               Nearby.getConnectionsClient(this).sendPayload(endPoints,payload)
           }

        }
    }


    fun getPathFromUri(uri: Uri) : String{
        var s : Array<String> = Array(1,init = {Images.Media.DATA})
        val cursor : Cursor = this.contentResolver.query(uri,s,null,null,null)
        if(cursor.moveToFirst()){
            val column_index : Int = cursor.getColumnIndexOrThrow(Images.Media.DATA)
           return cursor.getString(column_index)
        }
        return ""
    }
}
