// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.intendia.qualifier.processor.ProcessorTestUtils.qualifierProcessors;
import static org.truth0.Truth.ASSERT;

import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubject;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.truth0.AbstractVerb;

public class StaticQualifierMetamodelProcessorTest {
    @Test public void basicQualifier() {
        JavaFileObject model = JavaFileObjects.forSourceString("Model", "\n"
                + "import com.intendia.qualifier.annotation.Qualify;\n"
                + "\n"
                + "@Qualify interface Model {\n"
                + "    Child getChild();\n"
                + "\n"
                + "    void setChild(Child integer);\n"
                + "\n"
                + "    @Qualify(extend = @Qualify.Entry(key = \"testInteger\", type = Integer.class, value = \"1\"))\n"
                + "    Integer getConstantInteger();\n"
                + "}");
        JavaFileObject child = JavaFileObjects.forSourceString("Model", "\n"
                + "import com.intendia.qualifier.annotation.Qualify;\n"
                + "\n"
                + "@Qualify interface Child {\n"
                + "    Integer getInteger();\n"
                + "\n"
                + "    void setInteger(Integer integer);\n"
                + "}");
        assertJavaSources()
                .that(Arrays.asList(model, child))
                .processedWith(qualifierProcessors())
                .compilesWithoutError();
    }

    public AbstractVerb.DelegatedVerb<JavaSourcesSubject, Iterable<? extends JavaFileObject>> assertJavaSources() {
        return ASSERT.about(javaSources());
    }
}
