// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.Qualifiers.MEASURE_QUANTITY;
import static com.intendia.qualifier.Qualifiers.MEASURE_UNIT_OF_MEASURE;
import static java.lang.String.format;
import static javax.lang.model.element.Modifier.*;

import com.intendia.qualifier.annotation.Measure;
import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.util.EnumSet;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.measure.unit.Unit;
import javax.tools.Diagnostic;

public class MeasureQualifierProcessor extends AbstractQualifierProcessorExtension
        implements QualifierAnnotationAnalyzer<Measure> {

    public MeasureQualifierProcessor() {
        addAnnotationAnalyzer(Measure.class, this);
    }

    @Override
    public boolean processable() {
        return classExists("javax.measure.unit.Unit");
    }

    @Override
    public void processAnnotation(AnnotationContext<Measure> ctx) {
        final String unitOfMeasure = ctx.getAnnotation().unitOfMeasure();
        ctx.getContext().putIfNotNull(MEASURE_UNIT_OF_MEASURE, unitOfMeasure);
        ctx.getContext().putIfNotNull(MEASURE_QUANTITY, quantityType(ctx.getAnnotation()));

        try {
            Unit.valueOf(unitOfMeasure);
        } catch (Exception invalid) {
            // Find annotation mirror, type and value and print error message
            for (ExecutableElement executableElement : ctx.getAnnotationMirror().getElementValues().keySet()) {
                if ("unitOfMeasure".equals(executableElement.getSimpleName().toString())) {
                    final AnnotationValue value = ctx.getAnnotationMirror().getElementValues().get(executableElement);
                    getProcessingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR,
                            invalid.getMessage(), ctx.getAnnotatedElement(), ctx.getAnnotationMirror(), value);
                }
            }
        }
    }

    @Override
    public void processPropertyQualifier(final JavaWriter writer, String beanName, String propertyName,
            final QualifierDescriptor property) throws IOException {
        if (!property.getContext().contains(MEASURE_UNIT_OF_MEASURE)) return;
        String unit = property.getContext().getOrThrow(String.class, MEASURE_UNIT_OF_MEASURE);
        String quantity = property.getContext().getOrThrow(String.class, MEASURE_QUANTITY);

        writer.emitField(format("Unit<%s>", quantity), "UNIT", EnumSet.of(PRIVATE, STATIC, FINAL),
                format("Unit.valueOf(\"%s\").asType(%s.class)", unit, quantity));

        overrideMethod(writer, format("Unit<%s>", quantity), "unit", "UNIT");
        overrideMethod(writer, format("Class<%s>", quantity), "quantity", format("%s.class", quantity));
    }

    private String quantityType(Measure annotation) {
        // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        try {
            annotation.quantity();
            return null; // this must not happens
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror().toString();
        }
    }

}
