// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.auto.common.AnnotationMirrors.getAnnotatedAnnotations;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.google.auto.common.MoreTypes.isTypeOf;
import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.intendia.qualifier.processor.StaticQualifierMetamodelProcessor.getFlatName;
import static com.intendia.qualifier.processor.StaticQualifierMetamodelProcessor.getQualifierName;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.Qualify.Link;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
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
                metaqualifier.value(autoMethods).ifPresent(am -> {
                    String pLCName = e.getSimpleName().toString();
                    String pUCName = LOWER_CAMEL.converterTo(UPPER_CAMEL).convert(pLCName);
                    TypeMirror pRetType = e.getReturnType();
                    if (pRetType.getKind().isPrimitive()) {
                        pRetType = types().boxedClass((PrimitiveType) pRetType).asType();
                    }
                    boolean isLink = e.getAnnotation(Link.class) != null && isTypeOf(Class.class, pRetType);

                    String key = aLCName + "." + pLCName;
                    Object value = values.get(e).getValue();

                    String methodName = "get" + aUCName + pUCName;
                    Metaextension<Object> extension;
                    CodeBlock valueBlock;
                    TypeName returnType;

                    if (!isLink) {
                        extension = metaqualifier.use(key, value);
                        valueBlock = extension.valueBlock().get();
                        extension.valueBlock("$L()", methodName);
                        returnType = TypeName.get(pRetType);
                    } else {
                        String valType = getFlatName((TypeElement) types().asElement((DeclaredType) value));
                        valueBlock = CodeBlock.builder().add("$T.self", ClassName.bestGuess(getQualifierName(valType))).build();
                        extension = metaqualifier.literal(key, "$L()", methodName);
                        returnType = ParameterizedTypeName.get(ClassName.get(Qualifier.class),
                                TypeName.get(asDeclared(pRetType).getTypeArguments().get(0)));
                    }

                    am.add(MethodSpec.methodBuilder(methodName)
                            .addModifiers(PUBLIC)
                            .returns(returnType)
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
