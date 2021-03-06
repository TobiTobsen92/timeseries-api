/**
 * Copyright (C) 2013-2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.io.img;

import static org.n52.io.img.BarRenderer.createBarRenderer;
import static org.n52.io.img.LineRenderer.createStyledLineRenderer;
import static org.n52.io.style.BarStyle.createBarStyle;
import static org.n52.io.style.LineStyle.createLineStyle;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Month;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;
import org.n52.io.format.TvpDataCollection;
import org.n52.io.style.BarStyle;
import org.n52.io.style.LineStyle;
import org.n52.io.v1.data.FeatureOutput;
import org.n52.io.v1.data.ReferenceValueOutput;
import org.n52.io.v1.data.StyleProperties;
import org.n52.io.v1.data.TimeseriesData;
import org.n52.io.v1.data.TimeseriesDataMetadata;
import org.n52.io.v1.data.TimeseriesMetadataOutput;
import org.n52.io.v1.data.TimeseriesValue;

public class MultipleChartsRenderer extends ChartRenderer {

    public MultipleChartsRenderer(RenderingContext context, String locale) {
        super(context, locale);
    }

    @Override
    public void generateOutput(TvpDataCollection data) {
        Map<String, TimeseriesData> allTimeseries = data.getAllTimeseries();
        TimeseriesMetadataOutput[] timeseriesMetadatas = getTimeseriesMetadataOutputs();

        int rendererCount = timeseriesMetadatas.length;
        for (int rendererIndex = 0; rendererIndex < timeseriesMetadatas.length; rendererIndex++) {

            /*
             * For each index put data and its renderer configured to a particular style.
             * 
             * As each timeseries may define its custom styling and different chart types we have to loop over
             * all timeseries to configure chart rendering.
             */
            TimeseriesMetadataOutput timeseriesMetadata = timeseriesMetadatas[rendererIndex];

            String timeseriesId = timeseriesMetadata.getId();
            StyleProperties style = getTimeseriesStyleFor(timeseriesId);
            TimeseriesData timeseriesData = allTimeseries.get(timeseriesId);

            String chartId = createChartId(timeseriesMetadata);
            ChartIndexConfiguration configuration = new ChartIndexConfiguration(chartId, rendererIndex);
            configuration.setData(timeseriesData, timeseriesMetadata, style);
            configuration.setRenderer(createRenderer(style));

            if (timeseriesData.hasReferenceValues()) {
                int referenceIndex = rendererCount;

                /*
                 * Configure timeseries reference value renderers with the same metadata and add it at the end
                 * of the plot's renderer list.
                 */

                TimeseriesDataMetadata metadata = timeseriesData.getMetadata();
                Map<String, TimeseriesData> referenceValues = metadata.getReferenceValues();
                for (Entry<String, TimeseriesData> referencedTimeseries : referenceValues.entrySet()) {
                    String referenceTimeseriesId = referencedTimeseries.getKey();
                    ReferenceValueOutput referenceOutput = getReferenceValue(referenceTimeseriesId, timeseriesMetadata);
                    String referenceChartId = createChartId(timeseriesMetadata, referenceOutput.getLabel());

                    TimeseriesData referenceData = referenceValues.get(referenceTimeseriesId);
                    ChartIndexConfiguration referenceConfiguration = new ChartIndexConfiguration(referenceChartId,
                                                                                                 referenceIndex);
                    StyleProperties referenceStyle = getTimeseriesStyleFor(timeseriesId, referenceTimeseriesId);
                    referenceConfiguration.setReferenceData(referenceData, timeseriesMetadata, referenceStyle);
                    referenceConfiguration.setRenderer(createRenderer(referenceStyle));
                    referenceIndex++;
                }
            }
        }
    }

    private String createChartId(TimeseriesMetadataOutput metadata) {
        return createChartId(metadata, null);
    }

    private String createChartId(TimeseriesMetadataOutput metadata, String referenceId) {
        FeatureOutput feature = metadata.getParameters().getFeature();
        StringBuilder timeseriesLabel = new StringBuilder();
        timeseriesLabel.append(feature.getLabel());
        if (referenceId != null) {
            timeseriesLabel.append(", ").append(referenceId);
        }
        timeseriesLabel.append(" (").append(createRangeLabel(metadata)).append(")");
        return timeseriesLabel.toString();
    }

    private Renderer createRenderer(StyleProperties properties) {
        if (isBarStyle(properties)) {
            // configure bar chart renderer
            BarStyle barStyle = createBarStyle(properties);
            return createBarRenderer(barStyle);
        }
        // configure line chart renderer
        LineStyle lineStyle = createLineStyle(properties);
        return createStyledLineRenderer(lineStyle);
    }

    private ReferenceValueOutput getReferenceValue(String id, TimeseriesMetadataOutput timeseriesMetadata) {
        for (ReferenceValueOutput referenceOutput : timeseriesMetadata.getReferenceValues()) {
            if (referenceOutput.getReferenceValueId().equals(id)) {
                return referenceOutput;
            }
        }
        return null;
    }

    private class ChartIndexConfiguration {

        private int timeseriesIndex;

        private String chartId;

        public ChartIndexConfiguration(String chartId, int index) {
            if (chartId == null) {
                throw new NullPointerException("ChartId must not be null.");
            }
            this.timeseriesIndex = index;
            this.chartId = chartId;
        }

        public void setRenderer(Renderer renderer) {
            getXYPlot().setRenderer(timeseriesIndex, renderer.getXYRenderer());
            renderer.setColorForSeriesAt(timeseriesIndex);
        }

        public void setData(TimeseriesData data, TimeseriesMetadataOutput timeMetadata, StyleProperties style) {
            getXYPlot().setDataset(timeseriesIndex, createTimeseriesCollection(data, style));
            ValueAxis rangeAxis = createRangeAxis(timeMetadata);
            getXYPlot().setRangeAxis(timeseriesIndex, rangeAxis);
            getXYPlot().mapDatasetToRangeAxis(timeseriesIndex, timeseriesIndex);
        }

        public void setReferenceData(TimeseriesData data, TimeseriesMetadataOutput timeMetadata, StyleProperties style) {
            getXYPlot().setDataset(timeseriesIndex, createTimeseriesCollection(data, style));
        }

        private TimeSeriesCollection createTimeseriesCollection(TimeseriesData data, StyleProperties style) {
            TimeSeriesCollection timeseriesCollection = new TimeSeriesCollection();
            timeseriesCollection.addSeries(createDiscreteTimeseries(data, style));
            timeseriesCollection.setGroup(new DatasetGroup(chartId));
            return timeseriesCollection;
        }

        private TimeSeries createDiscreteTimeseries(TimeseriesData timeseriesData, StyleProperties style) {
            TimeSeries timeseries = new TimeSeries(chartId);
            if (hasValues(timeseriesData)) {
                if (isBarStyle(style)) {
                    TimeseriesValue timeseriesValue = timeseriesData.getValues()[0];
                    Date timeOfFirstValue = new Date(timeseriesValue.getTimestamp());
                    RegularTimePeriod timeinterval = determineTimeInterval(timeOfFirstValue, style);

                    double intervalSum = 0.0;
                    for (TimeseriesValue value : timeseriesData.getValues()) {
                        if (isValueInInterval(value, timeinterval)) {
                            intervalSum += value.getValue();
                        }
                        else {
                            timeseries.add(timeinterval, intervalSum);
                            timeinterval = determineTimeInterval(new Date(value.getTimestamp()), style);
                            intervalSum = value.getValue();
                        }
                    }
                }
                else if (isLineStyle(style)) {
                    for (TimeseriesValue value : timeseriesData.getValues()) {
                        Second second = new Second(new Date(value.getTimestamp()));
                        timeseries.add(second, value.getValue());
                    }
                }
            }
            return timeseries;
        }

        private boolean hasValues(TimeseriesData timeseriesData) {
            return timeseriesData.getValues().length > 0;
        }

        private RegularTimePeriod determineTimeInterval(Date date, StyleProperties styleProperties) {
            if (styleProperties.getProperties().containsKey("interval")) {
                String interval = styleProperties.getProperties().get("interval");
                if (interval.equals("byHour")) {
                    return new Hour(date);
                }
                else if (interval.equals("byDay")) {
                    return new Day(date);
                }
                else if (interval.equals("byMonth")) {
                    return new Month(date);
                }
            }
            return new Week(date);
        }

        /**
         * @param interval
         *        the interval to check.
         * @return <code>true</code> if timestamp is within the given interval, otherwise <code>false</code>
         *         is returned. If passed interval was <code>null</code> false will be returned.
         * @throws IllegalArgumentException
         *         if passed in value is <code>null</code>.
         */
        private boolean isValueInInterval(TimeseriesValue value, RegularTimePeriod interval) {
            if (value == null) {
                throw new IllegalArgumentException("TimeseriesValue must not be null.");
            }
            return interval == null
                    || interval.getStart().getTime() <= value.getTimestamp()
                    && value.getTimestamp() < interval.getEnd().getTime();
        }

    }

}
