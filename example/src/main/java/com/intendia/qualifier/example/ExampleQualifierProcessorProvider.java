package com.intendia.qualifier.example;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.processor.Metaqualifier;
import com.intendia.qualifier.processor.QualifierProcessorServiceProvider;
import java.time.Instant;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class ExampleQualifierProcessorProvider extends QualifierProcessorServiceProvider {
    private static final Extension<String> LOADED = Extension.key("simple.loaded");
    private static final Extension<Object> LITERAL = Extension.key("simple.literal");
    private static final Extension<String> ANONYMOUS = Extension.anonymous();

    @Override public void processAnnotated(Element element, Metaqualifier meta) {
        annotationApply(element, ExampleManual.class, a -> meta
                .use(ExampleManualQualifier.STRING, a.string()).done()
                .use(ExampleManualQualifier.INTEGER, a.integer()).done()
                .literal(ExampleManualQualifier.TYPE, "$T.class", readType(a)).type(classAsType()).done()
                .use(LOADED, Instant.now().toString()).done()
                .literal(LITERAL, "$S", "literal").done()
                .use(ANONYMOUS, "this value is not accessible at run time").done());
    }

    private TypeMirror classAsType() { return typeElementFor(Class.class).asType(); }

    private static TypeMirror readType(ExampleManual annotation) {
        // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        try {
            annotation.type();
            return null; // this must not happens
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }
}
