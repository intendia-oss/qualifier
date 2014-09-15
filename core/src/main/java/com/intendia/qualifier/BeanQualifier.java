package com.intendia.qualifier;

import java.util.Set;

/**
 * Based on {@code javax.persistence.metamodel.ManagedType} and {@code java.beans.BeanInfo}.
 * 
 * @see {@link com.intendia.qualifier.annotation.Qualify}
 */
public interface BeanQualifier<T> extends Qualifier<T, T> {

    /** Return the property qualifiers of the bean qualifier. */
    Set<Qualifier<? super T, ?>> getPropertyQualifiers();

}
