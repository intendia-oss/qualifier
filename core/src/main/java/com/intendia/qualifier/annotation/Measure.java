// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface Measure {

    /** (Required) The unit of measure. See {@code Unit.valueOf}. */
    String unitOfMeasure();

    /** (Optional) The quantity of the measure. See {@code Quantity}. */
    Class<? extends Quantity> quantity() default Dimensionless.class;

}
