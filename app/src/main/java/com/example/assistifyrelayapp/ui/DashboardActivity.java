package com.example.assistifyrelayapp.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.assistifyrelayapp.LocalStorage.LocalTransactionStorage;
import com.example.assistifyrelayapp.R;
import com.example.assistifyrelayapp.core.jsonclass.TransactionData;
import com.example.assistifyrelayapp.session.SessionController;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    static List<TransactionData> messages; // Now used for SMS message history
    int totalSessions = 0;
    int totalSmsSent = 0;

    TextView totalTransactionsTextView, totalAmountTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        totalTransactionsTextView = findViewById(R.id.totalTransactions);
        totalAmountTextView = findViewById(R.id.totalAmount);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashBord_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            loadMessages();
            return insets;
        });
    }

    private void addTransactionCard(String sessionId, String body, String receivedAt, String status) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 20, 0, 0);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16f);
        cardView.setCardElevation(8f);
        cardView.setCardBackgroundColor(Color.parseColor("#F0FFFF"));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(32, 24, 32, 24);

        TextView sessionText = new TextView(this);
        sessionText.setText("Session #" + sessionId);
        sessionText.setTextSize(17f);
        sessionText.setTypeface(null, Typeface.BOLD);
        sessionText.setTextColor(Color.BLACK);

        TextView bodyText = new TextView(this);
        bodyText.setText("SMS: " + body);
        bodyText.setTextSize(16f);
        bodyText.setTextColor(Color.parseColor("#2ECC71"));

        TextView timeText = new TextView(this);
        timeText.setText(receivedAt);
        timeText.setTextSize(14f);
        timeText.setTextColor(Color.GRAY);

        TextView textStatus = new TextView(this);
        textStatus.setText(status);
        textStatus.setTextSize(14f);
        if (status.equalsIgnoreCase("Sent") || status.equalsIgnoreCase("Received"))
            textStatus.setTextColor(Color.parseColor("#2ECC71"));
        else
            textStatus.setTextColor(Color.RED);

        innerLayout.addView(sessionText);
        innerLayout.addView(bodyText);
        innerLayout.addView(timeText);
        innerLayout.addView(textStatus);
        cardView.addView(innerLayout);

        LinearLayout container = findViewById(R.id.transactionListContainer);
        container.addView(cardView);
    }

    private void loadMessages() {
        totalSessions =   SessionController.getInstance(getApplicationContext()).getActiveSessions().size();
        totalSmsSent = 0;

        messages = LocalTransactionStorage.getAllTransactions(this);

        LinearLayout container = findViewById(R.id.transactionListContainer);
        container.removeAllViews();

        for (TransactionData msg : messages) {
            addTransactionCard(msg.getSessionId(), msg.getBody(), msg.getReceivedAt(), msg.getStatus());
            totalSmsSent++;
            // Assume sessions list is unique or otherwise count as needed

        }

        totalTransactionsTextView.setText(String.valueOf(totalSessions));
        totalAmountTextView.setText(String.valueOf(totalSmsSent));
    }

    public void onclearDataBtnClicked(View view) {
        LocalTransactionStorage.clearAllTransactions(getApplicationContext());
        LinearLayout container = findViewById(R.id.transactionListContainer);
        container.removeAllViews();
        totalSessions = 0;
        totalSmsSent = 0;
        totalTransactionsTextView.setText("0");
        totalAmountTextView.setText("0");
        Toast.makeText(DashboardActivity.this, "All SMS/session data cleared ✅", Toast.LENGTH_SHORT).show();
    }
}
