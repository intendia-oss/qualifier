package com.intendia.qualifier.example;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.processor.QualifierAnnotationAnalyzer;
import com.intendia.qualifier.processor.QualifierProcessorServiceProvider;
import java.time.Instant;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class ExampleQualifierProcessorProvider extends QualifierProcessorServiceProvider {
    Extension<String> LOADED = Extension.key("simple.loaded");
    Extension<Object> LITERAL = Extension.key("simple.literal");
    Extension<String> ANONYMOUS = Extension.anonymous();

    public ExampleQualifierProcessorProvider() { registerAnnotation(ExampleManual.class, this::processSimple); }

    private void processSimple(QualifierAnnotationAnalyzer.AnnotationContext<ExampleManual> simple) {
        final ExampleManual value = simple.annotation();
        final TypeMirror classType = typeElementFor(Class.class).asType();
        simple.metadata()
                .use(ExampleManualQualifier.STRING, value.string()).done()
                .use(ExampleManualQualifier.INTEGER, value.integer()).done()
                .literal(ExampleManualQualifier.TYPE, "$T.class", parametersType(value)).type(classType).done()
                .use(LOADED, Instant.now().toString()).done()
                .literal(LITERAL, "$S", "literal").done()
                .use(ANONYMOUS, "this value is not accessible at run time").done();
    }

    private static TypeMirror parametersType(ExampleManual annotation) {
        // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        try {
            annotation.type();
            return null; // this must not happens
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }
}
