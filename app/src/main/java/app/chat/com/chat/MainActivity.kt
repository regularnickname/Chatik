package app.chat.com.chat

import adapters.MessageAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import models.Message
import models.RSA
import java.io.File
import java.util.*




class MainActivity : AppCompatActivity() {

    var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initFirebase()
        
        setupSendButton()

        createFirebaseListener()

        try {
            if (!File(RSA.PRIVATE_KEY_FILE).exists() || !File(RSA.PUBLIC_KEY_FILE).exists()) {
                RSA.generateKey()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }


    }


    private fun initFirebase() {
        FirebaseApp.initializeApp(applicationContext)

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)

        databaseReference = FirebaseDatabase.getInstance().reference
    }

    private fun createFirebaseListener(){
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val toReturn: ArrayList<Message> = ArrayList();

                for(data in dataSnapshot.children){
                    val messageData = data.getValue<Message>(Message::class.java)

                    val message = messageData?.let { it } ?: continue

                    toReturn.add(message)
                }

                toReturn.sortBy { message ->
                    message.timestamp
                }

                setupAdapter(toReturn)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        databaseReference?.child("messages")?.addValueEventListener(postListener)
    }


    private fun setupAdapter(data: ArrayList<Message>){
        val linearLayoutManager = LinearLayoutManager(this)
        mainActivityRecyclerView.layoutManager = linearLayoutManager
        mainActivityRecyclerView.adapter = MessageAdapter(data) {
            Toast.makeText(this, "${it.text} clicked", Toast.LENGTH_SHORT).show()
        }

        mainActivityRecyclerView.scrollToPosition(data.size - 1)
    }


    private fun setupSendButton() {
        mainActivitySendButton.setOnClickListener {
            if (!mainActivityEditText.text.toString().isEmpty()){
                sendData()
            }else{
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun sendData(){

        databaseReference?.
                child("messages")?.
                child(java.lang.String.valueOf(System.currentTimeMillis()))?.
                setValue(Message(RSA.byte2Hex(RSA.encrypt(mainActivityEditText.text.toString(), RSA.restorePublic()))))

        mainActivityEditText.setText("")
    }
}
