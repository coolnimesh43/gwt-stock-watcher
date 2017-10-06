package org.coolnimesh.stock.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
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
    public Label errorMessageLabel = new Label();

    private List<String> stocks = new ArrayList<>();
    private int REFRESH_INTERVAL = 5000;

    private static final double MAX_PRICE = 100.00;
    private static final double MAX_PRICE_CHANGE = 0.02;
    private static final NumberFormat CHANGE_FORMAT = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
    private static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);
    private static final String JSON_DATA_URL = com.google.gwt.core.client.GWT.getModuleBaseURL() + "stockPrices?q=nimesh+mishra";

    private StockPriceServiceAsync stockPriceServiceAsync = GWT.create(StockPriceService.class);

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
        addPanel.addStyleName("addPanel");

        // assemble main panel

        errorMessageLabel.addStyleName("errorMessage");
        errorMessageLabel.setVisible(false);

        mainPanel.add(errorMessageLabel);
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
        if (stocks == null || stocks.size() == 0) {
            return;
        }
        String url = JSON_DATA_URL;
        Iterator<String> iterator = stocks.iterator();
        while (iterator.hasNext()) {
            url += iterator.next();
            if (iterator.hasNext()) {
                url += "+";
            }
        }

        url = URL.encode(url);
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
        try {
            Request request = builder.sendRequest(null, new RequestCallback() {

                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        updateStockTable(JsonUtils.<JsArray<StockData>> safeEval(response.getText()));
                    } else {
                        displayError("Couldn't retrieve JSON (" + response.getStatusText() + ")");
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    displayError("Couldn't retrieve JSON ");

                }
            });
        } catch (RequestException e) {
            displayError("Exception while fetching stock data from the server.");
            e.printStackTrace();
        }
        // RPC style call.
        // if (stockPriceServiceAsync == null) {
        // stockPriceServiceAsync = GWT.create(StockPriceService.class);
        // }
        // StockPriceCallBack stockPriceCallBack = new StockPriceCallBack(this);
        // stockPriceServiceAsync.getPrices(stocks.toArray(new String[0]), stockPriceCallBack);
    }

    private void displayError(String error) {
        errorMessageLabel.setText("Error: " + error);
        errorMessageLabel.setVisible(true);
    }

    private void updateStock(StockData price) {
        if (price == null || price.getSymbol() == null || !stocks.contains(price.getSymbol())) {
            return;
        }
        int row = stocks.indexOf(price.getSymbol()) + 1;
        String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
        String changeText = CHANGE_FORMAT.format(price.getChange());
        String changePercentText = CHANGE_FORMAT.format(price.getChangePercent());

        stocksFlexTable.setText(row, 1, priceText);
        Label changeWidget = (Label) stocksFlexTable.getWidget(row, 2);
        changeWidget.setText(changeText + " (" + changePercentText + "%)");

        String changeStyle = "noChange";
        if (price.getChangePercent() < -0.1) {
            changeStyle = "negativeChange";
        } else if (price.getChangePercent() > 0.1) {
            changeStyle = "positiveChange";
        }
        changeWidget.setStyleName(changeStyle);
        // stocksFlexTable.setText(row, 2, changeText + " (" + changePercentText + "%)");
    }

    public void updateStockTable(JsArray<StockData> stockPrices) {
        if (stockPrices != null && stockPrices.length() > 0) {
            for (int i = 0; i < stockPrices.length(); i++) {
                if (stockPrices.get(i) != null) {
                    updateStock(stockPrices.get(i));
                }
            }

            lastUpdatedLabel.setText("Last Updated on : " + DATE_FORMAT.format(new Date()));
            errorMessageLabel.setVisible(false);
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
            stocksFlexTable.setWidget(row, 2, new Label());
            stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
            stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
            stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
            // add remove button.
            Button removeStockButton = new Button("X");
            removeStockButton.addStyleDependentName("remove");
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
        stocksFlexTable.setCellPadding(6);
        // add header style
        stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
        stocksFlexTable.addStyleName("watchList");
        stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");
    }
}
