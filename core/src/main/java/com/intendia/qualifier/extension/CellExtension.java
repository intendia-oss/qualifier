// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.extension;

import com.google.gwt.cell.client.Cell;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.QualifierContext;
import com.intendia.qualifier.Qualifiers;

public interface CellExtension<T> extends Qualifier<T> {
    /** Default property cell. Default implementation return a simple <code>toString</code> cell implementation. */
    Cell<T> getCell();

    public static class DefaultCellExtension<T> implements CellExtension<T>,Qualifier<T> {

        public DefaultCellExtension(QualifierContext qualifier) {
            super(qualifier);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Cell<T> getCell() {
            final String key = getContext().getQualifier(Qualifiers.REPRESENTER_CELL);
            return getContext().getResourceProvider(Cell.class, key).get(this);
        }

        @Override
        public Qualifier<T> newInstance(QualifierContext qualifierContext) {
            return new DefaultCellExtension<>(qualifierContext);
        }
    }
}
