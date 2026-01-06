package com.example.moneyconverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.moneyconverter.databinding.FragmentFirstBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private final String API_KEY = "fca_live_RHtflr2rycR49il1mOTE7uoWf0gg92hOGDfNhsJI";
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        sharedPreferences = getContext().getSharedPreferences("MoneyConverterPrefs", Context.MODE_PRIVATE);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<String> currencies = new ArrayList<>();
        currencies.add("USD");
        currencies.add("EUR");
        currencies.add("JPY");
        currencies.add("GBP");
        currencies.add("AUD");
        currencies.add("CAD");
        currencies.add("CHF");
        currencies.add("CNY");
        currencies.add("SEK");
        currencies.add("NZD");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, currencies);
        binding.fromCurrencySpinner.setAdapter(adapter);
        binding.toCurrencySpinner.setAdapter(adapter);

        binding.convertButton.setOnClickListener(v -> {
            String amountStr = binding.amountToConvert.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountStr);
            String fromCurrency = binding.fromCurrencySpinner.getText().toString();
            String toCurrency = binding.toCurrencySpinner.getText().toString();

            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String rateKey = fromCurrency + "_" + toCurrency;
            String dateKey = rateKey + "_date";

            if (sharedPreferences.contains(rateKey) && todayDate.equals(sharedPreferences.getString(dateKey, ""))) {
                float rate = sharedPreferences.getFloat(rateKey, 0f);
                double result = amount * rate;
                binding.conversionResult.setText(String.format("%.2f", result));
                binding.toCurrencyName.setText(toCurrency);
            } else {
                fetchAndCacheRate(amount, fromCurrency, toCurrency, rateKey, dateKey, todayDate);
            }
        });

        binding.historyButton.setOnClickListener(v -> {
            String fromCurrency = binding.fromCurrencySpinner.getText().toString();
            String toCurrency = binding.toCurrencySpinner.getText().toString();

            Bundle bundle = new Bundle();
            bundle.putString("fromCurrency", fromCurrency);
            bundle.putString("toCurrency", toCurrency);

            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_HistoryFragment, bundle);
        });
    }

    private void fetchAndCacheRate(double amount, String fromCurrency, String toCurrency, String rateKey, String dateKey, String todayDate) {
        String url = "https://api.freecurrencyapi.com/v1/latest?apikey=" + API_KEY + "&base_currency=" + fromCurrency + "&currencies=" + toCurrency;
        RequestQueue queue = Volley.newRequestQueue(getContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONObject data = jsonObject.getJSONObject("data");
                        double rate = data.getDouble(toCurrency);

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putFloat(rateKey, (float) rate);
                        editor.putString(dateKey, todayDate);
                        editor.apply();

                        double result = amount * rate;
                        binding.conversionResult.setText(String.format("%.2f", result));
                        binding.toCurrencyName.setText(toCurrency);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
            Toast.makeText(getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
        });
        queue.add(stringRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
