package com.intendia.qualifier.processor;

import com.intendia.qualifier.Extension;
import java.time.Instant;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class SimpleProcessorExtension extends AbstractQualifierProcessorExtension {
    Extension<String> LOADED = Extension.key("simple.loaded");
    Extension<Object> LITERAL = Extension.key("simple.literal");
    Extension<String> ANONYMOUS = Extension.anonymous();

    public SimpleProcessorExtension() { registerAnnotation(Simple.class, this::processSimple); }

    private void processSimple(AnnotationContext<Simple> simple) {
        final Simple value = simple.annotation();
        final TypeMirror classType = typeElementFor(Class.class).asType();
        simple.metadata()
                .use(Simple.STRING, value.getString()).done()
                .use(Simple.INTEGER, value.getInteger()).done()
                .literal(Simple.TYPE, "$T.class", parametersType(value)).type(classType).done()
                .use(LOADED, Instant.now().toString()).done()
                .literal(LITERAL, "$S", "literal").done()
                .use(ANONYMOUS, "this value is not accessible at run time").done();
    }

    private static TypeMirror parametersType(Simple annotation) {
        // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        try {
            annotation.getType();
            return null; // this must not happens
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }
}
