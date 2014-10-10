// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.extension;

import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.QualifierContext;
import com.intendia.qualifier.Qualifiers;
import javax.annotation.Nullable;

public interface I18nExtension<T> extends Qualifier<T> {
    /** The name of the property (e.g. 'User logo'). Defaults to the property or field name. */
    String getSummary();

    /** The abbreviation or acronym of the property (e.g. 'Logo'). Defaults to the property summary. */
    @Nullable
    String getAbbreviation();

    /** The description of the property (e.g. 'The user profile logo.'). Defaults to the property summary. */
    @Nullable
    String getDescription();

    public static class DefaultI18nExtension<T> implements I18nExtension<T>,Qualifier<T> {
        public static <T> I18nExtension<T> i18nOf(Qualifier<T> qualifier) {
            return new DefaultI18nExtension<>(qualifier.getContext());
        }

        public DefaultI18nExtension(QualifierContext qualifierContext) {
            super(qualifierContext);
        }

        public String getSummary() {
            return getContext().getQualifier(Qualifiers.I18N_SUMMARY);
        }

        public String getAbbreviation() {
            return getContext().getQualifier(Qualifiers.I18N_ABBREVIATION);
        }

        public String getDescription() {
            return getContext().getQualifier(Qualifiers.I18N_DESCRIPTION);
        }

        @Override
        public I18nExtension<T> newInstance(QualifierContext qualifierContext) {
            return new DefaultI18nExtension<>(qualifierContext);
        }
    }
}
