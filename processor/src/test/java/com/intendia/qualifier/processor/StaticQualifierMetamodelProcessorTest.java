// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.intendia.qualifier.processor.ProcessorTestUtils.qualifierProcessors;
import static org.truth0.Truth.ASSERT;

import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

public class StaticQualifierMetamodelProcessorTest {
    @Test public void basicQualifier() {
        JavaFileObject sourceFile = JavaFileObjects.forSourceString("Model", "\n"
                + "import com.intendia.qualifier.annotation.Qualify;\n"
                + "import com.intendia.qualifier.annotation.QualifyExtension;\n"
                + "\n"
                + "@Qualify interface Model {\n"
                + "    Integer getInteger();\n"
                + "\n"
                + "    void setInteger(Integer integer);\n"
                + "\n"
                + "    @Qualify(extend = @QualifyExtension(key = \"testInteger\", type = Integer.class, value = \"1\"))\n"
                + "    Integer getConstantInteger();\n"
                + "}");
        ASSERT.about(javaSource()).that(sourceFile).processedWith(qualifierProcessors()).compilesWithoutError();
    }
}
