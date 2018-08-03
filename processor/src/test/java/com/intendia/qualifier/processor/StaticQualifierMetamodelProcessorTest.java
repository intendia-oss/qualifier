package com.intendia.qualifier.processor;

import static com.google.testing.compile.JavaSourcesSubject.assertThat;
import static com.intendia.qualifier.processor.ProcessorTestUtils.qualifierProcessors;

import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

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
        assertThat(model, child).processedWith(qualifierProcessors()).compilesWithoutError();
    }
}
