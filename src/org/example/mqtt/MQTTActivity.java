package org.example.mqtt;

import java.net.URISyntaxException;

import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.FutureConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import org.example.mqtt.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MQTTActivity extends Activity implements OnClickListener{
	
	private final String TAG = "MQTTClient";
	
	EditText addressET = null;
	EditText destinationET = null;
	EditText messageET = null;
	EditText receiveET = null;
	EditText userNameET = null;
	EditText passwordET = null;
	
	Button connectButton = null;
	Button disconnectButton = null;
	Button sendButton = null;
	
	private ProgressDialog progressDialog = null;
	
	String sAddress = null;
	String sUserName = null;
	String sPassword = null;
	String sDestination = null;
	String sMessage = null;
	
	MQTT mqtt = null;
	
	FutureConnection connection = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupView();
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	
    	disconnect();
    }
    
    public void setupView()
    {
    	// lock the screen in portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	
    	addressET = (EditText)findViewById(R.id.addressEditText);
    	addressET.setHint("tcp://192.168.1.15:1883");
    	userNameET = (EditText)findViewById(R.id.userNameEditText);
    	passwordET = (EditText)findViewById(R.id.passwordEditText);
    	destinationET = (EditText)findViewById(R.id.destinationEditText);
    	messageET = (EditText)findViewById(R.id.messageEditText);
    	receiveET = (EditText)findViewById(R.id.receiveEditText);
    	
    	connectButton = (Button)findViewById(R.id.connectButton);
    	connectButton.setOnClickListener(this);
    	
    	disconnectButton = (Button)findViewById(R.id.disconnectButton);
    	disconnectButton.setOnClickListener(this);
    	
    	sendButton = (Button)findViewById(R.id.sendButton);
    	sendButton.setOnClickListener(this);
    }

	public void onClick(View v) {
		if(v == connectButton)
		{
			sAddress = addressET.getText().toString().trim();
			sUserName = userNameET.getText().toString().trim();
			sPassword = passwordET.getText().toString().trim();
			
			if(sAddress.equals(""))
			{
				toast("Address must be provided");
			}
			else
			{
				connect();
			}
		}
		
		if(v == disconnectButton)
		{
			disconnect();
		}
		
		if(v == sendButton)
		{
			sDestination = destinationET.getText().toString().trim();
			sMessage = messageET.getText().toString().trim();
			
			// allow empty messages
			if(sDestination.equals(""))
			{
				toast("Destination must be provided");
			}
			else
			{
				send();
			}	
		}
	}
	
	// callback used for Future
	<T> Callback<T> onui(final Callback<T> original) {
		return new Callback<T>() {
			public void onSuccess(final T value) {
				runOnUiThread(new Runnable(){
					public void run() {
						original.onSuccess(value);
					}
				});
			}
			public void onFailure(final Throwable error) {
				runOnUiThread(new Runnable(){
					public void run() {
						original.onFailure(error);
					}
				});
			}
		};
	}
	
	private void connect()
	{
		mqtt = new MQTT();
		mqtt.setClientId("android-mqtt-example");

		try
		{
			mqtt.setHost(sAddress);
			Log.d(TAG, "Address set: " + sAddress);
		}
		catch(URISyntaxException urise)
		{
			Log.e(TAG, "URISyntaxException connecting to " + sAddress + " - " + urise);
		}
		
		if(sUserName != null && !sUserName.equals(""))
		{
			mqtt.setUserName(sUserName);
			Log.d(TAG, "UserName set: [" + sUserName + "]");
		}
		
		if(sPassword != null && !sPassword.equals(""))
		{
			mqtt.setPassword(sPassword);
			Log.d(TAG, "Password set: [" + sPassword + "]");
		}
		
		connection = mqtt.futureConnection();
		progressDialog = ProgressDialog.show(this, "", 
                "Connecting...", true);
		connection.connect().then(onui(new Callback<Void>(){
			public void onSuccess(Void value) {
				connectButton.setEnabled(false);
				progressDialog.dismiss();
				toast("Connected");
			}
			public void onFailure(Throwable e) {
				toast("Problem connecting to host");
				Log.e(TAG, "Exception connecting to " + sAddress + " - " + e);
				progressDialog.dismiss();
			}
		}));

	}
	
	private void disconnect()
	{
		connectButton.setEnabled(true);
		try
		{
			if(connection != null && connection.isConnected())
			{
				connection.disconnect().then(onui(new Callback<Void>(){
					public void onSuccess(Void value) {
						connectButton.setEnabled(true);
						toast("Disconnected");
					}
					public void onFailure(Throwable e) {
						toast("Problem disconnecting");
						Log.e(TAG, "Exception disconnecting from " + sAddress + " - " + e);
					}
				}));
			}
			else
			{
				toast("Not Connected");
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "Exception " + e);
		}
	}
	
	private void send()
	{
		if(connection != null)
		{
			// automatically connect if no longer connected
			if(!connection.isConnected())
			{
				connect();
			}
			
			Topic[] topics = {new Topic(sDestination, QoS.AT_LEAST_ONCE)};
			connection.subscribe(topics).then(onui(new Callback<byte[]>() {
				public void onSuccess(byte[] subscription) {
					
					Log.d(TAG, "Destination: " + sDestination);
					Log.d(TAG, "Message: " + sMessage);
					
					// publish message
					connection.publish(sDestination, sMessage.getBytes(), QoS.AT_LEAST_ONCE, false);
					destinationET.setText("");
					messageET.setText("");
					toast("Message sent");
					
					// receive message
					connection.receive().then(onui(new Callback<Message>() {
						public void onSuccess(Message message) {
							String receivedMesageTopic = message.getTopic();
							byte[] payload = message.getPayload();
							String messagePayLoad = new String(payload);
							message.ack();
							connection.unsubscribe(new String[]{sDestination});
							receiveET.setText(receivedMesageTopic + ":" + messagePayLoad);
						}
						
						public void onFailure(Throwable e) {
							Log.e(TAG, "Exception receiving message: " + e);
						}
					}));
					
				}
				
				public void onFailure(Throwable e) {
					Log.e(TAG, "Exception sending message: " + e);
				}
			}));
		}
		else
		{
			toast("No connection has been made, please create the connection");
		}
	}
	
	private void toast(String message)
	{
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
}