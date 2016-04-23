package psalata.sailboat;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import psalata.sailboat.android.widget.VerticalSeekbar;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener{

    Drawable seekbarThumb;
    VerticalSeekbar seekbar1, seekbar2, seekbar3;
    BluetoothHandler bluetoothHandler;
    TextView appNameTextView;
    TextView batteryStateTextView;
    ImageButton voltageRequestButton;
    CountDownLatch lock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lock = new CountDownLatch(1);

        seekbar1 = (VerticalSeekbar) findViewById(R.id.seekbarServo1);
        seekbar1.setOnSeekBarChangeListener(this);
        seekbar2 = (VerticalSeekbar) findViewById(R.id.seekbarServo2);
        seekbar2.setOnSeekBarChangeListener(this);
        seekbar3 = (VerticalSeekbar) findViewById(R.id.seekbarServo3);
        seekbar3.setOnSeekBarChangeListener(this);
        seekbarThumb = ResourcesCompat.getDrawable(getResources(),
                R.drawable.seekbar_thumb, null);

        appNameTextView = (TextView) findViewById(R.id.appName);
        appNameTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Bulldozer.ttf"));
        batteryStateTextView = (TextView) findViewById(R.id.batteryState);

        voltageRequestButton = (ImageButton) findViewById(R.id.voltageRequestButton);
        voltageRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeViaBluetooth(254, 254);
            }
        });


        bluetoothHandler = new BluetoothHandler(this);
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bluetoothHandler.reset();
    }


    public void writeViaBluetooth(int servoNumber, int angle){
        if(servoNumber != 254) {
            Log.d("Info", "Sending value " + angle + " to servo " + servoNumber);
        } else {
            Log.d("Info", "Sending request to check battery state.");
        }

        byte[] messageArrayBytes = ByteBuffer.allocate(8).putInt(servoNumber).putInt(angle).array();

        bluetoothHandler.write(messageArrayBytes);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seekbarServo1:
                writeViaBluetooth(1, progress);
                break;
            case R.id.seekbarServo2:
                writeViaBluetooth(2, progress);
                break;
            case R.id.seekbarServo3:
                writeViaBluetooth(3, progress);
                break;
        }

    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seekbarThumb.setBounds(0, 0, seekbarThumb.getIntrinsicWidth(),
                seekbarThumb.getIntrinsicHeight());
        seekBar.setThumb(seekbarThumb);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }
}
