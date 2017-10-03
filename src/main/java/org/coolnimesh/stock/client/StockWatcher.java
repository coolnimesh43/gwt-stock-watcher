package org.coolnimesh.stock.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockWatcher implements EntryPoint {
    // Widgets
    private VerticalPanel mainPanel = new VerticalPanel();
    private FlexTable stocksFlexTable = new FlexTable();
    private HorizontalPanel addPanel = new HorizontalPanel();
    private TextBox newStockTextBox = new TextBox();
    private Button addStockButton = new Button("Add");
    private Label lastUpdatedLabel = new Label();

    private List<String> stocks = new ArrayList<>();
    private int REFRESH_INTERVAL = 5000;

    private static final double MAX_PRICE = 100.00;
    private static final double MAX_PRICE_CHANGE = 0.02;
    private static final NumberFormat CHANGE_FORMAT = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
    private static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);

    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        // create flex table with header.
        createTable();
        // Assemble Add Stock panel
        addPanel.add(newStockTextBox);
        addPanel.add(addStockButton);

        // assemble main panel
        mainPanel.add(stocksFlexTable);
        mainPanel.add(addPanel);
        mainPanel.add(lastUpdatedLabel);

        // add main panel to the HTML host page.
        RootPanel.get("stockList").add(mainPanel);

        newStockTextBox.setFocus(Boolean.TRUE);

        // add click handler for add button
        addStockButton.addClickHandler(e -> {
            addStock();
        });
        // add enter handler for text box
        newStockTextBox.addKeyDownHandler(e -> {
            if (e.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                addStock();
            }
        });

        // add Timer
        Timer refreshTimer = new Timer() {
            @Override
            public void run() {
                refreshWatchList();
            }
        };
        refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
    }

    private void refreshWatchList() {
        StockPrice[] prices = new StockPrice[stocks.size()];
        for (int i = 0; i < stocks.size(); i++) {
            double price = Random.nextDouble() * MAX_PRICE;
            double change = price * MAX_PRICE_CHANGE * (Random.nextDouble() * 2.0 - 1.0);
            prices[i] = new StockPrice(stocks.get(i), price, change);
        }
        updateStockTable(prices);
    }

    private void updateStock(StockPrice price) {
        if (!stocks.contains(price.getSymbol())) {
            return;
        }
        int row = stocks.indexOf(price.getSymbol()) + 1;
        String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
        String changeText = CHANGE_FORMAT.format(price.getChange());
        String changePercentText = CHANGE_FORMAT.format(price.getChangePercent());

        stocksFlexTable.setText(row, 1, priceText);
        stocksFlexTable.setText(row, 2, changeText + " (" + changePercentText + "%)");
    }

    private void updateStockTable(StockPrice[] stockPrices) {
        if (stockPrices != null && stockPrices.length > 0) {
            for (int i = 0; i < stockPrices.length; i++) {
                updateStock(stockPrices[i]);
            }

            lastUpdatedLabel.setText("Last Updated on : " + DATE_FORMAT.format(new Date()));
        }
    }

    private void addStock() {
        if (validateAndSanitizeInput()) {
            String input = newStockTextBox.getText().toUpperCase().trim();
            newStockTextBox.setText("");

            // add input to table.
            int row = stocksFlexTable.getRowCount();
            stocks.add(input);
            stocksFlexTable.setText(row, 0, input);
            // add remove button.
            Button removeStockButton = new Button("X");
            removeStockButton.addClickHandler(e -> {
                int removedIndex = stocks.indexOf(input);
                stocks.remove(input);
                stocksFlexTable.removeRow(removedIndex + 1);
            });

            stocksFlexTable.setWidget(row, 3, removeStockButton);

            refreshWatchList();
        }
    }

    private Boolean validateAndSanitizeInput() {
        // get the input
        final String input = newStockTextBox.getText().toUpperCase().trim();
        newStockTextBox.setFocus(true);

        // validate the input
        if (!input.matches("^[0-9A-Z\\.]{1,10}$")) {
            com.google.gwt.user.client.Window.alert("' " + input + " ' is not a valid input");
            newStockTextBox.selectAll();
            return false;
        }
        if (stocks.contains(input)) {
            Window.alert("The stock ' " + input + " ' already exists.");
            newStockTextBox.selectAll();
            return false;
        }
        return true;
    }

    private void createTable() {
        stocksFlexTable.setText(0, 0, "Symbol");
        stocksFlexTable.setText(0, 1, "Price");
        stocksFlexTable.setText(0, 2, "Change");
        stocksFlexTable.setText(0, 3, "Remove");
    }
}
