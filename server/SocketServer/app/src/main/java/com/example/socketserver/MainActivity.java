package com.example.socketserver;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private TextView tvServerIp, tvServerPort, tvNotConnection;
    private Button btnStart, btnStop, btnSendMess;

    private EditText edtMess;

    private String serverIp = "10.0.2.16";
    private int serVerPort = 12345;

    private ServerThread serverThread;
    private ClientThread clientThread;
    private Handler handler;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvServerIp = findViewById(R.id.tvServerIp);
        tvServerPort = findViewById(R.id.tvServerPort);
        tvNotConnection = findViewById(R.id.tvNoConnect);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSendMess = findViewById(R.id.btnSendMess);
        edtMess = findViewById(R.id.edtInputServer);
        btnSendMess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSendMess();
            }
        });
        handler = new Handler(Looper.getMainLooper());
    }

    private void onClickSendMess() {
        final String message = edtMess.getText().toString();
        if (!message.isEmpty() && clientThread != null) {
            // Tạo một luồng mới để thực hiện gửi tin nhắn
            new Thread(new Runnable() {
                @Override
                public void run() {
                    clientThread.sendToServer(message);
                }
            }).start();
        }
    }

    public void onClickStartServer(View v) {
        // Initialize and start the server thread
        serverThread = new ServerThread();
        serverThread.startServer();
    }

    public void onClickStopServer(View v) {
        if (serverThread != null) {
            // Stop the server thread
            serverThread.stopServer();
            edtMess.setVisibility(View.GONE);
            btnSendMess.setVisibility(View.GONE);
        }
    }

    class ServerThread extends Thread {
        private boolean serverRunning;
        private ServerSocket serverSocket;
        private int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(serVerPort);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvNotConnection.setText("Waiting for Clients");
                    }
                });
                while (serverRunning) {
                    Socket socket = serverSocket.accept();
                    count++;
                    clientThread = new ClientThread(socket, count);
                    clientThread.start();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvNotConnection.setText("Connected to: " + socket.getLocalPort() + " : " + socket.getLocalAddress());
                            edtMess.setVisibility(View.VISIBLE);
                            btnSendMess.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void startServer() {
            serverRunning = true;
            start();
        }

        public void stopServer() {
            serverRunning = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ClientThread extends Thread {
        private Socket socket;
        private PrintWriter clientWriter;
        private BufferedReader serverReader;

        public ClientThread(Socket _socket, int _clientServer) {
            this.socket = _socket;
            try {
                clientWriter = new PrintWriter(socket.getOutputStream(), true);
                serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendToServer(String message) {
            if (clientWriter != null) {
                clientWriter.println(message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Message sent to server: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void run() {
            try {
                String receivedMessage;
                while ((receivedMessage = serverReader.readLine()) != null) {
                    final String finalMessage = receivedMessage;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Hiển thị tin nhắn từ client sử dụng Toast
                            Toast.makeText(MainActivity.this, "Message from client: " + finalMessage, Toast.LENGTH_SHORT).show();
                            tvServerIp.setText("Client: " + finalMessage); // Gán tin nhắn vào tvServerIp
                        }
                    });
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
