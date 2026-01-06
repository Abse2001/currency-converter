package com.example.moneyconverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.moneyconverter.databinding.FragmentHistoryBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private final String API_KEY = "fca_live_RHtflr2rycR49il1mOTE7uoWf0gg92hOGDfNhsJI";
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
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
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.fromCurrencySpinnerHistory.setAdapter(adapter);
        binding.toCurrencySpinnerHistory.setAdapter(adapter);

        binding.viewHistoryButton.setOnClickListener(v -> {
            String fromCurrency = binding.fromCurrencySpinnerHistory.getSelectedItem().toString();
            String toCurrency = binding.toCurrencySpinnerHistory.getSelectedItem().toString();
            fetchHistoricalData(fromCurrency, toCurrency);
        });

    }

    private void fetchHistoricalData(String fromCurrency, String toCurrency) {
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        RequestQueue queue = Volley.newRequestQueue(getContext());
        AtomicInteger requestsFinished = new AtomicInteger(0);
        int numberOfDays = 3;

        for (int i = 1; i <= numberOfDays; i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -i);
            Date date = calendar.getTime();
            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
            dates.add(new SimpleDateFormat("MMM dd", Locale.getDefault()).format(date));
            int finalI = i;

            String rateKey = fromCurrency + "_" + toCurrency + "_" + dateString;

            if (sharedPreferences.contains(rateKey)) {
                float rate = sharedPreferences.getFloat(rateKey, 0f);
                entries.add(new Entry(finalI, rate));
                if (requestsFinished.incrementAndGet() == numberOfDays) {
                    updateChart(entries, dates, fromCurrency, toCurrency);
                }
            } else {
                String url = "https://api.freecurrencyapi.com/v1/historical?apikey=" + API_KEY + "&date=" + dateString + "&base_currency=" + fromCurrency + "&currencies=" + toCurrency;
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response -> {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                JSONObject data = jsonObject.getJSONObject("data").getJSONObject(dateString);
                                double rate = data.getDouble(toCurrency);

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putFloat(rateKey, (float) rate);
                                editor.apply();

                                entries.add(new Entry(finalI, (float) rate));

                                if (requestsFinished.incrementAndGet() == numberOfDays) {
                                    updateChart(entries, dates, fromCurrency, toCurrency);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                            }
                        }, error -> {
                    Toast.makeText(getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
                });
                queue.add(stringRequest);
            }
        }
    }

    private void updateChart(List<Entry> entries, List<String> dates, String fromCurrency, String toCurrency) {
        Collections.sort(entries, (o1, o2) -> Float.compare(o1.getX(), o2.getX()));
        Collections.reverse(dates);

        LineDataSet dataSet = new LineDataSet(entries, "Exchange Rate (" + fromCurrency + " to " + toCurrency + ")");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.CYAN);

        LineData lineData = new LineData(dataSet);
        binding.historyChart.setData(lineData);

        XAxis xAxis = binding.historyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dates.get((int) value - 1);
            }
        });

        float minRate = Collections.min(entries, (o1, o2) -> Float.compare(o1.getY(), o2.getY())).getY();
        float maxRate = Collections.max(entries, (o1, o2) -> Float.compare(o1.getY(), o2.getY())).getY();
        float padding = (maxRate - minRate) * 0.1f;

        YAxis leftAxis = binding.historyChart.getAxisLeft();
        leftAxis.setAxisMinimum(minRate - padding);
        leftAxis.setAxisMaximum(maxRate + padding);

        binding.historyChart.getAxisRight().setEnabled(false);
        binding.historyChart.getDescription().setEnabled(false);
        binding.historyChart.invalidate(); // refresh
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
