package com.intendia.qualifier.example;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import com.intendia.qualifier.annotation.Qualify;
import java.lang.annotation.Retention;
import java.util.concurrent.TimeUnit;

@Qualify.Auto @Retention(SOURCE)
public @interface ExampleAuto {
    String string() default "def";

    int integer() default -1;

    Class<?> type() default Void.class;

    @Qualify.Link
    Class<?> link();

    TimeUnit enumeration() default TimeUnit.MILLISECONDS;

    TimeUnit[] enumerationList() default {};

    TimeUnit[] enumerationListWithDefaults() default { TimeUnit.DAYS, TimeUnit.HOURS };
}
