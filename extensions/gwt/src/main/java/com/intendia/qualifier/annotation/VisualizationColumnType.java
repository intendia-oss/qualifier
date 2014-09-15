// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.annotation;

import com.google.gwt.visualization.client.AbstractDataTable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface VisualizationColumnType {
    /** (Required) The type of a column in a {@link AbstractDataTable}. */
    AbstractDataTable.ColumnType value();
}
