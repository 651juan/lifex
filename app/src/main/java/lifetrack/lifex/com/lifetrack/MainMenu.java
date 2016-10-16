package lifetrack.lifex.com.lifetrack;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainMenu extends AppCompatActivity {

    private static final String LOG_TAG = "FTP Connection";
    EditText emailText;
    TextView responseView;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main_menu);

        responseView = (TextView) findViewById(R.id.responseView);
        emailText = (EditText) findViewById(R.id.emailText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Button queryButton = (Button) findViewById(R.id.queryButton);
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RetrieveFeedTask(getApplicationContext()).execute();
            }
        });
    }

    private class RetrieveFeedTask extends AsyncTask<Void, Void, String> {

        Context context;

        private RetrieveFeedTask(Context context) {
            this.context = context;
        }

        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            responseView.setText("");
        }

        protected String doInBackground(Void... urls) {
            Log.i("in Background", "now");
            try {
                File saveto = File.createTempFile("result", null, getApplicationContext().getCacheDir());
                if (!saveto.exists()) {
                    try {
                        saveto.createNewFile();
                    } catch (IOException e) {
                        Log.e(e.getMessage(), e.toString());
                    }
                }
                downloadAndSaveFile("78.133.21.48", 25, "juan", "juan", "result", saveto);
                return printfile(saveto);
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
            }
            return null;
        }

        protected void onPostExecute(String response) {
            progressBar.setVisibility(View.GONE);
            Log.i("INFO", response);
            responseView.setText(response);
            String rawData[] = response.split("\\r?\\n");
            Intent intent = new Intent(context, StepActivity.class);
            intent.putExtra("DATA", rawData);
            startActivity(intent);

        }


        private Boolean downloadAndSaveFile(String server, int portNumber, String user, String password, String filename, File localFile) throws IOException {
            FTPClient ftp = null;

            try {
                ftp = new FTPClient();
                ftp.connect(server, portNumber);
                Log.d(LOG_TAG, "Connected. Reply: " + ftp.getReplyString());

                ftp.login(user, password);
                Log.d(LOG_TAG, "Logged in");
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                Log.d(LOG_TAG, "Downloading");
                ftp.enterLocalPassiveMode();

                OutputStream outputStream = null;
                boolean success = false;
                try {
                    outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                    success = ftp.retrieveFile(filename, outputStream);
                } catch (Exception e) {
                    Log.i(e.getMessage(), e.toString());
                } finally {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }

                return success;
            } catch (Exception e) {
                Log.e("IOFail", e.getMessage());
            } finally {
                if (ftp != null) {
                    ftp.logout();
                    ftp.disconnect();
                }
            }
            return true;
        }

        private String printfile(File f) {
            StringBuilder sb = new StringBuilder();
            try {
                FileInputStream fis = new FileInputStream(f);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line + '\n');
                }

            } catch (IOException e) {
                Log.e(e.toString(), e.getMessage());
            }
            return sb.toString();
        }
    }

}
