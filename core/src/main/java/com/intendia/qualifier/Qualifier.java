// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import com.google.common.base.Function;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import java.util.Comparator;
import java.util.Map;
import javax.annotation.Nullable;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
public interface Qualifier<T, V> extends Function<T, V> {
    String getName();

    String getPath();

    Class<V> getType();

    /** Return the properties context of this qualifier. */
    Map<String, Object> getContext();

    // TODO Bean interface
    @Nullable V get(T object);
    Boolean isReadable();

    void set(T object, V value);
    Boolean isWritable();

    Comparator<? super T> getComparator();

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    <ValueU> Qualifier<T, ValueU> as(Qualifier<? super V, ValueU> property);

    // TODO I18n interface
    /** The name of the property (e.g. 'User logo'). Defaults to the property or field name. */
    @Deprecated String summary();
    /** The abbreviation or acronym of the property (e.g. 'Logo'). Defaults to the property summary. */
    @Deprecated @Nullable String abbreviation();
    /** The description of the property (e.g. 'The user profile logo.'). Defaults to the property summary. */
    @Deprecated @Nullable String description();


    // TODO Renderers interface
    /** Default property renderer. Default implementation return a <code>toString</code> renderer. */
    @Deprecated Renderer<V> getRenderer();
    /** Default property cell. Default implementation return a simple <code>toString</code> cell implementation. */
    @Deprecated SafeHtmlRenderer<V> getSafeHtmlRenderer();
    /** Default property cell. Default implementation return a simple <code>toString</code> cell implementation. */
    @Deprecated Cell<V> getCell();


    // TODO Measure interface
    /** The property unit of measure. Defaults to {@code ONE}. */
    @Deprecated Unit<? extends Quantity> unit();
    /** The property quantity type. Defaults to {@code javax.measure.quantity.Dimensionless}. */
    @Deprecated Class<? extends Quantity> quantity();
}
