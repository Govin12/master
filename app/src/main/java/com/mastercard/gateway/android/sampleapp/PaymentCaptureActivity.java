package com.mastercard.gateway.android.sampleapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonParseException;
import com.mastercard.gateway.android.sdk.CommsException;
import com.mastercard.gateway.android.sdk.CommsTimeoutException;
import com.mastercard.gateway.android.sdk.GatewayErrorException;
import com.mastercard.gateway.android.sdk.ValidationException;
import com.mastercard.gateway.android.sdk2.Gateway;
import com.mastercard.gateway.android.sdk2.api.GatewayCallback;
import com.mastercard.gateway.android.sdk2.api.UpdateSessionResponse;

import java.net.MalformedURLException;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static android.text.TextUtils.isEmpty;

public class PaymentCaptureActivity extends AbstractActivity {
    @Bind(R.id.sessionText)
    TextView sessionField;

    @Bind(R.id.nameOnCard)
    EditText nameOnCardField;

    @Bind(R.id.cardnumber)
    EditText cardNumberField;

    @Bind(R.id.expiry_month)
    EditText expiryMonthField;

    @Bind(R.id.expiry_year)
    EditText expiryYearField;

    @Bind(R.id.cvv)
    EditText cvvField;

    @Bind(R.id.submitButton)
    Button submitButton;

    Gateway gateway = new Gateway();

    private SharedPreferences prefs = null;

    protected String productId, nameOnCard, cardNumber, expiryMM, expiryYY, cvv, sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        gateway.setMerchantId(BuildConfig.MERCHANT_ID)
                .setApiEndpoint(BuildConfig.GATEWAY_URL);

        sessionField.setText(getIntent().getStringExtra("SESSION_ID"));
        nameOnCardField.requestFocus();

        submitButton.setEnabled(false);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_capture_payment_details;
    }

    @OnClick(R.id.submitButton)
    public void buyClicked(View submitButton) {
        productId = getIntent().getStringExtra("PRODUCT_ID");
        nameOnCard = this.nameOnCardField.getText().toString();
        cardNumber = this.cardNumberField.getText().toString();
        expiryMM = this.expiryMonthField.getText().toString();
        expiryYY = this.expiryYearField.getText().toString();
        cvv = this.cvvField.getText().toString();
        sessionId = sessionField.getText().toString();

        Log.i(getClass().getSimpleName(), "Making purchase");
        Log.i("Product ID", productId);

        submitButton.setEnabled(false);

        gateway.updateSessionWithPayerData(sessionId, nameOnCard, cardNumber, cvv, expiryMM, expiryYY, new UpdateSessionCallback());
    }

    private String maskedCardNumber() {
        int maskLen = cardNumber.length() - 4;
        char[] mask = new char[maskLen];
        Arrays.fill(mask, '*');
        return new String(mask) + cardNumber.substring(maskLen);
    }


    class UpdateSessionCallback implements GatewayCallback<UpdateSessionResponse> {

        @Override
        public void onSuccess(UpdateSessionResponse updateSessionResponse) {
            Intent intent = new Intent(PaymentCaptureActivity.this, PayActivity.class);
            intent.putExtra("PRODUCT_ID", productId);
            intent.putExtra("PAN_MASK", maskedCardNumber());
            intent.putExtra("SESSION_ID", sessionField.getText().toString());
            Log.i(PaymentCaptureActivity.class.getSimpleName(), "Successful pay");

            startActivity(intent);
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(PaymentCaptureActivity.class.getSimpleName(), throwable.getMessage(), throwable);

            if (throwable instanceof MalformedURLException) {
                startResultActivity(R.string.update_commserror_text,
                        R.string.update_commserror_explanation_badurl);
            } else if (throwable instanceof CommsTimeoutException) {
                startResultActivity(R.string.update_timeout_text,
                        R.string.update_timeout_explanation);
            } else if (throwable instanceof CommsException) {
                startResultActivity(R.string.update_commserror_text,
                        R.string.update_commserror_explanation);
            } else if (throwable instanceof JsonParseException) {
                startResultActivity(R.string.update_malformed_text,
                        R.string.update_malformed_explanation_parse);
            } else if (throwable instanceof GatewayErrorException) {
                GatewayErrorException gee = (GatewayErrorException) throwable;
                startResultActivity(R.string.update_error_text,
                        gee.getResponse().getError().getExplanation());
            } else if (throwable instanceof ValidationException) {
                startResultActivity(R.string.update_malformed_text,
                        getResources().getString(R.string.update_malformed_explanation_missing) +
                                ((ValidationException) throwable).path);
            } else {
                Log.e(PaymentCaptureActivity.class.getSimpleName(),
                        "Unexpected error type " + throwable.getClass().getName());

                startResultActivity(R.string.update_unknown_error_text,
                        R.string.update_unknown_error_explanation);
            }
        }
    }

    @OnTextChanged({R.id.nameOnCard, R.id.cardnumber, R.id.expiry_month, R.id.expiry_year, R.id.cvv})
    public void enableSubmitButton() {
        if (isEmpty(nameOnCardField.getText()) || isEmpty(cardNumberField.getText())
                || isEmpty(expiryMonthField.getText()) || isEmpty(expiryYearField.getText())
                || isEmpty(cvvField.getText()) || (cvvField.getText().toString().length() < 3)) {

            submitButton.setEnabled(false);
        } else {

            submitButton.setEnabled(true);
        }
    }
}