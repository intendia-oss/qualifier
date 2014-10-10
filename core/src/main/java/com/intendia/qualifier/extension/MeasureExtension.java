// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.extension;

import static com.intendia.qualifier.Qualifiers.MEASURE_QUANTITY;
import static com.intendia.qualifier.Qualifiers.MEASURE_UNIT_OF_MEASURE;

import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.QualifierContext;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

public interface MeasureExtension<T> extends Qualifier<T> {
    public Unit<? extends Quantity> unit() ;
    public Class<? extends Quantity> quantity() ;

    public static class DefaultMeasureExtension<T> implements MeasureExtension<T>,Qualifier<T> {
        public static <T> MeasureExtension<T> measureOf(Qualifier<T> qualifier) {
            return new DefaultMeasureExtension<>(qualifier.getContext());
        }

        protected DefaultMeasureExtension(QualifierContext qualifierContext) {
            super(qualifierContext);
        }

        @Override
        public Unit<? extends Quantity> unit() { //noinspection unchecked
            return getContext().getQualifier(MEASURE_UNIT_OF_MEASURE);
        }

        @Override
        public Class<? extends Quantity> quantity() { //noinspection unchecked
            return getContext().getQualifier(MEASURE_QUANTITY);
        }

        @Override
        public Qualifier<T> newInstance(QualifierContext qualifierContext) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
