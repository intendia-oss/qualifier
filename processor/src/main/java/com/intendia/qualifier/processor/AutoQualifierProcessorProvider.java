// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.auto.common.AnnotationMirrors.getAnnotatedAnnotations;
import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.annotation.Qualify;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

//TODO there are duplicated logic between static processor and here, unify!
public class AutoQualifierProcessorProvider extends QualifierProcessorServiceProvider {
    Extension<Collection<MethodSpec>> autoMethods = Extension.anonymous();
    Extension<Set<ClassName>> autoAnnotations = Extension.anonymous();

    @Override public void processAnnotated(Element element, Metaqualifier metaqualifier) {
        if (!metaqualifier.value(autoMethods).isPresent()) {
            metaqualifier.put(autoMethods).value(new ArrayList<>());
        }
        if (!metaqualifier.value(autoAnnotations).isPresent()) {
            metaqualifier.put(autoAnnotations).value(new HashSet<>());
        }
        for (AnnotationMirror aMirror : getAnnotatedAnnotations(element, Qualify.Auto.class)) {
            String packageName = elements().getPackageOf(aMirror.getAnnotationType().asElement()).toString();
            String aUCName = aMirror.getAnnotationType().asElement().getSimpleName().toString();
            String aLCName = UPPER_CAMEL.converterTo(LOWER_CAMEL).convert(aUCName);
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = aMirror.getElementValues();
            for (ExecutableElement e : values.keySet()) {
                String pLCName = e.getSimpleName().toString();
                String pUCName = LOWER_CAMEL.converterTo(UPPER_CAMEL).convert(pLCName);
                String key = aLCName + "." + pLCName;
                Metaextension<Object> extension = metaqualifier.use(key, values.get(e).getValue());
                CodeBlock valueBlock = extension.valueBlock().get();
                String methodName = "get" + aUCName + pUCName;
                extension.valueBlock(methodName + "()");

                metaqualifier.value(autoMethods).ifPresent(am -> {
                    TypeMirror returnType = e.getReturnType();
                    if (returnType instanceof PrimitiveType) {
                        returnType = types().boxedClass((PrimitiveType) returnType).asType();
                    }
                    am.add(MethodSpec.methodBuilder(methodName)
                            .addModifiers(PUBLIC)
                            .returns(TypeName.get(returnType))
                            .addCode("return ")
                            .addCode(valueBlock)
                            .addCode(";\n")
                            .build());
                });
                metaqualifier.value(autoAnnotations).ifPresent(aq ->
                        aq.add(ClassName.bestGuess(packageName + "." + aUCName + "Qualifier")));
            }
        }
    }

    @Override public void processProperty(TypeSpec.Builder writer, Metamodel descriptor) {
        descriptor.metadata().value(autoMethods).ifPresent(am -> am.forEach(writer::addMethod));
        descriptor.metadata().value(autoAnnotations).ifPresent(aa -> aa.forEach(writer::addSuperinterface));
    }
}
