package lifetrack.lifex.com.lifetrack;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.*;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class StepActivity extends AppCompatActivity {
    private int max = 0;
    private int year = 0;
    private final int monthDataCount[] = new int[12];
    private final int dayDataCount[][] = new int[12][31];
    private final static String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",};

    private LineChartView chartTop;
    private ColumnChartView chartBottom;

    private LineChartData lineData;
    private ColumnChartData columnData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step);

        // gets data from extra and parses it
        getAndParseData();

        // *** TOP LINE CHART ***
        chartTop = (LineChartView) findViewById(R.id.chart_top);

        // Generate and set data for line chart
        generateInitialLineData();

        // *** BOTTOM COLUMN CHART ***

        chartBottom = (ColumnChartView) findViewById(R.id.chart_bottom);

        generateColumnData();
    }

    private void getAndParseData() {
        String rawData[] = getIntent().getStringArrayExtra("DATA");
        for (String s : rawData) {
            String sSplit[] = s.split("=");
            Date currentDate = StringToDate(sSplit[0]);
            int value = Integer.parseInt(sSplit[1].trim());

            if (value > max) {
                max = value;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(currentDate);
            if (year == 0) {
                year = cal.get(Calendar.YEAR);
            }
            monthDataCount[cal.get(Calendar.MONTH)] += value;
            dayDataCount[cal.get(Calendar.MONTH)][cal.get(Calendar.DAY_OF_MONTH)] += value;
        }
    }

    private void generateColumnData() {

        int numSubcolumns = 1;
        int numColumns = months.length;
        int count = 0;

        List<AxisValue> axisValues = new ArrayList<>();
        List<Column> columns = new ArrayList<>();
        List<SubcolumnValue> values;
        for (int x : monthDataCount) count += x;
        for (int i = 0; i < numColumns; ++i) {

            values = new ArrayList<>();
            for (int j = 0; j < numSubcolumns; ++j) {
                values.add(new SubcolumnValue(( ((float) monthDataCount[i] / count) * 100), ChartUtils.pickColor()));
            }

            axisValues.add(new AxisValue(i).setLabel(months[i]));

            columns.add(new Column(values).setHasLabelsOnlyForSelected(true));
        }

        columnData = new ColumnChartData(columns);

        columnData.setAxisXBottom(new Axis(axisValues).setHasLines(true).setName("Month"));
        columnData.setAxisYLeft(new Axis().setHasLines(true).setMaxLabelChars(2).setName("Faces Detected '%'"));

        chartBottom.setColumnChartData(columnData);

        // Set value touch listener that will trigger changes for chartTop.
        chartBottom.setOnValueTouchListener(new ValueTouchListener());

        // Set selection mode to keep selected month column highlighted.
        chartBottom.setValueSelectionEnabled(true);

        chartBottom.setZoomType(ZoomType.HORIZONTAL);
    }

    /**
     * Generates initial data for line chart. At the beginning all Y values are equals 0. That will change when user
     * will select value on column chart.
     */
    private void generateInitialLineData() {
        int numValues = 31;

        List<AxisValue> axisValues = new ArrayList<>();
        List<PointValue> values = new ArrayList<>();
        for (int i = 0; i < numValues; ++i) {
            values.add(new PointValue(i, 0));
            axisValues.add(new AxisValue(i).setLabel(String.valueOf(i + 1)));
        }

        Line line = new Line(values);
        line.setColor(ChartUtils.COLOR_GREEN).setCubic(true);

        List<Line> lines = new ArrayList<>();
        lines.add(line);

        lineData = new LineChartData(lines);
        lineData.setAxisXBottom(new Axis(axisValues).setHasLines(true).setName("Days of the Month"));
        lineData.setAxisYLeft(new Axis().setHasLines(true).setMaxLabelChars(3).setName("Faces Detected"));

        chartTop.setLineChartData(lineData);

        // For build-up animation you have to disable viewport recalculation.
        chartTop.setViewportCalculationEnabled(false);

        // And set initial max viewport and current viewport- remember to set viewports after data.
        max = ((max + 9) / 10 ) * 10;
        max = Math.round(max);
        Viewport v = new Viewport(0, max, 31, 0);
        chartTop.setMaximumViewport(v);
        chartTop.setCurrentViewport(v);

        chartTop.setZoomType(ZoomType.HORIZONTAL);
    }

    private void generateMonthData(int color, int month) {
        // Cancel last animation if not finished.
        chartTop.cancelDataAnimation();

        TextView monthText = (TextView) findViewById(R.id.monthValue);
        monthText.setText(months[month]);

        Line line = lineData.getLines().get(0);// For this example there is always only one line.
        line.setColor(color);
        int localMax = 0;
        for (PointValue value : line.getValues()) {
            // Change target only for Y value.
            int valueY = dayDataCount[month][Math.round(value.getX())];
            value.setTarget(value.getX(), valueY);
            if (valueY > localMax) {
                localMax = valueY;
            }
        }
        localMax = Math.round(((localMax + 9) / 10 ) * 10);
        Calendar cal = new GregorianCalendar(year, month, 1);
        Viewport v = new Viewport(0, localMax, cal.getActualMaximum(Calendar.DAY_OF_MONTH)-1, 0);
        chartTop.setMaximumViewport(v);
        chartTop.setCurrentViewport(v);
        // Start new data animation with 300ms duration;
        chartTop.startDataAnimation(300);


    }


    private class ValueTouchListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            generateMonthData(value.getColor(), columnIndex);
        }

        @Override
        public void onValueDeselected() {

            // Cancel last animation if not finished.
            chartTop.cancelDataAnimation();

            TextView monthText = (TextView) findViewById(R.id.monthValue);
            monthText.setText("");

            // Modify data targets
            Line line = lineData.getLines().get(0);// For this example there is always only one line.
            line.setColor(ChartUtils.COLOR_GREEN);
            for (PointValue value : line.getValues()) {
                // Change target only for Y value.
                value.setTarget(value.getX(), 0);
            }

            // Start new data animation with 300ms duration;
            chartTop.startDataAnimation(300);

        }
    }

    private Date StringToDate(String s) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd", Locale.UK);
        try {
            return dateFormat.parse(s);
        } catch (ParseException e) {
            Log.e(e.getMessage(), e.toString());
        }
        return null;
    }

}