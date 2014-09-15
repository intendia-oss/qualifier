package com.intendia.qualifier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;

/**
 * Is used to specify the mapped qualifying for a resource property or field. If no <code>Qualify</code> annotation is
 * specified, the default values apply.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface Qualify {

    /** (Optional) Override the property name (e.g. 'userLogo'). Defaults to the property name. */
    String name() default "";

    /** (Optional) The type of the property (e.g. Number.class). Defaults to property type. */
    Class<?> type() default Default.class;

    /** (Optional) An array of <code>QualifyExtension</code> annotations. */
    QualifyExtension[] extend() default {};

    /** @deprecated use {@code I18n#summary()} instead. */
    @Deprecated String summary() default "";

    /** @deprecated use {@code I18n#abbreviation()} instead. */
    @Deprecated String abbreviation() default "";

    /** @deprecated use {@code I18n#description()} instead. */
    @Deprecated String description() default "";

    /** @deprecated use {@code Measure#unitOfMeasure()} instead. */
    @Deprecated String unitOfMeasure() default "";

    /** @deprecated use {@code Measure#quantity()} instead. */
    @Deprecated Class<? extends Quantity> quantity() default Dimensionless.class;

    /** @deprecated use {@code Representer#textRenderer()} instead. */
    @Deprecated String renderer() default "";

    /** @deprecated use {@code Representer#textRenderer()} instead. */
    @Deprecated String safeHtmlRenderer() default "";

    /** @deprecated use {@code Representer#cell()} instead. */
    @Deprecated String cell() default "";

    static class Default {}

}
