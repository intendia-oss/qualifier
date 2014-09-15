// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import com.google.gwt.visualization.client.AbstractDataTable;
import com.intendia.qualifier.annotation.VisualizationColumnType;

public class GwtVisualizationQualifierProcessor extends AbstractQualifierProcessorExtension {

    public GwtVisualizationQualifierProcessor() {
        addAnnotationAnalyzer(VisualizationColumnType.class, new QualifierAnnotationAnalyzer<VisualizationColumnType>() {
            @Override
            public void processAnnotation(AnnotationContext<VisualizationColumnType> ctx) {
                ctx.getContext().putIfNotNull(AbstractDataTable.ColumnType.class, ctx.getAnnotation().value());
            }
        });
    }

    @Override
    public boolean processable() {
        return classExists("com.google.gwt.visualization.client.visualizations.Visualization");
    }

}
