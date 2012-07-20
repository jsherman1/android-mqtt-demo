package org.example.mqtt;

import java.net.URISyntaxException;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import org.example.mqtt.R;

import android.app.Activity;
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
	
	EditText hostET = null;
	EditText portET = null;
	EditText destinationET = null;
	EditText messageET = null;
	EditText receiveET = null;
	EditText userNameET = null;
	EditText passwordET = null;
	
	Button connectButton = null;
	Button disconnectButton = null;
	Button sendButton = null;
	
	String sHost = null;
	String sPort = null;
	String sUserName = null;
	String sPassword = null;
	String sDestination = null;
	String sMessage = null;
	
	MQTT mqtt = null;
	
	BlockingConnection connection = null;
	
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
    	
    	hostET = (EditText)findViewById(R.id.hostEditText);
    	portET = (EditText)findViewById(R.id.portEditText);
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
			sHost = hostET.getText().toString().trim();
			sPort = portET.getText().toString().trim();
			sUserName = userNameET.getText().toString().trim();
			sPassword = passwordET.getText().toString().trim();
			
			Log.d(TAG, "UserName: " + sUserName + " Password: " + sPassword);
			
			if(sHost.equals(""))
			{
				Toast.makeText(this,"Host must be provided", Toast.LENGTH_LONG).show();
			}
			else if (sPort.equals(""))
			{
				Toast.makeText(this,"Port must be provided", Toast.LENGTH_LONG).show();
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
				Toast.makeText(this,"Destination must be provided", Toast.LENGTH_LONG).show();
			}
			else
			{
				send();
			}	
		}
	}
	
	private void connect()
	{
		mqtt = new MQTT();

		try
		{
			mqtt.setHost("tcp://" + sHost + ":" + Integer.parseInt(sPort));
			
			if(sUserName != null || !sUserName.equals(""))
			{
				mqtt.setUserName(sUserName);
				Log.d(TAG, "UserName set: " + sUserName);
			}
			
			if(sPassword != null || !sPassword.equals(""))
			{
				mqtt.setPassword(sPassword);
				Log.d(TAG, "Password set: " + sPassword);
			}
			
			connection = mqtt.blockingConnection();
			connection.connect();
			connectButton.setEnabled(false);
			Toast.makeText(this,"Connected", Toast.LENGTH_LONG).show();
		}
		catch(URISyntaxException urise)
		{
			Log.e(TAG, "URISyntaxException connecting to " + sHost + ":" + sPort + " - " + urise);
		}
		catch(Exception e)
		{
			Log.e(TAG, "Exception connecting to " + sHost + ":" + sPort + " - " + e);
		}
	}
	
	private void disconnect()
	{
		connectButton.setEnabled(true);
		try
		{
			if(connection != null && connection.isConnected())
			{
				connection.disconnect();
				Toast.makeText(this,"Disconnected", Toast.LENGTH_LONG).show();;
			}
			else
			{
				Toast.makeText(this,"Not Connected", Toast.LENGTH_SHORT).show();
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "Exception " + e);
		}
	}
	
	private void send()
	{
		try
		{
			if(!connection.isConnected())
			{
				connect();
			}
			
			// setup receiver
			Topic[] topics = {new Topic(sDestination, QoS.AT_LEAST_ONCE)};
			byte[] subscription = connection.subscribe(topics);
			
			// publish message
			connection.publish(sDestination, sMessage.getBytes(), QoS.AT_LEAST_ONCE, false);
			destinationET.setText("");
			messageET.setText("");
			Toast.makeText(this,"Message sent", Toast.LENGTH_LONG).show();
			
			// receive message
			Message message = connection.receive();
			String receivedMesageTopic = message.getTopic();
			byte[] payload = message.getPayload();
			String messagePayLoad = new String(payload);
			message.ack();
			connection.unsubscribe(new String[]{sDestination});
			
			receiveET.setText(receivedMesageTopic + ":" + messagePayLoad);
		}
		catch(Exception e)
		{
			Log.e(TAG, "Exception sending message: " + e);
		}
	}
}